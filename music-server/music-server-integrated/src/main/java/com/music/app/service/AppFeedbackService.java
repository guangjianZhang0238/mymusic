package com.music.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.music.app.entity.AppFeedback;
import com.music.common.core.domain.PageResult;

/**
 * App端用户反馈Service接口
 */
public interface AppFeedbackService extends IService<AppFeedback> {
    
    /**
     * 提交反馈
     */
    Long createFeedback(AppFeedback feedback);
    
    /**
     * 获取我的反馈（分页）
     */
    PageResult<AppFeedback> getMyFeedbacks(Long userId, int page, int size);
    
    /**
     * 获取所有反馈列表（管理端，分页）
     */
    PageResult<AppFeedback> getAllFeedbacks(int page, int size, String status, String type);
    
    /**
     * 处理反馈（管理端）
     */
    boolean handleFeedback(Long id, String status, String handleNote);
    
    /**
     * 检查是否重复提交（防重复机制）
     */
    boolean isDuplicateSubmission(Long userId, String type, String content);
}