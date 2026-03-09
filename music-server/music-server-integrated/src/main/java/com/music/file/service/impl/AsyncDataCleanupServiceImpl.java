package com.music.file.service.impl;

import com.music.file.service.AsyncDataCleanupService;
import com.music.file.service.DataCleanupService;
import com.music.system.service.AsyncTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 异步数据清理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncDataCleanupServiceImpl implements AsyncDataCleanupService {

    private final DataCleanupService dataCleanupService;
    private final AsyncTaskService asyncTaskService;

    private final ConcurrentHashMap<Long, AtomicBoolean> runningTasks = new ConcurrentHashMap<>();
    // 存储清理结果，供前端查询
    private final ConcurrentHashMap<Long, DataCleanupService.CleanupResult> cleanupResults = new ConcurrentHashMap<>();

    @Override
    public Long startCleanup() {
        Long taskId = asyncTaskService.createTask("DATA_CLEANUP", "数据清理");
        runningTasks.put(taskId, new AtomicBoolean(true));
        performCleanupAsync(taskId);
        log.info("启动异步数据清理任务: ID={}", taskId);
        return taskId;
    }

    @Async
    public void performCleanupAsync(Long taskId) {
        try {
            asyncTaskService.updateProgress(taskId, 5, "开始数据清理...");

            AtomicBoolean isRunning = runningTasks.get(taskId);

            DataCleanupService.CleanupResult result = dataCleanupService.performCleanup((percent, message) -> {
                // 检查是否被取消
                if (isRunning != null && !isRunning.get()) {
                    throw new RuntimeException("任务已被用户取消");
                }
                asyncTaskService.updateProgress(taskId, percent, message);
            });

            if (result.isSuccess()) {
                cleanupResults.put(taskId, result);
                String completionMessage = String.format(
                    "数据清理完成！删除歌曲: %d条, 歌词: %d条, 空专辑: %d个",
                    result.getDeletedSongs(),
                    result.getDeletedLyrics(),
                    result.getDeletedAlbums()
                );
                asyncTaskService.completeTask(taskId, completionMessage);
                log.info("异步数据清理完成: ID={}, {}", taskId, completionMessage);
            } else {
                asyncTaskService.failTask(taskId, "清理失败: " + result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("异步数据清理失败: ID={}", taskId, e);
            if (e.getMessage() != null && e.getMessage().contains("取消")) {
                asyncTaskService.failTask(taskId, "任务已被用户取消");
            } else {
                asyncTaskService.failTask(taskId, "清理失败: " + e.getMessage());
            }
        } finally {
            runningTasks.remove(taskId);
        }
    }

    @Override
    public CleanupProgress getCleanupProgress(Long taskId) {
        com.music.system.entity.AsyncTask task = asyncTaskService.getTask(taskId);
        if (task == null) {
            return null;
        }

        CleanupProgress progress = new CleanupProgress();
        progress.setTaskId(taskId);
        progress.setStatus(task.getStatus());
        progress.setProgress(task.getProgress());
        progress.setMessage(task.getMessage());
        progress.setErrorMessage(task.getErrorMessage());

        // 如果任务完成，附带清理结果
        if ("COMPLETED".equals(task.getStatus())) {
            progress.setCleanupResult(cleanupResults.get(taskId));
        }

        return progress;
    }

    @Override
    public void cancelCleanup(Long taskId) {
        AtomicBoolean isRunning = runningTasks.get(taskId);
        if (isRunning != null) {
            isRunning.set(false);
            asyncTaskService.failTask(taskId, "任务已被用户取消");
            runningTasks.remove(taskId);
            log.info("取消数据清理任务: ID={}", taskId);
        }
    }
}
