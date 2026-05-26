package com.example.guiterapp_1;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.guiterapp_1.data.AppDatabase;
import com.example.guiterapp_1.data.Instrument;
import com.example.guiterapp_1.data.Lesson;
import com.example.guiterapp_1.data.LessonWithExercise;
import com.example.guiterapp_1.databinding.DialogEditLessonBinding;
import com.example.guiterapp_1.databinding.FragmentProgressBinding;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class ProgressFragment extends Fragment implements LearningService.ServiceCallbacks {

    private FragmentProgressBinding binding;
    private String selectedDate;
    private Lesson selectedLesson;
    private int selectedLessonId = -1;
    private LessonAdapter adapter;

    private LearningService learningService;
    private boolean isBound = false;
    private boolean isMinimized = false;
    private List<Instrument> instruments;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean recordAudioGranted = result.getOrDefault(Manifest.permission.RECORD_AUDIO, false);
                if (recordAudioGranted) {
                    startRecording();
                } else {
                    Toast.makeText(getContext(), "Permission denied to record audio", Toast.LENGTH_SHORT).show();
                }
            });

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LearningService.LearningBinder binder = (LearningService.LearningBinder) service;
            learningService = binder.getService();
            learningService.setCallbacks(ProgressFragment.this);
            isBound = true;
            if (learningService.isRecording()) {
                showRecordingUI(true);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    private final ActivityResultLauncher<Intent> videoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri videoUri = result.getData().getData();
                    try {
                        requireContext().getContentResolver().takePersistableUriPermission(videoUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                    saveMediaToLesson(videoUri.toString(), true);
                }
            }
    );

    private final ActivityResultLauncher<Intent> videoCaptureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri videoUri = result.getData().getData();
                    saveMediaToLesson(videoUri.toString(), true);
                }
            }
    );


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isMinimized = getArguments().getBoolean("is_minimized", false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProgressBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Intent intent = new Intent(requireContext(), LearningService.class);
        requireContext().bindService(intent, connection, Context.BIND_AUTO_CREATE);

        adapter = new LessonAdapter(new LessonAdapter.OnLessonClickListener() {
            @Override
            public void onLessonClicked(Lesson lesson) {
                selectedLesson = lesson;
                selectedLessonId = lesson.id;
                adapter.setSelectedLessonId(lesson.id);
                Toast.makeText(getContext(), "Selected lesson from " + lesson.startTime, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onEditLesson(Lesson lesson) {
                showEditLessonDialog(lesson);
            }

            @Override
            public void onDeleteLesson(Lesson lesson) {
                showDeleteConfirmation(lesson);
            }
        });
        binding.lessonsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.lessonsRecyclerView.setAdapter(adapter);

        selectedDate = sdf.format(new Date());

        if (isMinimized) {
            setupMinimizedView();
        } else {
            setupFullView();
        }

        binding.calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            selectedDate = sdf.format(calendar.getTime());
            updateDetails(selectedDate);
        });

        binding.closeCalendarButton.setOnClickListener(v -> setupMinimizedView());

        binding.addLessonButton.setOnClickListener(v -> addNewLesson());

        binding.recordVideoButton.setOnClickListener(v -> {
            Intent captureIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            videoCaptureLauncher.launch(captureIntent);
        });

        binding.recordAudioButton.setOnClickListener(v -> {
            checkPermissionAndStartRecording();
        });

        binding.uploadVideoButton.setOnClickListener(v -> {
            Intent pickIntent = new Intent(Intent.ACTION_PICK);
            pickIntent.setType("video/*");
            Intent chooserIntent = Intent.createChooser(pickIntent, "Choose a source");
            videoPickerLauncher.launch(chooserIntent);
        });

        setupInlineRecordingControls();
        loadInstruments();
        updateDetails(selectedDate);
    }

    private void loadInstruments() {
        Context context = getContext();
        if (context == null) return;
        Context appContext = context.getApplicationContext();
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(appContext);
            instruments = db.instrumentDao().getAllInstruments();
        });
    }

    private void showEditLessonDialog(Lesson lesson) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        DialogEditLessonBinding dialogBinding = DialogEditLessonBinding.inflate(getLayoutInflater());
        builder.setView(dialogBinding.getRoot());

        dialogBinding.etLessonLength.setText(String.valueOf(lesson.lessonLength / 60));
        dialogBinding.etLessonNotes.setText(lesson.notes);
        dialogBinding.lessonRatingBar.setRating(lesson.rating);

        final Integer[] tempInstrumentId = {lesson.instrumentId};
        if (instruments != null) {
            List<String> names = new ArrayList<>();
            int selectedIndex = -1;
            for (int i = 0; i < instruments.size(); i++) {
                names.add(instruments.get(i).name);
                if (lesson.instrumentId != null && lesson.instrumentId.equals(instruments.get(i).instrumentId)) {
                    selectedIndex = i;
                }
            }
            ArrayAdapter<String> insAdapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_dropdown_item_1line, names);
            dialogBinding.instrumentAutoComplete.setAdapter(insAdapter);
            if (selectedIndex != -1) {
                dialogBinding.instrumentAutoComplete.setText(names.get(selectedIndex), false);
            }

            dialogBinding.instrumentAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
                String selectedName = (String) parent.getItemAtPosition(position);
                for (Instrument i : instruments) {
                    if (i.name.equals(selectedName)) {
                        tempInstrumentId[0] = i.instrumentId;
                        break;
                    }
                }
            });
        }

        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                String lengthStr = dialogBinding.etLessonLength.getText().toString();
                long newLengthMinutes = lengthStr.isEmpty() ? 0 : Long.parseLong(lengthStr);
                lesson.lessonLength = newLengthMinutes * 60;
                lesson.notes = dialogBinding.etLessonNotes.getText().toString();
                lesson.rating = (int) dialogBinding.lessonRatingBar.getRating();
                lesson.instrumentId = tempInstrumentId[0];

                Context context = getContext();
                if (context == null) return;
                Context appContext = context.getApplicationContext();

                Executors.newSingleThreadExecutor().execute(() -> {
                    AppDatabase.getInstance(appContext).lessonDao().update(lesson);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            updateDetails(selectedDate);
                            Toast.makeText(getContext(), "Lesson updated", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid duration", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showDeleteConfirmation(Lesson lesson) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Lesson")
                .setMessage("Are you sure you want to delete this lesson and all associated media files?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    Context context = getContext();
                    if (context == null) return;
                    Context appContext = context.getApplicationContext();
                    Executors.newSingleThreadExecutor().execute(() -> {
                        // Delete physical media files first
                        deleteLessonMediaFiles(lesson);
                        
                        // Delete the lesson entry from the database
                        AppDatabase.getInstance(appContext).lessonDao().delete(lesson);
                        
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (selectedLessonId == lesson.id) {
                                    selectedLessonId = -1;
                                    selectedLesson = null;
                                }
                                updateDetails(selectedDate);
                                Toast.makeText(getContext(), "Lesson and media files deleted", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteLessonMediaFiles(Lesson lesson) {
        if (lesson.audioPath != null && !lesson.audioPath.isEmpty()) {
            String[] audioFiles = lesson.audioPath.split("\\|");
            for (String path : audioFiles) {
                deleteFileByPath(path);
            }
        }
        if (lesson.videoPath != null && !lesson.videoPath.isEmpty()) {
            String[] videoFiles = lesson.videoPath.split("\\|");
            for (String path : videoFiles) {
                deleteFileByPath(path);
            }
        }
    }

    private void deleteFileByPath(String path) {
        if (path == null || path.isEmpty()) return;
        try {
            if (path.startsWith("content://")) {
                Uri uri = Uri.parse(path);
                requireContext().getContentResolver().delete(uri, null, null);
            } else {
                File file = new File(path);
                if (file.exists()) {
                    file.delete();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addNewLesson() {
        Lesson newLesson = new Lesson(
                "User_1",
                selectedDate,
                0,
                new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()),
                0,
                "Manual Entry"
        );
        Context context = getContext();
        if (context == null) return;
        Context appContext = context.getApplicationContext();
        Executors.newSingleThreadExecutor().execute(() -> {
            long newId = AppDatabase.getInstance(appContext).lessonDao().insert(newLesson);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "New practice log added!", Toast.LENGTH_SHORT).show();
                    selectedLessonId = (int) newId;
                    updateDetails(selectedDate);
                });
            }
        });
    }

    private void setupMinimizedView() {
        binding.calendarView.setVisibility(View.GONE);
        binding.rvWeekView.setVisibility(View.VISIBLE);
        binding.detailsCard.setVisibility(View.GONE);
        binding.closeCalendarButton.setVisibility(View.GONE);
        binding.rvWeekView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        isMinimized = true;
        loadWeekData();
    }

    private void setupFullView() {
        binding.calendarView.setVisibility(View.VISIBLE);
        binding.rvWeekView.setVisibility(View.GONE);
        binding.detailsCard.setVisibility(View.VISIBLE);
        binding.closeCalendarButton.setVisibility(View.VISIBLE);
        isMinimized = false;
        updateDetails(selectedDate);
    }

    private void loadWeekData() {
        Context context = getContext();
        if (context == null) return;
        Context appContext = context.getApplicationContext();

        Executors.newSingleThreadExecutor().execute(() -> {
            List<WeekDayAdapter.DayModel> weekDays = new ArrayList<>();
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -6);

            for (int i = 0; i < 7; i++) {
                Date date = cal.getTime();
                String dateStr = sdf.format(date);
                WeekDayAdapter.DayModel model = new WeekDayAdapter.DayModel(date);

                List<Lesson> lessons = AppDatabase.getInstance(appContext).lessonDao().getLessonsByDate(dateStr);
                if (lessons != null && !lessons.isEmpty()) {
                    model.hasPractice = true;
                    for (Lesson l : lessons) {
                        if (l.audioPath != null && !l.audioPath.isEmpty()) model.hasAudio = true;
                        if (l.videoPath != null && !l.videoPath.isEmpty()) model.hasVideo = true;
                    }
                }

                if (dateStr.equals(selectedDate)) {
                    model.isSelected = true;
                }

                weekDays.add(model);
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    WeekDayAdapter weekAdapter = new WeekDayAdapter(weekDays, day -> {
                        selectedDate = sdf.format(day.date);
                        setupFullView();
                        binding.calendarView.setDate(day.date.getTime());
                    });
                    binding.rvWeekView.setAdapter(weekAdapter);
                });
            }
        });
    }

    private void setupInlineRecordingControls() {
        binding.btnRecStop.setOnClickListener(v -> {
            if (isBound) learningService.stopRecording(false);
        });

        binding.btnRecPause.setOnClickListener(v -> {
            if (isBound && learningService.isRecording()) {
                if (learningService.isPaused()) learningService.resumeRecording();
                else learningService.pauseRecording();
            }
        });

        binding.btnRecCancel.setOnClickListener(v -> {
            if (isBound) learningService.stopRecording(true);
        });
    }

    private void checkPermissionAndStartRecording() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS};
        } else {
            permissions = new String[]{Manifest.permission.RECORD_AUDIO};
        }

        boolean allGranted = true;
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            requestPermissionLauncher.launch(permissions);
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        File directory = requireContext().getExternalFilesDir(null);
        if (directory != null) {
            String path = directory.getAbsolutePath() + "/audio_progress_" + System.currentTimeMillis() + ".mp4";
            learningService.startRecording(path);
        }
    }

    private void updateDetails(String date) {
        if (binding == null) return;
        binding.selectedDateText.setText("Date: " + date);

        Context context = getContext();
        if (context == null) return;
        Context appContext = context.getApplicationContext();

        Executors.newSingleThreadExecutor().execute(() -> {
            List<LessonWithExercise> lessons = AppDatabase.getInstance(appContext).lessonDao().getLessonsWithExerciseByDate(date);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding == null) return;
                    
                    boolean hasLessons = lessons != null && !lessons.isEmpty();

                    binding.recordVideoButton.setEnabled(hasLessons);
                    binding.recordAudioButton.setEnabled(hasLessons);
                    binding.uploadVideoButton.setEnabled(hasLessons);

                    float alpha = hasLessons ? 1.0f : 0.5f;
                    binding.recordVideoButton.setAlpha(alpha);
                    binding.recordAudioButton.setAlpha(alpha);
                    binding.uploadVideoButton.setAlpha(alpha);

                    if (hasLessons) {
                        adapter.setLessons(lessons);

                        boolean found = false;
                        for (LessonWithExercise lwe : lessons) {
                            Lesson l = lwe.lesson;
                            if (l.id == selectedLessonId) {
                                selectedLesson = l;
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            selectedLesson = lessons.get(lessons.size() - 1).lesson;
                            selectedLessonId = selectedLesson.id;
                        }

                        adapter.setSelectedLessonId(selectedLessonId);
                        binding.lessonsRecyclerView.setVisibility(View.VISIBLE);
                        binding.noLessonsText.setVisibility(View.GONE);
                    } else {
                        adapter.setLessons(java.util.Collections.emptyList());
                        selectedLesson = null;
                        selectedLessonId = -1;
                        adapter.setSelectedLessonId(-1);
                        binding.lessonsRecyclerView.setVisibility(View.GONE);
                        binding.noLessonsText.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    private void saveMediaToLesson(String path, boolean isVideo) {
        Context context = getContext();
        if (context == null) return;
        Context appContext = context.getApplicationContext();

        Executors.newSingleThreadExecutor().execute(() -> {
            Lesson targetLesson = null;
            if (selectedLessonId != -1) {
                targetLesson = AppDatabase.getInstance(appContext).lessonDao().getLessonById(selectedLessonId);
            }
            if (targetLesson == null) {
                List<Lesson> dayLessons = AppDatabase.getInstance(appContext).lessonDao().getLessonsByDate(selectedDate);
                if (dayLessons != null && !dayLessons.isEmpty()) {
                    targetLesson = dayLessons.get(dayLessons.size() - 1);
                }
            }
            if (targetLesson == null) {
                Lesson newLesson = new Lesson(
                        "User_1",
                        selectedDate,
                        0,
                        new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()),
                        0,
                        "Practice Session"
                );
                long newId = AppDatabase.getInstance(appContext).lessonDao().insert(newLesson);
                targetLesson = AppDatabase.getInstance(appContext).lessonDao().getLessonById((int)newId);
            }

            if (targetLesson != null) {
                if (isVideo) {
                    if (targetLesson.videoPath == null || targetLesson.videoPath.isEmpty()) {
                        targetLesson.videoPath = path;
                    } else {
                        targetLesson.videoPath += "|" + path;
                    }
                } else {
                    if (targetLesson.audioPath == null || targetLesson.audioPath.isEmpty()) {
                        targetLesson.audioPath = path;
                    } else {
                        targetLesson.audioPath += "|" + path;
                    }
                }

                AppDatabase.getInstance(appContext).lessonDao().update(targetLesson);

                final int finalId = targetLesson.id;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        selectedLessonId = finalId;
                        updateDetails(selectedDate);
                        Toast.makeText(getContext(), (isVideo ? "Video" : "Audio") + " added to log!", Toast.LENGTH_SHORT).show();
                        if (isMinimized) loadWeekData();
                    });
                }
            }
        });
    }

    @Override
    public void onTimerUpdate(long millis) {
    }

    @Override
    public void onRecordingStatusChanged(boolean recording, boolean cancelled) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                showRecordingUI(recording);
                if (!recording && !cancelled) {
                    saveMediaToLesson(learningService.getAudioPath(), false);
                }
            });
        }
    }

    @Override
    public void onRecordingPausedChanged(boolean paused) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (paused) {
                    binding.btnRecPause.setImageResource(android.R.drawable.ic_media_play);
                    binding.recStatusText.setText("II Paused");
                    binding.recStatusText.setTextColor(0xFFFFC107);
                } else {
                    binding.btnRecPause.setImageResource(android.R.drawable.ic_media_pause);
                    binding.recStatusText.setText("● Recording");
                    binding.recStatusText.setTextColor(0xFFFF5252);
                }
            });
        }
    }

    private void showRecordingUI(boolean show) {
        binding.inlineRecordingUi.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.mediaActions.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isBound) {
            if (getContext() != null) {
                requireContext().unbindService(connection);
            }
            isBound = false;
        }
        binding = null;
    }
}
