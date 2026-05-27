package com.example.guiterapp_1;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.guiterapp_1.data.AppDatabase;
import com.example.guiterapp_1.data.Exercise;
import com.example.guiterapp_1.data.Instrument;
import com.example.guiterapp_1.data.Lesson;
import com.example.guiterapp_1.databinding.FragmentSettingsBinding;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Executors;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnOpenMediaDir.setOnClickListener(v -> openMediaDirectory());

        binding.btnDebugPopulate.setOnClickListener(v -> {
            binding.btnDebugPopulate.setEnabled(false);
            populateMockData();
        });

        binding.btnDeleteAllData.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete All Data")
                    .setMessage("Are you sure you want to delete all user data? This action cannot be undone.")
                    .setPositiveButton("Delete", (dialog, which) -> deleteAllData())
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        binding.btnBackToProfile.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ProfileFragment())
                        .commit();
            }
        });
    }

    private void openMediaDirectory() {
        File mediaDir = MediaUtils.getMediaBaseDir(requireContext());
        if (mediaDir != null) {
            // Show the path to the user first
            Toast.makeText(getContext(), "Opening: " + mediaDir.getAbsolutePath(), Toast.LENGTH_SHORT).show();

            // Try to open with FileProvider and a directory MIME type
            Uri uri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", mediaDir);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "vnd.android.document/directory");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            try {
                startActivity(intent);
            } catch (Exception e) {
                // Fallback: try a more generic approach or a file picker
                try {
                    Intent fallbackIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    fallbackIntent.setDataAndType(uri, "*/*");
                    fallbackIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivity(Intent.createChooser(fallbackIntent, "Open Media Folder"));
                } catch (Exception e2) {
                    Toast.makeText(getContext(), "Could not find a file manager to open this directory.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void deleteAllData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            db.clearAllTables();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "All user data has been deleted.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void populateMockData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());

            db.clearAllTables();

            db.instrumentDao().insert(new Instrument("Guitar"));
            db.instrumentDao().insert(new Instrument("Bass"));
            var instruments = db.instrumentDao().getAllInstruments();
            int guitarId = instruments.get(0).instrumentId;
            int bassId = instruments.get(1).instrumentId;

            int majorScaleId = createExercise(db, "C Major Scale", "Scale", guitarId, 80, 130);
            int gMajorScaleId = createExercise(db, "G Major Scale", "Scale", guitarId, 70, 160);
            int eMinorScaleId = createExercise(db, "E Natural Minor", "Scale", guitarId, 75, 180);
            int pentaScaleId = createExercise(db, "A Minor Pentatonic", "Scale", guitarId, 90, 170);
            
            int sweepingId = createExercise(db, "Diminished Sweeping", "Sweeping", guitarId, 60, 120);
            int arpeggioId = createExercise(db, "Am Arpeggio Sweep", "Sweeping", guitarId, 55, 190);
            int tappingId = createExercise(db, "Eruption Tapping", "Technique", guitarId, 100, 200);
            
            int songId = createExercise(db, "Hotel California Solo", "Song", guitarId, 80, 100);
            int bassExId = createExercise(db, "Slap Bass Foundation", "Exercise", bassId, 90, 150);

            SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -90); 
            Random random = new Random();

            for (int i = 0; i < 90; i++) {
                if (random.nextInt(7) < 5) {
                    String dateStr = dateFmt.format(cal.getTime());

                    int sessions = random.nextInt(3) + 1;
                    for (int s = 0; s < sessions; s++) {
                        long length = 900 + random.nextInt(1800); 
                        int rating = 3 + random.nextInt(3); 
                        
                        Lesson lesson = new Lesson(
                            "User_1",
                            dateStr,
                            length,
                            (17 + s) + ":30:00",
                            rating,
                            "Mock practice session"
                        );
                        
                        int r = random.nextInt(100);
                        if (r < 20) { 
                            int bpm = 80 + (int)(i * 0.6) + random.nextInt(3);
                            saveMockLessonWithBpm(db, lesson, majorScaleId, bpm);
                        } else if (r < 35) {
                            int bpm = 60 + (int)(i * 0.7) + random.nextInt(5);
                            saveMockLessonWithBpm(db, lesson, sweepingId, bpm);
                        } else if (r < 50) {
                            int bpm = 90 + (int)(i * 0.5) + random.nextInt(4);
                            saveMockLessonWithBpm(db, lesson, pentaScaleId, bpm);
                        } else if (r < 65) {
                            int bpm = 70 + (int)(i * 0.5) + random.nextInt(3);
                            saveMockLessonWithBpm(db, lesson, gMajorScaleId, bpm);
                        } else if (r < 80) {
                            int bpm = 90 + (int)(i * 0.25) + random.nextInt(2);
                            lesson.instrumentId = bassId;
                            saveMockLessonWithBpm(db, lesson, bassExId, bpm);
                        } else {
                            lesson.exerciseId = songId;
                            lesson.instrumentId = guitarId;
                            db.lessonDao().insert(lesson);
                        }
                    }
                }
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    binding.btnDebugPopulate.setEnabled(true);
                    Toast.makeText(getContext(), "Mock data populated!", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private int createExercise(AppDatabase db, String title, String type, int instrumentId, int cur, int target) {
        Exercise ex = new Exercise();
        ex.title = title;
        ex.type = type;
        ex.instrument_id = instrumentId;
        ex.current_bpm = cur;
        ex.target_bpm = target;
        return (int) db.exerciseDao().insert(ex);
    }

    private void saveMockLessonWithBpm(AppDatabase db, Lesson lesson, int baseExerciseId, int bpm) {
        Exercise baseEx = db.exerciseDao().getExerciseById(baseExerciseId);
        if (baseEx != null) {
            Exercise sessionEx = new Exercise();
            sessionEx.title = baseEx.title;
            sessionEx.type = baseEx.type;
            sessionEx.description = "Practice session";
            sessionEx.instrument_id = baseEx.instrument_id;
            sessionEx.current_bpm = bpm;
            sessionEx.target_bpm = baseEx.target_bpm;
            
            int snapshotId = (int) db.exerciseDao().insert(sessionEx);
            lesson.exerciseId = snapshotId;
            lesson.instrumentId = baseEx.instrument_id;
            db.lessonDao().insert(lesson);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
