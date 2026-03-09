package com.music.file.controller;

import com.music.common.core.domain.Result;
import com.music.file.service.AsyncDataCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 数据清理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/data-cleanup")
@RequiredArgsConstructor
public class DataCleanupController {

    private final AsyncDataCleanupService asyncDataCleanupService;

    /**
     * 启动异步数据清理
     */
    @PostMapping("/start")
    public Result<Long> startCleanup() {
        try {
            Long taskId = asyncDataCleanupService.startCleanup();
            return Result.success(taskId);
        } catch (Exception e) {
            log.error("启动数据清理失败", e);
            return Result.error("启动清理失败: " + e.getMessage());
        }
    }

    /**
     * 获取清理进度
     */
    @GetMapping("/progress/{taskId}")
    public Result<AsyncDataCleanupService.CleanupProgress> getCleanupProgress(
            @PathVariable Long taskId) {
        try {
            AsyncDataCleanupService.CleanupProgress progress =
                asyncDataCleanupService.getCleanupProgress(taskId);

            if (progress == null) {
                return Result.error("任务不存在");
            }

            return Result.success(progress);
        } catch (Exception e) {
            log.error("获取清理进度失败: taskId={}", taskId, e);
            return Result.error("获取进度失败: " + e.getMessage());
        }
    }

    /**
     * 取消清理任务
     */
    @PostMapping("/cancel/{taskId}")
    public Result<Void> cancelCleanup(@PathVariable Long taskId) {
        try {
            asyncDataCleanupService.cancelCleanup(taskId);
            return Result.success();
        } catch (Exception e) {
            log.error("取消清理任务失败: taskId={}", taskId, e);
            return Result.error("取消任务失败: " + e.getMessage());
        }
    }
}
