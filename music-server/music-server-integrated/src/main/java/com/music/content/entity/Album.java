package com.music.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.music.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("content_album")
public class Album extends BaseEntity {
    
    private Long artistId;
    private String name;
    private String folderPath;
    private String coverImage;
    private LocalDate releaseDate;
    private String description;
    private Integer albumType;
    private Integer sortOrder;
    private Integer status;
}
