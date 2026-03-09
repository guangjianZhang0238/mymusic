package com.music.file.config;

import com.music.common.constant.MusicConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;

@Data
@Primary
@Component("storageConfig")
@ConfigurationProperties(prefix = "music.storage")
public class StorageConfig {
    
    private String basePath = MusicConstants.DEFAULT_STORAGE_PATH;
    private String tempPath = MusicConstants.DEFAULT_TEMP_PATH;
    // 单个文件最大上传大小（字节），默认 2GB（可被 application.yml 的 music.storage.max-file-size 覆盖）
    private long maxFileSize = 2L * 1024 * 1024 * 1024;
    private int uploadThreadCount = 5;
    
    public String getBasePath() {
        // 保持原始路径格式，让调用方决定如何处理分隔符
        return basePath;
    }
    
    public String getTempPath() {
        // 保持原始路径格式，让调用方决定如何处理分隔符
        return tempPath;
    }

    @PostConstruct
    public void ensureStorageDirectories() {
        ensureDir(getBasePath());
        ensureDir(getTempPath());
    }

    private void ensureDir(String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
}