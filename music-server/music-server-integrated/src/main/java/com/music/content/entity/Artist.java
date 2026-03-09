package com.music.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.music.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("content_artist")
public class Artist extends BaseEntity {
    
    private String name;
    private String nameEn;
    private String avatar;
    private String description;
    private String region;
    private Integer type;
    private Integer sortOrder;
    private Integer status;
}
