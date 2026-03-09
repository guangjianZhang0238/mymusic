-- =====================================================
-- 迁移脚本: 003_create_async_task_table
-- 描述: 创建异步任务表
-- 创建时间: 2026-03-09
-- =====================================================

USE music_db;

-- 异步任务表
CREATE TABLE IF NOT EXISTS sys_async_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '任务ID',
    task_type VARCHAR(50) NOT NULL COMMENT '任务类型',
    description VARCHAR(500) COMMENT '任务描述',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING,RUNNING,COMPLETED,FAILED,CANCELLED',
    progress INT DEFAULT 0 COMMENT '进度百分比(0-100)',
    message TEXT COMMENT '处理详情/消息',
    error_message TEXT COMMENT '错误信息',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '完成时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_task_type (task_type),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步任务表';
