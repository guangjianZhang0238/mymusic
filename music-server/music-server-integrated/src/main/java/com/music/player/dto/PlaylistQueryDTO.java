package com.music.player.dto;

import lombok.Data;

/**
 * 播放列表查询DTO
 */
@Data
public class PlaylistQueryDTO {
    /**
     * 用户ID（筛选特定用户的播放列表）
     */
    private Long userId;
    
    /**
     * 关键词（用于搜索播放列表名称或描述）
     */
    private String keyword;
    
    /**
     * 是否公开：1-公开，0-私有
     */
    private Integer isPublic;
    
    /**
     * 状态：1-正常，0-禁用
     */
    private Integer status;
    
    /**
     * 当前页码
     */
    private Integer current;
    
    /**
     * 每页大小
     */
    private Integer size;
}
