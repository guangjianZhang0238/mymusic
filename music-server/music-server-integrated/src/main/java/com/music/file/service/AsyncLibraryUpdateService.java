package com.music.file.service;

import com.music.file.service.FileScanService.ScanResult;

/**
 * 异步歌曲库更新服务
 */
public interface AsyncLibraryUpdateService {
    
    /**
     * 启动异步歌曲库更新
     * @return 任务ID
     */
    Long startLibraryUpdate();
    
    /**
     * 获取更新进度
     * @param taskId 任务ID
     * @return 进度信息
     */
    LibraryUpdateProgress getUpdateProgress(Long taskId);
    
    /**
     * 取消更新任务
     * @param taskId 任务ID
     */
    void cancelUpdate(Long taskId);
    
    /**
     * 图书馆更新进度信息
     */
    class LibraryUpdateProgress {
        private Long taskId;
        private String status; // PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
        private Integer progress; // 0-100
        private String message;
        private String errorMessage;
        private ScanResult scanResult; // 扫描结果
        
        // 构造函数、getter和setter方法
        public LibraryUpdateProgress() {}
        
        public LibraryUpdateProgress(Long taskId, String status, Integer progress, String message) {
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
        
        public ScanResult getScanResult() { return scanResult; }
        public void setScanResult(ScanResult scanResult) { this.scanResult = scanResult; }
    }
}