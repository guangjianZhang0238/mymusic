package com.music.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "歌曲信息")
public class SongVO {
    
    @Schema(description = "歌曲ID")
    private Long id;
    
    @Schema(description = "专辑ID")
    private Long albumId;
    
    @Schema(description = "专辑名称")
    private String albumName;
    
    @Schema(description = "歌手ID")
    private Long artistId;
    
    @Schema(description = "歌手名称")
    private String artistName;
    
    @Schema(description = "歌曲名称")
    private String title;
    
    @Schema(description = "英文名称")
    private String titleEn;
    
    @Schema(description = "文件路径")
    private String filePath;
    
    @Schema(description = "文件名")
    private String fileName;
    
    @Schema(description = "文件大小(字节)")
    private Long fileSize;
    
    @Schema(description = "文件大小(格式化)")
    private String fileSizeFormat;
    
    @Schema(description = "时长(秒)")
    private Integer duration;
    
    @Schema(description = "时长(格式化)")
    private String durationFormat;
    
    @Schema(description = "格式")
    private String format;
    
    @Schema(description = "采样率")
    private Integer sampleRate;
    
    @Schema(description = "位深")
    private Integer bitDepth;
    
    @Schema(description = "码率")
    private Integer bitRate;
    
    @Schema(description = "声道数")
    private Integer channels;
    
    @Schema(description = "音轨号")
    private Integer trackNumber;
    
    @Schema(description = "碟片号")
    private Integer discNumber;
    
    @Schema(description = "是否有歌词")
    private Integer hasLyrics;
    
    @Schema(description = "播放次数")
    private Integer playCount;
    
    @Schema(description = "排序")
    private Integer sortOrder;
    
    @Schema(description = "状态")
    private Integer status;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
