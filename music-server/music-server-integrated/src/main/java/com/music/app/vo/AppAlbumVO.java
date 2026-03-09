package com.music.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "App端专辑信息")
public class AppAlbumVO {

    @Schema(description = "专辑ID")
    private Long id;

    @Schema(description = "专辑名")
    private String name;

    @Schema(description = "专辑英文名")
    private String nameEn;

    @Schema(description = "歌手名")
    private String artistName;

    @Schema(description = "专辑封面")
    private String coverImage;

    @Schema(description = "歌曲数")
    private Integer songCount;

    @Schema(description = "发行日期")
    private LocalDate releaseDate;

    @Schema(description = "专辑简介")
    private String description;
}
