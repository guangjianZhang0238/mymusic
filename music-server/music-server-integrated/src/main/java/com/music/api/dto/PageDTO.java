package com.music.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "分页查询请求")
public class PageDTO {
    
    @Schema(description = "当前页码", defaultValue = "1")
    private Integer current = 1;
    
    @Schema(description = "每页大小", defaultValue = "10")
    private Integer size = 10;
    
    @Schema(description = "排序字段")
    private String orderBy;
    
    @Schema(description = "是否升序", defaultValue = "false")
    private Boolean ascending = false;
}
