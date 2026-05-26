package com.example.guiterapp_1;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.guiterapp_1.databinding.FragmentScalesBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScalesFragment extends Fragment {

    private FragmentScalesBinding binding;
    private String selectedRoot = "C";
    private String selectedScaleType = "Major";

    private final List<String> rootNotes = Arrays.asList("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B");
    private final List<String> scaleTypes = Arrays.asList("Major", "Minor", "Maj Pentatonic", "Min Pentatonic", "Blues", "Dorian", "Mixolydian");

    // Intervals in semitones
    private final int[] majorIntervals = {0, 2, 4, 5, 7, 9, 11};
    private final int[] minorIntervals = {0, 2, 3, 5, 7, 8, 10};
    private final int[] majPentatonicIntervals = {0, 2, 4, 7, 9};
    private final int[] minPentatonicIntervals = {0, 3, 5, 7, 10};
    private final int[] bluesIntervals = {0, 3, 5, 6, 7, 10};
    private final int[] dorianIntervals = {0, 2, 3, 5, 7, 9, 10};
    private final int[] mixolydianIntervals = {0, 2, 4, 5, 7, 9, 10};

    // Open string notes (String indices: 0=High E, 1=B, 2=G, 3=D, 4=A, 5=Low E)
    // E=4, B=11, G=7, D=2, A=9, E=4 (indices in rootNotes)
    private final int[] openStringNotes = {4, 11, 7, 2, 9, 4};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentScalesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        setupSelectors();
        updateScale();
    }

    private void setupSelectors() {
        binding.rvRootNotes.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        NoteAdapter rootAdapter = new NoteAdapter(rootNotes, note -> {
            selectedRoot = note;
            updateScale();
        });
        binding.rvRootNotes.setAdapter(rootAdapter);

        binding.rvScaleTypes.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        NoteAdapter typeAdapter = new NoteAdapter(scaleTypes, type -> {
            selectedScaleType = type;
            updateScale();
        });
        binding.rvScaleTypes.setAdapter(typeAdapter);
    }

    private void updateScale() {
        if (binding == null) return;

        int rootValue = rootNotes.indexOf(selectedRoot);
        int[] intervals = getIntervals(selectedScaleType);
        
        List<Integer> scaleNotes = new ArrayList<>();
        for (int interval : intervals) {
            scaleNotes.add((rootValue + interval) % 12);
        }

        List<GuitarNeckView.NoteMarker> markers = new ArrayList<>();

        for (int s = 0; s < 6; s++) {
            int openNote = openStringNotes[s];
            for (int f = 0; f <= 24; f++) {
                int currentNote = (openNote + f) % 12;
                if (scaleNotes.contains(currentNote)) {
                    String label = rootNotes.get(currentNote);
                    int color = (currentNote == rootValue) ? Color.parseColor("#FFD700") : Color.CYAN; // Gold for root
                    markers.add(new GuitarNeckView.NoteMarker(s, f, label, color));
                }
            }
        }

        binding.guitarNeckView.setMarkers(markers);
        String info = selectedRoot + " " + selectedScaleType;
        binding.tvScaleInfo.setText(info);
    }

    private int[] getIntervals(String type) {
        switch (type) {
            case "Minor": return minorIntervals;
            case "Maj Pentatonic": return majPentatonicIntervals;
            case "Min Pentatonic": return minPentatonicIntervals;
            case "Blues": return bluesIntervals;
            case "Dorian": return dorianIntervals;
            case "Mixolydian": return mixolydianIntervals;
            default: return majorIntervals;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
