package com.music.file.controller;

import com.music.api.vo.ArtistVO;
import com.music.api.vo.AlbumVO;
import com.music.common.core.domain.Result;
import com.music.file.service.MusicMetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 音乐元数据控制器
 * 提供歌手和专辑的搜索、匹配、创建功能
 */
@Tag(name = "音乐元数据管理")
@RestController
@RequestMapping("/api/music-metadata")
@RequiredArgsConstructor
public class MusicMetadataController {
    
    private final MusicMetadataService musicMetadataService;
    
    @Operation(summary = "搜索歌手")
    @GetMapping("/artists/search")
    public Result<List<ArtistVO>> searchArtists(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(musicMetadataService.searchArtists(keyword, limit));
    }
    
    @Operation(summary = "搜索专辑")
    @GetMapping("/albums/search")
    public Result<List<AlbumVO>> searchAlbums(
            @RequestParam(required = false) Long artistId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(musicMetadataService.searchAlbums(artistId, keyword, limit));
    }
    
    @Operation(summary = "自动匹配或创建歌手")
    @PostMapping("/artists/auto-match")
    public Result<ArtistVO> autoMatchOrCreateArtist(@RequestParam String artistName) {
        return Result.success(musicMetadataService.autoMatchOrCreateArtist(artistName));
    }
    
    @Operation(summary = "自动匹配或创建专辑")
    @PostMapping("/albums/auto-match")
    public Result<AlbumVO> autoMatchOrCreateAlbum(
            @RequestParam Long artistId,
            @RequestParam(required = false) String albumName) {
        return Result.success(musicMetadataService.autoMatchOrCreateAlbum(artistId, albumName));
    }
}