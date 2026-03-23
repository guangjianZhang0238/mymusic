package com.music.miniprogram.controller;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.music.common.core.domain.Result;
import com.music.common.utils.SecurityUtils;
import com.music.file.config.StorageConfig;
import com.music.player.entity.Playlist;
import com.music.player.mapper.PlaylistMapper;
import com.music.system.entity.User;
import com.music.system.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Tag(name = "小程序资源上传接口")
@RestController
@RequestMapping("/api/mp/asset")
@RequiredArgsConstructor
public class MiniprogramAssetController {

    private final StorageConfig storageConfig;
    private final UserMapper userMapper;
    private final PlaylistMapper playlistMapper;

    @Operation(summary = "上传当前用户头像")
    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("头像文件不能为空");
        }

        Long userId = getCurrentUserId();

        String relativePath = saveFile(file, "avatar", userId, null);

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setAvatar(relativePath);
        userMapper.updateById(user);

        Map<String, String> data = new HashMap<>();
        data.put("path", relativePath);
        data.put("url", toPublicUrl(relativePath));
        return Result.success(data);
    }

    @Operation(summary = "上传歌单封面")
    @PostMapping("/playlist-cover")
    public Result<Map<String, String>> uploadPlaylistCover(@RequestParam("playlistId") Long playlistId,
                                                           @RequestParam("file") MultipartFile file) {
        if (playlistId == null) {
            return Result.error("playlistId不能为空");
        }
        if (file == null || file.isEmpty()) {
            return Result.error("封面文件不能为空");
        }

        Long userId = getCurrentUserId();

        Playlist playlist = playlistMapper.selectById(playlistId);
        if (playlist == null) {
            return Result.error("歌单不存在");
        }
        if (!userId.equals(playlist.getUserId())) {
            return Result.error(403, "无权限修改该歌单封面");
        }

        String relativePath = saveFile(file, "playlist", userId, playlistId);
        playlist.setCoverImage(relativePath);
        playlistMapper.updateById(playlist);

        Map<String, String> data = new HashMap<>();
        data.put("path", relativePath);
        data.put("url", toPublicUrl(relativePath));
        return Result.success(data);
    }

    private Long getCurrentUserId() {
        Long userId = SecurityUtils.getUserId();
        return userId == null ? 1L : userId;
    }

    private String saveFile(MultipartFile file, String category, Long userId, Long playlistId) {
        try {
            String ext = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID().toString().replace("-", "") + ext;

            Path userRoot = Paths.get(storageConfig.getUserPath());
            Path dir;
            if ("playlist".equals(category) && playlistId != null) {
                dir = userRoot.resolve("playlist").resolve(String.valueOf(userId)).resolve(String.valueOf(playlistId));
            } else {
                dir = userRoot.resolve("avatar").resolve(String.valueOf(userId));
            }

            Files.createDirectories(dir);
            Path target = dir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            Path relative = userRoot.relativize(target);
            return relative.toString().replace('\\', '/');
        } catch (IOException e) {
            throw new RuntimeException("保存文件失败: " + e.getMessage(), e);
        }
    }

    private String getExtension(String originalFilename) {
        if (StringUtils.isBlank(originalFilename)) {
            return ".jpg";
        }
        int idx = originalFilename.lastIndexOf('.');
        if (idx < 0 || idx == originalFilename.length() - 1) {
            return ".jpg";
        }
        String ext = originalFilename.substring(idx).toLowerCase();
        if (ext.length() > 10) {
            return ".jpg";
        }
        return ext;
    }

    private String toPublicUrl(String relativePath) {
        return "/user-static/" + relativePath;
    }
}
