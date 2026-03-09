package com.music.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * App端播放列表信息
 */
@Data
@Schema(description = "App端播放列表信息")
public class AppPlaylistVO {

    @Schema(description = "播放列表ID")
    private Long id;

    @Schema(description = "创建者用户ID")
    private Long userId;

    @Schema(description = "播放列表名称")
    private String name;

    @Schema(description = "封面图片URL")
    private String coverImage;

    @Schema(description = "播放列表描述")
    private String description;

    @Schema(description = "是否公开：1-公开，0-私有")
    private Integer isPublic;

    @Schema(description = "歌曲数量")
    private Integer songCount;

    @Schema(description = "播放次数")
    private Integer playCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}