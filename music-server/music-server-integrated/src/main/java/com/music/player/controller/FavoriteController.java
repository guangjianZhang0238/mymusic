package com.music.player.controller;

import com.music.player.service.FavoriteService;
import com.music.player.dto.FavoriteDTO;
import com.music.common.core.domain.Result;
import com.music.common.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 收藏控制器
 */
@RestController
@RequestMapping("/favorite")
@Tag(name = "收藏管理", description = "收藏相关接口")
public class FavoriteController {

    @Resource
    private FavoriteService favoriteService;

    @Operation(summary = "添加收藏")
    @PostMapping
    public Result<Void> addFavorite(@RequestBody FavoriteDTO dto) {
        dto.setUserId(SecurityUtils.getUserId());
        favoriteService.addFavorite(dto);
        return Result.success();
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping
    public Result<Void> removeFavorite(
            @RequestParam Integer favoriteType,
            @RequestParam Long targetId) {
        Long userId = SecurityUtils.getUserId();
        favoriteService.removeFavorite(userId, favoriteType, targetId);
        return Result.success();
    }

    @Operation(summary = "检查是否已收藏")
    @GetMapping("/check")
    public Result<Boolean> checkFavorite(
            @RequestParam Integer favoriteType,
            @RequestParam Long targetId) {
        Long userId = SecurityUtils.getUserId();
        boolean isFavorited = favoriteService.checkFavorite(userId, favoriteType, targetId);
        return Result.success(isFavorited);
    }

    @Operation(summary = "获取用户的收藏列表")
    @GetMapping
    public Result<List<FavoriteDTO>> getUserFavorites(
            @RequestParam(required = false) Integer favoriteType) {
        Long userId = SecurityUtils.getUserId();
        List<FavoriteDTO> favorites = favoriteService.getUserFavorites(userId, favoriteType);
        return Result.success(favorites);
    }

    @Operation(summary = "获取用户收藏的歌曲")
    @GetMapping("/songs")
    public Result<List<Long>> getUserFavoriteSongs() {
        Long userId = SecurityUtils.getUserId();
        List<Long> songIds = favoriteService.getUserFavoriteSongs(userId);
        return Result.success(songIds);
    }

    @Operation(summary = "获取用户收藏的专辑")
    @GetMapping("/albums")
    public Result<List<Long>> getUserFavoriteAlbums() {
        Long userId = SecurityUtils.getUserId();
        List<Long> albumIds = favoriteService.getUserFavoriteAlbums(userId);
        return Result.success(albumIds);
    }

    @Operation(summary = "获取用户收藏的歌手")
    @GetMapping("/artists")
    public Result<List<Long>> getUserFavoriteArtists() {
        Long userId = SecurityUtils.getUserId();
        List<Long> artistIds = favoriteService.getUserFavoriteArtists(userId);
        return Result.success(artistIds);
    }

    @Operation(summary = "获取用户收藏的播放列表")
    @GetMapping("/playlists")
    public Result<List<Long>> getUserFavoritePlaylists() {
        Long userId = SecurityUtils.getUserId();
        List<Long> playlistIds = favoriteService.getUserFavoritePlaylists(userId);
        return Result.success(playlistIds);
    }
}
