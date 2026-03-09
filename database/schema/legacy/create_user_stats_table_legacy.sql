-- 创建用户统计表，用于存储用户的登录统计信息
CREATE TABLE IF NOT EXISTS sys_user_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    login_count INT DEFAULT 0 COMMENT '登录次数',
    total_online_time BIGINT DEFAULT 0 COMMENT '总在线时长(秒)',
    last_login_time DATETIME COMMENT '最后登录时间',
    last_logout_time DATETIME COMMENT '最后退出时间',
    current_session_start DATETIME COMMENT '当前会话开始时间',
    current_session_duration BIGINT DEFAULT 0 COMMENT '当前会话时长(秒)',
    total_play_count BIGINT DEFAULT 0 COMMENT '总播放歌曲数',
    last_play_time DATETIME COMMENT '最后播放时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除标志：0-未删除，1-已删除',
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_last_login_time (last_login_time),
    KEY idx_total_play_count (total_play_count),
    CONSTRAINT fk_user_stats_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户统计表';

-- 创建用户会话表，用于跟踪用户在线状态
CREATE TABLE IF NOT EXISTS sys_user_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    session_id VARCHAR(100) NOT NULL COMMENT '会话ID',
    device_info VARCHAR(255) COMMENT '设备信息',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    login_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    last_active_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    logout_time DATETIME COMMENT '退出时间',
    is_active TINYINT DEFAULT 1 COMMENT '是否活跃：1-活跃，0-已退出',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除标志：0-未删除，1-已删除',
    UNIQUE KEY uk_session_id (session_id),
    KEY idx_user_id (user_id),
    KEY idx_is_active (is_active),
    KEY idx_last_active_time (last_active_time),
    CONSTRAINT fk_user_session_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户会话表';