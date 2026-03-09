-- 修改content_album表的字段类型
ALTER TABLE content_album MODIFY COLUMN folder_path TEXT COMMENT '专辑所在路径';
ALTER TABLE content_album MODIFY COLUMN cover_image TEXT COMMENT '封面URL';
