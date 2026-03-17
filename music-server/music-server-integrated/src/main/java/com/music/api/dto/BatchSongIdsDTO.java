package com.music.api.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量操作：歌曲ID列表
 */
@Data
public class BatchSongIdsDTO {
    private List<Long> songIds;
}

