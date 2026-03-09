package com.music.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.music.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("content_song_artist")
public class SongArtist extends BaseEntity {
    
    private Long songId;
    private Long artistId;
    private Integer sortOrder;
}