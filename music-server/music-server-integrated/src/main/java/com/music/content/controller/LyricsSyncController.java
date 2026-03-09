package com.music.content.controller;

import com.music.common.core.domain.Result;
import com.music.content.service.LyricsSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 歌词同步控制器
 */
@Slf4j
@Tag(name = "歌词同步")
@RestController
@RequestMapping("/api/lyrics-sync")
@RequiredArgsConstructor
public class LyricsSyncController {
    
    private final LyricsSyncService lyricsSyncService;
    
    /**
     * 启动异步歌词同步
     */
    @Operation(summary = "启动异步歌词同步")
    @PostMapping("/start")
    public Result<Long> startLyricsSync() {
        try {
            Long taskId = lyricsSyncService.startLyricsSync();
            return Result.success(taskId);
        } catch (Exception e) {
            log.error("启动异步歌词同步失败", e);
            return Result.error("启动同步失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取同步进度
     */
    @Operation(summary = "获取歌词同步进度")
    @GetMapping("/progress/{taskId}")
    public Result<LyricsSyncService.LyricsSyncProgress> getSyncProgress(
            @PathVariable Long taskId) {
        try {
            LyricsSyncService.LyricsSyncProgress progress = 
                lyricsSyncService.getSyncProgress(taskId);
            
            if (progress == null) {
                return Result.error("任务不存在");
            }
            
            return Result.success(progress);
        } catch (Exception e) {
            log.error("获取同步进度失败: taskId={}", taskId, e);
            return Result.error("获取进度失败: " + e.getMessage());
        }
    }
    
    /**
     * 取消同步任务
     */
    @Operation(summary = "取消歌词同步任务")
    @PostMapping("/cancel/{taskId}")
    public Result<Void> cancelSync(@PathVariable Long taskId) {
        try {
            lyricsSyncService.cancelSync(taskId);
            return Result.success();
        } catch (Exception e) {
            log.error("取消同步任务失败: taskId={}", taskId, e);
            return Result.error("取消任务失败: " + e.getMessage());
        }
    }
}