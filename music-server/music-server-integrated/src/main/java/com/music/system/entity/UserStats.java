package com.music.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.music.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户统计实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_stats")
public class UserStats extends BaseEntity {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 登录次数
     */
    private Integer loginCount;
    
    /**
     * 总在线时长(秒)
     */
    private Long totalOnlineTime;
    
    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;
    
    /**
     * 最后退出时间
     */
    private LocalDateTime lastLogoutTime;
    
    /**
     * 当前会话开始时间
     */
    private LocalDateTime currentSessionStart;
    
    /**
     * 当前会话时长(秒)
     */
    private Long currentSessionDuration;
    
    /**
     * 总播放歌曲数
     */
    private Long totalPlayCount;
    
    /**
     * 最后播放时间
     */
    private LocalDateTime lastPlayTime;
}