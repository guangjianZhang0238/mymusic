package com.music.file.service;

import com.music.file.entity.TranscodingResult;

import java.io.File;

/**
 * 音频转码服务接口
 * 支持将DSF等高解析度音频格式转换为浏览器兼容的高质量格式
 */
public interface AudioTranscodingService {
    
    /**
     * 转码DSF文件为FLAC格式（保留最高音质）
     * @param sourceFile 源DSF文件
     * @param targetFile 目标FLAC文件
     * @return 转码结果
     */
    TranscodingResult transcodeDSFToFLAC(File sourceFile, File targetFile);
    
    /**
     * 转码DSF文件为AAC格式（平衡音质和文件大小）
     * @param sourceFile 源DSF文件
     * @param targetFile 目标AAC文件
     * @return 转码结果
     */
    TranscodingResult transcodeDSFToAAC(File sourceFile, File targetFile);
    
    /**
     * 自动选择最佳转码格式
     * @param sourceFile 源文件
     * @param targetDir 目标目录
     * @return 转码结果
     */
    TranscodingResult autoTranscode(File sourceFile, File targetDir);
    
    /**
     * 检查文件是否需要转码
     * @param file 待检查的文件
     * @return 是否需要转码
     */
    boolean needsTranscoding(File file);
    
    /**
     * 获取转码后的文件名
     * @param originalFileName 原始文件名
     * @param targetFormat 目标格式
     * @return 转码后的文件名
     */
    String getTranscodedFileName(String originalFileName, String targetFormat);

    /**
     * 是否启用自动转码（由配置 music.transcoding.auto-transcode 控制）
     * @return true 时上传与扫描中的 WAV/DSF 会尝试转码
     */
    boolean isTranscodingEnabled();

    /**
     * 检查 FFmpeg 是否可用（可执行且返回成功）
     * @return true 表示 ffmpeg 可正常调用
     */
    boolean isFfmpegAvailable();
}