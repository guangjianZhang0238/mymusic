package com.music.file.entity;

import lombok.Data;

/**
 * 转码结果实体
 */
@Data
public class TranscodingResult {
    
    /**
     * 转码是否成功
     */
    private boolean success;
    
    /**
     * 源文件路径
     */
    private String sourcePath;
    
    /**
     * 目标文件路径
     */
    private String targetPath;
    
    /**
     * 源文件格式
     */
    private String sourceFormat;
    
    /**
     * 目标文件格式
     */
    private String targetFormat;
    
    /**
     * 转码耗时（毫秒）
     */
    private long durationMs;
    
    /**
     * 源文件大小（字节）
     */
    private long sourceSize;
    
    /**
     * 目标文件大小（字节）
     */
    private long targetSize;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * FFmpeg命令输出
     */
    private String ffmpegOutput;
    
    /**
     * 是否已删除源文件
     */
    private boolean sourceDeleted;
    
    public TranscodingResult() {
        this.success = false;
        this.durationMs = 0;
        this.sourceSize = 0;
        this.targetSize = 0;
        this.sourceDeleted = false;
    }
    
    public static TranscodingResult success(String sourcePath, String targetPath, 
                                          String sourceFormat, String targetFormat,
                                          long durationMs, long sourceSize, long targetSize) {
        TranscodingResult result = new TranscodingResult();
        result.success = true;
        result.sourcePath = sourcePath;
        result.targetPath = targetPath;
        result.sourceFormat = sourceFormat;
        result.targetFormat = targetFormat;
        result.durationMs = durationMs;
        result.sourceSize = sourceSize;
        result.targetSize = targetSize;
        return result;
    }
    
    public static TranscodingResult failure(String sourcePath, String errorMessage) {
        TranscodingResult result = new TranscodingResult();
        result.success = false;
        result.sourcePath = sourcePath;
        result.errorMessage = errorMessage;
        return result;
    }
    
    /**
     * 格式化转码信息
     */
    public String formatInfo() {
        if (success) {
            double ratio = targetSize > 0 ? (double) targetSize / sourceSize : 0;
            return String.format("转码成功: %s -> %s (%.1fMB -> %.1fMB, %.1f%%)", 
                               sourceFormat, targetFormat,
                               sourceSize / 1024.0 / 1024.0,
                               targetSize / 1024.0 / 1024.0,
                               ratio * 100);
        } else {
            return String.format("转码失败: %s - %s", sourcePath, errorMessage);
        }
    }
}