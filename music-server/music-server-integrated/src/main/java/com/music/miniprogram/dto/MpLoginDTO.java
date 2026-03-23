package com.music.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "小程序登录请求")
public class MpLoginDTO {

    @NotBlank(message = "code不能为空")
    @Schema(description = "微信登录code", required = true)
    private String code;

    @NotBlank(message = "手机号凭证不能为空")
    @Schema(description = "手机号获取凭证", required = true)
    private String phoneCode;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatar;
}
