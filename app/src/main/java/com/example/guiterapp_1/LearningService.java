package com.example.guiterapp_1;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;

public class LearningService extends Service {

    private static final String CHANNEL_ID = "LearningServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final String TAG = "LearningService";

    private final IBinder binder = new LearningBinder();
    private boolean isLearning = false;
    private long startTimeMillis = 0;
    
    private MediaRecorder recorder = null;
    private String audioPath = null;
    private boolean isRecording = false;
    private boolean isPaused = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ServiceCallbacks callbacks;

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isLearning) {
                long elapsed = System.currentTimeMillis() - startTimeMillis;
                if (callbacks != null) {
                    callbacks.onTimerUpdate(elapsed);
                }
                // Schedule next update to be exactly on the next second boundary
                long nextDelay = 1000 - (elapsed % 1000);
                handler.postDelayed(this, nextDelay);
            }
        }
    };

    public interface ServiceCallbacks {
        void onTimerUpdate(long millis);
        void onRecordingStatusChanged(boolean recording, boolean cancelled);
        void onRecordingPausedChanged(boolean paused);
    }

    public class LearningBinder extends Binder {
        LearningService getService() {
            return LearningService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Mandatory startForeground call for Android 8+ when started via startForegroundService
        startForegroundServiceCompatible(isLearning ? "Learning session in progress..." : "Training ready");
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setCallbacks(ServiceCallbacks callbacks) {
        this.callbacks = callbacks;
        if (isLearning && callbacks != null) {
            callbacks.onTimerUpdate(System.currentTimeMillis() - startTimeMillis);
        }
    }

    public void startLearning() {
        if (isLearning) return;
        isLearning = true;
        startTimeMillis = System.currentTimeMillis();
        
        startForegroundServiceCompatible("Learning session in progress...");
        
        // Immediate update
        if (callbacks != null) {
            callbacks.onTimerUpdate(0);
        }
        
        handler.removeCallbacks(timerRunnable);
        handler.postDelayed(timerRunnable, 1000);
    }

    private void startForegroundServiceCompatible(String contentText) {
        Notification notification = getNotification(contentText);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                boolean hasMicPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
                // On Android 14+, microphone type requires permission.
                if (Build.VERSION.SDK_INT >= 34 && !hasMicPermission) {
                    startForeground(NOTIFICATION_ID, notification);
                } else {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
                }
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting foreground service", e);
            try {
                startForeground(NOTIFICATION_ID, notification);
            } catch (Exception ex) {
                Log.e(TAG, "Critical error starting foreground service", ex);
            }
        }
    }

    public void stopLearning() {
        isLearning = false;
        handler.removeCallbacks(timerRunnable);
        if (isRecording) {
            stopRecording(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        stopSelf();
    }

    public boolean isLearning() {
        return isLearning;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public void startRecording(String path) {
        if (isRecording) return;
        audioPath = path;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            recorder = new MediaRecorder(this);
        } else {
            recorder = new MediaRecorder();
        }

        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setOutputFile(audioPath);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setAudioSamplingRate(44100);
        recorder.setAudioEncodingBitRate(128000);

        try {
            recorder.prepare();
            recorder.start();
            isRecording = true;
            isPaused = false;
            if (callbacks != null) callbacks.onRecordingStatusChanged(true, false);
            updateNotification("Recording audio...");
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed", e);
            audioPath = null;
        }
    }

    public void pauseRecording() {
        if (isRecording && !isPaused && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                recorder.pause();
                isPaused = true;
                if (callbacks != null) callbacks.onRecordingPausedChanged(true);
                updateNotification("Recording paused");
            } catch (RuntimeException e) {
                Log.e(TAG, "pause() failed", e);
            }
        }
    }

    public void resumeRecording() {
        if (isRecording && isPaused && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                recorder.resume();
                isPaused = false;
                if (callbacks != null) callbacks.onRecordingPausedChanged(false);
                updateNotification("Recording audio...");
            } catch (RuntimeException e) {
                Log.e(TAG, "resume() failed", e);
            }
        }
    }

    public void stopRecording(boolean deleteFile) {
        if (!isRecording) return;
        if (recorder != null) {
            try {
                recorder.stop();
            } catch (RuntimeException e) {
                Log.e(TAG, "stop() failed", e);
            }
            recorder.release();
            recorder = null;
        }
        isRecording = false;
        isPaused = false;
        
        if (deleteFile && audioPath != null) {
            new File(audioPath).delete();
            audioPath = null;
        }
        
        if (callbacks != null) callbacks.onRecordingStatusChanged(false, deleteFile);
        updateNotification("Learning session in progress...");
    }

    public boolean isRecording() {
        return isRecording;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public String getAudioPath() {
        return audioPath;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Learning Session Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification getNotification(String contentText) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Guitar App")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .build();
    }

    private void updateNotification(String contentText) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, getNotification(contentText));
        }
    }
}
