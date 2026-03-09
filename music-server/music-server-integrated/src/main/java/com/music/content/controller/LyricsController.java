package com.music.content.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.music.api.dto.LyricsDTO;
import com.music.api.vo.LyricsVO;
import com.music.common.core.domain.Result;
import com.music.content.service.LyricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "歌词管理")
@RestController
@RequestMapping("/api/lyrics")
@RequiredArgsConstructor
public class LyricsController {
    
    private final LyricsService lyricsService;
    
    @Operation(summary = "分页查询歌词列表")
    @GetMapping("/page")
    public Result<Page<LyricsVO>> pageList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long songId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(lyricsService.pageList(keyword, songId, current, size));
    }
    
    @Operation(summary = "根据歌曲ID获取歌词")
    @GetMapping("/song/{songId}")
    public Result<LyricsVO> getBySongId(@PathVariable Long songId) {
        return Result.success(lyricsService.getBySongId(songId));
    }
    
    @Operation(summary = "创建歌词")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody LyricsDTO dto) {
        return Result.success(lyricsService.create(dto));
    }
    
    @Operation(summary = "更新歌词")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody LyricsDTO dto) {
        lyricsService.update(dto);
        return Result.success();
    }
    
    @Operation(summary = "删除歌词")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        lyricsService.delete(id);
        return Result.success();
    }
    
    @Operation(summary = "上传歌词文件")
    @PostMapping("/upload")
    public Result<Map<String, Object>> uploadLyricsFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("songId") Long songId) {
        return Result.success(lyricsService.uploadLyricsFile(file, songId));
    }
    
    @Operation(summary = "更新歌词偏移量")
    @PutMapping("/update-offset")
    public Result<Void> updateLyricsOffset(
            @RequestBody Map<String, Object> params) {
        Long songId = Long.parseLong(params.get("songId").toString());
        Double lyricsOffset = Double.parseDouble(params.get("lyricsOffset").toString());
        lyricsService.updateLyricsOffset(songId, lyricsOffset);
        return Result.success();
    }
    
    @Operation(summary = "自动同步歌词")
    @PostMapping("/auto-sync")
    public Result<Map<String, Object>> autoSyncLyrics() {
        Map<String, Object> result = lyricsService.autoSyncLyrics();
        return Result.success(result);
    }
}
