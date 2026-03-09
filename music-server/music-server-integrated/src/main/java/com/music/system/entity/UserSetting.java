package com.music.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.music.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_setting")
public class UserSetting extends BaseEntity {
    
    private Long userId;
    private String settingKey;
    private String settingValue;
    private String settingType;
    private String description;
}