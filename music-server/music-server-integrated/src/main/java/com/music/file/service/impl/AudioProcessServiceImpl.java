package com.music.file.service.impl;

import com.music.common.constant.MusicConstants;
import com.music.common.exception.BusinessException;
import com.music.file.entity.AudioInfo;
import com.music.file.service.AudioProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;

@Slf4j
@Service
public class AudioProcessServiceImpl implements AudioProcessService {
    
    @Override
    public AudioInfo analyzeAudio(File file) {
        try {
            AudioInfo audioInfo = new AudioInfo();
            audioInfo.setFileSize(file.length());
            audioInfo.setDurationMs(0L); // 暂时设为0
            audioInfo.setDurationSec(0); // 暂时设为0
            
            String fileName = file.getName();
            String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
            audioInfo.setFormat(extension);
            
            // 估算位深
            if ("wav".equals(extension) || "flac".equals(extension)) {
                audioInfo.setBitDepth(24); // 默认24位
            } else if ("dsf".equals(extension)) {
                audioInfo.setBitDepth(1); // DSD使用1位
                audioInfo.setSampleRate(getDSFSampleRate(file)); // 获取实际采样率
            } else {
                audioInfo.setBitDepth(16); // 默认16位
            }
            
            return audioInfo;
        } catch (Exception e) {
            log.error("音频分析失败: {}", e.getMessage());
            throw BusinessException.of("音频分析失败: " + e.getMessage());
        }
    }
    
    @Override
    public AudioInfo analyzeAudio(String filePath) {
        return analyzeAudio(new File(filePath));
    }
    
    @Override
    public boolean validateAudio(File file) {
        try {
            AudioInfo info = analyzeAudio(file);
            
            // 验证格式
            String format = info.getFormat();
            if (!Arrays.asList(MusicConstants.SUPPORTED_AUDIO_FORMATS).contains(format)) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean validateAudio(String filePath) {
        return validateAudio(new File(filePath));
    }
    
    /**
     * 获取DSF文件的实际采样率
     * @param file DSF文件
     * @return 采样率(Hz)
     */
    private int getDSFSampleRate(File file) {
        try {
            // DSF文件头部包含采样率信息
            // 偏移量0x1C处存储4字节的采样率值
            byte[] header = new byte[32];
            try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                if (fis.read(header) >= 32) {
                    // 检查DSF标识符 "DSD "
                    if (header[0] == 'D' && header[1] == 'S' && header[2] == 'D' && header[3] == ' ') {
                        // 从偏移量0x1C读取4字节采样率
                        int sampleRate = ((header[28] & 0xFF) | 
                                        ((header[29] & 0xFF) << 8) | 
                                        ((header[30] & 0xFF) << 16) | 
                                        ((header[31] & 0xFF) << 24));
                        return sampleRate;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("读取DSF文件采样率失败: {}", e.getMessage());
        }
        // 默认返回DSD64的采样率
        return 2822400;
    }
}