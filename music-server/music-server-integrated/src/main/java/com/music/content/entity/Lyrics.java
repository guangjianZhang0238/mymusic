package com.music.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.music.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("content_lyrics")
public class Lyrics extends BaseEntity {
    
    private Long songId;
    private Integer lyricsType;
    private String content;
    private String translation;
    private String source;
    private String sourceUrl;
    private String filePath;
    private Double lyricsOffset;
}
