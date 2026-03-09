package com.music.app.service;

import com.music.system.vo.UserSettingVO;

import java.util.List;

public interface AppUserSettingService {
    
    /**
     * 获取当前用户特定设置
     */
    UserSettingVO getCurrentUserSetting(String settingKey);
    
    /**
     * 获取当前用户所有设置
     */
    List<UserSettingVO> getCurrentUserSettings();
    
    /**
     * 保存或更新当前用户设置
     */
    UserSettingVO saveOrUpdateCurrentUserSetting(String settingKey, String settingValue, String settingType, String description);
    
    /**
     * 删除当前用户设置
     */
    void deleteCurrentUserSetting(String settingKey);
    
    /**
     * 批量保存当前用户设置
     */
    void batchSaveCurrentUserSettings(List<UserSettingVO> settings);
}