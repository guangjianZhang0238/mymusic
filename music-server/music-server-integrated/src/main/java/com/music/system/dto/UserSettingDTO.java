package com.music.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户设置请求")
public class UserSettingDTO {
    
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
}