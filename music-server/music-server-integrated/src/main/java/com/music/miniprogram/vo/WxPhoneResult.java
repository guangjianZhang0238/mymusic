package com.music.miniprogram.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WxPhoneResult {

    @JsonProperty("phone_info")
    private PhoneInfo phoneInfo;

    @JsonProperty("errcode")
    private Integer errCode;

    @JsonProperty("errmsg")
    private String errMsg;

    @Data
    public static class PhoneInfo {
        @JsonProperty("phoneNumber")
        private String phoneNumber;

        @JsonProperty("purePhoneNumber")
        private String purePhoneNumber;

        @JsonProperty("countryCode")
        private String countryCode;
    }
}
