package com.music.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "小程序手机号解密请求")
public class MpPhoneDTO {

    @NotBlank(message = "code不能为空")
    @Schema(description = "手机号获取凭证", required = true)
    private String code;
}
