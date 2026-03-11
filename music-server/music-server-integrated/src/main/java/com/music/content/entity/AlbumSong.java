package com.music.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.music.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("content_album_song")
public class AlbumSong extends BaseEntity {

    private Long albumId;
    private Long songId;
    /**
     * 曲目序号（在专辑中的排序）
     */
    private Integer trackNumber;
    /**
     * 光盘号（多碟专辑时使用，可选）
     */
    private Integer discNumber;
    /**
     * 额外排序字段，兼容扩展
     */
    private Integer sortOrder;
}

