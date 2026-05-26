package com.example.guiterapp_1;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.guiterapp_1.databinding.FragmentMetronomeBinding;

public class MetronomeFragment extends Fragment {

    private FragmentMetronomeBinding binding;
    private boolean isRunning = false;
    private int bpm = 120;
    private int beatsPerMeasure = 4;
    private int currentBeat = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private AudioTrack strongTrack;
    private AudioTrack weakTrack;

    private Runnable tickRunnable;
    private double nextTickTimeDouble;
    private double lastTickTimeDouble;

    private static final String[] TIME_SIGNATURES = {"2/4", "3/4", "4/4", "5/4", "6/8"};
    private static final int[] BEATS = {2, 3, 4, 5, 6};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMetronomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() != null) {
            getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
        }

        initAudioTracks();
        setupTimeSignatureSpinner();

        binding.btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });

        binding.sbBpm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                bpm = Math.max(1, progress);
                binding.tvBpm.setText(String.valueOf(bpm));
                if (isRunning) {
                    rescheduleNextTick();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        binding.btnMinus.setOnClickListener(v -> {
            if (bpm > 40) {
                bpm--;
                binding.sbBpm.setProgress(bpm);
            }
        });

        binding.btnPlus.setOnClickListener(v -> {
            if (bpm < 300) {
                bpm++;
                binding.sbBpm.setProgress(bpm);
            }
        });

        binding.btnStartStop.setOnClickListener(v -> toggleMetronome());

        tickRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    playTick();
                    flashIndicator();

                    currentBeat = (currentBeat + 1) % beatsPerMeasure;

                    lastTickTimeDouble = nextTickTimeDouble;
                    nextTickTimeDouble += getMsPerTick();

                    // Catch-up protection in case of system lag
                    long now = SystemClock.uptimeMillis();
                    if (nextTickTimeDouble < now - 1000) {
                        nextTickTimeDouble = now;
                    }

                    handler.postAtTime(this, (long) nextTickTimeDouble);
                }
            }
        };
    }

    private double getMsPerTick() {
        return (beatsPerMeasure == 6) ? 30000.0 / bpm : 60000.0 / bpm;
    }

    private void rescheduleNextTick() {
        if (!isRunning) return;
        handler.removeCallbacks(tickRunnable);
        
        long now = SystemClock.uptimeMillis();
        double msPerTick = getMsPerTick();

        nextTickTimeDouble = lastTickTimeDouble + msPerTick;

        if (nextTickTimeDouble < now) {
            nextTickTimeDouble = now;
        }
        
        handler.postAtTime(tickRunnable, (long) nextTickTimeDouble);
    }

    private void setupTimeSignatureSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, TIME_SIGNATURES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spTimeSignature.setAdapter(adapter);
        binding.spTimeSignature.setSelection(2);

        binding.spTimeSignature.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                beatsPerMeasure = BEATS[position];
                currentBeat = 0;
                if (isRunning) {
                    rescheduleNextTick();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void initAudioTracks() {
        int sampleRate = 44100;
        int durationMs = 20;

        byte[] strongData = generateClickData(sampleRate, 1000, durationMs);
        byte[] weakData = generateClickData(sampleRate, 700, durationMs);

        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        AudioFormat format = new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build();

        strongTrack = new AudioTrack(attrs, format, strongData.length, AudioTrack.MODE_STATIC, AudioManager.AUDIO_SESSION_ID_GENERATE);
        strongTrack.write(strongData, 0, strongData.length);

        weakTrack = new AudioTrack(attrs, format, weakData.length, AudioTrack.MODE_STATIC, AudioManager.AUDIO_SESSION_ID_GENERATE);
        weakTrack.write(weakData, 0, weakData.length);
    }

    private byte[] generateClickData(int sampleRate, int frequency, int durationMs) {
        double durationSec = durationMs / 1000.0;
        int numSamples = (int) (sampleRate * durationSec);
        byte[] buffer = new byte[2 * numSamples];

        for (int i = 0; i < numSamples; i++) {
            double time = (double) i / sampleRate;
            double signal = Math.sin(2.0 * Math.PI * frequency * time);
            double noise = (Math.random() * 2.0 - 1.0) * 0.3;
            signal = signal + noise;
            double envelope = Math.exp(-time * 400.0);
            signal = signal * envelope;

            if (signal > 1.0) signal = 1.0;
            if (signal < -1.0) signal = -1.0;

            short sample = (short) (signal * Short.MAX_VALUE);
            buffer[2 * i] = (byte) (sample & 0xff);
            buffer[2 * i + 1] = (byte) ((sample >> 8) & 0xff);
        }
        return buffer;
    }

    private void toggleMetronome() {
        if (isRunning) {
            stop();
        } else {
            start();
        }
    }

    private void start() {
        isRunning = true;
        currentBeat = 0;
        binding.btnStartStop.setText("STOP");
        
        nextTickTimeDouble = SystemClock.uptimeMillis();
        lastTickTimeDouble = nextTickTimeDouble;
        
        handler.postAtTime(tickRunnable, (long) nextTickTimeDouble);
    }

    private void stop() {
        isRunning = false;
        binding.btnStartStop.setText("START");
        handler.removeCallbacks(tickRunnable);
    }

    private void playTick() {
        AudioTrack track = (currentBeat == 0) ? strongTrack : weakTrack;
        if (track != null) {
            track.stop();
            track.reloadStaticData();
            track.play();
        }
    }

    private void flashIndicator() {
        binding.vBeatIndicator.setAlpha(1.0f);
        float scale = (currentBeat == 0) ? 1.5f : 1.0f;
        binding.vBeatIndicator.setScaleX(scale);
        binding.vBeatIndicator.setScaleY(scale);

        binding.vBeatIndicator.animate()
                .alpha(0.1f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(150)
                .start();
    }

    @Override
    public void onPause() {
        super.onPause();
        stop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (strongTrack != null) {
            strongTrack.release();
            strongTrack = null;
        }
        if (weakTrack != null) {
            weakTrack.release();
            weakTrack = null;
        }
        binding = null;
    }
}
