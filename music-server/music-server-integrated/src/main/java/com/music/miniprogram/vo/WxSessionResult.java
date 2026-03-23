package com.music.miniprogram.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WxSessionResult {

    @JsonProperty("openid")
    private String openId;

    @JsonProperty("unionid")
    private String unionId;

    @JsonProperty("session_key")
    private String sessionKey;

    @JsonProperty("errcode")
    private Integer errCode;

    @JsonProperty("errmsg")
    private String errMsg;
}
