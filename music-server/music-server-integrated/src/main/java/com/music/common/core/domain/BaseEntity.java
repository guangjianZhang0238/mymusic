package com.music.common.core.domain;

import lombok.Data;

@Data
public class BaseEntity {
    private Long id;
    private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime updateTime;
}
