-- 创建数据库
CREATE DATABASE IF NOT EXISTS music_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE music_db;

-- 1. 用户表
CREATE TABLE IF NOT EXISTS `music_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码',
  `nickname` VARCHAR(50) NOT NULL COMMENT '昵称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `gender` TINYINT DEFAULT 0 COMMENT '性别 0未知 1男 2女',
  `birthday` DATE DEFAULT NULL COMMENT '生日',
  `region` VARCHAR(50) DEFAULT NULL COMMENT '地区',
  `bio` VARCHAR(255) DEFAULT NULL COMMENT '个人简介',
  `role` TINYINT NOT NULL DEFAULT 0 COMMENT '角色 0普通用户 1管理员',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1正常 0禁用',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 2. 歌手表
CREATE TABLE IF NOT EXISTS `music_artist` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '歌手ID',
  `name` VARCHAR(100) NOT NULL COMMENT '歌手名称',
  `name_en` VARCHAR(200) DEFAULT NULL COMMENT '英文名称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像',
  `description` TEXT DEFAULT NULL COMMENT '歌手简介',
  `region` VARCHAR(50) DEFAULT NULL COMMENT '地区',
  `type` TINYINT NOT NULL DEFAULT 0 COMMENT '类型 0个人 1组合',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序权重',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1正常 0禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_type` (`type`),
  KEY `idx_status` (`status`),
  KEY `idx_sort_order` (`sort_order`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='歌手表';

-- 3. 专辑表
CREATE TABLE IF NOT EXISTS `music_album` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '专辑ID',
  `artist_id` BIGINT NOT NULL COMMENT '歌手ID',
  `name` VARCHAR(100) NOT NULL COMMENT '专辑名称',
  `cover_image` VARCHAR(255) DEFAULT NULL COMMENT '封面图片',
  `release_date` DATE DEFAULT NULL COMMENT '发行日期',
  `description` TEXT DEFAULT NULL COMMENT '专辑简介',
  `album_type` TINYINT NOT NULL DEFAULT 0 COMMENT '专辑类型 0录音室 1现场 2精选 3合辑',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序权重',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1正常 0禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_artist_id` (`artist_id`),
  KEY `idx_album_type` (`album_type`),
  KEY `idx_status` (`status`),
  KEY `idx_sort_order` (`sort_order`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_album_artist` FOREIGN KEY (`artist_id`) REFERENCES `music_artist` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专辑表';

-- 4. 歌曲表
CREATE TABLE IF NOT EXISTS `music_song` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '歌曲ID',
  `album_id` BIGINT NOT NULL COMMENT '专辑ID',
  `artist_id` BIGINT NOT NULL COMMENT '歌手ID',
  `title` VARCHAR(100) NOT NULL COMMENT '歌曲标题',
  `title_en` VARCHAR(200) DEFAULT NULL COMMENT '英文标题',
  `file_path` VARCHAR(255) NOT NULL COMMENT '文件路径',
  `file_name` VARCHAR(100) NOT NULL COMMENT '文件名',
  `file_size` BIGINT NOT NULL COMMENT '文件大小(字节)',
  `duration` INT NOT NULL COMMENT '时长(秒)',
  `format` VARCHAR(10) NOT NULL COMMENT '文件格式',
  `sample_rate` INT NOT NULL COMMENT '采样率(Hz)',
  `bit_depth` TINYINT NOT NULL COMMENT '位深度',
  `bit_rate` INT NOT NULL COMMENT '码率(kbps)',
  `channels` TINYINT NOT NULL COMMENT '声道数',
  `track_number` INT DEFAULT 0 COMMENT '音轨号',
  `disc_number` INT DEFAULT 0 COMMENT '碟片号',
  `has_lyrics` TINYINT NOT NULL DEFAULT 0 COMMENT '是否有歌词 1有 0无',
  `play_count` INT NOT NULL DEFAULT 0 COMMENT '播放次数',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序权重',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1正常 0禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_album_id` (`album_id`),
  KEY `idx_artist_id` (`artist_id`),
  KEY `idx_format` (`format`),
  KEY `idx_play_count` (`play_count`),
  KEY `idx_status` (`status`),
  KEY `idx_sort_order` (`sort_order`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_song_album` FOREIGN KEY (`album_id`) REFERENCES `music_album` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_song_artist` FOREIGN KEY (`artist_id`) REFERENCES `music_artist` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='歌曲表';

-- 5. 歌词表
CREATE TABLE IF NOT EXISTS `music_lyrics` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '歌词ID',
  `song_id` BIGINT NOT NULL COMMENT '歌曲ID',
  `lyrics_type` TINYINT NOT NULL DEFAULT 0 COMMENT '歌词类型 0LRC 1KRC 2QRC',
  `content` TEXT NOT NULL COMMENT '歌词内容',
  `translation` TEXT DEFAULT NULL COMMENT '歌词翻译',
  `source` VARCHAR(50) DEFAULT NULL COMMENT '歌词来源',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_song_type` (`song_id`, `lyrics_type`),
  KEY `idx_song_id` (`song_id`),
  KEY `idx_lyrics_type` (`lyrics_type`),
  CONSTRAINT `fk_lyrics_song` FOREIGN KEY (`song_id`) REFERENCES `music_song` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='歌词表';

-- 6. 播放列表表
CREATE TABLE IF NOT EXISTS `music_playlist` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '播放列表ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `name` VARCHAR(100) NOT NULL COMMENT '播放列表名称',
  `cover_image` VARCHAR(255) DEFAULT NULL COMMENT '封面图片',
  `description` TEXT DEFAULT NULL COMMENT '播放列表描述',
  `is_public` TINYINT NOT NULL DEFAULT 0 COMMENT '是否公开 1公开 0私有',
  `song_count` INT NOT NULL DEFAULT 0 COMMENT '歌曲数量',
  `play_count` INT NOT NULL DEFAULT 0 COMMENT '播放次数',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序权重',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1正常 0禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_public` (`is_public`),
  KEY `idx_status` (`status`),
  KEY `idx_sort_order` (`sort_order`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_playlist_user` FOREIGN KEY (`user_id`) REFERENCES `music_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='播放列表表';

-- 7. 播放列表歌曲关联表
CREATE TABLE IF NOT EXISTS `music_playlist_song` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `playlist_id` BIGINT NOT NULL COMMENT '播放列表ID',
  `song_id` BIGINT NOT NULL COMMENT '歌曲ID',
  `add_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序权重',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_playlist_song` (`playlist_id`, `song_id`),
  KEY `idx_playlist_id` (`playlist_id`),
  KEY `idx_song_id` (`song_id`),
  KEY `idx_sort_order` (`sort_order`),
  CONSTRAINT `fk_playlist_song_playlist` FOREIGN KEY (`playlist_id`) REFERENCES `music_playlist` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_playlist_song_song` FOREIGN KEY (`song_id`) REFERENCES `music_song` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='播放列表歌曲关联表';

-- 8. 收藏表
CREATE TABLE IF NOT EXISTS `music_favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `favorite_type` TINYINT NOT NULL COMMENT '收藏类型 1歌曲 2专辑 3歌手 4播放列表',
  `target_id` BIGINT NOT NULL COMMENT '目标ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_favorite` (`user_id`, `favorite_type`, `target_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_favorite_type` (`favorite_type`),
  KEY `idx_target_id` (`target_id`),
  CONSTRAINT `fk_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `music_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- 9. 播放历史表
CREATE TABLE IF NOT EXISTS `music_play_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '历史ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `song_id` BIGINT NOT NULL COMMENT '歌曲ID',
  `play_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '播放时间',
  `duration_played` INT NOT NULL DEFAULT 0 COMMENT '播放时长(秒)',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_song_id` (`song_id`),
  KEY `idx_play_time` (`play_time`),
  CONSTRAINT `fk_play_history_user` FOREIGN KEY (`user_id`) REFERENCES `music_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_play_history_song` FOREIGN KEY (`song_id`) REFERENCES `music_song` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='播放历史表';

-- 10. 歌手专辑关联表（用于合辑等多个歌手的专辑）
CREATE TABLE IF NOT EXISTS `music_artist_album` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `artist_id` BIGINT NOT NULL COMMENT '歌手ID',
  `album_id` BIGINT NOT NULL COMMENT '专辑ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_artist_album` (`artist_id`, `album_id`),
  KEY `idx_artist_id` (`artist_id`),
  KEY `idx_album_id` (`album_id`),
  CONSTRAINT `fk_artist_album_artist` FOREIGN KEY (`artist_id`) REFERENCES `music_artist` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_artist_album_album` FOREIGN KEY (`album_id`) REFERENCES `music_album` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='歌手专辑关联表';

-- 11. 系统配置表
CREATE TABLE IF NOT EXISTS `music_system_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_key` VARCHAR(50) NOT NULL COMMENT '配置键',
  `config_value` VARCHAR(255) NOT NULL COMMENT '配置值',
  `config_desc` VARCHAR(255) DEFAULT NULL COMMENT '配置描述',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 12. 操作日志表
CREATE TABLE IF NOT EXISTS `music_operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `username` VARCHAR(50) DEFAULT NULL COMMENT '用户名',
  `operation` VARCHAR(100) NOT NULL COMMENT '操作内容',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` VARCHAR(255) DEFAULT NULL COMMENT '用户代理',
  `request_url` VARCHAR(255) DEFAULT NULL COMMENT '请求URL',
  `request_method` VARCHAR(10) DEFAULT NULL COMMENT '请求方法',
  `request_params` TEXT DEFAULT NULL COMMENT '请求参数',
  `response_code` INT DEFAULT NULL COMMENT '响应代码',
  `response_time` INT DEFAULT NULL COMMENT '响应时间(ms)',
  `success` TINYINT NOT NULL DEFAULT 1 COMMENT '是否成功 1成功 0失败',
  `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_operation` (`operation`),
  KEY `idx_success` (`success`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- 插入默认管理员用户
INSERT INTO `music_user` (`username`, `password`, `nickname`, `avatar`, `phone`, `email`, `gender`, `role`, `status`) VALUES
('admin', '$2a$10$6t8UQZJzGf9V2r2xq3y9nO7W0X5Y6Z7A8B9C10D11E12F13G14H15I16J17K18L19M20N', '管理员', 'https://neeko-copilot.bytedance.net/api/text2image?prompt=user%20avatar&size=square', '13800138000', 'admin@example.com', 0, 1, 1)
ON DUPLICATE KEY UPDATE `nickname` = VALUES(`nickname`);

-- 插入默认系统配置
INSERT INTO `music_system_config` (`config_key`, `config_value`, `config_desc`, `status`) VALUES
('system_name', '音乐管理系统', '系统名称', 1),
('system_version', '1.0.0', '系统版本', 1),
('max_upload_size', '500', '最大上传文件大小(MB)', 1),
('allowed_formats', 'wav,flac,mp3,aac,m4a', '允许的音频格式', 1),
('music_storage_path', 'D:\\music_source', '音乐文件存储路径', 1),
('temp_storage_path', 'D:\\music_temp', '临时文件存储路径', 1)
ON DUPLICATE KEY UPDATE `config_value` = VALUES(`config_value`);

-- 插入示例歌手数据
INSERT INTO `music_artist` (`name`, `name_en`, `avatar`, `description`, `region`, `type`, `sort_order`, `status`) VALUES
('周杰伦', 'Jay Chou', 'https://neeko-copilot.bytedance.net/api/text2image?prompt=Jay%20Chou%20singer%20avatar&size=square', '华语流行音乐天王', '中国台湾', 0, 1, 1),
('林俊杰', 'JJ Lin', 'https://neeko-copilot.bytedance.net/api/text2image?prompt=JJ%20Lin%20singer%20avatar&size=square', '新加坡实力派歌手', '新加坡', 0, 2, 1),
('Taylor Swift', 'Taylor Swift', 'https://neeko-copilot.bytedance.net/api/text2image?prompt=Taylor%20Swift%20singer%20avatar&size=square', '美国流行天后', '美国', 0, 3, 1)
ON DUPLICATE KEY UPDATE `avatar` = VALUES(`avatar`);

-- 插入示例专辑数据
INSERT INTO `music_album` (`artist_id`, `name`, `cover_image`, `release_date`, `description`, `album_type`, `sort_order`, `status`) VALUES
(1, '七里香', 'https://neeko-copilot.bytedance.net/api/text2image?prompt=Jay%20Chou%20album%20cover%20Qi%20Li%20Xiang&size=square', '2004-08-03', '周杰伦经典专辑', 0, 1, 1),
(2, '江南', 'https://neeko-copilot.bytedance.net/api/text2image?prompt=JJ%20Lin%20album%20cover%20Jiang%20Nan&size=square', '2004-06-04', '林俊杰成名作', 0, 2, 1),
(3, '1989', 'https://neeko-copilot.bytedance.net/api/text2image?prompt=Taylor%20Swift%20album%20cover%201989&size=square', '2014-10-27', 'Taylor Swift流行转型之作', 0, 3, 1)
ON DUPLICATE KEY UPDATE `cover_image` = VALUES(`cover_image`);

-- 插入示例歌曲数据
INSERT INTO `music_song` (
  `album_id`, `artist_id`, `title`, `title_en`, `file_path`, `file_name`, 
  `file_size`, `duration`, `format`, `sample_rate`, `bit_depth`, `bit_rate`, `channels`, 
  `track_number`, `disc_number`, `has_lyrics`, `play_count`, `sort_order`, `status`
) VALUES
(1, 1, '七里香', 'Common Jasmine Orange', 'D:\\music_source\\周杰伦\\七里香\\七里香.wav', '七里香.wav', 
  52428800, 280, 'wav', 44100, 16, 1411, 2, 1, 1, 1, 1000, 1, 1),
(1, 1, '借口', 'Excuse', 'D:\\music_source\\周杰伦\\七里香\\借口.wav', '借口.wav', 
  48318566, 258, 'wav', 44100, 16, 1411, 2, 2, 1, 1, 800, 2, 1),
(2, 2, '江南', 'River South', 'D:\\music_source\\林俊杰\\江南\\江南.wav', '江南.wav', 
  49314432, 264, 'wav', 44100, 16, 1411, 2, 1, 1, 1, 900, 1, 1),
(3, 3, 'Shake It Off', 'Shake It Off', 'D:\\music_source\\Taylor Swift\\1989\\Shake It Off.wav', 'Shake It Off.wav', 
  45298176, 240, 'wav', 44100, 16, 1411, 2, 1, 1, 1, 1200, 1, 1)
ON DUPLICATE KEY UPDATE `file_path` = VALUES(`file_path`);

-- 插入示例歌词数据
INSERT INTO `music_lyrics` (`song_id`, `lyrics_type`, `content`, `translation`) VALUES
(1, 0, '[00:00.00]七里香 - 周杰伦\n[00:05.00]作词：方文山\n[00:10.00]作曲：周杰伦\n[00:15.00]窗外的麻雀 在电线杆上多嘴\n[00:20.00]你说这一句 很有夏天的感觉\n[00:25.00]手中的铅笔 在纸上来来回回\n[00:30.00]我用几行字形容你是我的谁', NULL),
(3, 0, '[00:00.00]江南 - 林俊杰\n[00:05.00]作词：李瑞洵\n[00:10.00]作曲：林俊杰\n[00:15.00]风到这里就是粘\n[00:20.00]粘住过客的思念\n[00:25.00]雨到了这里缠成线\n[00:30.00]缠着我们留恋人世间', NULL)
ON DUPLICATE KEY UPDATE `content` = VALUES(`content`);

-- 插入示例播放列表数据
INSERT INTO `music_playlist` (`user_id`, `name`, `cover_image`, `description`, `is_public`, `song_count`, `play_count`, `sort_order`, `status`) VALUES
(1, '我喜欢的音乐', 'https://neeko-copilot.bytedance.net/api/text2image?prompt=music%20playlist%20cover&size=square', '个人收藏的音乐', 0, 4, 50, 1, 1),
(1, '经典流行', 'https://neeko-copilot.bytedance.net/api/text2image?prompt=classic%20pop%20playlist%20cover&size=square', '经典流行歌曲合集', 1, 4, 200, 2, 1)
ON DUPLICATE KEY UPDATE `cover_image` = VALUES(`cover_image`);

-- 插入播放列表歌曲关联数据
INSERT INTO `music_playlist_song` (`playlist_id`, `song_id`, `sort_order`) VALUES
(1, 1, 1),
(1, 2, 2),
(1, 3, 3),
(1, 4, 4),
(2, 1, 1),
(2, 3, 2),
(2, 4, 3)
ON DUPLICATE KEY UPDATE `sort_order` = VALUES(`sort_order`);

-- 更新播放列表歌曲数量
UPDATE `music_playlist` p
SET `song_count` = (
  SELECT COUNT(*) FROM `music_playlist_song` ps WHERE ps.playlist_id = p.id
);

-- 插入示例收藏数据
INSERT INTO `music_favorite` (`user_id`, `favorite_type`, `target_id`) VALUES
(1, 1, 1),  -- 收藏歌曲1
(1, 1, 3),  -- 收藏歌曲3
(1, 2, 1),  -- 收藏专辑1
(1, 3, 1),  -- 收藏歌手1
(1, 4, 2)   -- 收藏播放列表2
ON DUPLICATE KEY UPDATE `target_id` = VALUES(`target_id`);

-- 插入示例播放历史数据
INSERT INTO `music_play_history` (`user_id`, `song_id`, `play_time`, `duration_played`) VALUES
(1, 1, NOW() - INTERVAL 1 DAY, 280),
(1, 3, NOW() - INTERVAL 2 DAY, 264),
(1, 4, NOW() - INTERVAL 3 DAY, 240),
(1, 2, NOW() - INTERVAL 4 DAY, 258)
ON DUPLICATE KEY UPDATE `play_time` = VALUES(`play_time`);

-- 提交事务
COMMIT;

-- 优化表结构
OPTIMIZE TABLE `music_user`, `music_artist`, `music_album`, `music_song`, `music_lyrics`, `music_playlist`, `music_playlist_song`, `music_favorite`, `music_play_history`, `music_artist_album`, `music_system_config`, `music_operation_log`;

-- 显示创建结果
SELECT '数据库初始化完成！' AS result;
SELECT '默认管理员账号：admin / 123456' AS admin_account;
SELECT COUNT(*) AS user_count FROM `music_user`;
SELECT COUNT(*) AS artist_count FROM `music_artist`;
SELECT COUNT(*) AS album_count FROM `music_album`;
SELECT COUNT(*) AS song_count FROM `music_song`;
SELECT COUNT(*) AS playlist_count FROM `music_playlist`;
