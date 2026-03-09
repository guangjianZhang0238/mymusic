package com.music.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.music.system.entity.UserSession;
import com.music.system.mapper.UserSessionMapper;
import com.music.system.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 用户会话服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSessionServiceImpl extends ServiceImpl<UserSessionMapper, UserSession> implements UserSessionService {
    
    private final UserSessionMapper userSessionMapper;
    
    @Override
    @Transactional
    public UserSession createUserSession(Long userId, String sessionId, String deviceInfo, String ipAddress) {
        UserSession session = new UserSession();
        session.setUserId(userId);
        session.setSessionId(sessionId);
        session.setDeviceInfo(deviceInfo);
        session.setIpAddress(ipAddress);
        session.setIsActive(1);
        session.setLoginTime(LocalDateTime.now());
        session.setLastActiveTime(LocalDateTime.now());
        
        save(session);
        log.info("用户 {} 创建新会话: {}", userId, sessionId);
        return session;
    }
    
    @Override
    public List<UserSession> getUserActiveSessions(Long userId) {
        return userSessionMapper.getUserActiveSessions(userId);
    }
    
    @Override
    @Transactional
    public boolean forceLogoutUser(Long userId) {
        try {
            int affectedRows = userSessionMapper.forceLogoutUser(userId);
            log.info("强制用户 {} 下线，结束 {} 个活跃会话", userId, affectedRows);
            return affectedRows > 0;
        } catch (Exception e) {
            log.error("强制用户下线失败，userId={}", userId, e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean updateSessionActiveTime(String sessionId) {
        try {
            int affectedRows = userSessionMapper.updateLastActiveTime(sessionId);
            if (affectedRows > 0) {
                log.debug("更新会话 {} 活跃时间", sessionId);
            }
            return affectedRows > 0;
        } catch (Exception e) {
            log.error("更新会话活跃时间失败，sessionId={}", sessionId, e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean endSession(String sessionId) {
        try {
            int affectedRows = userSessionMapper.endSession(sessionId);
            if (affectedRows > 0) {
                log.info("结束会话: {}", sessionId);
            }
            return affectedRows > 0;
        } catch (Exception e) {
            log.error("结束会话失败，sessionId={}", sessionId, e);
            return false;
        }
    }
    
    @Override
    public boolean isSessionValid(String sessionId) {
        try {
            UserSession session = userSessionMapper.getBySessionId(sessionId);
            if (session == null || session.getIsActive() == 0) {
                return false;
            }
            
            // 检查会话是否过期（30分钟无活动）
            LocalDateTime lastActive = session.getLastActiveTime();
            LocalDateTime now = LocalDateTime.now();
            long minutesSinceActive = java.time.Duration.between(lastActive, now).toMinutes();
            
            if (minutesSinceActive > 30) {
                // 会话已过期，自动结束
                endSession(sessionId);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("验证会话有效性失败，sessionId={}", sessionId, e);
            return false;
        }
    }
    
    @Override
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    @Transactional
    public void cleanupExpiredSessions() {
        try {
            LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(30);
            List<UserSession> expiredSessions = lambdaQuery()
                    .eq(UserSession::getIsActive, 1)
                    .lt(UserSession::getLastActiveTime, expiredTime)
                    .list();
            
            for (UserSession session : expiredSessions) {
                endSession(session.getSessionId());
            }
            
            log.info("清理过期会话完成，共清理 {} 个会话", expiredSessions.size());
        } catch (Exception e) {
            log.error("清理过期会话失败", e);
        }
    }
}