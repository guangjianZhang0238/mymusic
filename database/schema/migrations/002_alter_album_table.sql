-- =====================================================
-- 迁移脚本: 002_alter_album_table
-- 描述: 修改content_album表的字段类型
-- 创建时间: 2026-03-09
-- =====================================================

USE music_db;

-- 修改字段类型以支持更长的路径
ALTER TABLE content_album MODIFY COLUMN folder_path TEXT COMMENT '专辑所在路径';
ALTER TABLE content_album MODIFY COLUMN cover_image TEXT COMMENT '封面URL';

-- 验证表结构
DESCRIBE content_album;
