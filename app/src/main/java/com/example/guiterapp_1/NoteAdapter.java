package com.example.guiterapp_1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private List<String> items;
    private final OnNoteSelectedListener listener;
    private int selectedPosition = 0;

    public interface OnNoteSelectedListener {
        void onItemSelected(String item);
    }

    public NoteAdapter(List<String> items, OnNoteSelectedListener listener) {
        this.items = new ArrayList<>(items);
        this.listener = listener;
    }

    public void updateItems(List<String> newItems, String currentSelected) {
        this.items = new ArrayList<>(newItems);
        selectedPosition = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).equals(currentSelected)) {
                selectedPosition = i;
                break;
            }
        }
        if (selectedPosition == -1 && !items.isEmpty()) {
            selectedPosition = 0;
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note_chip, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        String item = items.get(position);
        holder.chip.setText(item);
        holder.chip.setChecked(position == selectedPosition);
        
        holder.chip.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION && currentPos != selectedPosition) {
                int oldPos = selectedPosition;
                selectedPosition = currentPos;
                notifyItemChanged(oldPos);
                notifyItemChanged(selectedPosition);
                listener.onItemSelected(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        Chip chip;
        NoteViewHolder(View itemView) {
            super(itemView);
            chip = (Chip) itemView;
        }
    }
}
