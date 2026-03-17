package com.music.api.dto;

import lombok.Data;

/**
 * 切换专辑歌手请求
 */
@Data
public class SwitchAlbumArtistDTO {
    private Long targetArtistId;
    private String targetArtistName;
}

