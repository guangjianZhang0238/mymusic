package com.music.content.service;

import com.music.content.service.impl.LyricsSyncServiceImpl;

import java.util.List;

/**
 * 歌词同步服务接口
 */
public interface LyricsSyncService {
    
    /**
     * 启动歌词同步任务
     * @return 任务ID
     */
    Long startLyricsSync();
    
    /**
     * 获取同步进度
     * @param taskId 任务ID
     * @return 进度信息
     */
    LyricsSyncProgress getSyncProgress(Long taskId);
    
    /**
     * 取消同步任务
     * @param taskId 任务ID
     */
    void cancelSync(Long taskId);
    
    /**
     * 歌词同步进度信息
     */
    class LyricsSyncProgress {
        private Long taskId;
        private String status; // PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
        private Integer progress; // 0-100
        private String message;
        private String errorMessage;
        private Integer totalCount; // 总歌曲数
        private Integer processedCount; // 已处理歌曲数
        private Integer successCount; // 成功同步数
        
        // 构造函数
        public LyricsSyncProgress() {}
        
        public LyricsSyncProgress(Long taskId, String status, Integer progress, String message) {
            this.taskId = taskId;
            this.status = status;
            this.progress = progress;
            this.message = message;
        }
        
        // getters and setters
        public Long getTaskId() { return taskId; }
        public void setTaskId(Long taskId) { this.taskId = taskId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Integer getProgress() { return progress; }
        public void setProgress(Integer progress) { this.progress = progress; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public Integer getTotalCount() { return totalCount; }
        public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
        
        public Integer getProcessedCount() { return processedCount; }
        public void setProcessedCount(Integer processedCount) { this.processedCount = processedCount; }
        
        public Integer getSuccessCount() { return successCount; }
        public void setSuccessCount(Integer successCount) { this.successCount = successCount; }
    }
}