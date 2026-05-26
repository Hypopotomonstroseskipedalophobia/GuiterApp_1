package com.example.guiterapp_1;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.guiterapp_1.databinding.FragmentChordLibraryBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChordLibraryFragment extends Fragment {

    private FragmentChordLibraryBinding binding;
    private ChordAdapter chordAdapter;
    private NoteAdapter baseAdapter;
    
    // Cache the data statically so it's only loaded once during app lifetime
    private static JSONObject cachedChordData;
    private JSONObject chordData;

    private String selectedRoot = "C";
    private String selectedType = "maj";
    private String selectedBase = "None";

    private final List<String> rootNotes = Arrays.asList("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B");
    private final List<String> chordTypes = Arrays.asList("maj", "min", "7", "m7", "maj7", "sus4", "dim", "aug");
    private final List<String> baseNotes = Arrays.asList("None", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChordLibraryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });

        setupSelectors();
        setupChordList();
        
        loadChordData();
    }

    private void loadChordData() {
        if (cachedChordData != null) {
            this.chordData = cachedChordData;
            updateBaseNoteSelector();
            updateChordList();
            return;
        }

        new Thread(() -> {
            try {
                Context context = getContext();
                if (context == null) return;

                InputStream is = context.getAssets().open("chords.complete.json");
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                
                String json = new String(buffer, StandardCharsets.UTF_8);
                final JSONObject data = new JSONObject(json);
                cachedChordData = data;

                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        chordData = data;
                        updateBaseNoteSelector();
                        updateChordList();
                    });
                }
            } catch (IOException | JSONException e) {
                Log.e("ChordLibrary", "Error loading chord data", e);
            }
        }).start();
    }

    private void setupSelectors() {
        // Root Note Selector
        binding.rvRootNotes.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        NoteAdapter rootAdapter = new NoteAdapter(rootNotes, note -> {
            selectedRoot = note;
            // Deselect base if it matches new root
            if (selectedBase.equals(selectedRoot)) {
                selectedBase = "None";
            }
            updateBaseNoteSelector();
            updateChordList();
        });
        binding.rvRootNotes.setAdapter(rootAdapter);

        // Chord Type Selector
        binding.rvChordTypes.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        NoteAdapter typeAdapter = new NoteAdapter(chordTypes, type -> {
            selectedType = type;
            updateChordList();
        });
        binding.rvChordTypes.setAdapter(typeAdapter);

        // Base Note Selector
        binding.rvBaseNotes.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        baseAdapter = new NoteAdapter(new ArrayList<>(baseNotes), note -> {
            selectedBase = note;
            updateChordList();
        });
        binding.rvBaseNotes.setAdapter(baseAdapter);
    }

    private void updateBaseNoteSelector() {
        if (baseAdapter == null) return;
        
        List<String> filteredBaseNotes = new ArrayList<>();
        for (String note : baseNotes) {
            if (note.equals("None") || !note.equals(selectedRoot)) {
                filteredBaseNotes.add(note);
            }
        }
        baseAdapter.updateItems(filteredBaseNotes, selectedBase);
    }

    private void setupChordList() {
        chordAdapter = new ChordAdapter();
        binding.rvChords.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.rvChords.setAdapter(chordAdapter);
    }

    private void updateChordList() {
        if (chordData == null || binding == null) return;

        List<ChordPosition> chords = new ArrayList<>();
        String typeSuffix = "";
        switch (selectedType) {
            case "maj": typeSuffix = ""; break;
            case "min": typeSuffix = "m"; break;
            case "7": typeSuffix = "7"; break;
            case "m7": typeSuffix = "m7"; break;
            case "maj7": typeSuffix = "maj7"; break;
            case "sus4": typeSuffix = "sus4"; break;
            case "dim": typeSuffix = "dim"; break;
            case "aug": typeSuffix = "aug"; break;
        }

        String key = selectedRoot + typeSuffix;
        if (!selectedBase.equals("None")) {
            key += "/" + selectedBase;
        }
        
        try {
            if (chordData.has(key)) {
                JSONArray positionsArray = chordData.getJSONArray(key);
                for (int i = 0; i < positionsArray.length(); i++) {
                    JSONObject posObj = positionsArray.getJSONObject(i);
                    if (posObj.isNull("positions")) continue;

                    JSONArray fretsJson = posObj.getJSONArray("positions");
                    
                    int[] frets = new int[6];
                    for (int j = 0; j < 6; j++) {
                        String f = fretsJson.getString(j);
                        if (f.equalsIgnoreCase("x")) {
                            frets[j] = -1;
                        } else {
                            frets[j] = Integer.parseInt(f);
                        }
                    }
                    
                    String displayName = selectedRoot + " " + selectedType;
                    if (!selectedBase.equals("None")) {
                        displayName += "/" + selectedBase;
                    }
                    if (positionsArray.length() > 1) {
                        displayName += " (Pos " + (i + 1) + ")";
                    }
                    
                    chords.add(new ChordPosition(displayName, frets));
                }
            }
        } catch (JSONException e) {
            Log.e("ChordLibrary", "Error parsing chord position for key: " + key, e);
        }

        chordAdapter.setChords(chords);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
