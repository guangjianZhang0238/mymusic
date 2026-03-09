package com.music.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户密码修改DTO
 */
@Data
@Schema(description = "用户密码修改DTO")
public class UserPasswordDTO {
    
    @Schema(description = "新密码")
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}