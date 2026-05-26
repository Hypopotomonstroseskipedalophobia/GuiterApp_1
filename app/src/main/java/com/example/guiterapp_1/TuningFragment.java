package com.example.guiterapp_1;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.guiterapp_1.databinding.FragmentTuningBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class TuningFragment extends Fragment {

    private FragmentTuningBinding binding;
    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SIZE = 4096;

    private AudioRecord audioRecord;
    private Thread recordingThread;
    private final AtomicBoolean isRecording = new AtomicBoolean(false);

    private static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

    private static final String[] TUNING_NAMES = {
            "E Standard", "Eb Standard", "D Standard", "Drop C", "Drop B",
            "Open G", "Open D", "Open C", "Open D Minor"
    };

    private static final double[][] FREQS_6 = {
            {82.41, 110.00, 146.83, 196.00, 246.94, 329.63}, // E Standard
            {77.78, 103.83, 138.59, 185.00, 233.08, 311.13}, // Eb Standard
            {73.42, 98.00, 130.81, 174.61, 220.00, 293.66},  // D Standard
            {65.41, 98.00, 130.81, 174.61, 220.00, 293.66},  // Drop C
            {61.74, 92.50, 123.47, 164.81, 207.65, 277.18},  // Drop B
            {73.42, 98.00, 146.83, 196.00, 246.94, 293.66},  // Open G
            {73.42, 110.00, 146.83, 185.00, 220.00, 293.66}, // Open D
            {65.41, 98.00, 130.81, 196.00, 261.63, 329.63},  // Open C
            {73.42, 110.00, 146.83, 174.61, 220.00, 293.66}  // Open D Minor
    };

    private static final String[][] LABELS_6 = {
            {"E2", "A2", "D3", "G3", "B3", "E4"},
            {"Eb2", "Ab2", "Db3", "Gb3", "Bb3", "Eb4"},
            {"D2", "G2", "C3", "F3", "A3", "D4"},
            {"C2", "G2", "C3", "F3", "A3", "D4"},
            {"B1", "F#2", "B2", "E3", "G#3", "C#4"},
            {"D2", "G2", "D3", "G3", "B3", "D4"},
            {"D2", "A2", "D3", "F#3", "A3", "D4"},
            {"C2", "G2", "C3", "G3", "C4", "E4"},
            {"D2", "A2", "D3", "F3", "A3", "D4"}
    };

    private static final double[] FREQS_7TH = {61.74, 58.27, 55.00, 49.00, 46.25, 55.00, 55.00, 49.00, 55.00};
    private static final String[] LABELS_7TH = {"B1", "Bb1", "A1", "G1", "F#1", "A1", "A1", "G1", "A1"};

    private TextView[] stringViews;
    private double[] currentTargetFreqs;
    private String[] currentTargetLabels;

    private final List<Double> frequencyBuffer = new ArrayList<>();
    private static final int SMOOTHING_WINDOW_SIZE = 5;
    private static final double NOISE_THRESHOLD = 200.0;
    private static final double TUNED_TOLERANCE = 0.5;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTuningBinding.inflate(inflater, container, false);
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

        stringViews = new TextView[]{
                binding.string1, binding.string2, binding.string3,
                binding.string4, binding.string5, binding.string6, binding.string7
        };

        setupTuningSpinner();

        binding.startButton.setOnClickListener(v -> {
            if (isRecording.get()) {
                stopTuner();
            } else {
                startTuner();
            }
        });

        binding.keepNoteSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                clearUI();
            }
        });

        binding.sevenStringSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateTuningList();
        });

        updateTuningList();
    }

    private void setupTuningSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, TUNING_NAMES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.tuningSpinner.setAdapter(adapter);

        binding.tuningSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTuningList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateTuningList() {
        if (binding == null) return;
        int tuningIdx = binding.tuningSpinner.getSelectedItemPosition();
        boolean is7String = binding.sevenStringSwitch.isChecked();

        int numStrings = is7String ? 7 : 6;
        currentTargetFreqs = new double[numStrings];
        currentTargetLabels = new String[numStrings];

        if (is7String) {
            currentTargetFreqs[0] = FREQS_7TH[tuningIdx];
            currentTargetLabels[0] = LABELS_7TH[tuningIdx];
            System.arraycopy(FREQS_6[tuningIdx], 0, currentTargetFreqs, 1, 6);
            System.arraycopy(LABELS_6[tuningIdx], 0, currentTargetLabels, 1, 6);
            binding.string7.setVisibility(View.VISIBLE);
        } else {
            System.arraycopy(FREQS_6[tuningIdx], 0, currentTargetFreqs, 0, 6);
            System.arraycopy(LABELS_6[tuningIdx], 0, currentTargetLabels, 0, 6);
            binding.string7.setVisibility(View.GONE);
        }

        // Reset text and colors
        for (int i = 0; i < 7; i++) {
            if (i < numStrings) {
                TextView tv = is7String ? stringViews[6-i] : stringViews[5-i];
                tv.setText(String.format("%s (%.1f)", currentTargetLabels[i], currentTargetFreqs[i]));
                tv.setTextColor(Color.WHITE);
            }
        }
    }

    private void startTuner() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 101);
            return;
        }

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE * 2);

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Toast.makeText(getContext(), "Failed to initialize AudioRecord", Toast.LENGTH_SHORT).show();
            return;
        }

        audioRecord.startRecording();
        isRecording.set(true);
        binding.startButton.setText("Stop Tuner");

        recordingThread = new Thread(this::processAudio);
        recordingThread.start();
    }

    private void stopTuner() {
        isRecording.set(false);
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if (binding != null) {
            binding.startButton.setText("Start Tuner");
        }
        clearUI();
        frequencyBuffer.clear();
    }

    private void clearUI() {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            if (binding == null) return;
            binding.noteText.setText("--");
            binding.prevNoteText.setText("");
            binding.nextNoteText.setText("");
            binding.frequencyText.setText("0.0 Hz");
            binding.tunerBar.setProgress(50);
            updateTuningList();
        });
    }

    private void processAudio() {
        short[] buffer = new short[BUFFER_SIZE];
        while (isRecording.get()) {
            AudioRecord record = audioRecord;
            if (record == null) break;

            int readSize = record.read(buffer, 0, BUFFER_SIZE);
            if (readSize > 0) {
                double amplitude = 0;
                for (short s : buffer) amplitude += Math.abs(s);
                amplitude /= readSize;

                if (amplitude > NOISE_THRESHOLD) {
                    double frequency = calculateFrequency(buffer, readSize);
                    if (frequency > 20 && frequency < 2000) {
                        updateUI(smoothFrequency(frequency));
                    }
                } else {
                    FragmentTuningBinding b = binding;
                    if (b != null && !b.keepNoteSwitch.isChecked()) {
                        clearUI();
                    }
                }
            }
        }
    }

    private double smoothFrequency(double frequency) {
        frequencyBuffer.add(frequency);
        if (frequencyBuffer.size() > SMOOTHING_WINDOW_SIZE) {
            frequencyBuffer.remove(0);
        }
        List<Double> sorted = new ArrayList<>(frequencyBuffer);
        Collections.sort(sorted);
        return sorted.get(sorted.size() / 2);
    }

    private double calculateFrequency(short[] buffer, int n) {
        double mean = 0;
        for (int i = 0; i < n; i++) mean += buffer[i];
        mean /= n;

        double[] result = new double[n];
        for (int tau = 0; tau < n; tau++) {
            for (int i = 0; i < n - tau; i++) {
                result[tau] += (double) (buffer[i] - mean) * (buffer[i + tau] - mean);
            }
        }

        int startSearch = 0;
        while (startSearch < n - 1 && result[startSearch] >= result[startSearch + 1]) {
            startSearch++;
        }

        int period = -1;
        for (int i = startSearch; i < n - 1; i++) {
            if (i > 0 && result[i] > result[i - 1] && result[i] > result[i + 1]) {
                if (period == -1 || result[i] > result[period]) {
                    period = i;
                }
            }
        }

        if (period > 0 && period < n - 1) {
            // Parabolic interpolation for sub-sample precision
            double y0 = result[period - 1];
            double y1 = result[period];
            double y2 = result[period + 1];

            double denominator = 2 * (2 * y1 - y2 - y0);
            if (Math.abs(denominator) > 1e-6) {
                double refinedPeriod = period + (y2 - y0) / denominator;
                return (double) SAMPLE_RATE / refinedPeriod;
            }
            return (double) SAMPLE_RATE / period;
        }
        return 0;
    }

    private void updateUI(double frequency) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (binding == null) return;
            binding.frequencyText.setText(String.format("%.1f Hz", frequency));

            double midi = 69 + 12 * (Math.log(frequency / 440.0) / Math.log(2));
            int roundedMidi = (int) Math.round(midi);
            double centsOff = (midi - roundedMidi) * 100;

            if (roundedMidi >= 0 && roundedMidi < 128) {
                binding.noteText.setText(NOTE_NAMES[roundedMidi % 12]);
                binding.prevNoteText.setText(NOTE_NAMES[(roundedMidi - 1 + 12) % 12]);
                binding.nextNoteText.setText(NOTE_NAMES[(roundedMidi + 1) % 12]);

                int progress = (int) (centsOff + 50);
                if (progress < 0) progress = 0;
                if (progress > 100) progress = 100;
                binding.tunerBar.setProgress(progress);

                checkStringTuning(frequency);
            }
        });
    }

    private void checkStringTuning(double currentFreq) {
        if (binding == null) return;
        boolean is7String = binding.sevenStringSwitch.isChecked();
        int numStrings = is7String ? 7 : 6;

        for (int i = 0; i < numStrings; i++) {
            TextView tv = is7String ? stringViews[6-i] : stringViews[5-i];
            if (Math.abs(currentFreq - currentTargetFreqs[i]) <= TUNED_TOLERANCE) {
                tv.setText(String.format("%s (%.1f) ✓", currentTargetLabels[i], currentTargetFreqs[i]));
                tv.setTextColor(Color.GREEN);
            } else if (Math.abs(currentFreq - currentTargetFreqs[i]) < 10) {
                tv.setTextColor(Color.CYAN);
            } else {
                tv.setTextColor(Color.WHITE);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopTuner();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startTuner();
        } else {
            Toast.makeText(getContext(), "Permission denied to record audio", Toast.LENGTH_SHORT).show();
        }
    }
}
