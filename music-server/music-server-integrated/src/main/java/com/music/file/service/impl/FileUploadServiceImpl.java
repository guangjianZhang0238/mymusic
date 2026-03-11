package com.music.file.service.impl;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.music.api.dto.SongDTO;
import com.music.api.vo.ArtistVO;
import com.music.common.exception.BusinessException;
import com.music.content.entity.Album;
import com.music.content.entity.Artist;
import com.music.content.entity.SongArtist;
import com.music.content.mapper.ArtistMapper;
import com.music.content.mapper.SongArtistMapper;
import com.music.content.service.AlbumService;
import com.music.content.service.SongService;
import com.music.content.service.ArtistService;
import com.music.file.config.StorageConfig;
import com.music.file.service.FileUploadService;
import com.music.file.service.MusicMetadataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class FileUploadServiceImpl implements FileUploadService {
    
    @Resource(name = "storageConfig")
    private StorageConfig storageConfig;

    @Resource
    private AlbumService albumService;

    @Resource
    private ArtistService artistService;

    @Resource
    private MusicMetadataService musicMetadataService;

    @Resource
    private SongService songService;

    @Resource
    private SongArtistMapper songArtistMapper;

    @Resource
    private ArtistMapper artistMapper;
    
    @Override
    public Map<String, Object> uploadFile(MultipartFile file, Long userId) {
        if (file.isEmpty()) {
            throw BusinessException.of("文件不能为空");
        }
        
        if (file.getSize() > storageConfig.getMaxFileSize()) {
            throw BusinessException.of("文件大小超过限制");
        }
        
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        
        String relativePath = generateRelativePath(userId, extension);
        String filePath = saveFile(file, relativePath);
        
        Map<String, Object> result = new HashMap<>();
        result.put("filename", originalFilename);
        result.put("size", file.getSize());
        result.put("path", relativePath);
        result.put("fullPath", filePath);
        result.put("userId", userId);
        result.put("contentType", file.getContentType());
        
        log.info("文件上传成功：userId={}, filename={}, size={}, path={}", userId, originalFilename, file.getSize(), relativePath);
        
        return result;
    }
    
    @Override
    public Map<String, Object> uploadFileWithAlbum(
            MultipartFile file,
            Long userId,
            Long albumId,
            List<Long> chorusArtistIds,
            List<String> chorusArtistNames) {
        // 检查文件是否为空
        if (file.isEmpty()) {
            throw BusinessException.of("文件不能为空");
        }
        
        // 检查文件大小
        if (file.getSize() > storageConfig.getMaxFileSize()) {
            throw BusinessException.of("文件大小超过限制");
        }
        
        // 获取专辑信息
        Album album = albumService.getById(albumId);
        if (album == null) {
            throw BusinessException.of("专辑不存在");
        }
        
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        
        // 生成基于专辑的相对路径
        String relativePath = album.getFolderPath() + "/" + originalFilename;
        String filePath = saveFile(file, relativePath);
        
        // 合唱歌手：已有 ID 的直接使用，仅有名称的自动匹配或创建，得到最终 ID 列表
        List<Long> resolvedChorusIds = resolveChorusArtistIds(chorusArtistIds, chorusArtistNames);

        // 直接创建歌曲记录并建立歌手关联（无需 .artists.json）
        createSongAndArtistLinks(album, relativePath, originalFilename, file.getSize(), extension, resolvedChorusIds);
        
        Map<String, Object> result = new HashMap<>();
        result.put("filename", originalFilename);
        result.put("size", file.getSize());
        result.put("path", relativePath);
        result.put("fullPath", filePath);
        result.put("userId", userId);
        result.put("albumId", albumId);
        result.put("contentType", file.getContentType());
        
        log.info("带专辑文件上传成功：userId={}, albumId={}, filename={}, size={}, path={}", 
                userId, albumId, originalFilename, file.getSize(), relativePath);
        
        return result;
    }
    
    @Override
    public Map<String, Object> uploadFiles(List<MultipartFile> files, Long userId) {
        if (files == null || files.isEmpty()) {
            throw BusinessException.of("文件列表不能为空");
        }
        
        int successCount = 0;
        int errorCount = 0;
        Map<String, Object> results = new HashMap<>();
        
        for (MultipartFile file : files) {
            try {
                Map<String, Object> fileResult = uploadFile(file, userId);
                results.put(file.getOriginalFilename(), fileResult);
                successCount++;
            } catch (Exception e) {
                log.error("批量上传文件失败：{}", file.getOriginalFilename(), e);
                results.put(file.getOriginalFilename(), Map.of("error", e.getMessage()));
                errorCount++;
            }
        }
        
        return Map.of(
                "files", results,
                "successCount", successCount,
                "errorCount", errorCount,
                "totalCount", files.size()
        );
    }
    
    @Override
    public Map<String, Object> uploadArtistAvatar(MultipartFile file, Long artistId, String artistName) {
        // 检查文件是否为空
        if (file.isEmpty()) {
            throw BusinessException.of("文件不能为空");
        }
        
        // 检查文件大小
        if (file.getSize() > storageConfig.getMaxFileSize()) {
            throw BusinessException.of("文件大小超过限制");
        }
        
        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw BusinessException.of("请上传图片文件");
        }
        
        // 检查歌手名称
        if (StringUtils.isEmpty(artistName)) {
            throw BusinessException.of("歌手名称不能为空");
        }
        
        // 清理歌手名称，移除非法的文件路径字符
        String cleanedArtistName = artistName.replaceAll("[\\/:*?\"<>|]", "");
        if (StringUtils.isEmpty(cleanedArtistName)) {
            throw BusinessException.of("歌手名称包含非法字符");
        }
        
        // 确保基础路径存在
        String basePath = storageConfig.getBasePath();
        File baseDir = new File(basePath);
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            throw BusinessException.of("无法创建基础存储目录: " + basePath);
        }
        
        // 确保歌手文件夹存在
        String artistFolderPath = basePath + File.separator + cleanedArtistName;
        File artistFolder = new File(artistFolderPath);
        if (!artistFolder.exists()) {
            if (!artistFolder.mkdirs()) {
                throw BusinessException.of("无法创建歌手文件夹: " + artistFolderPath);
            }
        } else if (!artistFolder.isDirectory()) {
            throw BusinessException.of("歌手文件夹路径已存在但不是一个目录: " + artistFolderPath);
        }
        
        // 生成头像文件路径，使用正斜杠确保跨平台兼容性
        String relativePath = cleanedArtistName + "/cover.jpg";
        String filePath = saveFile(file, relativePath);
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("filename", file.getOriginalFilename());
        result.put("size", file.getSize());
        result.put("path", filePath);
        result.put("artistId", artistId);
        result.put("artistName", cleanedArtistName);
        result.put("contentType", contentType);
        
        log.info("歌手头像上传成功：artistName={}, cleanedArtistName={}, filePath={}", artistName, cleanedArtistName, filePath);
        
        return result;
    }
    
    @Override
    public Map<String, Object> uploadAlbumCover(MultipartFile file, Long albumId, String folderPath) {
        // 检查文件是否为空
        if (file.isEmpty()) {
            throw BusinessException.of("文件不能为空");
        }
        
        // 检查文件大小
        if (file.getSize() > storageConfig.getMaxFileSize()) {
            throw BusinessException.of("文件大小超过限制");
        }
        
        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw BusinessException.of("请上传图片文件");
        }
        
        // 检查专辑ID
        if (albumId == null) {
            throw BusinessException.of("专辑ID不能为空");
        }
        
        // 检查folderPath
        if (StringUtils.isEmpty(folderPath)) {
            throw BusinessException.of("专辑文件夹路径不能为空");
        }

        // 检查专辑文件夹是否存在，不存在则创建
        String basePath = storageConfig.getBasePath();
        File baseDir = new File(basePath);
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            throw BusinessException.of("无法创建基础存储目录: " + basePath);
        }

        // 标准化folderPath，确保使用正斜杠
        String normalizedFolderPath = folderPath.replace('\\', '/');
        String albumFolderPath = basePath + File.separator + normalizedFolderPath;
        File albumFolder = new File(albumFolderPath);
        
        if (!albumFolder.exists()) {
            if (!albumFolder.mkdirs()) {
                throw BusinessException.of("无法创建专辑文件夹: " + albumFolderPath);
            }
        } else if (!albumFolder.isDirectory()) {
            throw BusinessException.of("专辑文件夹路径已存在但不是一个目录: " + albumFolderPath);
        }

        String relativePath = normalizedFolderPath + "/cover.jpg";
        String normalizedRelativePath = relativePath.replace('/', File.separatorChar);
        Path destPath = Paths.get(storageConfig.getBasePath(), normalizedRelativePath);

        try {
            Files.write(destPath, file.getBytes());
        } catch (IOException e) {
            throw BusinessException.of("文件保存失败: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("filename", file.getOriginalFilename());
        result.put("size", file.getSize());
        result.put("path", relativePath);
        result.put("albumId", albumId);
        result.put("folderPath", normalizedFolderPath);
        result.put("contentType", contentType);

        log.info("专辑封面上传成功：albumId={}, folderPath={}, filePath={}", albumId, normalizedFolderPath, relativePath);
        return result;
    }

    @Override
    public Map<String, Object> uploadAlbumCoverByBytes(byte[] fileBytes, String originalFilename, String contentType, Long albumId, String folderPath) {
        // 检查文件是否为空
        if (fileBytes == null || fileBytes.length == 0) {
            throw BusinessException.of("文件不能为空");
        }
        
        // 检查文件大小
        if (fileBytes.length > storageConfig.getMaxFileSize()) {
            throw BusinessException.of("文件大小超过限制");
        }
        
        // 检查文件类型
        if (contentType == null || !contentType.startsWith("image/")) {
            throw BusinessException.of("请上传图片文件");
        }
        
        // 检查专辑ID
        if (albumId == null) {
            throw BusinessException.of("专辑ID不能为空");
        }
        
        // 检查folderPath
        if (StringUtils.isEmpty(folderPath)) {
            throw BusinessException.of("专辑文件夹路径不能为空");
        }

        // 检查专辑文件夹是否存在，不存在则创建
        String basePath = storageConfig.getBasePath();
        File baseDir = new File(basePath);
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            throw BusinessException.of("无法创建基础存储目录: " + basePath);
        }

        // 标准化folderPath，确保使用正斜杠
        String normalizedFolderPath = folderPath.replace('\\', '/');
        String albumFolderPath = basePath + File.separator + normalizedFolderPath;
        File albumFolder = new File(albumFolderPath);
        
        if (!albumFolder.exists()) {
            if (!albumFolder.mkdirs()) {
                throw BusinessException.of("无法创建专辑文件夹: " + albumFolderPath);
            }
        } else if (!albumFolder.isDirectory()) {
            throw BusinessException.of("专辑文件夹路径已存在但不是一个目录: " + albumFolderPath);
        }

        String relativePath = normalizedFolderPath + "/cover.jpg";
        String normalizedRelativePath = relativePath.replace('/', File.separatorChar);
        Path destPath = Paths.get(storageConfig.getBasePath(), normalizedRelativePath);

        try {
            Files.write(destPath, fileBytes);
        } catch (IOException e) {
            throw BusinessException.of("文件保存失败: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("filename", originalFilename);
        result.put("size", fileBytes.length);
        result.put("path", relativePath);
        result.put("albumId", albumId);
        result.put("folderPath", normalizedFolderPath);
        result.put("contentType", contentType);

        log.info("专辑封面上传成功(字节流)：albumId={}, folderPath={}, filePath={}", albumId, normalizedFolderPath, relativePath);
        return result;
    }
    
    /** 将合唱歌手 ID 列表与名称列表合并为最终 ID 列表，仅有名称的会 autoMatchOrCreate */
    private List<Long> resolveChorusArtistIds(List<Long> chorusArtistIds, List<String> chorusArtistNames) {
        List<Long> result = new ArrayList<>();
        if (chorusArtistIds != null) {
            for (Long id : chorusArtistIds) {
                if (id != null && !result.contains(id)) result.add(id);
            }
        }
        if (chorusArtistNames != null) {
            for (String name : chorusArtistNames) {
                if (name == null || name.trim().isEmpty()) continue;
                try {
                    ArtistVO a = musicMetadataService.autoMatchOrCreateArtist(name.trim());
                    if (a != null && a.getId() != null && !result.contains(a.getId())) {
                        result.add(a.getId());
                        log.info("自动匹配/创建合唱歌手: name={}, id={}", a.getName(), a.getId());
                    }
                } catch (Exception e) {
                    log.warn("合唱歌手匹配失败，跳过: name={}, error={}", name, e.getMessage());
                }
            }
        }
        return result;
    }

    /**
     * 上传时直接创建歌曲记录并建立主歌手与合唱歌手的关联
     */
    private void createSongAndArtistLinks(Album album, String relativePath, String fileName, long fileSize,
                                          String format, List<Long> chorusArtistIds) {
        try {
            Long mainArtistId = album.getArtistId();
            if (mainArtistId == null) {
                log.warn("专辑无主歌手，跳过创建歌曲记录");
                return;
            }
            String title = fileName;
            int lastDot = fileName.lastIndexOf('.');
            if (lastDot > 0) {
                title = fileName.substring(0, lastDot);
            }
            SongDTO dto = new SongDTO();
            dto.setAlbumId(album.getId());
            dto.setArtistId(mainArtistId);
            dto.setTitle(title);
            dto.setTitleEn("");
            dto.setFilePath(relativePath);
            dto.setFileName(fileName);
            dto.setFileSize(fileSize);
            dto.setFormat(format);
            dto.setDuration(0);
            dto.setStatus(1);
            Long songId = songService.create(dto);
            if (songId == null) {
                log.warn("创建歌曲记录失败: {}", relativePath);
                return;
            }
            Artist mainArtist = artistMapper.selectById(mainArtistId);
            if (mainArtist == null) {
                log.warn("主歌手不存在: {}", mainArtistId);
                return;
            }
            List<Long> chorusIds = chorusArtistIds != null ? chorusArtistIds : List.of();
            linkArtistsForSong(songId, mainArtist, chorusIds);
            log.info("已创建歌曲及歌手关联: songId={}, title={}, artists={}", songId, title,
                    chorusIds.isEmpty() ? mainArtist.getName() : mainArtist.getName() + "+" + chorusIds.size() + "合唱");
        } catch (Exception e) {
            log.warn("创建歌曲记录失败，不影响文件上传: path={}, error={}", relativePath, e.getMessage());
        }
    }

    private void linkArtistsForSong(Long songId, Artist mainArtist, List<Long> chorusArtistIds) {
        try {
            SongArtist main = new SongArtist();
            main.setSongId(songId);
            main.setArtistId(mainArtist.getId());
            main.setSortOrder(0);
            songArtistMapper.insert(main);
            if (chorusArtistIds != null && !chorusArtistIds.isEmpty()) {
                int order = 1;
                for (Long id : chorusArtistIds) {
                    if (id == null || Objects.equals(id, mainArtist.getId())) continue;
                    SongArtist sa = new SongArtist();
                    sa.setSongId(songId);
                    sa.setArtistId(id);
                    sa.setSortOrder(order++);
                    songArtistMapper.insert(sa);
                }
            }
            List<String> names = new ArrayList<>();
            names.add(mainArtist.getName());
            if (chorusArtistIds != null) {
                for (Long id : chorusArtistIds) {
                    if (id == null || Objects.equals(id, mainArtist.getId())) continue;
                    Artist a = artistMapper.selectById(id);
                    if (a != null && a.getName() != null && !a.getName().isEmpty()) {
                        names.add(a.getName());
                    }
                }
            }
            if (names.size() > 1) {
                com.music.content.entity.Song song = songService.getById(songId);
                if (song != null) {
                    song.setArtistNames(String.join(" / ", names));
                    songService.updateById(song);
                }
            }
        } catch (Exception e) {
            log.warn("写入歌手关联失败: songId={}, error={}", songId, e.getMessage());
        }
    }

    
    @Override
    public String saveFile(MultipartFile file, String relativePath) {
        // 替换relativePath中的正斜杠为系统默认分隔符
        String normalizedRelativePath = relativePath.replace('/', File.separatorChar);
        String fullPath = storageConfig.getBasePath() + File.separator + normalizedRelativePath;
        File destFile = new File(fullPath);
        
        log.info("准备保存文件: relativePath={}, normalizedRelativePath={}, fullPath={}", relativePath, normalizedRelativePath, fullPath);
        
        // 确保目标文件的父目录存在
        File parentDir = destFile.getParentFile();
        if (parentDir != null) {
            log.info("父目录: {}", parentDir.getAbsolutePath());
            if (!parentDir.exists()) {
                log.info("父目录不存在，正在创建...");
                boolean created = parentDir.mkdirs();
                log.info("父目录创建结果: {}", created);
                if (!created) {
                    throw BusinessException.of("无法创建父目录: " + parentDir.getAbsolutePath());
                }
            }
            if (!parentDir.isDirectory()) {
                throw BusinessException.of("父路径不是一个目录: " + parentDir.getAbsolutePath());
            }
            if (!parentDir.canWrite()) {
                throw BusinessException.of("父目录没有写入权限: " + parentDir.getAbsolutePath());
            }
        }
        
        try {
            // 如果文件已存在，删除它
            if (destFile.exists()) {
                log.info("文件已存在，正在删除...");
                boolean deleted = destFile.delete();
                log.info("文件删除结果: {}", deleted);
            }
            
            log.info("正在保存文件...");
            file.transferTo(destFile);
            log.info("文件保存成功: {}", destFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw BusinessException.of("文件保存失败: " + e.getMessage());
        }
        
        return relativePath;
    }
    
    @Override
    public void deleteFile(String filePath) {
        if (StringUtils.isEmpty(filePath)) {
            return;
        }
        
        String fullPath = storageConfig.getBasePath() + File.separator + filePath.replace('/', File.separatorChar);
        File file = new File(fullPath);
        
        if (file.exists() && file.isFile()) {
            boolean deleted = file.delete();
            log.info("文件删除{}: {}", deleted ? "成功" : "失败", fullPath);
        } else {
            log.warn("文件不存在或不是文件: {}", fullPath);
        }
    }
    
    @Override
    public File getFile(String filePath) {
        if (StringUtils.isEmpty(filePath)) {
            return null;
        }
        
        String fullPath = storageConfig.getBasePath() + File.separator + filePath.replace('/', File.separatorChar);
        File file = new File(fullPath);
        
        return file.exists() && file.isFile() ? file : null;
    }
    
    private String generateRelativePath(Long userId, String extension) {
        // 生成基于用户ID和时间戳的相对路径
        long timestamp = System.currentTimeMillis();
        return "user_" + userId + "/" + timestamp + "." + extension;
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}