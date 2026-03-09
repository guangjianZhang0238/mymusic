package com.music.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "歌曲创建/更新请求")
public class SongDTO {
    
    @Schema(description = "歌曲ID，更新时必填")
    private Long id;
    
    @NotNull(message = "专辑ID不能为空")
    @Schema(description = "专辑ID", required = true)
    private Long albumId;
    
    @Schema(description = "专辑名称")
    private String albumName;
    
    @NotNull(message = "歌手ID不能为空")
    @Schema(description = "歌手ID", required = true)
    private Long artistId;
    
    @Schema(description = "歌手名称")
    private String artistName;
    
    @NotBlank(message = "歌曲名称不能为空")
    @Schema(description = "歌曲名称", required = true)
    private String title;
    
    @Schema(description = "英文名称")
    private String titleEn;
    
    @Schema(description = "文件路径")
    private String filePath;
    
    @Schema(description = "文件名称")
    private String fileName;
    
    @Schema(description = "文件大小")
    private Long fileSize;
    
    @Schema(description = "歌曲时长")
    private Integer duration;
    
    @Schema(description = "文件格式")
    private String format;
    
    @Schema(description = "音轨号")
    private Integer trackNumber;
    
    @Schema(description = "碟片号")
    private Integer discNumber;
    
    @Schema(description = "排序")
    private Integer sortOrder;
    
    @Schema(description = "状态：0-禁用 1-启用")
    private Integer status;
}
