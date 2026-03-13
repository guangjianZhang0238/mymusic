package com.music.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "匹配头像结果")
public class MatchAvatarResultVO {

    @Schema(description = "头像相对路径（前端展示时拼 /static/）", example = "Adele/cover.jpg")
    private String avatar;
}

