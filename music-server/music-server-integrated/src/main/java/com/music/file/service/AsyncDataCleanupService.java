package com.music.file.service;

import com.music.file.service.DataCleanupService.CleanupResult;

/**
 * 异步数据清理服务
 */
public interface AsyncDataCleanupService {

    /**
     * 启动异步数据清理任务
     * @return 任务ID
     */
    Long startCleanup();

    /**
     * 获取清理进度
     * @param taskId 任务ID
     * @return 进度信息
     */
    CleanupProgress getCleanupProgress(Long taskId);

    /**
     * 取消清理任务
     * @param taskId 任务ID
     */
    void cancelCleanup(Long taskId);

    /**
     * 清理进度信息
     */
    class CleanupProgress {
        private Long taskId;
        private String status;
        private Integer progress;
        private String message;
        private String errorMessage;
        private CleanupResult cleanupResult;

        public CleanupProgress() {}

        public CleanupProgress(Long taskId, String status, Integer progress, String message) {
            this.taskId = taskId;
            this.status = status;
            this.progress = progress;
            this.message = message;
        }

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

        public CleanupResult getCleanupResult() { return cleanupResult; }
        public void setCleanupResult(CleanupResult cleanupResult) { this.cleanupResult = cleanupResult; }
    }
}
