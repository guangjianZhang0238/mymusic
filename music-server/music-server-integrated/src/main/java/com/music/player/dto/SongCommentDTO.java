package com.music.player.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 歌曲评论DTO
 */
@Data
public class SongCommentDTO {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 歌曲ID
     */
    private Long songId;
    
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
     * 用户头像
     */
    private String avatar;
    
    /**
     * 评论内容
     */
    private String content;
    
    /**
     * 点赞数
     */
    private Integer likeCount;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}