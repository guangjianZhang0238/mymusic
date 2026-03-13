package com.music.content.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.music.api.dto.ArtistDTO;
import com.music.api.vo.ArtistVO;
import com.music.api.vo.MatchAvatarResultVO;
import com.music.common.core.domain.Result;
import com.music.content.service.ArtistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "歌手管理")
@RestController
@RequestMapping("/api/artist")
@RequiredArgsConstructor
public class ArtistController {
    
    private final ArtistService artistService;
    
    @Operation(summary = "分页查询歌手列表")
    @GetMapping("/page")
    public Result<Page<ArtistVO>> pageList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(artistService.pageList(keyword, current, size));
    }
    
    @Operation(summary = "获取歌手详情")
    @GetMapping("/{id}")
    public Result<ArtistVO> getDetail(@PathVariable Long id) {
        return Result.success(artistService.getDetail(id));
    }
    
    @Operation(summary = "创建歌手")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ArtistDTO dto) {
        return Result.success(artistService.create(dto));
    }
    
    @Operation(summary = "更新歌手")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody ArtistDTO dto) {
        artistService.update(dto);
        return Result.success();
    }
    
    @Operation(summary = "删除歌手")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        artistService.delete(id);
        return Result.success();
    }
    
    @Operation(summary = "扫描歌手")
    @PostMapping("/scan")
    public Result<java.util.Map<String, Object>> scanArtists() {
        return Result.success(artistService.scanArtists());
    }

    @Operation(summary = "匹配歌手头像（iTunes）")
    @PostMapping("/{id}/match-avatar")
    public Result<MatchAvatarResultVO> matchAvatar(@PathVariable Long id) {
        return Result.success(artistService.matchAvatar(id));
    }
}
