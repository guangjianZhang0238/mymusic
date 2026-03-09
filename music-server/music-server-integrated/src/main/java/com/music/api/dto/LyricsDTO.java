package com.music.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "歌词创建/更新请求")
public class LyricsDTO {
    
    @Schema(description = "歌词ID，更新时必填")
    private Long id;
    
    @NotNull(message = "歌曲ID不能为空")
    @Schema(description = "歌曲ID", required = true)
    private Long songId;
    
    @Schema(description = "歌词类型：0-纯文本 1-LRC格式")
    private Integer lyricsType;
    
    @Schema(description = "歌词内容")
    private String content;
    
    @Schema(description = "翻译歌词")
    private String translation;
    
    @Schema(description = "来源")
    private String source;
    
    @Schema(description = "歌词来源链接")
    private String sourceUrl;
    
    @Schema(description = "歌词文件路径")
    private String filePath;
    
    @Schema(description = "歌词偏移量（秒）")
    private Double lyricsOffset;
}
