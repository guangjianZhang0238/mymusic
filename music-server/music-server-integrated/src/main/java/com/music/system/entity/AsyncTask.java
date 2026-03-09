package com.music.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 异步任务实体
 */
@Data
@TableName("sys_async_task")
public class AsyncTask {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 任务类型
     */
    private String taskType;
    
    /**
     * 任务描述
     */
    private String description;
    
    /**
     * 任务状态：PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
     */
    private String status;
    
    /**
     * 进度百分比 (0-100)
     */
    private Integer progress;
    
    /**
     * 处理详情/消息
     */
    private String message;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 完成时间
     */
    private LocalDateTime endTime;
    
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
}