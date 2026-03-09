package com.music.player.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.music.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("music_lyrics_share")
public class LyricsShare extends BaseEntity {
    
    /**
     * 歌词ID
     */
    private Long lyricsId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 分享类型：text/image/link
     */
    private String shareType;
    
    /**
     * 逻辑删除标志
     */
    private Integer deleted;
}