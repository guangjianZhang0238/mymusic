package com.music.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.music.system.entity.UserStats;

/**
 * 用户统计服务接口
 */
public interface UserStatsService extends IService<UserStats> {
    
    /**
     * 获取用户统计信息
     */
    UserStats getUserStats(Long userId);
    
    /**
     * 用户登录时更新统计信息
     */
    void updateUserLoginStats(Long userId);
    
    /**
     * 更新用户在线时长
     */
    void updateUserOnlineTime(Long userId, Long duration);
    
    /**
     * 更新用户播放统计
     */
    void updateUserPlayStats(Long userId);
    
    /**
     * 开始新的用户会话
     */
    void startUserSession(Long userId);
    
    /**
     * 更新当前会话时长
     */
    void updateCurrentSessionDuration(Long userId);
    
    /**
     * 初始化用户统计信息
     */
    void initializeUserStats(Long userId);
}