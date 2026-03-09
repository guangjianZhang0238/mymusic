package com.music.file.service;

import com.music.file.entity.AudioInfo;

import java.io.File;

public interface AudioProcessService {
    
    AudioInfo analyzeAudio(File file);
    
    AudioInfo analyzeAudio(String filePath);
    
    boolean validateAudio(File file);
    
    boolean validateAudio(String filePath);
}
