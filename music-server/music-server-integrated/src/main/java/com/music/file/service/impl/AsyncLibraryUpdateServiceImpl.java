package com.music.file.service.impl;

import com.music.file.service.AsyncLibraryUpdateService;
import com.music.file.service.FileScanService;
import com.music.system.service.AsyncTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 异步歌曲库更新服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncLibraryUpdateServiceImpl implements AsyncLibraryUpdateService {
    
    private final FileScanService fileScanService;
    private final AsyncTaskService asyncTaskService;
    
    // 存储正在运行的任务状态
    private final ConcurrentHashMap<Long, AtomicBoolean> runningTasks = new ConcurrentHashMap<>();
    
    @Override
    public Long startLibraryUpdate() {
        // 创建异步任务
        Long taskId = asyncTaskService.createTask("LIBRARY_UPDATE", "更新歌曲库");
        
        // 标记任务为运行中
        runningTasks.put(taskId, new AtomicBoolean(true));
        
        // 异步执行更新
        performLibraryUpdateAsync(taskId);
        
        log.info("启动异步歌曲库更新任务: ID={}", taskId);
        return taskId;
    }
    
    @Async
    public void performLibraryUpdateAsync(Long taskId) {
        try {
            asyncTaskService.updateProgress(taskId, 10, "开始扫描音乐库...");
            
            // 执行扫描
            FileScanService.ScanResult result = fileScanService.scanMusicLibrary();
            
            asyncTaskService.updateProgress(taskId, 80, "扫描完成，正在处理结果...");
            
            // 检查任务是否被取消
            AtomicBoolean isRunning = runningTasks.get(taskId);
            if (isRunning != null && !isRunning.get()) {
                asyncTaskService.failTask(taskId, "任务已被用户取消");
                return;
            }
            
            // 完成任务
            String completionMessage = String.format(
                "歌曲库更新完成！新增: %d首, 更新: %d首, 跳过: %d首, 错误: %d个",
                result.getAddedSongs(),
                result.getUpdatedSongs(),
                result.getSkippedSongs(),
                result.getErrors().size()
            );
            
            asyncTaskService.completeTask(taskId, completionMessage);
            
            log.info("异步歌曲库更新完成: ID={}, {}", taskId, completionMessage);
            
        } catch (Exception e) {
            log.error("异步歌曲库更新失败: ID={}", taskId, e);
            asyncTaskService.failTask(taskId, "更新失败: " + e.getMessage());
        } finally {
            // 清理任务状态
            runningTasks.remove(taskId);
        }
    }
    
    @Override
    public LibraryUpdateProgress getUpdateProgress(Long taskId) {
        com.music.system.entity.AsyncTask task = asyncTaskService.getTask(taskId);
        if (task == null) {
            return null;
        }
        
        LibraryUpdateProgress progress = new LibraryUpdateProgress();
        progress.setTaskId(taskId);
        progress.setStatus(task.getStatus());
        progress.setProgress(task.getProgress());
        progress.setMessage(task.getMessage());
        progress.setErrorMessage(task.getErrorMessage());
        
        return progress;
    }
    
    @Override
    public void cancelUpdate(Long taskId) {
        AtomicBoolean isRunning = runningTasks.get(taskId);
        if (isRunning != null) {
            isRunning.set(false);
            asyncTaskService.failTask(taskId, "任务已被用户取消");
            runningTasks.remove(taskId);
            log.info("取消歌曲库更新任务: ID={}", taskId);
        }
    }
}