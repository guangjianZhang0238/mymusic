package com.music.common.utils;

import java.io.File;
import java.util.Arrays;
import com.music.common.constant.MusicConstants;

public class FileUtils {
    
    public static String getExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }
    
    public static String getBaseName(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return filename;
        }
        return filename.substring(0, lastDotIndex);
    }
    
    public static boolean isAudioFile(String filename) {
        String extension = getExtension(filename);
        return Arrays.asList(MusicConstants.SUPPORTED_AUDIO_FORMATS).contains(extension);
    }
    
    public static boolean isLyricsFile(String filename) {
        String extension = getExtension(filename);
        return Arrays.asList(MusicConstants.SUPPORTED_LYRICS_FORMATS).contains(extension);
    }
    
    public static boolean isImageFile(String filename) {
        String extension = getExtension(filename);
        return Arrays.asList(MusicConstants.SUPPORTED_IMAGE_FORMATS).contains(extension);
    }
    
    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }
    
    public static String formatDuration(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }
    
    public static String formatDuration(long milliseconds) {
        int totalSeconds = (int) (milliseconds / 1000);
        return formatDuration(totalSeconds);
    }
    
    public static void ensureDirectoryExists(String path) {
        File directory = new File(path);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }
    
    public static void ensureParentDirectoryExists(File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }
}
