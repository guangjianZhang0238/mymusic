package com.music.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "App端歌曲信息")
public class AppSongVO {

    @Schema(description = "歌曲ID")
    private Long id;

    @Schema(description = "歌曲名称")
    private String title;

    @Schema(description = "歌曲英文名")
    private String titleEn;

    @Schema(description = "歌手名")
    private String artistName;

    @Schema(description = "所有歌手名称（主+合作），以 / 分隔")
    private String artistNames;

    @Schema(description = "专辑ID")
    private Long albumId;

    @Schema(description = "专辑名")
    private String albumName;

    @Schema(description = "专辑封面")
    private String albumCover;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "是否有歌词")
    private Integer hasLyrics;

    @Schema(description = "时长（秒）")
    private Integer duration;

    @Schema(description = "时长格式化")
    private String durationFormat;

    @Schema(description = "播放次数")
    private Integer playCount;
}
