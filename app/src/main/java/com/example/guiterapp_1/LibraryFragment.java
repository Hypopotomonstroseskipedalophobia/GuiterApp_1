package com.example.guiterapp_1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.guiterapp_1.databinding.FragmentLibraryBinding;

import java.util.ArrayList;
import java.util.List;

public class LibraryFragment extends Fragment {

    private FragmentLibraryBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLibraryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        List<LibraryMenuItem> items = new ArrayList<>();
        items.add(new LibraryMenuItem("Chords", android.R.drawable.ic_menu_agenda, new ChordLibraryFragment()));
        items.add(new LibraryMenuItem("Tuner", android.R.drawable.ic_menu_preferences, new TuningFragment()));
        items.add(new LibraryMenuItem("Metronome", android.R.drawable.ic_lock_idle_alarm, new MetronomeFragment()));
        items.add(new LibraryMenuItem("Scales", android.R.drawable.ic_menu_sort_by_size, new ScalesFragment()));
        items.add(new LibraryMenuItem("Circle of Fifths", android.R.drawable.ic_menu_compass, new CircleOfFifthsFragment()));

        LibraryMenuAdapter adapter = new LibraryMenuAdapter(items, item -> {
            if (item.getFragment() != null && item.getFragment().getClass() != Fragment.class) {
                getParentFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, item.getFragment())
                        .hide(this) // Hide the library menu
                        .addToBackStack(null)
                        .commit();
            }
        });

        binding.rvLibraryMenu.setLayoutManager(new GridLayoutManager(getContext(), 3));
        binding.rvLibraryMenu.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
