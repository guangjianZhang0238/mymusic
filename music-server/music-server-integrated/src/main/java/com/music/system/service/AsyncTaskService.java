package com.music.system.service;

import com.music.system.entity.AsyncTask;

import java.util.List;

/**
 * 异步任务服务
 */
public interface AsyncTaskService {
    
    /**
     * 创建异步任务
     * @param taskType 任务类型
     * @param description 任务描述
     * @return 任务ID
     */
    Long createTask(String taskType, String description);
    
    /**
     * 更新任务进度
     * @param taskId 任务ID
     * @param progress 进度百分比
     * @param message 处理消息
     */
    void updateProgress(Long taskId, Integer progress, String message);
    
    /**
     * 完成任务
     * @param taskId 任务ID
     * @param message 完成消息
     */
    void completeTask(Long taskId, String message);
    
    /**
     * 任务失败
     * @param taskId 任务ID
     * @param errorMessage 错误信息
     */
    void failTask(Long taskId, String errorMessage);
    
    /**
     * 获取任务详情
     * @param taskId 任务ID
     * @return 任务详情
     */
    AsyncTask getTask(Long taskId);
    
    /**
     * 获取所有运行中的任务
     * @return 任务列表
     */
    List<AsyncTask> getRunningTasks();
    
    /**
     * 清理已完成的任务（保留最近的记录）
     * @param days 保留天数
     */
    void cleanupCompletedTasks(int days);
}