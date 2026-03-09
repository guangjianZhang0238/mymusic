package com.music.file.entity;

import lombok.Data;

@Data
public class AudioInfo {
    
    private String format;
    private Integer sampleRate;
    private Integer bitDepth;
    private Integer bitRate;
    private Integer channels;
    private Long durationMs;
    private Integer durationSec;
    private Long fileSize;
    private String codec;
    
    @Override
    public String toString() {
        return String.format("%s - %dHz/%dbits - %d channels - %s", 
                format, sampleRate, bitDepth, channels, formatDuration());
    }
    
    public String formatDuration() {
        if (durationSec == null) return "0:00";
        int minutes = durationSec / 60;
        int seconds = durationSec % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
