package com.music.player.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("music_playlist_song")
public class PlaylistSong {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 播放列表ID
     */
    private Long playlistId;
    
    /**
     * 歌曲ID
     */
    private Long songId;
    
    /**
     * 排序序号
     */
    private Integer sortOrder;
    
    /**
     * 添加时间
     */
    private LocalDateTime addTime;
}
