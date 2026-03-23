package com.music.miniprogram.vo;

import com.music.api.vo.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "小程序登录响应")
public class MpLoginVO {

    @Schema(description = "登录token")
    private String token;

    @Schema(description = "用户信息")
    private UserVO userInfo;
}
