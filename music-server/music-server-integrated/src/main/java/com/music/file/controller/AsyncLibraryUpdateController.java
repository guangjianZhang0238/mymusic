package com.music.file.controller;

import com.music.common.core.domain.Result;
import com.music.file.service.AsyncLibraryUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 异步歌曲库更新控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/library")
@RequiredArgsConstructor
public class AsyncLibraryUpdateController {
    
    private final AsyncLibraryUpdateService asyncLibraryUpdateService;
    
    /**
     * 启动异步歌曲库更新
     */
    @PostMapping("/update-async")
    public Result<Long> startLibraryUpdate() {
        try {
            Long taskId = asyncLibraryUpdateService.startLibraryUpdate();
            return Result.success(taskId);
        } catch (Exception e) {
            log.error("启动异步歌曲库更新失败", e);
            return Result.error("启动更新失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取更新进度
     */
    @GetMapping("/progress/{taskId}")
    public Result<AsyncLibraryUpdateService.LibraryUpdateProgress> getUpdateProgress(
            @PathVariable Long taskId) {
        try {
            AsyncLibraryUpdateService.LibraryUpdateProgress progress = 
                asyncLibraryUpdateService.getUpdateProgress(taskId);
            
            if (progress == null) {
                return Result.error("任务不存在");
            }
            
            return Result.success(progress);
        } catch (Exception e) {
            log.error("获取更新进度失败: taskId={}", taskId, e);
            return Result.error("获取进度失败: " + e.getMessage());
        }
    }
    
    /**
     * 取消更新任务
     */
    @PostMapping("/cancel/{taskId}")
    public Result<Void> cancelUpdate(@PathVariable Long taskId) {
        try {
            asyncLibraryUpdateService.cancelUpdate(taskId);
            return Result.success();
        } catch (Exception e) {
            log.error("取消更新任务失败: taskId={}", taskId, e);
            return Result.error("取消任务失败: " + e.getMessage());
        }
    }
}