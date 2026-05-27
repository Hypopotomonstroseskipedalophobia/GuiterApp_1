package com.example.guiterapp_1;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.guiterapp_1.databinding.ItemWeekDayBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WeekDayAdapter extends RecyclerView.Adapter<WeekDayAdapter.ViewHolder> {

    public static class DayModel {
        public Date date;
        public boolean hasPractice;
        public boolean hasAudio;
        public boolean hasVideo;
        public boolean hasExercise;
        public boolean isSelected;

        public DayModel(Date date) {
            this.date = date;
        }
    }

    private final List<DayModel> days;
    private final OnDayClickListener listener;
    private final SimpleDateFormat nameFormat = new SimpleDateFormat("EEE", Locale.getDefault());
    private final SimpleDateFormat numberFormat = new SimpleDateFormat("d", Locale.getDefault());

    public interface OnDayClickListener {
        void onDayClick(DayModel day);
    }

    public WeekDayAdapter(List<DayModel> days, OnDayClickListener listener) {
        this.days = days;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWeekDayBinding binding = ItemWeekDayBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DayModel day = days.get(position);
        holder.binding.tvDayName.setText(nameFormat.format(day.date));
        holder.binding.tvDayNumber.setText(numberFormat.format(day.date));

        if (day.isSelected) {
            holder.binding.tvDayNumber.setBackgroundResource(android.R.drawable.editbox_dropdown_light_frame);
            holder.binding.tvDayNumber.setTextColor(Color.BLACK);
        } else {
            holder.binding.tvDayNumber.setBackgroundColor(Color.TRANSPARENT);
            holder.binding.tvDayNumber.setTextColor(Color.WHITE);
        }

        holder.binding.indicatorPractice.setVisibility(day.hasPractice ? View.VISIBLE : View.GONE);
        holder.binding.indicatorAudio.setVisibility(day.hasAudio ? View.VISIBLE : View.GONE);
        holder.binding.indicatorVideo.setVisibility(day.hasVideo ? View.VISIBLE : View.GONE);
        holder.binding.indicatorExercise.setVisibility(day.hasExercise ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> listener.onDayClick(day));
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemWeekDayBinding binding;

        ViewHolder(ItemWeekDayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
