package com.example.guiterapp_1;

import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.guiterapp_1.databinding.ActivityMainBinding;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MediaPlayer mediaPlayer;
    private Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable updateProgressRunnable;

    private Fragment libraryFragment;
    private Fragment learningFragment;
    private Fragment profileFragment;
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            binding.bottomNavigation.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        setupAudioPlayer();

        if (!isSetupComplete()) {
            showGreeting();
        } else {
            showMainContent();
        }
    }

    private boolean isSetupComplete() {
        SharedPreferences pref = getSharedPreferences("GuitarApp", MODE_PRIVATE);
        return pref.getBoolean("setup_complete", false);
    }

    private void showGreeting() {
        binding.bottomNavigation.setVisibility(View.GONE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new GreetingFragment())
                .commit();
    }

    public void showMainContent() {
        libraryFragment = new LibraryFragment();
        learningFragment = new LearningFragment();
        profileFragment = new ProfileFragment();

        activeFragment = learningFragment;

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, libraryFragment, "LIBRARY").hide(libraryFragment).commit();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, profileFragment, "PROFILE").hide(profileFragment).commit();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, learningFragment, "LEARNING").commit();

        binding.bottomNavigation.setSelectedItemId(R.id.page_2);
        setupNavigation();
        updateUIForFragment();
    }

    private void setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment target = null;

            if (id == R.id.page_1) target = libraryFragment;
            else if (id == R.id.page_2) target = learningFragment;
            else if (id == R.id.page_3) target = profileFragment;

            if (target != null && target != activeFragment) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

                Fragment currentVisible = getVisibleFragment();
                if (currentVisible != null) {
                    ft.hide(currentVisible);
                }

                if (target == libraryFragment) {
                    Fragment sub = getTopSubFragment();
                    if (sub != null) {
                        ft.show(sub);
                    } else {
                        ft.show(target);
                    }
                } else {
                    ft.show(target);
                }

                ft.commit();
                activeFragment = target;
                updateUIForFragment();
                return true;
            }
            return false;
        });
    }

    public void setBottomNavigationVisibility(int visibility) {
        if (binding != null && binding.bottomNavigation != null) {
            binding.bottomNavigation.setVisibility(visibility);
        }
    }

    private Fragment getVisibleFragment() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment != null && fragment.isAdded() && fragment.isVisible())
                return fragment;
        }
        return null;
    }

    private Fragment getTopSubFragment() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        for (int i = fragments.size() - 1; i >= 0; i--) {
            Fragment f = fragments.get(i);
            if (f instanceof ChordLibraryFragment || f instanceof TuningFragment ||
                f instanceof CircleOfFifthsFragment || f instanceof MetronomeFragment) {
                return f;
            }
        }
        return null;
    }

    private void updateUIForFragment() {
        binding.bottomNavigation.setVisibility(View.VISIBLE);
    }

    private void setupAudioPlayer() {
        binding.btnPlayPause.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                } else {
                    mediaPlayer.start();
                    binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                    updateProgress();
                }
            }
        });

        binding.btnStop.setOnClickListener(v -> stopAudio());

        updateProgressRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    binding.audioProgress.setProgress(mediaPlayer.getCurrentPosition());
                    progressHandler.postDelayed(this, 1000);
                }
            }
        };
    }

    public void playAudio(String path, String title) {
        stopAudio() ;
        mediaPlayer = new MediaPlayer();
        try {
            if (path.startsWith("content://") || path.startsWith("http")) {
                mediaPlayer.setDataSource(this, Uri.parse(path));
            } else {
                mediaPlayer.setDataSource(path);
            }
            mediaPlayer.prepare();
            mediaPlayer.start();
            binding.audioPlayerBar.setVisibility(View.VISIBLE);
            binding.audioTitle.setText(title != null ? title : "Playing audio...");
            binding.audioProgress.setMax(mediaPlayer.getDuration());
            binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            updateProgress();
            mediaPlayer.setOnCompletionListener(mp -> stopAudio());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateProgress() {
        progressHandler.removeCallbacks(updateProgressRunnable);
        progressHandler.post(updateProgressRunnable);
    }

    public void stopAudio() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (binding != null) {
            binding.audioPlayerBar.setVisibility(View.GONE);
        }
        progressHandler.removeCallbacks(updateProgressRunnable);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else if (activeFragment != learningFragment && activeFragment != null) {
            binding.bottomNavigation.setSelectedItemId(R.id.page_2);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAudio();
    }
}
