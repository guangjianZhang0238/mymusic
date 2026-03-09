package com.music.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
@Schema(description = "专辑创建/更新请求")
public class AlbumDTO {
    
    @Schema(description = "专辑ID，更新时必填")
    private Long id;
    
    @NotNull(message = "歌手ID不能为空")
    @Schema(description = "歌手ID", required = true)
    private Long artistId;
    
    @NotBlank(message = "专辑名称不能为空")
    @Schema(description = "专辑名称", required = true)
    private String name;
    
    @Schema(description = "专辑文件夹路径（相对于音乐根目录）")
    private String folderPath;

    @Schema(description = "封面图片URL")
    private String coverImage;
    
    @Schema(description = "发行日期")
    private LocalDate releaseDate;
    
    @Schema(description = "简介")
    private String description;
    
    @Schema(description = "专辑类型：0-专辑 1-EP 2-单曲 3-合辑")
    private Integer albumType;
    
    @Schema(description = "排序")
    private Integer sortOrder;
    
    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;
}
