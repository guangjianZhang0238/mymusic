package com.music.app.service.impl;

import com.music.app.service.AppUserSettingService;
import com.music.common.exception.BusinessException;
import com.music.common.utils.SecurityUtils;
import com.music.system.service.UserSettingService;
import com.music.system.vo.UserSettingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppUserSettingServiceImpl implements AppUserSettingService {
    
    private final UserSettingService userSettingService;
    
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
        if (userId == null) {
            throw BusinessException.of("用户未登录");
        }
        return userId;
    }
}