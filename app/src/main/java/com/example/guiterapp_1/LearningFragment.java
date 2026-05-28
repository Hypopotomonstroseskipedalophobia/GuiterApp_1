package com.example.guiterapp_1;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
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

import com.example.guiterapp_1.data.AppDatabase;
import com.example.guiterapp_1.data.Exercise;
import com.example.guiterapp_1.data.Instrument;
import com.example.guiterapp_1.data.Lesson;
import com.example.guiterapp_1.databinding.FragmentLearningBinding;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class LearningFragment extends Fragment implements LearningService.ServiceCallbacks {

    private FragmentLearningBinding binding;
    private LearningService learningService;
    private boolean isBound = false;

    private String currentStartTimeStr;
    private String currentStartDateStr;
    private long lastDurationSeconds = 0;

    private List<Instrument> instruments;
    private Integer selectedInstrumentId = null;
    private Integer exerciseSelectedInstrumentId = null;

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
            learningService.setCallbacks(LearningFragment.this);
            isBound = true;
            updateUIState();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLearningBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Intent intent = new Intent(requireContext(), LearningService.class);
        requireContext().bindService(intent, connection, Context.BIND_AUTO_CREATE);

        binding.startLearningButton.setOnClickListener(v -> {
            if (isBound) {
                if (learningService.isLearning()) {
                    stopLearning();
                } else {
                    startLearning();
                }
            }
        });

        binding.recordAudioButton.setOnClickListener(v -> {
            if (isBound) {
                if (learningService.isRecording()) {
                    learningService.stopRecording(false);
                } else {
                    checkPermissionAndStartRecording();
                }
            }
        });

        binding.pauseResumeButton.setOnClickListener(v -> {
            if (isBound && learningService.isRecording()) {
                if (learningService.isPaused()) {
                    learningService.resumeRecording();
                } else {
                    learningService.pauseRecording();
                }
            }
        });

        binding.cancelRecordingButton.setOnClickListener(v -> {
            if (isBound && learningService.isRecording()) {
                learningService.stopRecording(true);
            }
        });

        binding.exerciseButton.setOnClickListener(v -> {
            binding.exerciseForm.setVisibility(binding.exerciseForm.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        });

        binding.rateButton.setOnClickListener(v -> {
            int rating = (int) binding.ratingBar.getRating();
            String notes = binding.notesEditText.getText().toString();
            
            Exercise exercise = null;
            String exTitle = binding.exerciseTitleEditText.getText().toString().trim();
            if (!exTitle.isEmpty()) {
                exercise = new Exercise();
                exercise.title = exTitle;
                exercise.type = binding.exerciseTypeEditText.getText().toString().trim();
                exercise.description = binding.exerciseDescEditText.getText().toString().trim();
                try {
                    exercise.current_bpm = Integer.parseInt(binding.exerciseCurrentBpmEditText.getText().toString());
                } catch (NumberFormatException e) {
                    exercise.current_bpm = 0;
                }
                try {
                    exercise.target_bpm = Integer.parseInt(binding.exerciseBpmEditText.getText().toString());
                } catch (NumberFormatException e) {
                    exercise.target_bpm = 0;
                }
                
                if (exerciseSelectedInstrumentId != null) {
                    exercise.instrument_id = exerciseSelectedInstrumentId;
                } else if (selectedInstrumentId != null) {
                    exercise.instrument_id = selectedInstrumentId;
                }
            }

            saveLesson(lastDurationSeconds, rating, notes, learningService != null ? learningService.getAudioPath() : null, exercise, selectedInstrumentId);
            resetAfterSave();
        });

        binding.skipButton.setOnClickListener(v -> {
            saveLesson(lastDurationSeconds, 0, "Skipped rating", learningService != null ? learningService.getAudioPath() : null, null, null);
            resetAfterSave();
        });

        loadInstruments();
        loadLastSession();
    }

    private void loadInstruments() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            instruments = db.instrumentDao().getAllInstruments();
            
            if (instruments.isEmpty()) {
                db.instrumentDao().insert(new Instrument("Guitar"));
                db.instrumentDao().insert(new Instrument("Bass"));
                db.instrumentDao().insert(new Instrument("Ukulele"));
                instruments = db.instrumentDao().getAllInstruments();
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    List<String> names = new ArrayList<>();
                    for (Instrument i : instruments) {
                        names.add(i.name);
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_dropdown_item_1line, names);
                    
                    binding.instrumentAutoComplete.setAdapter(adapter);
                    binding.exerciseInstrumentAutoComplete.setAdapter(adapter);
                    
                    binding.instrumentAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
                        String selectedName = (String) parent.getItemAtPosition(position);
                        for (Instrument i : instruments) {
                            if (i.name.equals(selectedName)) {
                                selectedInstrumentId = i.instrumentId;
                                break;
                            }
                        }
                    });

                    binding.exerciseInstrumentAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
                        String selectedName = (String) parent.getItemAtPosition(position);
                        for (Instrument i : instruments) {
                            if (i.name.equals(selectedName)) {
                                exerciseSelectedInstrumentId = i.instrumentId;
                                break;
                            }
                        }
                    });
                });
            }
        });
    }

    private void updateUIState() {
        if (learningService.isLearning()) {
            binding.startLearningButton.setText("Stop Training");
            binding.recordingControlsCard.setVisibility(View.VISIBLE);
            binding.lastSessionCard.setVisibility(View.GONE);
            binding.ratingGroup.setVisibility(View.GONE);
            binding.mainTrainingCard.setVisibility(View.VISIBLE);
            if (learningService.isRecording()) {
                onRecordingStatusChanged(true, false);
                onRecordingPausedChanged(learningService.isPaused());
            }
        } else {
            binding.startLearningButton.setText("Start Training");
            binding.recordingControlsCard.setVisibility(View.GONE);
            binding.ratingGroup.setVisibility(View.GONE);
            binding.mainTrainingCard.setVisibility(View.VISIBLE);
        }
    }

    private void startLearning() {
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        currentStartDateStr = dateFmt.format(new Date());
        currentStartTimeStr = timeFmt.format(new Date());

        Intent serviceIntent = new Intent(requireContext(), LearningService.class);
        ContextCompat.startForegroundService(requireContext(), serviceIntent);
        learningService.startLearning();

        binding.startLearningButton.setText("Stop Training");
        binding.ratingGroup.setVisibility(View.GONE);
        binding.lastSessionCard.setVisibility(View.GONE);
        binding.recordingControlsCard.setVisibility(View.VISIBLE);
        binding.mainTrainingCard.setVisibility(View.VISIBLE);
    }

    private void stopLearning() {
        lastDurationSeconds = learningService.getElapsedMillis() / 1000;
        learningService.stopLearning();
        
        binding.startLearningButton.setText("Start Training");
        binding.timerText.setText("00:00:00");
        binding.recordingControlsCard.setVisibility(View.GONE);
        binding.recordingIndicator.setVisibility(View.GONE);
        binding.pauseResumeButton.setVisibility(View.GONE);
        binding.cancelRecordingButton.setVisibility(View.GONE);
        binding.mainTrainingCard.setVisibility(View.GONE);
        binding.lastSessionCard.setVisibility(View.GONE);

        if (lastDurationSeconds >= 10) {
            binding.ratingGroup.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(getContext(), "Session too short to save", Toast.LENGTH_SHORT).show();
            resetAfterSave();
        }
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

        if (allGranted) {
            startRecording();
        } else {
            requestPermissionLauncher.launch(permissions);
        }
    }

    private void startRecording() {
        File recordingsDir = new File(requireContext().getExternalFilesDir(null), "recordings");
        if (!recordingsDir.exists()) recordingsDir.mkdirs();
        
        String fileName = "recording_" + System.currentTimeMillis() + ".mp4";
        File audioFile = new File(recordingsDir, fileName);
        learningService.startRecording(audioFile.getAbsolutePath());
    }

    @Override
    public void onTimerUpdate(long millis) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                int seconds = (int) (millis / 1000) % 60;
                int minutes = (int) ((millis / (1000 * 60)) % 60);
                int hours = (int) ((millis / (1000 * 60 * 60)) % 24);
                binding.timerText.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
            });
        }
    }

    @Override
    public void onRecordingStatusChanged(boolean recording, boolean cancelled) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (recording) {
                    binding.recordAudioButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                    binding.recordingStatusText.setText("Recording...");
                    binding.recordingIndicator.setVisibility(View.VISIBLE);
                    binding.pauseResumeButton.setVisibility(View.VISIBLE);
                    binding.cancelRecordingButton.setVisibility(View.VISIBLE);
                } else {
                    binding.recordAudioButton.setImageResource(android.R.drawable.ic_btn_speak_now);
                    binding.recordingStatusText.setText("Recording Ready");
                    binding.recordingIndicator.setVisibility(View.GONE);
                    binding.pauseResumeButton.setVisibility(View.GONE);
                    binding.cancelRecordingButton.setVisibility(View.GONE);
                    if (!cancelled) {
                        Toast.makeText(getContext(), "Recording saved", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    @Override
    public void onRecordingPausedChanged(boolean paused) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                binding.pauseResumeButton.setImageResource(paused ? android.R.drawable.ic_media_play : android.R.drawable.ic_media_pause);
                binding.recordingStatusText.setText(paused ? "Recording Paused" : "Recording...");
            });
        }
    }

    private void saveLesson(long durationSeconds, int rating, String notes, String audioPath, Exercise exercise, Integer instrumentId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            
            Integer exerciseId = null;
            if (exercise != null) {
                exerciseId = (int) db.exerciseDao().insert(exercise);
            }

            Lesson lesson = new Lesson(
                "user1",
                currentStartDateStr,
                durationSeconds,
                currentStartTimeStr,
                rating,
                notes
            );
            lesson.audioPath = audioPath;
            lesson.instrumentId = instrumentId;
            lesson.exerciseId = exerciseId;

            db.lessonDao().insert(lesson);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Lesson saved!", Toast.LENGTH_SHORT).show();
                    loadLastSession();
                });
            }
        });
    }

    private void resetAfterSave() {
        binding.ratingGroup.setVisibility(View.GONE);
        binding.exerciseForm.setVisibility(View.GONE);
        binding.notesEditText.setText("");
        binding.ratingBar.setRating(0);
        binding.exerciseTitleEditText.setText("");
        binding.exerciseTypeEditText.setText("");
        binding.exerciseDescEditText.setText("");
        binding.exerciseBpmEditText.setText("");
        binding.exerciseCurrentBpmEditText.setText("");
        binding.mainTrainingCard.setVisibility(View.VISIBLE);
        lastDurationSeconds = 0;
        exerciseSelectedInstrumentId = null;
        binding.exerciseInstrumentAutoComplete.setText("");
    }

    private void loadLastSession() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            Lesson lastLesson = db.lessonDao().getLastLesson();
            if (lastLesson != null) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        binding.lastSessionCard.setVisibility(View.VISIBLE);
                        String duration = String.format(Locale.getDefault(), "%d min", lastLesson.lessonLength / 60);
                        binding.lastSessionText.setText(String.format(Locale.getDefault(), "Last session: %s (%s)", lastLesson.lessonDate, duration));
                    });
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isBound) {
            requireContext().unbindService(connection);
            isBound = false;
        }
        binding = null;
    }
}
