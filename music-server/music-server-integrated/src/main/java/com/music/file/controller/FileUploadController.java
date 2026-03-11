package com.music.file.controller;

import com.music.api.dto.AlbumDTO;
import com.music.common.core.domain.Result;
import com.music.content.service.AlbumService;
import com.music.file.config.StorageConfig;
import com.music.file.service.FileScanService;
import com.music.file.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.io.IOException;

@Tag(name = "文件上传")
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {
    
    private final FileUploadService fileUploadService;
    private final FileScanService fileScanService;
    private final AlbumService albumService;
    
    @Operation(summary = "上传单个文件")
    @PostMapping("/single")
    public Result<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "albumId", required = false) Long albumId,
            @RequestParam(value = "chorusArtistIds", required = false) List<Long> chorusArtistIds,
            @RequestParam(value = "chorusArtistNames", required = false) List<String> chorusArtistNames) {
        
        log.info("接收到文件上传请求: filename={}, size={}, contentType={}, userId={}, albumId={}, chorusArtistIds={}, chorusArtistNames={}", 
                file.getOriginalFilename(), file.getSize(), file.getContentType(), userId, albumId, chorusArtistIds, chorusArtistNames);
        
        // 文件完整性检查
        if (file.isEmpty()) {
            log.warn("上传文件为空: filename={}", file.getOriginalFilename());
            return Result.error("上传文件为空");
        }
        
        // 文件大小检查
        if (file.getSize() == 0) {
            log.warn("上传文件大小为0: filename={}", file.getOriginalFilename());
            return Result.error("上传文件大小为0");
        }
        
        try {
            // 检查文件是否可以读取
            file.getInputStream();
        } catch (IOException e) {
            log.error("文件读取失败: filename={}, error={}", file.getOriginalFilename(), e.getMessage(), e);
            return Result.error("文件读取失败: " + e.getMessage());
        }
        
        // 如果没有提供userId，使用默认值1
        if (userId == null) {
            userId = 1L;
        }
        
        log.info("开始处理文件上传: filename={}, size={} bytes", file.getOriginalFilename(), file.getSize());
        
        // 如果提供了albumId，使用专门的专辑上传方法，并透传可选的合唱歌手信息（支持多人）
        if (albumId != null) {
            return Result.success(fileUploadService.uploadFileWithAlbum(
                    file,
                    userId,
                    albumId,
                    chorusArtistIds != null ? chorusArtistIds : List.of(),
                    chorusArtistNames != null ? chorusArtistNames : List.of()));
        }
        
        return Result.success(fileUploadService.uploadFile(file, userId));
    }
    
    @Operation(summary = "批量上传文件")
    @PostMapping("/batch")
    public Result<Map<String, Object>> uploadFiles(
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "file", required = false) MultipartFile singleFile,
            @RequestParam("userId") Long userId) {
        
        // 处理单文件上传
        if (singleFile != null && !singleFile.isEmpty()) {
            Map<String, Object> result = fileUploadService.uploadFile(singleFile, userId);
            return Result.success(Map.of("files", List.of(result), "successCount", 1, "errorCount", 0));
        }
        
        // 处理批量文件上传
        if (files != null && !files.isEmpty()) {
            return Result.success(fileUploadService.uploadFiles(files, userId));
        }
        
        return Result.error("没有接收到文件");
    }
    
    @Operation(summary = "上传歌手头像")
    @PostMapping("/artist-avatar")
    public Result<Map<String, Object>> uploadArtistAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestParam("artistId") Long artistId,
            @RequestParam("artistName") String artistName) {
        return Result.success(fileUploadService.uploadArtistAvatar(file, artistId, artistName));
    }
    
    @Operation(summary = "上传专辑封面")
    @PostMapping("/album-cover")
    public Result<Map<String, Object>> uploadAlbumCover(
            @RequestParam("file") MultipartFile file,
            @RequestParam("albumId") Long albumId,
            @RequestParam(value = "folderPath", required = false) String folderPath) {
        log.info("接收到专辑封面上传请求: albumId={}, folderPath={}, file={}", albumId, folderPath, file.getOriginalFilename());
        
        // 检查文件是否为空
        if (file.isEmpty()) {
            log.warn("上传的文件为空");
            return Result.error("文件不能为空");
        }
        
        // 优先使用请求中的folderPath，若为空则根据albumId从数据库回填，避免前端路径参数不一致
        if ((folderPath == null || folderPath.isBlank()) && albumId != null) {
            var album = albumService.getById(albumId);
            if (album != null) {
                folderPath = album.getFolderPath();
                log.info("从数据库获取folderPath: {}", folderPath);
            }
        }

        // 上传专辑封面文件
        Map<String, Object> result = fileUploadService.uploadAlbumCover(file, albumId, folderPath);
        syncAlbumCoverPath(albumId, result);
        return Result.success(result);
    }

    @Operation(summary = "上传专辑封面(Base64，规避multipart在部分环境EOF问题)")
    @PostMapping("/album-cover/base64")
    public Result<Map<String, Object>> uploadAlbumCoverBase64(@RequestBody Map<String, Object> payload) {
        log.info("接收到Base64专辑封面上传请求: payload={}", payload);
        
        Long albumId = payload.get("albumId") == null ? null : Long.valueOf(String.valueOf(payload.get("albumId")));
        String folderPath = payload.get("folderPath") == null ? null : String.valueOf(payload.get("folderPath"));
        String fileName = payload.get("fileName") == null ? "cover.jpg" : String.valueOf(payload.get("fileName"));
        String contentType = payload.get("contentType") == null ? "image/jpeg" : String.valueOf(payload.get("contentType"));
        String base64 = payload.get("data") == null ? null : String.valueOf(payload.get("data"));

        if (albumId == null) {
            log.warn("albumId不能为空");
            return Result.error("albumId不能为空");
        }
        if (base64 == null || base64.isBlank()) {
            log.warn("封面数据不能为空");
            return Result.error("封面数据不能为空");
        }

        log.info("处理专辑封面上传: albumId={}, fileName={}, contentType={}", albumId, fileName, contentType);

        if ((folderPath == null || folderPath.isBlank()) && albumId != null) {
            var album = albumService.getById(albumId);
            if (album != null) {
                folderPath = album.getFolderPath();
                log.info("从数据库获取folderPath: {}", folderPath);
            }
        }

        byte[] fileBytes = Base64.getDecoder().decode(base64);
        log.info("解码Base64数据，大小: {} bytes", fileBytes.length);
        
        Map<String, Object> result = fileUploadService.uploadAlbumCoverByBytes(fileBytes, fileName, contentType, albumId, folderPath);
        syncAlbumCoverPath(albumId, result);
        log.info("专辑封面上传完成: path={}", result.get("path"));
        return Result.success(result);
    }

    private void syncAlbumCoverPath(Long albumId, Map<String, Object> result) {
        String coverImagePath = (String) result.get("path");
        if (coverImagePath != null && albumId != null) {
            AlbumDTO albumDTO = new AlbumDTO();
            albumDTO.setId(albumId);
            albumDTO.setCoverImage(coverImagePath);
            albumService.update(albumDTO);
        }
    }
    
    @Operation(summary = "更新歌曲库")
    @PostMapping("/scan-library")
    public Result<Map<String, Object>> scanMusicLibrary() {
        var result = fileScanService.scanMusicLibrary();
        Map<String, Object> data = Map.of(
                "addedSongs", result.getAddedSongs(),
                "updatedSongs", result.getUpdatedSongs(),
                "skippedSongs", result.getSkippedSongs(),
                "errors", result.getErrors()
        );
        return Result.success(data);
    }
}
