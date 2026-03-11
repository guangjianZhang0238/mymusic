package com.music.content.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.music.api.dto.AlbumDTO;
import com.music.api.dto.AlbumSongBindDTO;
import com.music.api.vo.AlbumVO;
import com.music.common.core.domain.Result;
import com.music.content.service.AlbumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "专辑管理")
@RestController
@RequestMapping("/api/album")
@RequiredArgsConstructor
public class AlbumController {
    
    private final AlbumService albumService;
    
    @Operation(summary = "分页查询专辑列表")
    @GetMapping("/page")
    public Result<Page<AlbumVO>> pageList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long artistId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(albumService.pageList(keyword, artistId, current, size));
    }
    
    @Operation(summary = "获取专辑详情")
    @GetMapping("/{id}")
    public Result<AlbumVO> getDetail(@PathVariable Long id) {
        return Result.success(albumService.getDetail(id));
    }
    
    @Operation(summary = "创建专辑")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody AlbumDTO dto) {
        return Result.success(albumService.create(dto));
    }
    
    @Operation(summary = "更新专辑")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody AlbumDTO dto) {
        albumService.update(dto);
        return Result.success();
    }
    
    @Operation(summary = "删除专辑")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        albumService.delete(id);
        return Result.success();
    }

    @Operation(summary = "批量将歌曲收录到专辑中")
    @PostMapping("/{albumId}/songs/batch-bind")
    public Result<Void> bindSongs(@PathVariable Long albumId,
                                  @RequestBody AlbumSongBindDTO dto) {
        albumService.bindSongs(albumId, dto != null ? dto.getSongIds() : null);
        return Result.success();
    }
}
