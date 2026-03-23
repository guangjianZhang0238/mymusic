-- =====================================================
-- 音乐管理系统 - 数据库Schema
-- 整合所有建表SQL，方便项目迁移
-- 创建时间: 2026-03-09
-- =====================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS music_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE music_db;

-- =====================================================
-- 一、系统模块表
-- =====================================================

-- 1.1 系统用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    nickname VARCHAR(50) COMMENT '昵称',
    avatar VARCHAR(255) COMMENT '头像',
    phone VARCHAR(20) COMMENT '手机号',
    union_id VARCHAR(64) COMMENT '微信unionId',
    open_id VARCHAR(64) COMMENT '微信openId',
    email VARCHAR(100) COMMENT '邮箱',
    status INT DEFAULT 1 COMMENT '状态 1正常 0禁用',
    role INT DEFAULT 0 COMMENT '角色 0普通用户 1管理员',
    last_login_time DATETIME COMMENT '最后登录时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    UNIQUE KEY uk_union_id (union_id),
    UNIQUE KEY uk_open_id (open_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 1.2 异步任务表
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

-- 1.3 用户统计表
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

-- 1.4 用户会话表
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

-- =====================================================
-- 二、内容模块表
-- =====================================================

-- 2.1 歌手表
CREATE TABLE IF NOT EXISTS content_artist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '歌手ID',
    name VARCHAR(100) NOT NULL COMMENT '歌手名称',
    avatar VARCHAR(255) COMMENT '头像',
    description TEXT COMMENT '歌手简介',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='歌手表';

-- 2.2 专辑表
CREATE TABLE IF NOT EXISTS content_album (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '专辑ID',
    artist_id BIGINT NOT NULL COMMENT '歌手ID',
    name VARCHAR(100) NOT NULL COMMENT '专辑名称',
    cover_url VARCHAR(255) COMMENT '封面URL',
    folder_path TEXT COMMENT '专辑所在路径',
    cover_image TEXT COMMENT '封面图片',
    release_date DATE COMMENT '发行日期',
    description TEXT COMMENT '专辑简介',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专辑表';

-- 2.3 歌曲表
CREATE TABLE IF NOT EXISTS content_song (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '歌曲ID',
    artist_id BIGINT NOT NULL COMMENT '歌手ID',
    album_id BIGINT NOT NULL COMMENT '专辑ID',
    name VARCHAR(100) NOT NULL COMMENT '歌曲名称',
    file_url VARCHAR(255) NOT NULL COMMENT '文件URL',
    file_size BIGINT COMMENT '文件大小(字节)',
    duration INT COMMENT '时长(秒)',
    sample_rate INT COMMENT '采样率(Hz)',
    bit_depth INT COMMENT '位深度',
    format VARCHAR(20) COMMENT '文件格式',
    cover_url VARCHAR(255) COMMENT '封面URL',
    artist_ids JSON NULL COMMENT '歌手ID数组，用于存储多个歌手',
    artist_names TEXT NULL COMMENT '歌手名称列表，用于展示',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    INDEX idx_artist_id (artist_id),
    INDEX idx_album_id (album_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='歌曲表';

-- 2.4 歌词表
CREATE TABLE IF NOT EXISTS content_lyrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '歌词ID',
    song_id BIGINT NOT NULL COMMENT '歌曲ID',
    content TEXT NOT NULL COMMENT '歌词内容',
    language VARCHAR(20) COMMENT '语言',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='歌词表';

-- =====================================================
-- 三、播放器模块表
-- =====================================================

-- 3.1 播放列表表
CREATE TABLE IF NOT EXISTS player_playlist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '播放列表ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(100) NOT NULL COMMENT '播放列表名称',
    description TEXT COMMENT '播放列表描述',
    cover_url VARCHAR(255) COMMENT '封面URL',
    song_count INT DEFAULT 0 COMMENT '歌曲数量',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='播放列表表';

-- 3.2 播放列表歌曲关联表
CREATE TABLE IF NOT EXISTS player_playlist_song (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    playlist_id BIGINT NOT NULL COMMENT '播放列表ID',
    song_id BIGINT NOT NULL COMMENT '歌曲ID',
    position INT COMMENT '位置',
    add_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='播放列表歌曲关联表';

-- 3.3 收藏表
CREATE TABLE IF NOT EXISTS player_favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '收藏ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    favorite_type INT NOT NULL COMMENT '收藏类型 1歌曲 2专辑 3歌手 4播放列表',
    target_id BIGINT NOT NULL COMMENT '目标ID',
    add_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- 3.4 播放历史表
CREATE TABLE IF NOT EXISTS player_play_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '历史ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    song_id BIGINT NOT NULL COMMENT '歌曲ID',
    play_duration INT COMMENT '播放时长(秒)',
    play_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '播放时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='播放历史表';

-- =====================================================
-- 四、社区模块表
-- =====================================================

-- 4.1 歌曲评论表
CREATE TABLE IF NOT EXISTS music_song_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '评论ID',
    song_id BIGINT NOT NULL COMMENT '歌曲ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    content TEXT NOT NULL COMMENT '评论内容',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='歌曲评论表';

-- 4.2 歌词分享表
CREATE TABLE IF NOT EXISTS music_lyrics_share (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分享ID',
    lyrics_id BIGINT NOT NULL COMMENT '歌词ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    share_type VARCHAR(20) COMMENT '分享类型：text/image/link',
    share_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '分享时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='歌词分享表';

-- 4.3 用户反馈表
CREATE TABLE IF NOT EXISTS app_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '反馈ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    type VARCHAR(50) NOT NULL COMMENT '反馈类型：LYRICS_ERROR/LYRICS_OFFSET/SONG_MISSING/OTHER',
    content TEXT NOT NULL COMMENT '反馈内容',
    song_id BIGINT COMMENT '关联歌曲ID',
    keyword VARCHAR(100) COMMENT '搜索关键词',
    contact VARCHAR(100) COMMENT '联系方式',
    scene VARCHAR(50) COMMENT '反馈场景',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '处理状态：PENDING/RESOLVED/FUTURE/UNABLE',
    handle_note TEXT COMMENT '处理意见',
    handle_time DATETIME COMMENT '处理时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_type (type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈表';

-- =====================================================
-- 五、初始化数据
-- =====================================================

-- 插入默认管理员用户
INSERT INTO sys_user (username, password, nickname, status, role) VALUES 
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin', 1, 1)
ON DUPLICATE KEY UPDATE username=username;

-- =====================================================
-- 数据库初始化完成
-- =====================================================
