package com.music.content.service.impl;

import com.music.common.exception.BusinessException;
import com.music.content.entity.Artist;
import com.music.content.entity.Song;
import com.music.content.mapper.ArtistMapper;
import com.music.content.mapper.SongMapper;
import com.music.content.service.LyricsService;
import com.music.content.service.LyricsSyncService;
import com.music.system.entity.AsyncTask;
import com.music.system.service.AsyncTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 歌词同步服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LyricsSyncServiceImpl implements LyricsSyncService {
    
    private final LyricsService lyricsService;
    private final SongMapper songMapper;
    private final ArtistMapper artistMapper;
    private final AsyncTaskService asyncTaskService;
    
    // 存储正在运行的任务状态
    private final ConcurrentHashMap<Long, TaskState> runningTasks = new ConcurrentHashMap<>();
    
    @Override
    public Long startLyricsSync() {
        // 创建异步任务
        Long taskId = asyncTaskService.createTask("LYRICS_SYNC", "同步歌词");
        
        // 初始化任务状态
        TaskState taskState = new TaskState();
        taskState.isRunning = new AtomicBoolean(true);
        taskState.totalCount = new AtomicInteger(0);
        taskState.processedCount = new AtomicInteger(0);
        taskState.successCount = new AtomicInteger(0);
        
        runningTasks.put(taskId, taskState);
        
        // 异步执行同步
        performLyricsSyncAsync(taskId);
        
        log.info("启动异步歌词同步任务: ID={}", taskId);
        return taskId;
    }
    
    @Async
    public void performLyricsSyncAsync(Long taskId) {
        TaskState taskState = runningTasks.get(taskId);
        if (taskState == null) {
            return;
        }
        
        try {
            asyncTaskService.updateProgress(taskId, 5, "正在查询需要同步的歌曲...");
            
            // 查询未同步歌词的歌曲
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Song> wrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            wrapper.and(w -> w
                .eq(Song::getHasLyrics, 0)
                .or()
                .isNull(Song::getLyricsId));
            
            List<Song> songsWithoutLyrics = songMapper.selectList(wrapper);
            
            int totalCount = songsWithoutLyrics.size();
            taskState.totalCount.set(totalCount);
            
            log.info("找到 {} 首需要同步歌词的歌曲", totalCount);
            
            if (totalCount == 0) {
                asyncTaskService.completeTask(taskId, "没有需要同步的歌曲");
                return;
            }
            
            asyncTaskService.updateProgress(taskId, 10, String.format("开始同步 %d 首歌曲的歌词...", totalCount));
            
            int processedCount = 0;
            int successCount = 0;
            
            // 逐个处理歌曲
            for (int i = 0; i < songsWithoutLyrics.size(); i++) {
                Song song = songsWithoutLyrics.get(i);
                
                // 检查任务是否被取消
                if (!taskState.isRunning.get()) {
                    asyncTaskService.failTask(taskId, "任务已被用户取消");
                    return;
                }
                
                try {
                    // 调用歌词服务进行同步
                    String message = String.format("正在同步歌曲: %s (%d/%d)", 
                        song.getTitle(), i + 1, totalCount);
                    asyncTaskService.updateProgress(taskId, 
                        10 + (i * 80 / totalCount), message);
                    
                    // 执行歌词同步
                    boolean success = syncSingleSongLyrics(song);
                    if (success) {
                        successCount++;
                    }
                    
                } catch (Exception e) {
                    log.warn("同步歌曲ID {} 的歌词失败: {}", song.getId(), e.getMessage());
                }
                
                processedCount++;
                taskState.processedCount.set(processedCount);
                taskState.successCount.set(successCount);
                
                // 更新进度
                int progress = 10 + (processedCount * 80 / totalCount);
                String message = String.format("已处理 %d/%d 首歌曲，成功同步 %d 首", 
                    processedCount, totalCount, successCount);
                asyncTaskService.updateProgress(taskId, progress, message);
                
                // 短暂延迟避免API调用过于频繁
                Thread.sleep(100);
            }
            
            // 完成任务
            String completionMessage = String.format(
                "歌词同步完成！总共处理: %d首, 成功同步: %d首", 
                processedCount, successCount);
            
            asyncTaskService.completeTask(taskId, completionMessage);
            log.info("异步歌词同步完成: ID={}, {}", taskId, completionMessage);
            
        } catch (Exception e) {
            log.error("异步歌词同步失败: ID={}", taskId, e);
            asyncTaskService.failTask(taskId, "同步失败: " + e.getMessage());
        } finally {
            // 清理任务状态
            runningTasks.remove(taskId);
        }
    }
    
    /**
     * 同步单首歌曲的歌词
     */
    private boolean syncSingleSongLyrics(Song song) {
        try {
            // 调用现有的歌词同步方法
            String songName = song.getTitle();
            String artistName = getArtistName(song.getArtistId());
            
            if (songName != null && artistName != null) {
                String apiUrl = String.format("https://api.lrc.cx/lyrics?title=%s&artistName=%s", 
                    songName, artistName);
                
                // 直接调用歌词服务的公共方法
                java.util.Map<String, Object> result = 
                    lyricsService.autoMatchLyricsFromApi(apiUrl, song.getId());
                
                return Boolean.TRUE.equals(result.get("success"));
            }
        } catch (Exception e) {
            log.warn("同步歌曲 {} 的歌词时出错: {}", song.getTitle(), e.getMessage());
        }
        return false;
    }
    
    /**
     * 获取歌手名称
     */
    private String getArtistName(Long artistId) {
        if (artistId == null) return null;
        
        try {
            Artist artist = artistMapper.selectById(artistId);
            return artist != null ? artist.getName() : null;
        } catch (Exception e) {
            log.warn("获取歌手ID {} 的名称失败", artistId, e);
            return null;
        }
    }
    
    @Override
    public LyricsSyncProgress getSyncProgress(Long taskId) {
        com.music.system.entity.AsyncTask task = asyncTaskService.getTask(taskId);
        if (task == null) {
            return null;
        }
        
        LyricsSyncProgress progress = new LyricsSyncProgress();
        progress.setTaskId(taskId);
        progress.setStatus(task.getStatus());
        progress.setProgress(task.getProgress());
        progress.setMessage(task.getMessage());
        progress.setErrorMessage(task.getErrorMessage());
        
        // 设置详细统计信息
        TaskState taskState = runningTasks.get(taskId);
        if (taskState != null) {
            progress.setTotalCount(taskState.totalCount.get());
            progress.setProcessedCount(taskState.processedCount.get());
            progress.setSuccessCount(taskState.successCount.get());
        }
        
        return progress;
    }
    
    @Override
    public void cancelSync(Long taskId) {
        TaskState taskState = runningTasks.get(taskId);
        if (taskState != null) {
            taskState.isRunning.set(false);
            asyncTaskService.failTask(taskId, "任务已被用户取消");
            runningTasks.remove(taskId);
            log.info("取消歌词同步任务: ID={}", taskId);
        }
    }
    
    /**
     * 任务状态内部类
     */
    private static class TaskState {
        AtomicBoolean isRunning;
        AtomicInteger totalCount;
        AtomicInteger processedCount;
        AtomicInteger successCount;
    }
}