package com.music.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户管理VO类
 */
@Data
@Schema(description = "用户管理信息")
public class UserManageVO {
    
    @Schema(description = "用户ID")
    private Long id;
    
    @Schema(description = "用户名")
    private String username;
    
    @Schema(description = "昵称")
    private String nickname;
    
    @Schema(description = "头像")
    private String avatar;
    
    @Schema(description = "手机号")
    private String phone;
    
    @Schema(description = "邮箱")
    private String email;
    
    @Schema(description = "角色：0-普通用户 1-管理员")
    private Integer role;
    
    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;
    
    // 用户统计信息
    @Schema(description = "登录次数")
    private Integer loginCount;
    
    @Schema(description = "总在线时长(秒)")
    private Long totalOnlineTime;
    
    @Schema(description = "当前会话开始时间")
    private LocalDateTime currentSessionStart;
    
    @Schema(description = "当前会话时长(秒)")
    private Long currentSessionDuration;
    
    @Schema(description = "总播放歌曲数")
    private Long totalPlayCount;
    
    @Schema(description = "最后播放时间")
    private LocalDateTime lastPlayTime;
    
    @Schema(description = "是否在线")
    private Boolean isOnline;
}