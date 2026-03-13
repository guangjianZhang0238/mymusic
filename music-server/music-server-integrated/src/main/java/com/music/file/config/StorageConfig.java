package com.music.file.config;

import com.music.common.constant.MusicConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Data
@Primary
@Component("storageConfig")
@ConfigurationProperties(prefix = "music.storage")
public class StorageConfig {
    
    /**
     * 数据根目录，可以是绝对路径或相对路径。
     * - 绝对路径：直接使用
     * - 相对路径：相对于应用启动目录（user.dir）
     * 为空时，默认使用应用启动目录。
     */
    private String dataRootUrl;

    /**
     * 在数据根目录下的业务存储根，相对路径（默认使用 MusicConstants 中的目录名）。
     */
    private String basePath = MusicConstants.DEFAULT_STORAGE_PATH;

    /**
     * 在数据根目录下的临时目录，相对路径（默认使用 MusicConstants 中的目录名）。
     */
    private String tempPath = MusicConstants.DEFAULT_TEMP_PATH;
    // 单个文件最大上传大小（字节），默认 2GB（可被 application.yml 的 music.storage.max-file-size 覆盖）
    private long maxFileSize = 2L * 1024 * 1024 * 1024;
    private int uploadThreadCount = 5;

    /**
     * 解析并返回数据根目录的绝对路径。
     */
    public Path getDataRoot() {
        Path rootPath;
        if (dataRootUrl == null || dataRootUrl.isBlank()) {
            rootPath = Paths.get(System.getProperty("user.dir"));
        } else {
            Path configured = Paths.get(dataRootUrl);
            if (configured.isAbsolute()) {
                rootPath = configured;
            } else {
                rootPath = Paths.get(System.getProperty("user.dir")).resolve(configured);
            }
        }
        return rootPath.normalize();
    }

    /**
     * 返回基础存储目录的绝对路径字符串。
     */
    public String getBasePath() {
        return resolvePathUnderRoot(basePath).toString();
    }

    /**
     * 返回临时存储目录的绝对路径字符串。
     */
    public String getTempPath() {
        return resolvePathUnderRoot(tempPath).toString();
    }

    private Path resolvePathUnderRoot(String child) {
        Path root = getDataRoot();
        if (child == null || child.isBlank()) {
            return root;
        }
        Path childPath = Paths.get(child);
        if (childPath.isAbsolute()) {
            return childPath.normalize();
        }
        return root.resolve(childPath).normalize();
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