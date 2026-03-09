package com.music.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * App端歌词分享信息
 */
@Data
@Schema(description = "App端歌词分享信息")
public class AppLyricsShareVO {

    @Schema(description = "分享ID")
    private Long id;

    @Schema(description = "歌词ID")
    private Long lyricsId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "用户头像URL")
    private String avatar;

    @Schema(description = "分享类型：text/image/link")
    private String shareType;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}