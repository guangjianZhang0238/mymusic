package com.music.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "歌手信息")
public class ArtistVO {
    
    @Schema(description = "歌手ID")
    private Long id;
    
    @Schema(description = "歌手名称")
    private String name;
    
    @Schema(description = "英文名称")
    private String nameEn;
    
    @Schema(description = "头像")
    private String avatar;
    
    @Schema(description = "简介")
    private String description;
    
    @Schema(description = "地区")
    private String region;
    
    @Schema(description = "类型：0-个人 1-组合")
    private Integer type;
    
    @Schema(description = "专辑数量")
    private Integer albumCount;
    
    @Schema(description = "歌曲数量")
    private Integer songCount;
    
    @Schema(description = "排序")
    private Integer sortOrder;
    
    @Schema(description = "状态")
    private Integer status;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
