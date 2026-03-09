package com.music.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "专辑信息")
public class AlbumVO {
    
    @Schema(description = "专辑ID")
    private Long id;
    
    @Schema(description = "歌手ID")
    private Long artistId;
    
    @Schema(description = "歌手名称")
    private String artistName;
    
    @Schema(description = "专辑名称")
    private String name;
    
    @Schema(description = "专辑所在路径")
    private String folderPath;
    
    @Schema(description = "封面图片")
    private String coverImage;
    
    @Schema(description = "发行日期")
    private LocalDate releaseDate;
    
    @Schema(description = "简介")
    private String description;
    
    @Schema(description = "专辑类型：0-专辑 1-EP 2-单曲 3-合辑")
    private Integer albumType;
    
    @Schema(description = "歌曲数量")
    private Integer songCount;
    
    @Schema(description = "排序")
    private Integer sortOrder;
    
    @Schema(description = "状态")
    private Integer status;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
