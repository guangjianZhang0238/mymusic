package com.music.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "歌手创建/更新请求")
public class ArtistDTO {
    
    @Schema(description = "歌手ID，更新时必填")
    private Long id;
    
    @NotBlank(message = "歌手名称不能为空")
    @Schema(description = "歌手名称", required = true)
    private String name;
    
    @Schema(description = "英文名称")
    private String nameEn;
    
    @Schema(description = "头像URL")
    private String avatar;
    
    @Schema(description = "简介")
    private String description;
    
    @Schema(description = "地区")
    private String region;
    
    @Schema(description = "类型：0-个人 1-组合")
    private Integer type;
    
    @Schema(description = "排序")
    private Integer sortOrder;
    
    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;
}
