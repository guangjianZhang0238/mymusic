package com.music.player.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 播放历史DTO
 */
@Data
public class PlayHistoryDTO {
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 歌曲ID
     */
    private Long songId;
    
    /**
     * 歌曲标题
     */
    private String songTitle;
    
    /**
     * 艺术家名称
     */
    private String artistName;
    
    /**
     * 专辑名称
     */
    private String albumName;
    
    /**
     * 封面图片URL
     */
    private String coverImage;
    
    /**
     * 歌曲总时长（秒）
     */
    private Integer duration;
    
    /**
     * 播放时间
     */
    private LocalDateTime playTime;
    
    /**
     * 实际播放时长（秒）
     */
    private Integer durationPlayed;
    
}
