package com.music.player.controller;

import com.music.player.service.PlaylistService;
import com.music.player.dto.PlaylistDTO;
import com.music.player.dto.PlaylistQueryDTO;
import com.music.common.core.domain.Result;
import com.music.common.core.domain.PageResult;
import com.music.common.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 播放列表控制器
 */
@RestController
@RequestMapping("/playlist")
@Tag(name = "播放列表管理", description = "播放列表相关接口")
public class PlaylistController {

    @Resource
    private PlaylistService playlistService;

    @Operation(summary = "创建播放列表")
    @PostMapping
    public Result<Long> createPlaylist(@RequestBody PlaylistDTO dto) {
        dto.setUserId(SecurityUtils.getUserId());
        Long playlistId = playlistService.createPlaylist(dto);
        return Result.success(playlistId);
    }

    @Operation(summary = "更新播放列表")
    @PutMapping
    public Result<Void> updatePlaylist(@RequestBody PlaylistDTO dto) {
        playlistService.updatePlaylist(dto);
        return Result.success();
    }

    @Operation(summary = "删除播放列表")
    @DeleteMapping("/{id}")
    public Result<Void> deletePlaylist(@PathVariable Long id) {
        playlistService.deletePlaylist(id);
        return Result.success();
    }

    @Operation(summary = "获取播放列表详情")
    @GetMapping("/{id}")
    public Result<PlaylistDTO> getPlaylistDetail(@PathVariable Long id) {
        PlaylistDTO dto = playlistService.getPlaylistDetail(id);
        return Result.success(dto);
    }

    @Operation(summary = "分页查询播放列表")
    @GetMapping("/page")
    public Result<PageResult<PlaylistDTO>> pagePlaylist(PlaylistQueryDTO queryDTO) {
        PageResult<PlaylistDTO> result = playlistService.pagePlaylist(queryDTO);
        return Result.success(result);
    }

    @Operation(summary = "获取用户的播放列表")
    @GetMapping("/user")
    public Result<List<PlaylistDTO>> getUserPlaylists() {
        Long userId = SecurityUtils.getUserId();
        List<PlaylistDTO> playlists = playlistService.getUserPlaylists(userId);
        return Result.success(playlists);
    }

    @Operation(summary = "获取公开的播放列表")
    @GetMapping("/public")
    public Result<List<PlaylistDTO>> getPublicPlaylists(@RequestParam(defaultValue = "10") int limit) {
        List<PlaylistDTO> playlists = playlistService.getPublicPlaylists(limit);
        return Result.success(playlists);
    }

    @Operation(summary = "向播放列表添加歌曲")
    @PostMapping("/{playlistId}/song/{songId}")
    public Result<Void> addSongToPlaylist(@PathVariable Long playlistId, @PathVariable Long songId) {
        playlistService.addSongToPlaylist(playlistId, songId);
        return Result.success();
    }

    @Operation(summary = "从播放列表移除歌曲")
    @DeleteMapping("/{playlistId}/song/{songId}")
    public Result<Void> removeSongFromPlaylist(@PathVariable Long playlistId, @PathVariable Long songId) {
        playlistService.removeSongFromPlaylist(playlistId, songId);
        return Result.success();
    }

    @Operation(summary = "获取播放列表中的歌曲")
    @GetMapping("/{playlistId}/songs")
    public Result<List<Long>> getPlaylistSongs(@PathVariable Long playlistId) {
        List<Long> songIds = playlistService.getPlaylistSongs(playlistId);
        return Result.success(songIds);
    }

    @Operation(summary = "增加播放列表播放次数")
    @PostMapping("/{playlistId}/play")
    public Result<Void> incrementPlayCount(@PathVariable Long playlistId) {
        playlistService.incrementPlayCount(playlistId);
        return Result.success();
    }
}
