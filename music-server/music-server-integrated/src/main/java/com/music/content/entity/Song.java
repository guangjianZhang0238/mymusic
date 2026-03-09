package com.music.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.music.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("content_song")
public class Song extends BaseEntity {
    
    private Long albumId;
    private Long artistId; // 主要歌手ID
    private String title;
    private String titleEn;
    private String filePath;
    private String fileName;
    private Long fileSize;
    private Integer duration;
    private String format;
    private Integer sampleRate;
    private Integer bitDepth;
    private Integer bitRate;
    private Integer channels;
    private Integer trackNumber;
    private Integer discNumber;
    private Long lyricsId;
    private Integer hasLyrics;
    private Integer playCount;
    private Integer sortOrder;
    private Integer status;
    
    // 关联字段（非数据库字段）
    private List<Long> artistIds; // 所有关联的歌手ID
    private String artistNames; // 歌手名称列表，用于展示
}
