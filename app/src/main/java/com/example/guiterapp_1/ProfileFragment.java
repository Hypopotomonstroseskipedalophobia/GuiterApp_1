package com.example.guiterapp_1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.guiterapp_1.data.AppDatabase;
import com.example.guiterapp_1.data.Exercise;
import com.example.guiterapp_1.data.Instrument;
import com.example.guiterapp_1.data.Lesson;
import com.example.guiterapp_1.data.LessonWithExercise;
import com.example.guiterapp_1.data.User;
import com.example.guiterapp_1.databinding.DialogAddEditExerciseBinding;
import com.example.guiterapp_1.databinding.FragmentProfileBinding;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ExerciseStatsAdapter exerciseAdapter;
    private List<Instrument> instruments = new ArrayList<>();
    private Integer dialogSelectedInstrumentId = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadUserProfile();

        binding.btnProfileMenu.setOnClickListener(this::showPopupMenu);

        exerciseAdapter = new ExerciseStatsAdapter();
        exerciseAdapter.setOnExerciseClickListener(new ExerciseStatsAdapter.OnExerciseClickListener() {
            @Override
            public void onEditClick(ExerciseStatsAdapter.ExerciseStat stat) {
                showExerciseDialog(stat);
            }

            @Override
            public void onDeleteClick(ExerciseStatsAdapter.ExerciseStat stat) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Delete Exercise")
                        .setMessage("Are you sure you want to delete this exercise?")
                        .setPositiveButton("Delete", (dialog, which) -> deleteExercise(stat.id))
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
        binding.rvExerciseStats.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvExerciseStats.setAdapter(exerciseAdapter);

        setupActivityChartTheme();

        binding.btnAddExercise.setOnClickListener(v -> showAddExerciseDialog());

        binding.cgStatsFilter.setOnCheckedChangeListener((group, checkedId) -> {
            loadStats();
        });

        loadStats();
        loadInstruments();

        ProgressFragment progressFragment = new ProgressFragment();
        Bundle args = new Bundle();
        args.putBoolean("is_minimized", true);
        progressFragment.setArguments(args);

        getChildFragmentManager().beginTransaction()
                .replace(R.id.progress_container, progressFragment)
                .commit();
    }

    private void loadUserProfile() {
        Context context = getContext();
        if (context == null) return;
        SharedPreferences pref = context.getSharedPreferences("GuitarApp", Context.MODE_PRIVATE);
        int userId = pref.getInt("current_user_id", -1);

        if (userId != -1) {
            Context appContext = context.getApplicationContext();
            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase db = AppDatabase.getInstance(appContext);
                User user = db.userDao().getUserById(userId);
                if (user != null && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (binding != null) {
                            binding.tvUsername.setText(user.username);
                            binding.tvUserEmail.setText(user.email);
                        }
                    });
                }
            });
        }
    }

    private void showPopupMenu(View view) {
        Context context = getContext();
        if (context == null) return;
        PopupMenu popup = new PopupMenu(context, view);
        popup.getMenuInflater().inflate(R.menu.profile_menu, popup.getMenu());
        
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_settings) {
                navigateToSettings();
                return true;
            } else if (id == R.id.action_logout) {
                handleLogout();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void handleLogout() {
        Context context = getContext();
        if (context == null) return;
        SharedPreferences pref = context.getSharedPreferences("GuitarApp", Context.MODE_PRIVATE);
        pref.edit().remove("current_user_id").apply();
        
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void navigateToSettings() {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new SettingsFragment())
                .addToBackStack(null)
                .commit();
    }

    private void setupActivityChartTheme() {
        binding.lineChart.getDescription().setEnabled(false);
        binding.lineChart.setNoDataText("Practice more to see your activity!");
        binding.lineChart.setNoDataTextColor(Color.GRAY);
        binding.lineChart.getLegend().setTextColor(Color.WHITE);
        
        YAxis leftAxis = binding.lineChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setGridColor(Color.parseColor("#333333"));
        leftAxis.setDrawZeroLine(true);
        leftAxis.setZeroLineColor(Color.WHITE);
        
        binding.lineChart.getAxisRight().setEnabled(false);
        
        XAxis xAxis = binding.lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);
        xAxis.setAxisMinimum(0f);
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

    private void loadStats() {
        if (binding == null) return;
        final int checkedId = binding.cgStatsFilter.getCheckedChipId();

        Context context = getContext();
        if (context == null) return;
        Context appContext = context.getApplicationContext();

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(appContext);
            
            List<Lesson> filteredLessons;
            if (checkedId == R.id.chip_all) {
                filteredLessons = db.lessonDao().getAllLessons();
            } else {
                String startDate = getStartDateForFilter(checkedId);
                filteredLessons = db.lessonDao().getLessonsAfterDate(startDate);
            }
            
            processLessonsForStats(filteredLessons);

            List<Exercise> allExercises = db.exerciseDao().getAllExercises();
            List<LessonWithExercise> allHistory = db.lessonDao().getAllLessonsWithExercise();
            
            List<Instrument> allInstruments = db.instrumentDao().getAllInstruments();
            Map<Integer, String> instrumentMap = new HashMap<>();
            for (Instrument i : allInstruments) {
                instrumentMap.put(i.instrumentId, i.name);
            }
            
            processExerciseStats(allExercises, allHistory, instrumentMap);
        });
    }

    private String getStartDateForFilter(int checkedId) {
        Calendar cal = Calendar.getInstance();
        if (checkedId == R.id.chip_3_months) {
            cal.add(Calendar.MONTH, -3);
        } else if (checkedId == R.id.chip_1_month) {
            cal.add(Calendar.MONTH, -1);
        } else if (checkedId == R.id.chip_week) {
            cal.add(Calendar.WEEK_OF_YEAR, -1);
        }
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
    }

    private void processLessonsForStats(List<Lesson> lessons) {
        if (lessons != null) {
            long totalSeconds = 0;
            long longestSeconds = 0;
            int sessionCount = lessons.size();

            for (Lesson lesson : lessons) {
                totalSeconds += lesson.lessonLength;
                if (lesson.lessonLength > longestSeconds) {
                    longestSeconds = lesson.lessonLength;
                }
            }

            long avgSeconds = sessionCount > 0 ? totalSeconds / sessionCount : 0;

            final String totalStr = formatDuration(totalSeconds);
            final String avgStr = formatDuration(avgSeconds);
            final String longestStr = formatDuration(longestSeconds);
            final String countStr = String.valueOf(sessionCount);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        binding.tvTotalPlayTime.setText(totalStr);
                        binding.tvTotalSessions.setText(countStr);
                        binding.tvAvgSessionTime.setText(avgStr);
                        binding.tvLongestSession.setText(longestStr);
                        setupLineChart(lessons);
                    }
                });
            }
        }
    }

    private void setupLineChart(List<Lesson> lessons) {
        if (lessons == null || lessons.isEmpty() || binding == null) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        binding.lineChart.clear();
                        binding.lineChart.setNoDataText("Practice more to see your activity!");
                        binding.lineChart.invalidate();
                    }
                });
            }
            return;
        }

        Map<String, Long> dateMap = new TreeMap<>();
        for (Lesson l : lessons) {
            if (l.lessonDate != null) {
                long current = dateMap.getOrDefault(l.lessonDate, 0L);
                dateMap.put(l.lessonDate, current + (l.lessonLength / 60));
            }
        }

        if (dateMap.isEmpty()) return;

        List<Entry> entries = new ArrayList<>();
        final List<String> labels = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, Long> entry : dateMap.entrySet()) {
            entries.add(new Entry(index++, entry.getValue().floatValue()));
            String d = entry.getKey();
            labels.add(d.length() > 5 ? d.substring(5) : d);
        }

        LineDataSet dataSet = new LineDataSet(entries, "Minutes Practiced");
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setCircleColor(Color.parseColor("#4CAF50"));
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#4CAF50"));
        dataSet.setFillAlpha(40);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (binding != null) {
                    binding.lineChart.setData(lineData);

                    XAxis xAxis = binding.lineChart.getXAxis();
                    xAxis.setGranularity(1f);
                    xAxis.setAxisMinimum(0f);
                    if (entries.size() > 1) {
                        xAxis.setAxisMaximum(entries.size() - 0.5f);
                    } else {
                        xAxis.setAxisMaximum(0.5f);
                    }
                    
                    xAxis.setValueFormatter(new ValueFormatter() {
                        @Override
                        public String getFormattedValue(float value) {
                            int i = (int) value;
                            return (i >= 0 && i < labels.size()) ? labels.get(i) : "";
                        }
                    });

                    binding.lineChart.getAxisLeft().setAxisMinimum(0f);
                    binding.lineChart.animateY(800);
                    binding.lineChart.invalidate();
                }
            });
        }
    }

    private void processExerciseStats(List<Exercise> allExercises, List<LessonWithExercise> history, Map<Integer, String> instrumentMap) {
        Map<Integer, List<LessonWithExercise>> historyByExerciseId = new HashMap<>();
        if (history != null) {
            for (LessonWithExercise lwe : history) {
                if (lwe.exercise != null) {
                    int exId = lwe.exercise.id;
                    if (!historyByExerciseId.containsKey(exId)) {
                        historyByExerciseId.put(exId, new ArrayList<>());
                    }
                    historyByExerciseId.get(exId).add(lwe);
                }
            }
        }

        List<ExerciseStatsAdapter.ExerciseStat> statsList = new ArrayList<>();
        List<ExerciseStatsAdapter.ExerciseStat> reachedGoalsList = new ArrayList<>();
        
        for (Exercise ex : allExercises) {
            List<LessonWithExercise> lessons = historyByExerciseId.get(ex.id);
            int startBpm = ex.current_bpm; 
            int currentBpm = ex.current_bpm;
            String lastDate = null;

            ExerciseStatsAdapter.ExerciseStat stat = new ExerciseStatsAdapter.ExerciseStat(
                    ex.id,
                    ex.title,
                    ex.description,
                    ex.type,
                    startBpm,
                    currentBpm,
                    ex.target_bpm,
                    lastDate,
                    ex.instrument_id,
                    instrumentMap.get(ex.instrument_id)
            );

            if (lessons != null && !lessons.isEmpty()) {
                Collections.sort(lessons, (o1, o2) -> {
                    int dateComp = o1.lesson.lessonDate.compareTo(o2.lesson.lessonDate);
                    if (dateComp != 0) return dateComp;
                    return o1.lesson.startTime.compareTo(o2.lesson.startTime);
                });

                stat.startBpm = lessons.get(0).exercise.current_bpm;
                LessonWithExercise latest = lessons.get(lessons.size() - 1);
                stat.currentBpm = latest.exercise.current_bpm;
                stat.lastPracticed = latest.lesson.lessonDate;

                for (LessonWithExercise lwe : lessons) {
                    if (lwe.lesson.lessonDate != null && lwe.lesson.lessonDate.length() >= 7) {
                        String month = lwe.lesson.lessonDate.substring(0, 7);
                        int bpm = lwe.exercise.current_bpm;
                        Integer existingMax = stat.monthlyBpm.get(month);
                        if (existingMax == null || bpm > existingMax) {
                            stat.monthlyBpm.put(month, bpm);
                        }
                    }
                }
            }
            
            String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new java.util.Date());
            if (!stat.monthlyBpm.containsKey(currentMonth)) {
                stat.monthlyBpm.put(currentMonth, ex.current_bpm);
            }

            statsList.add(stat);
            
            // Check if goal is reached
            if (stat.targetBpm > 0 && stat.currentBpm >= stat.targetBpm) {
                reachedGoalsList.add(stat);
            }
        }

        Collections.sort(statsList, (s1, s2) -> Integer.compare(s2.getImprovement(), s1.getImprovement()));

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (exerciseAdapter != null) {
                    exerciseAdapter.setStats(statsList);
                }
                updateReachedGoalsUI(reachedGoalsList);
            });
        }
    }

    private void updateReachedGoalsUI(List<ExerciseStatsAdapter.ExerciseStat> reachedGoals) {
        if (binding == null) return;

        List<ExerciseStatsAdapter.ExerciseStat> groupedGoals = ExerciseStatsAdapter.groupExercises(reachedGoals);

        if (groupedGoals.isEmpty()) {
            binding.tvReachedGoalsLabel.setVisibility(View.GONE);
            binding.cvReachedGoals.setVisibility(View.GONE);
            return;
        }

        binding.tvReachedGoalsLabel.setVisibility(View.VISIBLE);
        binding.cvReachedGoals.setVisibility(View.VISIBLE);
        binding.llReachedGoalsContainer.removeAllViews();

        Context context = getContext();
        if (context == null) return;
        LayoutInflater inflater = LayoutInflater.from(context);
        for (ExerciseStatsAdapter.ExerciseStat stat : groupedGoals) {
            View itemView = inflater.inflate(android.R.layout.simple_list_item_2, binding.llReachedGoalsContainer, false);
            
            TextView text1 = itemView.findViewById(android.R.id.text1);
            TextView text2 = itemView.findViewById(android.R.id.text2);
            
            text1.setText("🏆 " + stat.title);
            text1.setTextColor(Color.WHITE);
            text1.setTextSize(16);
            
            text2.setText(String.format(Locale.getDefault(), "Target %d BPM reached! (Current: %d)", stat.targetBpm, stat.currentBpm));
            text2.setTextColor(0xFF4CAF50);
            
            itemView.setPadding(8, 16, 8, 8);
            
            Button btnSetNewGoal = new Button(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btnSetNewGoal.setText("Set New Goal");
            btnSetNewGoal.setAllCaps(false);
            btnSetNewGoal.setTextColor(Color.WHITE);
            btnSetNewGoal.setTextSize(12);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 16);
            btnSetNewGoal.setLayoutParams(params);
            
            btnSetNewGoal.setOnClickListener(v -> showExerciseDialog(stat));

            binding.llReachedGoalsContainer.addView(itemView);
            binding.llReachedGoalsContainer.addView(btnSetNewGoal);
        }
    }

    private void showAddExerciseDialog() {
        showExerciseDialog(null);
    }

    private void showExerciseDialog(@Nullable ExerciseStatsAdapter.ExerciseStat stat) {
        Context context = getContext();
        if (context == null) return;

        DialogAddEditExerciseBinding dialogBinding = DialogAddEditExerciseBinding.inflate(getLayoutInflater());
        
        dialogSelectedInstrumentId = (stat != null) ? stat.instrumentId : null;

        List<String> instrumentNames = new ArrayList<>();
        for (Instrument i : instruments) {
            instrumentNames.add(i.name);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line, instrumentNames);
        dialogBinding.instrumentAutoComplete.setAdapter(adapter);
        
        if (stat != null) {
            dialogBinding.tvDialogTitle.setText("Edit Exercise");
            dialogBinding.etExerciseTitle.setText(stat.title);
            dialogBinding.etExerciseType.setText(stat.type);
            dialogBinding.etExerciseDesc.setText(stat.description);
            dialogBinding.etCurrentBpm.setText(String.valueOf(stat.currentBpm));
            dialogBinding.etTargetBpm.setText(String.valueOf(stat.targetBpm));
            if (stat.instrumentName != null) {
                dialogBinding.instrumentAutoComplete.setText(stat.instrumentName, false);
            }
        } else {
            dialogBinding.tvDialogTitle.setText("Add New Exercise");
        }

        dialogBinding.instrumentAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            dialogSelectedInstrumentId = instruments.get(position).instrumentId;
        });

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(stat != null ? "Update" : "Add", null)
                .setNegativeButton("Cancel", null);

        if (stat != null) {
            builder.setNeutralButton("Delete", (dialogInterface, i) -> {
                new MaterialAlertDialogBuilder(context)
                        .setTitle("Delete Exercise")
                        .setMessage("Are you sure you want to delete this exercise?")
                        .setPositiveButton("Delete", (d, w) -> deleteExercise(stat.id))
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String title = dialogBinding.etExerciseTitle.getText().toString().trim();
                if (title.isEmpty()) {
                    dialogBinding.etExerciseTitle.setError("Title required");
                    return;
                }

                int curBpm = 0;
                int tarBpm = 0;
                try {
                    curBpm = Integer.parseInt(dialogBinding.etCurrentBpm.getText().toString());
                    tarBpm = Integer.parseInt(dialogBinding.etTargetBpm.getText().toString());
                } catch (NumberFormatException ignored) {}

                String desc = dialogBinding.etExerciseDesc.getText().toString().trim();
                String type = dialogBinding.etExerciseType.getText().toString().trim();

                if (dialogSelectedInstrumentId == null) {
                    Toast.makeText(getContext(), "Please select an instrument", Toast.LENGTH_SHORT).show();
                    return;
                }

                Exercise exercise = new Exercise();
                if (stat != null) exercise.id = stat.id;
                exercise.title = title;
                exercise.description = desc;
                exercise.type = type;
                exercise.current_bpm = curBpm;
                exercise.target_bpm = tarBpm;
                exercise.instrument_id = dialogSelectedInstrumentId;

                saveExercise(exercise);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void saveExercise(Exercise exercise) {
        Context context = getContext();
        if (context == null) return;
        Context appContext = context.getApplicationContext();
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(appContext);
            if (exercise.id == 0) {
                db.exerciseDao().insert(exercise);
            } else {
                db.exerciseDao().update(exercise);
            }
            loadStats(); 
        });
    }

    private void deleteExercise(int exerciseId) {
        Context context = getContext();
        if (context == null) return;
        Context appContext = context.getApplicationContext();
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(appContext);
            Exercise ex = new Exercise();
            ex.id = exerciseId;
            db.exerciseDao().delete(ex);
            loadStats();
        });
    }

    private String formatDuration(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) {
            return String.format(Locale.getDefault(), "%dh %dm", h, m);
        } else if (m > 0) {
            return String.format(Locale.getDefault(), "%dm %ds", m, s);
        } else {
            return String.format(Locale.getDefault(), "%ds", s);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
