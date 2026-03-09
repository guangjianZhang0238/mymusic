package com.music.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户设置响应")
public class UserSettingVO {
    
    @Schema(description = "设置ID")
    private Long id;
    
    @Schema(description = "用户ID")
    private Long userId;
    
    @Schema(description = "设置键")
    private String settingKey;
    
    @Schema(description = "设置值")
    private String settingValue;
    
    @Schema(description = "设置类型")
    private String settingType;
    
    @Schema(description = "描述")
    private String description;
    
    @Schema(description = "创建时间")
    private java.time.LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private java.time.LocalDateTime updateTime;
}