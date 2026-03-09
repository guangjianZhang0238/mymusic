package com.music.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.music.system.entity.UserStats;
import com.music.system.mapper.UserStatsMapper;
import com.music.system.service.UserStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户统计服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatsServiceImpl extends ServiceImpl<UserStatsMapper, UserStats> implements UserStatsService {
    
    private final UserStatsMapper userStatsMapper;
    
    @Override
    public UserStats getUserStats(Long userId) {
        UserStats stats = userStatsMapper.getByUserId(userId);
        if (stats == null) {
            // 如果不存在，初始化用户统计信息
            initializeUserStats(userId);
            stats = userStatsMapper.getByUserId(userId);
        }
        return stats;
    }
    
    @Override
    @Transactional
    public void updateUserLoginStats(Long userId) {
        UserStats stats = getUserStats(userId);
        if (stats == null) {
            initializeUserStats(userId);
        }
        
        // 增加登录次数
        userStatsMapper.incrementLoginCount(userId);
        
        // 开始新的会话
        userStatsMapper.startNewSession(userId);
        
        log.info("用户 {} 登录统计已更新", userId);
    }
    
    @Override
    @Transactional
    public void updateUserOnlineTime(Long userId, Long duration) {
        UserStats stats = getUserStats(userId);
        if (stats != null) {
            userStatsMapper.updateOnlineTime(userId, duration);
            log.debug("用户 {} 在线时长已更新: {}秒", userId, duration);
        }
    }
    
    @Override
    @Transactional
    public void updateUserPlayStats(Long userId) {
        UserStats stats = getUserStats(userId);
        if (stats != null) {
            userStatsMapper.incrementPlayCount(userId);
            log.debug("用户 {} 播放统计已更新", userId);
        }
    }
    
    @Override
    @Transactional
    public void startUserSession(Long userId) {
        UserStats stats = getUserStats(userId);
        if (stats != null) {
            userStatsMapper.startNewSession(userId);
            log.info("用户 {} 新会话已开始", userId);
        }
    }
    
    @Override
    @Transactional
    public void updateCurrentSessionDuration(Long userId) {
        UserStats stats = getUserStats(userId);
        if (stats != null && stats.getCurrentSessionStart() != null) {
            userStatsMapper.updateCurrentSessionDuration(userId);
            log.debug("用户 {} 当前会话时长已更新", userId);
        }
    }
    
    @Override
    @Transactional
    public void initializeUserStats(Long userId) {
        UserStats stats = new UserStats();
        stats.setUserId(userId);
        stats.setLoginCount(0);
        stats.setTotalOnlineTime(0L);
        stats.setTotalPlayCount(0L);
        stats.setCurrentSessionDuration(0L);
        save(stats);
        log.info("用户 {} 统计信息已初始化", userId);
    }
}