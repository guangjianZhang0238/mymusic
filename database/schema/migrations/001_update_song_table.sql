-- =====================================================
-- 迁移脚本: 001_update_song_table
-- 描述: 更新歌曲表，添加多歌手支持字段
-- 创建时间: 2026-03-09
-- =====================================================

USE music_db;

-- 添加 artist_ids 字段（JSON格式存储多个歌手ID）
ALTER TABLE content_song 
ADD COLUMN IF NOT EXISTS artist_ids JSON NULL COMMENT '歌手ID数组，用于存储多个歌手' AFTER artist_id;

-- 添加 artist_names 字段（文本格式存储歌手名称）
ALTER TABLE content_song 
ADD COLUMN IF NOT EXISTS artist_names TEXT NULL COMMENT '歌手名称列表，用于展示' AFTER artist_ids;

-- 为现有数据填充默认值
UPDATE content_song 
SET artist_ids = JSON_ARRAY(artist_id),
    artist_names = (SELECT name FROM content_artist WHERE id = content_song.artist_id)
WHERE artist_ids IS NULL;

-- 创建索引以提高查询性能
CREATE INDEX IF NOT EXISTS idx_content_song_artist_ids ON content_song((CAST(artist_ids AS CHAR(255))));

-- 验证表结构
DESCRIBE content_song;
