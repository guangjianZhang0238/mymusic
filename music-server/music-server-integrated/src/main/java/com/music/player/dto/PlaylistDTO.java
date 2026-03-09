package com.music.player.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 播放列表DTO
 */
@Data
public class PlaylistDTO {
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 创建者用户ID
     */
    private Long userId;
    
    /**
     * 播放列表名称
     */
    private String name;
    
    /**
     * 封面图片URL
     */
    private String coverImage;
    
    /**
     * 播放列表描述
     */
    private String description;
    
    /**
     * 是否公开：1-公开，0-私有
     */
    private Integer isPublic;
    
    /**
     * 歌曲数量
     */
    private Integer songCount;
    
    /**
     * 播放次数
     */
    private Integer playCount;
    
    /**
     * 排序序号
     */
    private Integer sortOrder;
    
    /**
     * 状态：1-正常，0-禁用
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
