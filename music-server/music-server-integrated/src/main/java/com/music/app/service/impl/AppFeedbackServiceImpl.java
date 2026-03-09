package com.music.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.music.app.entity.AppFeedback;
import com.music.app.mapper.AppFeedbackMapper;
import com.music.app.service.AppFeedbackService;
import com.music.common.core.domain.PageResult;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * App端用户反馈Service实现类
 */
@Service
public class AppFeedbackServiceImpl extends ServiceImpl<AppFeedbackMapper, AppFeedback> implements AppFeedbackService {
    
    // 反馈状态常量
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_RESOLVED = "RESOLVED";
    private static final String STATUS_FUTURE = "FUTURE";
    private static final String STATUS_UNABLE = "UNABLE";
    
    // 防重复提交时间窗口（5分钟）
    private static final long DUPLICATE_THRESHOLD_MINUTES = 5;
    
    @Override
    public Long createFeedback(AppFeedback feedback) {
        feedback.setStatus(STATUS_PENDING);
        feedback.setCreateTime(LocalDateTime.now());
        feedback.setUpdateTime(LocalDateTime.now());
        feedback.setDeleted(0);
        
        if (this.save(feedback)) {
            return feedback.getId();
        }
        return null;
    }
    
    @Override
    public PageResult<AppFeedback> getMyFeedbacks(Long userId, int page, int size) {
        Page<AppFeedback> pageObj = new Page<>(page, size);
        QueryWrapper<AppFeedback> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                   .eq("deleted", 0)
                   .orderByDesc("create_time");
        
        Page<AppFeedback> result = this.page(pageObj, queryWrapper);
        
        return PageResult.of(
            result.getRecords(),
            result.getTotal(),
            result.getSize(),
            result.getCurrent()
        );
    }
    
    @Override
    public PageResult<AppFeedback> getAllFeedbacks(int page, int size, String status, String type) {
        Page<AppFeedback> pageObj = new Page<>(page, size);
        QueryWrapper<AppFeedback> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0);
        
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq("status", status);
        }
        
        if (type != null && !type.isEmpty()) {
            queryWrapper.eq("type", type);
        }
        
        queryWrapper.orderByDesc("create_time");
        
        Page<AppFeedback> result = this.page(pageObj, queryWrapper);
        
        return PageResult.of(
            result.getRecords(),
            result.getTotal(),
            result.getSize(),
            result.getCurrent()
        );
    }
    
    @Override
    public boolean handleFeedback(Long id, String status, String handleNote) {
        AppFeedback feedback = this.getById(id);
        if (feedback == null) {
            return false;
        }
        
        feedback.setStatus(status);
        feedback.setHandleNote(handleNote);
        feedback.setHandleTime(LocalDateTime.now());
        feedback.setUpdateTime(LocalDateTime.now());
        
        return this.updateById(feedback);
    }
    
    @Override
    public boolean isDuplicateSubmission(Long userId, String type, String content) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(DUPLICATE_THRESHOLD_MINUTES);
        
        QueryWrapper<AppFeedback> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                   .eq("type", type)
                   .eq("content", content)
                   .eq("deleted", 0)
                   .ge("create_time", cutoffTime);
        
        return this.count(queryWrapper) > 0;
    }
}