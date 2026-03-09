package com.music.player.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("music_favorite")
public class Favorite {
    
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
     * 创建时间
     */
    private LocalDateTime createTime;
}