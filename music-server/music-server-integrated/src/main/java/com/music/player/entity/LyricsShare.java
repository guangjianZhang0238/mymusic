package com.music.player.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对应表 music_lyrics_share（使用 share_time，无 create_time/update_time，勿继承 BaseEntity）
 */
@Data
@TableName("music_lyrics_share")
public class LyricsShare implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long lyricsId;

    private Long userId;

    private String shareType;

    /**
     * 数据库列 share_time
     */
    @TableField("share_time")
    private LocalDateTime shareTime;

    private Integer deleted;
}
