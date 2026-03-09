package com.music.player.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.music.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("music_playlist")
public class Playlist extends BaseEntity {
    
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
}
