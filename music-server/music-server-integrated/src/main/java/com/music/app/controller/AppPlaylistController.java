package com.music.app.controller;

import com.music.app.vo.AppPlaylistVO;
import com.music.common.core.domain.Result;
import com.music.common.utils.SecurityUtils;
import com.music.player.dto.PlaylistDTO;
import com.music.player.service.PlaylistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * App端播放列表接口
 */
@Tag(name = "App端播放列表接口")
@RestController
@RequestMapping("/api/app/music/playlist")
@RequiredArgsConstructor
public class AppPlaylistController {

    private static final Logger log = LoggerFactory.getLogger(AppPlaylistController.class);
    
    private final PlaylistService playlistService;

    // 获取当前用户ID
    private Long getCurrentUserId() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            // 如果未登录，返回默认用户ID（为了向后兼容）
            return 1L;
        }
        return userId;
    }

    // 转换DTO到VO
    private AppPlaylistVO convertToVO(PlaylistDTO dto) {
        if (dto == null) {
            return null;
        }
        AppPlaylistVO vo = new AppPlaylistVO();
        BeanUtils.copyProperties(dto, vo);
        return vo;
    }

    @Operation(summary = "创建播放列表")
    @PostMapping
    public Result<Long> createPlaylist(@RequestBody PlaylistDTO dto) {
        log.info("访问接口：开始创建播放列表，名称: {}", dto.getName());
        dto.setUserId(getCurrentUserId());
        Long playlistId = playlistService.createPlaylist(dto);
        return Result.success(playlistId);
    }

    @Operation(summary = "更新播放列表")
    @PutMapping
    public Result<Void> updatePlaylist(@RequestBody PlaylistDTO dto) {
        log.info("访问接口：开始更新播放列表，ID: {}", dto.getId());
        playlistService.updatePlaylist(dto);
        return Result.success();
    }

    @Operation(summary = "删除播放列表")
    @DeleteMapping("/{id}")
    public Result<Void> deletePlaylist(@PathVariable Long id) {
        log.info("访问接口：开始删除播放列表，ID: {}", id);
        playlistService.deletePlaylist(id);
        return Result.success();
    }

    @Operation(summary = "获取播放列表详情")
    @GetMapping("/{id}")
    public Result<AppPlaylistVO> getPlaylistDetail(@PathVariable Long id) {
        log.info("访问接口：开始获取播放列表详情，ID: {}", id);
        PlaylistDTO dto = playlistService.getPlaylistDetail(id);
        return Result.success(convertToVO(dto));
    }

    @Operation(summary = "获取用户的播放列表")
    @GetMapping("/user")
    public Result<List<AppPlaylistVO>> getUserPlaylists() {
        log.info("访问接口：开始获取用户的播放列表");
        Long userId = getCurrentUserId();
        List<PlaylistDTO> playlists = playlistService.getUserPlaylists(userId);
        List<AppPlaylistVO> vos = playlists.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return Result.success(vos);
    }

    @Operation(summary = "获取公开的播放列表")
    @GetMapping("/public")
    public Result<List<AppPlaylistVO>> getPublicPlaylists(@RequestParam(defaultValue = "10") int limit) {
        log.info("访问接口：开始获取公开的播放列表，限制数量: {}", limit);
        List<PlaylistDTO> playlists = playlistService.getPublicPlaylists(limit);
        List<AppPlaylistVO> vos = playlists.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return Result.success(vos);
    }

    @Operation(summary = "向播放列表添加歌曲")
    @PostMapping("/{playlistId}/song/{songId}")
    public Result<Void> addSongToPlaylist(@PathVariable Long playlistId, @PathVariable Long songId) {
        log.info("访问接口：开始向播放列表添加歌曲，播放列表ID: {}, 歌曲ID: {}", playlistId, songId);
        playlistService.addSongToPlaylist(playlistId, songId);
        return Result.success();
    }

    @Operation(summary = "从播放列表移除歌曲")
    @DeleteMapping("/{playlistId}/song/{songId}")
    public Result<Void> removeSongFromPlaylist(@PathVariable Long playlistId, @PathVariable Long songId) {
        log.info("访问接口：开始从播放列表移除歌曲，播放列表ID: {}, 歌曲ID: {}", playlistId, songId);
        playlistService.removeSongFromPlaylist(playlistId, songId);
        return Result.success();
    }

    @Operation(summary = "清空播放列表中的歌曲")
    @DeleteMapping("/{playlistId}/songs")
    public Result<Void> clearPlaylistSongs(@PathVariable Long playlistId) {
        log.info("访问接口：开始清空播放列表中的歌曲，播放列表ID: {}", playlistId);
        playlistService.clearPlaylistSongs(playlistId);
        return Result.success();
    }

    @Operation(summary = "获取播放列表中的歌曲ID列表")
    @GetMapping("/{playlistId}/songs")
    public Result<List<Long>> getPlaylistSongs(@PathVariable Long playlistId) {
        log.info("访问接口：开始获取播放列表中的歌曲ID列表，播放列表ID: {}", playlistId);
        List<Long> songIds = playlistService.getPlaylistSongs(playlistId);
        return Result.success(songIds);
    }

    @Operation(summary = "增加播放列表播放次数")
    @PostMapping("/{playlistId}/play")
    public Result<Void> incrementPlayCount(@PathVariable Long playlistId) {
        log.info("访问接口：开始增加播放列表播放次数，播放列表ID: {}", playlistId);
        playlistService.incrementPlayCount(playlistId);
        return Result.success();
    }
}