package com.example.guiterapp_1;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.guiterapp_1.data.AppDatabase;
import com.example.guiterapp_1.data.Instrument;
import com.example.guiterapp_1.databinding.FragmentInstrumentSelectionBinding;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class InstrumentSelectionFragment extends Fragment {

    private FragmentInstrumentSelectionBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentInstrumentSelectionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Hide bottom navigation when selecting instruments
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }

        binding.btnAddCustom.setOnClickListener(v -> addCustomInstrument());

        binding.btnContinue.setOnClickListener(v -> {
            saveSelection();
        });
    }

    private void addCustomInstrument() {
        String name = binding.etOtherInstrument.getText().toString().trim();
        if (name.isEmpty()) {
            binding.tilOtherInstrument.setError("Enter instrument name");
            return;
        }

        binding.tilOtherInstrument.setError(null);
        
        // Create new chip
        Chip chip = new Chip(requireContext());
        chip.setText(name);
        chip.setCheckable(true);
        chip.setChecked(true);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> binding.cgInstruments.removeView(chip));
        
        // Use a style if defined, or just let it be default
        // In XML we used @style/Widget.Material3.Chip.Filter
        
        binding.cgInstruments.addView(chip);
        binding.etOtherInstrument.setText("");
    }

    private void saveSelection() {
        List<String> selectedNames = new ArrayList<>();
        for (int i = 0; i < binding.cgInstruments.getChildCount(); i++) {
            View child = binding.cgInstruments.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                if (chip.isChecked()) {
                    selectedNames.add(chip.getText().toString());
                }
            }
        }

        if (selectedNames.isEmpty()) {
            Toast.makeText(getContext(), "Please select at least one instrument", Toast.LENGTH_SHORT).show();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            
            // Optionally clear existing or just append. 
            // Since this is first time setup, we can just insert.
            for (String name : selectedNames) {
                db.instrumentDao().insert(new Instrument(name));
            }

            SharedPreferences pref = requireContext().getSharedPreferences("GuitarApp", Context.MODE_PRIVATE);
            pref.edit().putBoolean("setup_complete", true).apply();

            if (getActivity() != null) {
                getActivity().runOnUiThread(this::finishSetup);
            }
        });
    }

    private void finishSetup() {
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            activity.setBottomNavigationVisibility(View.VISIBLE);
            
            // Navigate back or to main content
            getParentFragmentManager().beginTransaction()
                    .remove(this)
                    .commit();
            
            activity.showMainContent();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
