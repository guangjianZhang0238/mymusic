package com.music.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "歌词信息")
public class LyricsVO {
    
    @Schema(description = "歌词ID")
    private Long id;
    
    @Schema(description = "歌曲ID")
    private Long songId;
    
    @Schema(description = "歌曲名称")
    private String songTitle;
    
    @Schema(description = "歌手名称")
    private String artistName;
    
    @Schema(description = "专辑名称")
    private String albumName;
    
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
    
    @Schema(description = "解析后的歌词行")
    private List<LyricsLine> lines;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Data
    @Schema(description = "歌词行")
    public static class LyricsLine {
        @Schema(description = "时间(毫秒)")
        private Long time;
        
        @Schema(description = "歌词文本")
        private String text;
    }
}
