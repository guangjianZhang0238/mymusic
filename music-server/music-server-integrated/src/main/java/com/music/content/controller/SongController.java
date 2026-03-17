package com.music.content.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.music.api.dto.BatchSongIdsDTO;
import com.music.api.dto.BatchSwitchArtistDTO;
import com.music.api.dto.SongDTO;
import com.music.api.dto.SongQueryDTO;
import com.music.api.vo.BatchOperationResultVO;
import com.music.api.vo.BatchSwitchArtistResultVO;
import com.music.api.vo.SongVO;
import com.music.common.core.domain.Result;
import com.music.content.service.SongService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Tag(name = "歌曲管理")
@RestController
@RequestMapping("/api/song")
@RequiredArgsConstructor
public class SongController {
    
    private final SongService songService;
    
    @Operation(summary = "分页查询歌曲列表")
    @GetMapping("/page")
    public Result<Page<SongVO>> pageList(SongQueryDTO query) {
        return Result.success(songService.pageList(query));
    }
    
    @Operation(summary = "获取歌曲详情")
    @GetMapping("/{id}")
    public Result<SongVO> getDetail(@PathVariable Long id) {
        return Result.success(songService.getDetail(id));
    }
    
    @Operation(summary = "创建歌曲")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody SongDTO dto) {
        return Result.success(songService.create(dto));
    }
    
    @Operation(summary = "更新歌曲")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody SongDTO dto) {
        songService.update(dto);
        return Result.success();
    }
    
    @Operation(summary = "删除歌曲")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        songService.delete(id);
        return Result.success();
    }

    @Operation(summary = "批量删除歌曲（含清理关联关系）")
    @PostMapping("/batch-delete")
    public Result<BatchOperationResultVO> batchDelete(@RequestBody BatchSongIdsDTO dto) {
        return Result.success(songService.batchDelete(dto));
    }

    @Operation(summary = "批量切换歌曲歌手（可选专辑，空则默认专辑；含文件迁移与路径同步）")
    @PostMapping("/batch-switch-artist")
    public Result<BatchSwitchArtistResultVO> batchSwitchArtist(@RequestBody BatchSwitchArtistDTO dto) {
        return Result.success(songService.batchSwitchArtist(dto));
    }
    
    @Operation(summary = "获取热门歌曲")
    @GetMapping("/hot")
    public Result<List<SongVO>> getHotSongs() {
        return Result.success(songService.getHotSongs());
    }
    
    @Operation(summary = "自动匹配歌词")
    @PostMapping("/{id}/auto-match-lyrics")
    public Result<Map<String, Object>> autoMatchLyrics(@PathVariable Long id) {
        Map<String, Object> result = songService.autoMatchLyrics(id);
        return Result.success(result);
    }
}
