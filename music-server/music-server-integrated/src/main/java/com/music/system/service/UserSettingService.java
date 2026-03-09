package com.music.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.music.system.entity.UserSetting;
import com.music.system.vo.UserSettingVO;

import java.util.List;

public interface UserSettingService extends IService<UserSetting> {
    
    /**
     * 获取用户特定设置
     */
    UserSettingVO getUserSetting(Long userId, String settingKey);
    
    /**
     * 获取用户所有设置
     */
    List<UserSettingVO> getUserSettings(Long userId);
    
    /**
     * 保存或更新用户设置
     */
    UserSettingVO saveOrUpdateSetting(Long userId, String settingKey, String settingValue, String settingType, String description);
    
    /**
     * 删除用户设置
     */
    void deleteUserSetting(Long userId, String settingKey);
    
    /**
     * 批量保存用户设置
     */
    void batchSaveSettings(Long userId, List<UserSettingVO> settings);
}