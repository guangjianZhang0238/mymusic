package com.music.api.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量将歌曲收录到专辑的请求参数
 */
@Data
public class AlbumSongBindDTO {

    /**
     * 要收录到专辑中的歌曲ID列表
     */
    private List<Long> songIds;
}

