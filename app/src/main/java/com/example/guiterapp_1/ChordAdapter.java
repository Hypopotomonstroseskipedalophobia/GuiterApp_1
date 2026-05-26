package com.example.guiterapp_1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ChordAdapter extends RecyclerView.Adapter<ChordAdapter.ChordViewHolder> {

    private List<ChordPosition> chords = new ArrayList<>();

    public void setChords(List<ChordPosition> chords) {
        this.chords = chords;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chord_card, parent, false);
        return new ChordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChordViewHolder holder, int position) {
        holder.bind(chords.get(position));
    }

    @Override
    public int getItemCount() {
        return chords.size();
    }

    static class ChordViewHolder extends RecyclerView.ViewHolder {
        private final ChordDiagramView chordDiagramView;

        public ChordViewHolder(@NonNull View itemView) {
            super(itemView);
            chordDiagramView = itemView.findViewById(R.id.chord_diagram_view);
        }

        public void bind(ChordPosition chord) {
            chordDiagramView.setChordData(chord.getFrets(), chord.getName());
        }
    }
}
