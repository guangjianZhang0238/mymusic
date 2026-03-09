package com.music.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "播放列表信息")
public class PlaylistVO {
    
    @Schema(description = "播放列表ID")
    private Long id;
    
    @Schema(description = "用户ID")
    private Long userId;
    
    @Schema(description = "播放列表名称")
    private String name;
    
    @Schema(description = "封面图片")
    private String coverImage;
    
    @Schema(description = "简介")
    private String description;
    
    @Schema(description = "是否公开")
    private Integer isPublic;
    
    @Schema(description = "歌曲数量")
    private Integer songCount;
    
    @Schema(description = "播放次数")
    private Integer playCount;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
