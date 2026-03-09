package com.music.app.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * App端用户反馈实体类
 */
@Data
@TableName("app_feedback")
public class AppFeedback {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    /**
     * 反馈类型：LYRICS_ERROR/LYRICS_OFFSET/SONG_MISSING/OTHER/NO_LYRICS
     */
    private String type;
    
    /**
     * 反馈内容
     */
    private String content;
    
    /**
     * 关联歌曲ID（可选）
     */
    private Long songId;
    
    /**
     * 搜索关键词（可选）
     */
    private String keyword;
    
    /**
     * 联系方式（可选）
     */
    private String contact;
    
    /**
     * 反馈场景（例如 SEARCH_EMPTY）
     */
    private String scene;
    
    /**
     * 处理状态：PENDING-待处理/RESOLVED-已解决/FUTURE-后续版本解决/UNABLE-无法解决
     */
    private String status;
    
    /**
     * 处理意见
     */
    private String handleNote;
    
    /**
     * 处理时间
     */
    private LocalDateTime handleTime;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 删除标识：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;
}