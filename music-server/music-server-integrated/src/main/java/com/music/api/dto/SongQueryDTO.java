package com.music.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "歌曲查询请求")
public class SongQueryDTO extends PageDTO {
    
    @Schema(description = "关键词")
    private String keyword;
    
    @Schema(description = "歌手ID")
    private Long artistId;
    
    @Schema(description = "专辑ID")
    private Long albumId;
    
    @Schema(description = "格式")
    private String format;
    
    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "是否有歌词：0-无歌词 1-有歌词")
    private Integer hasLyrics;
}
