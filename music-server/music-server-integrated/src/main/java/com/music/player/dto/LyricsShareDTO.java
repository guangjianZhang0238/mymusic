package com.music.player.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 歌词分享DTO
 */
@Data
public class LyricsShareDTO {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 歌词ID
     */
    private Long lyricsId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 用户昵称
     */
    private String nickname;
    
    /**
     * 用户头像URL
     */
    private String avatar;
    
    /**
     * 分享类型：text/image/link
     */
    private String shareType;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}