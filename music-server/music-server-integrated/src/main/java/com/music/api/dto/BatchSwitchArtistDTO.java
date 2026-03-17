package com.music.api.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量切换歌曲歌手（可选专辑）
 */
@Data
public class BatchSwitchArtistDTO {
    private List<Long> songIds;

    /**
     * 目标歌手ID（优先使用）
     */
    private Long targetArtistId;

    /**
     * 目标歌手名称（仅用于兜底校验/日志）
     */
    private String targetArtistName;

    /**
     * 目标专辑名称（可空：默认专辑）
     */
    private String targetAlbumName;
}

