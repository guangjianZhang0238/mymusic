package com.music.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.music.common.exception.BusinessException;
import com.music.system.entity.AsyncTask;
import com.music.system.mapper.AsyncTaskMapper;
import com.music.system.service.AsyncTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 异步任务服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncTaskServiceImpl implements AsyncTaskService {
    
    private final AsyncTaskMapper asyncTaskMapper;
    
    @Override
    @Transactional
    public Long createTask(String taskType, String description) {
        AsyncTask task = new AsyncTask();
        task.setTaskType(taskType);
        task.setDescription(description);
        task.setStatus("PENDING");
        task.setProgress(0);
        task.setMessage("任务已创建");
        task.setStartTime(LocalDateTime.now());
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        
        asyncTaskMapper.insert(task);
        log.info("创建异步任务: ID={}, 类型={}, 描述={}", task.getId(), taskType, description);
        return task.getId();
    }
    
    @Override
    @Transactional
    public void updateProgress(Long taskId, Integer progress, String message) {
        AsyncTask task = asyncTaskMapper.selectById(taskId);
        if (task == null) {
            throw BusinessException.of("任务不存在: " + taskId);
        }
        
        if (!"PENDING".equals(task.getStatus()) && !"RUNNING".equals(task.getStatus())) {
            throw BusinessException.of("任务状态不允许更新进度: " + task.getStatus());
        }
        
        task.setStatus("RUNNING");
        task.setProgress(progress);
        task.setMessage(message);
        task.setUpdateTime(LocalDateTime.now());
        
        asyncTaskMapper.updateById(task);
        log.debug("更新任务进度: ID={}, 进度={}%, 消息={}", taskId, progress, message);
    }
    
    @Override
    @Transactional
    public void completeTask(Long taskId, String message) {
        AsyncTask task = asyncTaskMapper.selectById(taskId);
        if (task == null) {
            throw BusinessException.of("任务不存在: " + taskId);
        }
        
        task.setStatus("COMPLETED");
        task.setProgress(100);
        task.setMessage(message);
        task.setEndTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        
        asyncTaskMapper.updateById(task);
        log.info("任务完成: ID={}, 消息={}", taskId, message);
    }
    
    @Override
    @Transactional
    public void failTask(Long taskId, String errorMessage) {
        AsyncTask task = asyncTaskMapper.selectById(taskId);
        if (task == null) {
            throw BusinessException.of("任务不存在: " + taskId);
        }
        
        task.setStatus("FAILED");
        task.setErrorMessage(errorMessage);
        task.setEndTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        
        asyncTaskMapper.updateById(task);
        log.error("任务失败: ID={}, 错误={}", taskId, errorMessage);
    }
    
    @Override
    public AsyncTask getTask(Long taskId) {
        return asyncTaskMapper.selectById(taskId);
    }
    
    @Override
    public List<AsyncTask> getRunningTasks() {
        QueryWrapper<AsyncTask> wrapper = new QueryWrapper<>();
        wrapper.in("status", "PENDING", "RUNNING");
        wrapper.orderByDesc("create_time");
        return asyncTaskMapper.selectList(wrapper);
    }
    
    @Override
    @Transactional
    public void cleanupCompletedTasks(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        QueryWrapper<AsyncTask> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "COMPLETED");
        wrapper.lt("end_time", cutoffDate);
        
        int deletedCount = asyncTaskMapper.delete(wrapper);
        log.info("清理已完成任务: 删除{}条记录，保留{}天内的记录", deletedCount, days);
    }
}