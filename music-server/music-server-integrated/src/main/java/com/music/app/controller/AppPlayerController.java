package com.music.app.controller;

import com.music.common.core.domain.Result;
import com.music.common.utils.SecurityUtils;
import com.music.player.dto.FavoriteDTO;
import com.music.player.dto.PlayHistoryDTO;
import com.music.player.dto.PlaybackPlaylistDTO;
import com.music.player.service.FavoriteService;
import com.music.player.service.PlayHistoryService;
import com.music.player.service.PlaybackPlaylistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * App端播放相关接口
 */
@Tag(name = "App端播放接口")
@RestController
@RequestMapping("/api/app/music/player")
@RequiredArgsConstructor
public class AppPlayerController {

    private static final Logger log = LoggerFactory.getLogger(AppPlayerController.class);
    
    private final PlayHistoryService playHistoryService;
    private final FavoriteService favoriteService;
    private final PlaybackPlaylistService playbackPlaylistService;

    // 获取当前用户ID，如果未登录则返回null
    private Long getCurrentUserId() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            return 1L;
        }
        return userId;
    }

    // ==================== 播放历史 ====================

    @Operation(summary = "添加播放历史")
    @PostMapping("/history")
    public Result<Void> addPlayHistory(@RequestBody PlayHistoryDTO dto) {
        log.info("访问接口：开始添加播放历史，歌曲ID: {}", dto.getSongId());
        dto.setUserId(getCurrentUserId());
        playHistoryService.addPlayHistory(dto);
        return Result.success();
    }

    @Operation(summary = "获取最近播放的歌曲ID列表")
    @GetMapping("/history/recent")
    public Result<List<Long>> getRecentPlays(@RequestParam(defaultValue = "20") int limit) {
        log.info("访问接口：开始获取最近播放的歌曲，限制数量: {}", limit);
        List<Long> songIds = playHistoryService.getUserRecentPlays(getCurrentUserId(), limit);
        return Result.success(songIds);
    }

    @Operation(summary = "清空播放历史")
    @DeleteMapping("/history")
    public Result<Void> clearPlayHistory() {
        log.info("访问接口：开始清空播放历史");
        playHistoryService.clearPlayHistory(getCurrentUserId());
        return Result.success();
    }

    @Operation(summary = "获取播放统计")
    @GetMapping("/history/stats")
    public Result<Long> getPlayCount() {
        log.info("访问接口：开始获取播放统计");
        Long count = playHistoryService.getUserPlayCount(getCurrentUserId());
        return Result.success(count);
    }

    // ==================== 收藏 ====================

    @Operation(summary = "添加收藏")
    @PostMapping("/favorite")
    public Result<Void> addFavorite(@RequestBody FavoriteDTO dto) {
        log.info("访问接口：开始添加收藏，类型: {}, 目标ID: {}", dto.getFavoriteType(), dto.getTargetId());
        dto.setUserId(getCurrentUserId());
        favoriteService.addFavorite(dto);
        return Result.success();
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/favorite")
    public Result<Void> removeFavorite(
            @RequestParam Integer favoriteType,
            @RequestParam Long targetId) {
        log.info("访问接口：开始取消收藏，类型: {}, 目标ID: {}", favoriteType, targetId);
        favoriteService.removeFavorite(getCurrentUserId(), favoriteType, targetId);
        return Result.success();
    }

    @Operation(summary = "检查是否已收藏")
    @GetMapping("/favorite/check")
    public Result<Boolean> checkFavorite(
            @RequestParam Integer favoriteType,
            @RequestParam Long targetId) {
        log.info("访问接口：开始检查是否已收藏，类型: {}, 目标ID: {}", favoriteType, targetId);
        boolean isFavorited = favoriteService.checkFavorite(getCurrentUserId(), favoriteType, targetId);
        return Result.success(isFavorited);
    }

    @Operation(summary = "获取收藏的歌曲ID列表")
    @GetMapping("/favorite/songs")
    public Result<List<Long>> getFavoriteSongs() {
        log.info("访问接口：开始获取收藏的歌曲ID列表");
        List<Long> songIds = favoriteService.getUserFavoriteSongs(getCurrentUserId());
        return Result.success(songIds);
    }

    @Operation(summary = "获取收藏的专辑ID列表")
    @GetMapping("/favorite/albums")
    public Result<List<Long>> getFavoriteAlbums() {
        log.info("访问接口：开始获取收藏的专辑ID列表");
        List<Long> albumIds = favoriteService.getUserFavoriteAlbums(getCurrentUserId());
        return Result.success(albumIds);
    }

    @Operation(summary = "获取收藏的歌手ID列表")
    @GetMapping("/favorite/artists")
    public Result<List<Long>> getFavoriteArtists() {
        log.info("访问接口：开始获取收藏的歌手ID列表");
        List<Long> artistIds = favoriteService.getUserFavoriteArtists(getCurrentUserId());
        return Result.success(artistIds);
    }

    // ==================== 播放列表缓存 ====================

    @Operation(summary = "保存播放列表")
    @PostMapping("/playlist")
    public Result<Void> savePlaybackPlaylist(@RequestBody PlaybackPlaylistDTO dto) {
        log.info("访问接口：开始保存播放列表，歌曲数量: {}, 当前索引: {}", 
                dto.getSongIds() != null ? dto.getSongIds().size() : 0, 
                dto.getCurrentIndex());
        playbackPlaylistService.savePlaybackPlaylist(
                getCurrentUserId(), 
                dto.getSongIds(), 
                dto.getCurrentIndex()
        );
        return Result.success();
    }

    @Operation(summary = "获取播放列表")
    @GetMapping("/playlist")
    public Result<PlaybackPlaylistDTO> getPlaybackPlaylist() {
        log.info("访问接口：开始获取播放列表");
        List<Long> songIds = playbackPlaylistService.getPlaybackPlaylist(getCurrentUserId());
        Integer currentIndex = playbackPlaylistService.getPlaybackIndex(getCurrentUserId());
        PlaybackPlaylistDTO dto = new PlaybackPlaylistDTO();
        dto.setSongIds(songIds);
        dto.setCurrentIndex(currentIndex);
        return Result.success(dto);
    }

    @Operation(summary = "清空播放列表")
    @DeleteMapping("/playlist")
    public Result<Void> clearPlaybackPlaylist() {
        log.info("访问接口：开始清空播放列表");
        playbackPlaylistService.clearPlaybackPlaylist(getCurrentUserId());
        return Result.success();
    }
}
