package com.music.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * App端用户反馈信息
 */
@Data
@Schema(description = "App端用户反馈信息")
public class AppFeedbackVO {

    @Schema(description = "反馈ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "反馈类型：LYRICS_ERROR/LYRICS_OFFSET/SONG_MISSING/OTHER/NO_LYRICS")
    private String type;

    @Schema(description = "反馈内容")
    private String content;

    @Schema(description = "关联歌曲ID（可选）")
    private Long songId;

    @Schema(description = "歌曲名称")
    private String songTitle;

    @Schema(description = "歌手名称")
    private String artistName;

    @Schema(description = "专辑名称")
    private String albumName;

    @Schema(description = "搜索关键词（可选）")
    private String keyword;

    @Schema(description = "联系方式（可选）")
    private String contact;

    @Schema(description = "反馈场景（例如 SEARCH_EMPTY）")
    private String scene;

    @Schema(description = "处理状态：PENDING-待处理/RESOLVED-已解决/FUTURE-后续版本解决/UNABLE-无法解决")
    private String status;

    @Schema(description = "处理意见")
    private String handleNote;

    @Schema(description = "处理时间")
    private LocalDateTime handleTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
