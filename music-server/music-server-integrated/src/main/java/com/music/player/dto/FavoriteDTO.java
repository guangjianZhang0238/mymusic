package com.music.player.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏DTO
 */
@Data
public class FavoriteDTO {
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 收藏类型：1-歌曲，2-专辑，3-歌手，4-播放列表
     */
    private Integer favoriteType;
    
    /**
     * 目标ID，根据收藏类型对应不同的ID
     */
    private Long targetId;
    
    /**
     * 目标名称（如歌曲名、专辑名等）
     */
    private String targetName;
    
    /**
     * 目标封面图片URL
     */
    private String targetCover;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
