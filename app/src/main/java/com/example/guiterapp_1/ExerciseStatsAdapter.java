package com.example.guiterapp_1;

import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.guiterapp_1.databinding.ItemExerciseStatsBinding;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ExerciseStatsAdapter extends RecyclerView.Adapter<ExerciseStatsAdapter.ViewHolder> {

    private List<ExerciseStat> displayStats = new ArrayList<>();
    private OnExerciseClickListener listener;

    public interface OnExerciseClickListener {
        void onEditClick(ExerciseStat stat);
        void onDeleteClick(ExerciseStat stat);
    }

    public void setOnExerciseClickListener(OnExerciseClickListener listener) {
        this.listener = listener;
    }

    public static class ExerciseStat {
        public int id;
        public String title;
        public String description;
        public int startBpm;
        public int currentBpm;
        public int targetBpm;
        public String type;
        public String lastPracticed;
        public Integer instrumentId;
        public String instrumentName;
        public boolean isExpanded = false;
        public boolean isStackExpanded = false;
        public Map<String, Integer> monthlyBpm = new TreeMap<>();
        public List<ExerciseStat> stackedExercises = new ArrayList<>();

        public ExerciseStat(int id, String title, String description, String type, int startBpm, int currentBpm, int targetBpm, String lastPracticed, Integer instrumentId, String instrumentName) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.type = type;
            this.startBpm = startBpm;
            this.currentBpm = currentBpm;
            this.targetBpm = targetBpm;
            this.lastPracticed = lastPracticed;
            this.instrumentId = instrumentId;
            this.instrumentName = instrumentName;
        }

        public int getImprovement() {
            return currentBpm - startBpm;
        }
    }

    public void setStats(List<ExerciseStat> allStats) {
        this.displayStats = groupExercises(allStats);
        notifyDataSetChanged();
    }

    public static List<ExerciseStat> groupExercises(List<ExerciseStat> allStats) {
        if (allStats == null || allStats.isEmpty()) return new ArrayList<>();

        // Sort by last practiced date descending so the most recent exercise is the "head" of the stack
        Collections.sort(allStats, (a, b) -> {
            String dateA = a.lastPracticed != null ? a.lastPracticed : "";
            String dateB = b.lastPracticed != null ? b.lastPracticed : "";
            return dateB.compareTo(dateA);
        });

        List<ExerciseStat> result = new ArrayList<>();
        boolean[] processed = new boolean[allStats.size()];

        for (int i = 0; i < allStats.size(); i++) {
            if (processed[i]) continue;

            ExerciseStat head = allStats.get(i);
            processed[i] = true;
            head.stackedExercises = new ArrayList<>();
            
            for (int j = i + 1; j < allStats.size(); j++) {
                if (processed[j]) continue;
                
                ExerciseStat candidate = allStats.get(j);
                if (isSimilar(head, candidate)) {
                    head.stackedExercises.add(candidate);
                    processed[j] = true;
                }
            }
            result.add(head);
        }
        return result;
    }

    public static boolean isSimilar(ExerciseStat s1, ExerciseStat s2) {
        return areTitlesSimilar(s1.title, s2.title);
    }

    private static boolean areTitlesSimilar(String t1, String t2) {
        if (t1 == null || t2 == null) return false;
        
        Set<String> words1 = getSignificantWords(t1);
        Set<String> words2 = getSignificantWords(t2);
        
        if (words1.isEmpty() || words2.isEmpty()) return false;
        
        int matches = 0;
        for (String w1 : words1) {
            for (String w2 : words2) {
                if (isWordSimilar(w1, w2)) {
                    matches++;
                    break;
                }
            }
        }
        
        int maxWords = Math.max(words1.size(), words2.size());
        return (double) matches / maxWords >= 0.75;
    }

    private static boolean isWordSimilar(String w1, String w2) {
        if (w1.equals(w2)) return true;
        if (w1.length() < 1 || w2.length() < 1) return false;
        
        if (w1.startsWith(w2) || w2.startsWith(w1)) {
            return Math.abs(w1.length() - w2.length()) <= 2;
        }
        return false;
    }

    private static Set<String> getSignificantWords(String text) {
        Set<String> result = new HashSet<>();
        if (text == null) return result;
        String[] words = text.toLowerCase().split("[\\s,.:;!?-]+");
        for (String w : words) {
            if (w.length() >= 1) result.add(w);
        }
        return result;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemExerciseStatsBinding binding = ItemExerciseStatsBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExerciseStat stat = displayStats.get(position);
        
        int minStartBpm = stat.startBpm;
        int maxCurrentBpm = stat.currentBpm;
        
        for (ExerciseStat s : stat.stackedExercises) {
            if (s.startBpm > 0 && s.startBpm < minStartBpm) minStartBpm = s.startBpm;
            if (s.currentBpm > maxCurrentBpm) maxCurrentBpm = s.currentBpm;
        }
        
        int totalImprovement = maxCurrentBpm - minStartBpm;

        holder.binding.tvExerciseTitle.setText(stat.title);
        
        String subtitle = (stat.type != null ? stat.type : "") + (stat.instrumentName != null ? " | " + stat.instrumentName : "");
        holder.binding.tvExerciseDescription.setText(stat.description != null && !stat.description.isEmpty() ? stat.description : subtitle);

        holder.binding.tvStartBpm.setText(String.format(Locale.getDefault(), "%d BPM", minStartBpm));
        holder.binding.tvCurrentBpm.setText(String.format(Locale.getDefault(), "%d BPM", maxCurrentBpm));
        holder.binding.tvLastPracticed.setText(stat.lastPracticed != null ? "Last practiced: " + stat.lastPracticed : "Never practiced");

        holder.binding.tvBpmSummary.setText((totalImprovement >= 0 ? "+" : "") + totalImprovement + " BPM");
        holder.binding.tvBpmSummary.setTextColor(totalImprovement >= 0 ? 0xFF4CAF50 : 0xFFFF5252);
        
        double percent = minStartBpm > 0 ? (totalImprovement * 100.0 / minStartBpm) : 0;
        holder.binding.tvImprovementDetail.setText(String.format(Locale.getDefault(), "%s%.1f%%", (totalImprovement >= 0 ? "+" : ""), percent));
        holder.binding.tvImprovementDetail.setTextColor(totalImprovement >= 0 ? 0xFF4CAF50 : 0xFFFF5252);

        if (!stat.stackedExercises.isEmpty()) {
            holder.binding.tvStackIndicator.setVisibility(View.VISIBLE);
            holder.binding.tvStackIndicator.setText(String.format(Locale.getDefault(), "+%d more related", stat.stackedExercises.size()));
            
            holder.binding.btnUnrollStack.setVisibility(View.VISIBLE);
            holder.binding.btnUnrollStack.setText(stat.isStackExpanded ? "Hide related" : "Show related");
            
            holder.binding.llStackContainer.setVisibility(stat.isStackExpanded ? View.VISIBLE : View.GONE);
            if (stat.isStackExpanded) populateStack(holder.binding.llStackContainer, stat.stackedExercises);
        } else {
            holder.binding.tvStackIndicator.setVisibility(View.GONE);
            holder.binding.btnUnrollStack.setVisibility(View.GONE);
            holder.binding.llStackContainer.setVisibility(View.GONE);
        }

        holder.binding.llDetails.setVisibility(stat.isExpanded ? View.VISIBLE : View.GONE);

        if (stat.isExpanded) {
            setupDetailChart(holder.binding.exerciseBarChart, stat);
        }

        holder.binding.rlHeader.setOnClickListener(v -> {
            stat.isExpanded = !stat.isExpanded;
            notifyItemChanged(holder.getBindingAdapterPosition());
        });

        holder.binding.btnUnrollStack.setOnClickListener(v -> {
            stat.isStackExpanded = !stat.isStackExpanded;
            notifyItemChanged(holder.getBindingAdapterPosition());
        });

        holder.binding.rlHeader.setOnLongClickListener(v -> {
            if (listener != null) listener.onEditClick(stat);
            return true;
        });

        holder.binding.btnDeleteExercise.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(stat);
        });
    }

    @Override
    public int getItemCount() {
        return displayStats.size();
    }

    private void populateStack(LinearLayout container, List<ExerciseStat> exercises) {
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        for (ExerciseStat ex : exercises) {
            View view = inflater.inflate(R.layout.item_related_exercise, container, false);
            TextView text1 = view.findViewById(R.id.tv_title);
            TextView text2 = view.findViewById(R.id.tv_info);
            ImageButton btnEdit = view.findViewById(R.id.btn_edit_related);
            ImageButton btnDelete = view.findViewById(R.id.btn_delete_related);

            text1.setText(ex.title);
            String info = String.format(Locale.getDefault(), "%d BPM (%+d) | %s", ex.currentBpm, ex.getImprovement(), ex.lastPracticed != null ? ex.lastPracticed : "Never");
            text2.setText(info);

            btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEditClick(ex);
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(ex);
            });

            container.addView(view);
        }
    }

    private Map<String, Integer> getMergedBpmData(ExerciseStat head) {
        Map<String, Integer> merged = new TreeMap<>(head.monthlyBpm);
        for (ExerciseStat s : head.stackedExercises) {
            for (Map.Entry<String, Integer> entry : s.monthlyBpm.entrySet()) {
                Integer currentMax = merged.get(entry.getKey());
                if (currentMax == null || entry.getValue() > currentMax) {
                    merged.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return merged;
    }

    private void setupDetailChart(BarChart chart, ExerciseStat head) {
        Map<String, Integer> mergedBpm = getMergedBpmData(head);
        if (mergedBpm.isEmpty()) {
            chart.setVisibility(View.GONE);
            return;
        }
        chart.setVisibility(View.VISIBLE);

        List<BarEntry> entries = new ArrayList<>();
        final List<String> labels = new ArrayList<>();
        int index = 0;
        
        List<String> sortedMonths = new ArrayList<>(mergedBpm.keySet());
        // Show last 6 months for a balanced, tidy look
        int start = Math.max(0, sortedMonths.size() - 6);
        
        SimpleDateFormat inFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        SimpleDateFormat outFormat = new SimpleDateFormat("MMM", Locale.getDefault());

        for (int i = start; i < sortedMonths.size(); i++) {
            String monthKey = sortedMonths.get(i);
            entries.add(new BarEntry(index++, mergedBpm.get(monthKey)));
            
            String label = monthKey;
            try {
                Date d = inFormat.parse(monthKey);
                if (d != null) label = outFormat.format(d).toUpperCase();
            } catch (Exception ignored) {}
            labels.add(label);
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(Color.parseColor("#4CAF50")); 
        dataSet.setDrawValues(true);
        dataSet.setValueTextColor(Color.parseColor("#CCCCCC"));
        dataSet.setValueTextSize(8f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f); // Tidy balance between bar thickness and spacing

        chart.setData(barData);
        chart.setFitBars(true);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);

        chart.setExtraTopOffset(15f);
        chart.setExtraBottomOffset(5f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setTextColor(Color.parseColor("#999999"));
        xAxis.setTextSize(8f);
        xAxis.setGranularity(1f);
        xAxis.setYOffset(5f); // Space between labels and bars
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int i = (int) value;
                return (i >= 0 && i < labels.size()) ? labels.get(i) : "";
            }
        });

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setDrawLabels(false);
        leftAxis.setAxisMinimum(0f);

        // headroom
        float maxVal = 0;
        for (BarEntry e : entries) if (e.getY() > maxVal) maxVal = e.getY();
        leftAxis.setAxisMaximum(maxVal * 1.4f); 

        chart.getAxisRight().setEnabled(false);
        chart.animateY(600);
        chart.invalidate();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemExerciseStatsBinding binding;
        public ViewHolder(ItemExerciseStatsBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
