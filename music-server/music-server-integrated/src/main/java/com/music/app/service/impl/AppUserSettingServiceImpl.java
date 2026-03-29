package com.music.app.service.impl;

import com.music.app.service.AppUserSettingService;
import com.music.common.exception.BusinessException;
import com.music.common.utils.JwtUtils;
import com.music.common.utils.SecurityUtils;
import com.music.system.service.UserSettingService;
import com.music.system.vo.UserSettingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppUserSettingServiceImpl implements AppUserSettingService {
    
    private final UserSettingService userSettingService;
    private final JwtUtils jwtUtils;
    
    @Override
    public UserSettingVO getCurrentUserSetting(String settingKey) {
        Long userId = getCurrentUserId();
        return userSettingService.getUserSetting(userId, settingKey);
    }
    
    @Override
    public List<UserSettingVO> getCurrentUserSettings() {
        Long userId = getCurrentUserId();
        return userSettingService.getUserSettings(userId);
    }
    
    @Override
    public UserSettingVO saveOrUpdateCurrentUserSetting(String settingKey, String settingValue, String settingType, String description) {
        Long userId = getCurrentUserId();
        return userSettingService.saveOrUpdateSetting(userId, settingKey, settingValue, settingType, description);
    }
    
    @Override
    public void deleteCurrentUserSetting(String settingKey) {
        Long userId = getCurrentUserId();
        userSettingService.deleteUserSetting(userId, settingKey);
    }
    
    @Override
    public void batchSaveCurrentUserSettings(List<UserSettingVO> settings) {
        Long userId = getCurrentUserId();
        userSettingService.batchSaveSettings(userId, settings);
    }
    
    private Long getCurrentUserId() {
        Long userId = SecurityUtils.getUserId();
        if (userId != null) {
            return userId;
        }

        // 兜底方案：若 SecurityContext 未正确注入，尝试直接从 Authorization 解析 JWT
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            HttpServletRequest request = servletRequestAttributes.getRequest();
            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Bearer ")) {
                String token = authorization.substring(7).trim();
                if (!token.isEmpty() && jwtUtils.validateToken(token) && !jwtUtils.isTokenExpired(token)) {
                    Long parsedUserId = jwtUtils.getUserId(token);
                    if (parsedUserId != null) {
                        return parsedUserId;
                    }
                }
            }
        }

        throw BusinessException.of("用户未登录");
    }
}