package com.music.file.service.impl;

import com.music.common.constant.MusicConstants;
import com.music.file.entity.TranscodingResult;
import com.music.file.service.AudioTranscodingService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * 音频转码服务实现
 * 使用FFmpeg进行高质量音频转码
 */
@Slf4j
@Service
public class AudioTranscodingServiceImpl implements AudioTranscodingService {
    
    // FFmpeg可执行文件路径，默认从系统 PATH 中查找 "ffmpeg"
    @Value("${music.transcoding.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;
    
    // 是否在转码后删除源文件
    @Value("${music.transcoding.delete-source:true}")
    private boolean deleteSourceAfterTranscoding;

    // 是否启用自动转码（上传与扫描中的 WAV/DSF）
    @Value("${music.transcoding.auto-transcode:true}")
    private boolean autoTranscodeEnabled;

    @PostConstruct
    public void checkFfmpegOnStartup() {
        if (!isFfmpegAvailable()) {
            log.warn("FFmpeg 不可用，转码将失败: path={}", ffmpegPath);
        }
    }

    @Override
    public boolean isTranscodingEnabled() {
        return autoTranscodeEnabled;
    }

    @Override
    public boolean isFfmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(ffmpegPath, "-version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            boolean finished = p.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                return false;
            }
            return p.exitValue() == 0;
        } catch (Exception e) {
            log.debug("FFmpeg 可用性检查失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public TranscodingResult transcodeDSFToFLAC(File sourceFile, File targetFile) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 验证源文件
            if (!sourceFile.exists() || !sourceFile.isFile()) {
                return TranscodingResult.failure(sourceFile.getAbsolutePath(), "源文件不存在");
            }
            
            // 确保目标目录存在
            targetFile.getParentFile().mkdirs();
            
            // 构建FFmpeg命令 - DSF到FLAC的高质量转码
            ProcessBuilder processBuilder = new ProcessBuilder(
                ffmpegPath,
                "-i", sourceFile.getAbsolutePath(),           // 输入文件
                "-c:a", "flac",                               // 音频编码器：FLAC
                "-compression_level", "8",                    // 压缩级别（12可能导致问题，改为8）
                "-sample_fmt", "s32",                         // 32位采样格式
                "-ar", "96000",                               // 目标采样率96kHz（更稳定）
                "-threads", "0",                              // 自动线程数
                "-y",                                         // 覆盖输出文件
                targetFile.getAbsolutePath()                  // 输出文件
            );
            
            log.info("开始转码DSF到FLAC: {} -> {}", sourceFile.getName(), targetFile.getName());
            
            // 执行转码
            Process process = processBuilder.start();
            
            // 异步读取FFmpeg输出，避免缓冲区阻塞
            StringBuilder outputBuilder = new StringBuilder();
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        outputBuilder.append(line).append("\n");
                        log.debug("FFmpeg输出: {}", line);
                    }
                } catch (IOException e) {
                    log.warn("读取FFmpeg输出时发生错误: {}", e.getMessage());
                }
            });
            
            Thread errorThread = new Thread(() -> {
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        log.debug("FFmpeg错误输出: {}", line);
                    }
                } catch (IOException e) {
                    log.warn("读取FFmpeg错误输出时发生错误: {}", e.getMessage());
                }
            });
            
            outputThread.start();
            errorThread.start();
            
            // 等待转码完成（最多30分钟）
            boolean finished = process.waitFor(30, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                // 等待线程结束
                outputThread.join(5000);
                errorThread.join(5000);
                return TranscodingResult.failure(sourceFile.getAbsolutePath(), "转码超时");
            }
            
            // 等待输出线程完成
            outputThread.join(5000);
            errorThread.join(5000);
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                // 读取错误输出
                StringBuilder errorBuilder = new StringBuilder();
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorBuilder.append(line).append("\n");
                    }
                }
                return TranscodingResult.failure(sourceFile.getAbsolutePath(), 
                    "FFmpeg转码失败，退出码: " + exitCode + ", 错误信息: " + errorBuilder.toString());
            }
            
            // 验证目标文件
            if (!targetFile.exists() || targetFile.length() == 0) {
                return TranscodingResult.failure(sourceFile.getAbsolutePath(), "转码后文件不存在或为空");
            }
            
            long durationMs = System.currentTimeMillis() - startTime;
            long sourceSize = sourceFile.length();
            long targetSize = targetFile.length();
            
            TranscodingResult result = TranscodingResult.success(
                sourceFile.getAbsolutePath(),
                targetFile.getAbsolutePath(),
                "DSF",
                "FLAC",
                durationMs,
                sourceSize,
                targetSize
            );
            result.setFfmpegOutput(outputBuilder.toString());
            
            // 删除源文件（如果配置允许）
            if (deleteSourceAfterTranscoding) {
                if (sourceFile.delete()) {
                    result.setSourceDeleted(true);
                    log.info("已删除源文件: {}", sourceFile.getAbsolutePath());
                } else {
                    log.warn("无法删除源文件: {}", sourceFile.getAbsolutePath());
                }
            }
            
            log.info("DSF到FLAC转码完成: {}", result.formatInfo());
            return result;
            
        } catch (Exception e) {
            log.error("DSF转FLAC转码失败: {}", e.getMessage(), e);
            return TranscodingResult.failure(sourceFile.getAbsolutePath(), "转码异常: " + e.getMessage());
        }
    }
    
    /**
     * WAV转FLAC转码方法
     * @param sourceFile 源WAV文件
     * @param targetFile 目标FLAC文件
     * @return 转码结果
     */
    public TranscodingResult transcodeWAVToFLAC(File sourceFile, File targetFile) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 验证源文件
            if (!sourceFile.exists() || !sourceFile.isFile()) {
                return TranscodingResult.failure(sourceFile.getAbsolutePath(), "源文件不存在");
            }
            
            // 确保目标目录存在
            targetFile.getParentFile().mkdirs();
            
            // 构建FFmpeg命令 - WAV到FLAC的高质量转码
            ProcessBuilder processBuilder = new ProcessBuilder(
                ffmpegPath,
                "-i", sourceFile.getAbsolutePath(),           // 输入文件
                "-c:a", "flac",                               // 音频编码器：FLAC
                "-compression_level", "8",                    // 压缩级别
                "-threads", "0",                              // 自动线程数
                "-y",                                         // 覆盖输出文件
                targetFile.getAbsolutePath()                  // 输出文件
            );
            
            log.info("开始转码WAV到FLAC: {} -> {}", sourceFile.getName(), targetFile.getName());
            
            // 执行转码
            Process process = processBuilder.start();
            
            // 异步读取FFmpeg输出，避免缓冲区阻塞
            StringBuilder outputBuilder = new StringBuilder();
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        outputBuilder.append(line).append("\n");
                        log.debug("FFmpeg输出: {}", line);
                    }
                } catch (IOException e) {
                    log.warn("读取FFmpeg输出时发生错误: {}", e.getMessage());
                }
            });
            
            Thread errorThread = new Thread(() -> {
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        log.debug("FFmpeg错误输出: {}", line);
                    }
                } catch (IOException e) {
                    log.warn("读取FFmpeg错误输出时发生错误: {}", e.getMessage());
                }
            });
            
            outputThread.start();
            errorThread.start();
            
            // 等待转码完成（最多30分钟）
            boolean finished = process.waitFor(30, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                // 等待线程结束
                outputThread.join(5000);
                errorThread.join(5000);
                return TranscodingResult.failure(sourceFile.getAbsolutePath(), "转码超时");
            }
            
            // 等待输出线程完成
            outputThread.join(5000);
            errorThread.join(5000);
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                // 读取错误输出
                StringBuilder errorBuilder = new StringBuilder();
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorBuilder.append(line).append("\n");
                    }
                }
                return TranscodingResult.failure(sourceFile.getAbsolutePath(), 
                    "FFmpeg转码失败，退出码: " + exitCode + ", 错误信息: " + errorBuilder.toString());
            }
            
            // 验证目标文件
            if (!targetFile.exists() || targetFile.length() == 0) {
                return TranscodingResult.failure(sourceFile.getAbsolutePath(), "转码后文件不存在或为空");
            }
            
            long durationMs = System.currentTimeMillis() - startTime;
            long sourceSize = sourceFile.length();
            long targetSize = targetFile.length();
            
            TranscodingResult result = TranscodingResult.success(
                sourceFile.getAbsolutePath(),
                targetFile.getAbsolutePath(),
                "WAV",
                "FLAC",
                durationMs,
                sourceSize,
                targetSize
            );
            result.setFfmpegOutput(outputBuilder.toString());
            
            // 删除源文件（如果配置允许）
            if (deleteSourceAfterTranscoding) {
                if (sourceFile.delete()) {
                    result.setSourceDeleted(true);
                    log.info("已删除源文件: {}", sourceFile.getAbsolutePath());
                } else {
                    log.warn("无法删除源文件: {}", sourceFile.getAbsolutePath());
                }
            }
            
            log.info("WAV到FLAC转码完成: {}", result.formatInfo());
            return result;
            
        } catch (Exception e) {
            log.error("WAV转FLAC转码失败: {}", e.getMessage(), e);
            return TranscodingResult.failure(sourceFile.getAbsolutePath(), "转码异常: " + e.getMessage());
        }
    }
    
    /**
     * WAV转AAC转码方法
     * @param sourceFile 源WAV文件
     * @param targetFile 目标AAC文件
     * @return 转码结果
     */
    public TranscodingResult transcodeWAVToAAC(File sourceFile, File targetFile) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 验证源文件
            if (!sourceFile.exists() || !sourceFile.isFile()) {
                return TranscodingResult.failure(sourceFile.getAbsolutePath(), "源文件不存在");
            }
            
            // 确保目标目录存在
            targetFile.getParentFile().mkdirs();
            
            // 构建FFmpeg命令 - WAV到AAC的高质量转码
            ProcessBuilder processBuilder = new ProcessBuilder(
                ffmpegPath,
                "-i", sourceFile.getAbsolutePath(),           // 输入文件
                "-c:a", "aac",                                // 音频编码器：AAC
                "-b:a", "320k",                               // 比特率320kbps
                "-ar", "48000",                               // 目标采样率48kHz
                "-profile:a", "aac_low",                      // AAC低复杂度配置文件
                "-threads", "0",                              // 自动线程数
                "-y",                                         // 覆盖输出文件
                targetFile.getAbsolutePath()                  // 输出文件
            );
            
            log.info("开始转码WAV到AAC: {} -> {}", sourceFile.getName(), targetFile.getName());
            
            // 执行转码
            Process process = processBuilder.start();
            
            // 异步读取FFmpeg输出，避免缓冲区阻塞
            StringBuilder outputBuilder = new StringBuilder();
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        outputBuilder.append(line).append("\n");
                        log.debug("FFmpeg输出: {}", line);
                    }
                } catch (IOException e) {
                    log.warn("读取FFmpeg输出时发生错误: {}", e.getMessage());
                }
            });
            
            Thread errorThread = new Thread(() -> {
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        log.debug("FFmpeg错误输出: {}", line);
                    }
                } catch (IOException e) {
                    log.warn("读取FFmpeg错误输出时发生错误: {}", e.getMessage());
                }
            });
            
            outputThread.start();
            errorThread.start();
            
            // 等待转码完成
            boolean finished = process.waitFor(30, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                // 等待线程结束
                outputThread.join(5000);
                errorThread.join(5000);
                return TranscodingResult.failure(sourceFile.getAbsolutePath(), "转码超时");
            }
            
            // 等待输出线程完成
            outputThread.join(5000);
            errorThread.join(5000);
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                // 读取错误输出
                StringBuilder errorBuilder = new StringBuilder();
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorBuilder.append(line).append("\n");
                    }
                }
                return TranscodingResult.failure(sourceFile.getAbsolutePath(), 
                    "FFmpeg转码失败，退出码: " + exitCode + ", 错误信息: " + errorBuilder.toString());
            }
            
            // 验证目标文件
            if (!targetFile.exists() || targetFile.length() == 0) {
                return TranscodingResult.failure(sourceFile.getAbsolutePath(), "转码后文件不存在或为空");
            }
            
            long durationMs = System.currentTimeMillis() - startTime;
            long sourceSize = sourceFile.length();
            long targetSize = targetFile.length();
            
            TranscodingResult result = TranscodingResult.success(
                sourceFile.getAbsolutePath(),
                targetFile.getAbsolutePath(),
                "WAV",
                "AAC",
                durationMs,
                sourceSize,
                targetSize
            );
            result.setFfmpegOutput(outputBuilder.toString());
            
            // 删除源文件（如果配置允许）
            if (deleteSourceAfterTranscoding) {
                if (sourceFile.delete()) {
                    result.setSourceDeleted(true);
                    log.info("已删除源文件: {}", sourceFile.getAbsolutePath());
                } else {
                    log.warn("无法删除源文件: {}", sourceFile.getAbsolutePath());
                }
            }
            
            log.info("WAV到AAC转码完成: {}", result.formatInfo());
            return result;
            
        } catch (Exception e) {
            log.error("WAV转AAC转码失败: {}", e.getMessage(), e);
            return TranscodingResult.failure(sourceFile.getAbsolutePath(), "转码异常: " + e.getMessage());
        }
    }
    
    @Override
    public TranscodingResult transcodeDSFToAAC(File sourceFile, File targetFile) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 验证源文件
            if (!sourceFile.exists() || !sourceFile.isFile()) {
                return TranscodingResult.failure(sourceFile.getAbsolutePath(), "源文件不存在");
            }
            
            // 确保目标目录存在
            targetFile.getParentFile().mkdirs();
            
            // 构建FFmpeg命令 - DSF到AAC的高质量转码
            ProcessBuilder processBuilder = new ProcessBuilder(
                ffmpegPath,
                "-i", sourceFile.getAbsolutePath(),           // 输入文件
                "-c:a", "aac",                                // 音频编码器：AAC
                "-b:a", "320k",                               // 比特率320kbps
                "-ar", "48000",                               // 目标采样率48kHz
                "-profile:a", "aac_low",                      // AAC低复杂度配置文件
                "-threads", "0",                              // 自动线程数
                "-y",                                         // 覆盖输出文件
                targetFile.getAbsolutePath()                  // 输出文件
            );
            
            log.info("开始转码DSF到AAC: {} -> {}", sourceFile.getName(), targetFile.getName());
            
            // 执行转码
            Process process = processBuilder.start();
            
            // 异步读取FFmpeg输出，避免缓冲区阻塞
            StringBuilder outputBuilder = new StringBuilder();
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        outputBuilder.append(line).append("\n");
                        log.debug("FFmpeg输出: {}", line);
                    }
                } catch (IOException e) {
                    log.warn("读取FFmpeg输出时发生错误: {}", e.getMessage());
                }
            });
            
            Thread errorThread = new Thread(() -> {
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        log.debug("FFmpeg错误输出: {}", line);
                    }
                } catch (IOException e) {
                    log.warn("读取FFmpeg错误输出时发生错误: {}", e.getMessage());
                }
            });
            
            outputThread.start();
            errorThread.start();
            
            // 等待转码完成
            boolean finished = process.waitFor(30, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                // 等待线程结束
                outputThread.join(5000);
                errorThread.join(5000);
                return TranscodingResult.failure(sourceFile.getAbsolutePath(), "转码超时");
            }
            
            // 等待输出线程完成
            outputThread.join(5000);
            errorThread.join(5000);
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                // 读取错误输出
                StringBuilder errorBuilder = new StringBuilder();
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorBuilder.append(line).append("\n");
                    }
                }
                return TranscodingResult.failure(sourceFile.getAbsolutePath(), 
                    "FFmpeg转码失败，退出码: " + exitCode + ", 错误信息: " + errorBuilder.toString());
            }
            
            // 验证目标文件
            if (!targetFile.exists() || targetFile.length() == 0) {
                return TranscodingResult.failure(sourceFile.getAbsolutePath(), "转码后文件不存在或为空");
            }
            
            long durationMs = System.currentTimeMillis() - startTime;
            long sourceSize = sourceFile.length();
            long targetSize = targetFile.length();
            
            TranscodingResult result = TranscodingResult.success(
                sourceFile.getAbsolutePath(),
                targetFile.getAbsolutePath(),
                "DSF",
                "AAC",
                durationMs,
                sourceSize,
                targetSize
            );
            result.setFfmpegOutput(outputBuilder.toString());
            
            // 删除源文件（如果配置允许）
            if (deleteSourceAfterTranscoding) {
                if (sourceFile.delete()) {
                    result.setSourceDeleted(true);
                    log.info("已删除源文件: {}", sourceFile.getAbsolutePath());
                } else {
                    log.warn("无法删除源文件: {}", sourceFile.getAbsolutePath());
                }
            }
            
            log.info("DSF到AAC转码完成: {}", result.formatInfo());
            return result;
            
        } catch (Exception e) {
            log.error("DSF转AAC转码失败: {}", e.getMessage(), e);
            return TranscodingResult.failure(sourceFile.getAbsolutePath(), "转码异常: " + e.getMessage());
        }
    }
    
    @Override
    public TranscodingResult autoTranscode(File sourceFile, File targetDir) {
        String fileName = sourceFile.getName();
        String extension = getFileExtension(fileName).toLowerCase();
        
        // 只对DSF和WAV文件进行转码
        if (!("dsf".equals(extension) || "wav".equals(extension))) {
            return TranscodingResult.failure(sourceFile.getAbsolutePath(), "非DSF/WAV文件，无需转码");
        }
        
        // 优先转码为FLAC（最高音质）
        String flacFileName = getTranscodedFileName(fileName, "flac");
        File flacFile = new File(targetDir, flacFileName);
        
        TranscodingResult result;
        
        // 根据文件类型选择对应的转码方法
        if ("dsf".equals(extension)) {
            result = transcodeDSFToFLAC(sourceFile, flacFile);
        } else {
            result = transcodeWAVToFLAC(sourceFile, flacFile);
        }
        
        // 如果FLAC转码失败，尝试AAC
        if (!result.isSuccess()) {
            log.warn("FLAC转码失败，尝试AAC转码: {}", result.getErrorMessage());
            String aacFileName = getTranscodedFileName(fileName, "aac");
            File aacFile = new File(targetDir, aacFileName);
            
            if ("dsf".equals(extension)) {
                result = transcodeDSFToAAC(sourceFile, aacFile);
            } else {
                result = transcodeWAVToAAC(sourceFile, aacFile);
            }
        }
        
        return result;
    }
    
    @Override
    public boolean needsTranscoding(File file) {
        if (!file.exists() || !file.isFile()) {
            return false;
        }
        
        String extension = getFileExtension(file.getName()).toLowerCase();
        // WAV和DSF文件都需要转码
        return "dsf".equals(extension) || "wav".equals(extension);
    }
    
    @Override
    public String getTranscodedFileName(String originalFileName, String targetFormat) {
        String nameWithoutExt = removeFileExtension(originalFileName);
        return nameWithoutExt + "." + targetFormat.toLowerCase();
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }
    
    /**
     * 移除文件扩展名
     */
    private String removeFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(0, lastDotIndex);
        }
        return fileName;
    }
    
    private boolean isFileAccessible(File file) {
        try {
            // 尝试获取文件通道来检查是否被锁定
            try (RandomAccessFile raf = new RandomAccessFile(file, "r");
                 FileChannel channel = raf.getChannel()) {
                // 尝试获取共享锁
                FileLock lock = channel.tryLock(0, Long.MAX_VALUE, true);
                if (lock != null) {
                    lock.release();
                }
                return true;
            }
        } catch (IOException e) {
            log.warn("文件访问检查失败 {}: {}", file.getAbsolutePath(), e.getMessage());
            return false;
        }
    }
    
    /**
     * 添加短暂延迟以释放文件锁
     */
    private void waitForFileRelease(File file, int maxAttempts) {
        for (int i = 0; i < maxAttempts; i++) {
            if (isFileAccessible(file)) {
                return;
            }
            try {
                Thread.sleep(100); // 等待100毫秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        log.warn("文件 {} 在 {} 次尝试后仍然被锁定", file.getAbsolutePath(), maxAttempts);
    }
}