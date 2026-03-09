package com.music.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.music.system.entity.UserSession;

import java.util.List;

/**
 * 用户会话服务接口
 */
public interface UserSessionService extends IService<UserSession> {
    
    /**
     * 创建用户会话
     */
    UserSession createUserSession(Long userId, String sessionId, String deviceInfo, String ipAddress);
    
    /**
     * 获取用户活跃会话列表
     */
    List<UserSession> getUserActiveSessions(Long userId);
    
    /**
     * 强制用户下线
     */
    boolean forceLogoutUser(Long userId);
    
    /**
     * 更新会话活跃时间
     */
    boolean updateSessionActiveTime(String sessionId);
    
    /**
     * 结束指定会话
     */
    boolean endSession(String sessionId);
    
    /**
     * 验证会话是否有效
     */
    boolean isSessionValid(String sessionId);
    
    /**
     * 清理过期会话
     */
    void cleanupExpiredSessions();
}