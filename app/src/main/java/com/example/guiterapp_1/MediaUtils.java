package com.example.guiterapp_1;

import android.content.Context;
import java.io.File;

public class MediaUtils {
    private static final String BASE_FOLDER = "GAMedia";
    private static final String AUDIO_FOLDER = "Audio";
    private static final String VIDEO_FOLDER = "Video";

    public static File getMediaBaseDir(Context context) {
        File baseDir = new File(context.getExternalFilesDir(null), BASE_FOLDER);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        return baseDir;
    }

    public static File getAudioDir(Context context) {
        File audioDir = new File(getMediaBaseDir(context), AUDIO_FOLDER);
        if (!audioDir.exists()) {
            audioDir.mkdirs();
        }
        return audioDir;
    }

    public static File getVideoDir(Context context) {
        File videoDir = new File(getMediaBaseDir(context), VIDEO_FOLDER);
        if (!videoDir.exists()) {
            videoDir.mkdirs();
        }
        return videoDir;
    }

    public static String getNewAudioPath(Context context, String prefix) {
        return new File(getAudioDir(context), prefix + "_" + System.currentTimeMillis() + ".mp4").getAbsolutePath();
    }

    public static String getNewVideoPath(Context context, String prefix) {
        return new File(getVideoDir(context), prefix + "_" + System.currentTimeMillis() + ".mp4").getAbsolutePath();
    }
}
