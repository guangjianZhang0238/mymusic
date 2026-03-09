package com.music.player.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("music_play_history")
public class PlayHistory {
    
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
     * 播放时长（秒）
     */
    private Integer playDuration;
    
    /**
     * 播放时间
     */
    private LocalDateTime playTime;
}
