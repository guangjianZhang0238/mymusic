package com.music.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.music.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户会话实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_session")
public class UserSession extends BaseEntity {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 设备信息
     */
    private String deviceInfo;
    
    /**
     * IP地址
     */
    private String ipAddress;
    
    /**
     * 登录时间
     */
    private LocalDateTime loginTime;
    
    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveTime;
    
    /**
     * 退出时间
     */
    private LocalDateTime logoutTime;
    
    /**
     * 是否活跃：1-活跃，0-已退出
     */
    private Integer isActive;
}