package com.music.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "App端歌手信息")
public class AppArtistVO {

    @Schema(description = "歌手ID")
    private Long id;

    @Schema(description = "歌手名")
    private String name;

    @Schema(description = "歌手英文名")
    private String nameEn;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "简介")
    private String description;

    @Schema(description = "地区")
    private String region;

    @Schema(description = "专辑数")
    private Integer albumCount;

    @Schema(description = "歌曲数")
    private Integer songCount;
}
