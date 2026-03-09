# 数据库SQL脚本说明

## 目录结构

```
database/
├── schema/                      # SQL脚本主目录
│   ├── schema.sql               # 主Schema文件（整合所有建表语句）
│   ├── migrations/              # 迁移脚本目录
│   │   ├── 001_update_song_table.sql      # 歌曲表多歌手支持
│   │   ├── 002_alter_album_table.sql      # 专辑表字段修改
│   │   ├── 003_create_async_task_table.sql # 异步任务表
│   │   └── 004_create_user_stats_table.sql # 用户统计表
│   └── legacy/                  # 旧版SQL备份
│       ├── init_legacy.sql
│       ├── schema_legacy.sql
│       ├── update_song_table_legacy.sql
│       ├── alter_album_table_legacy.sql
│       ├── create_async_task_table_legacy.sql
│       └── create_user_stats_table_legacy.sql
├── init.sql                     # 旧版初始化脚本（已废弃，保留参考）
└── update_song_table.sql        # 旧版更新脚本（已废弃，保留参考）
```

## 使用说明

### 新项目初始化

1. 执行主Schema文件创建所有表：
```bash
mysql -u root -p < database/schema/schema.sql
```

### 已有项目升级

如果数据库已存在，只需执行新增的迁移脚本：

```bash
# 按顺序执行迁移脚本
mysql -u root -p music_db < database/schema/migrations/001_update_song_table.sql
mysql -u root -p music_db < database/schema/migrations/002_alter_album_table.sql
mysql -u root -p music_db < database/schema/migrations/003_create_async_task_table.sql
mysql -u root -p music_db < database/schema/migrations/004_create_user_stats_table.sql
```

## 表结构说明

### 系统模块 (sys_*)
- `sys_user` - 系统用户表
- `sys_async_task` - 异步任务表
- `sys_user_stats` - 用户统计表
- `sys_user_session` - 用户会话表

### 内容模块 (content_*)
- `content_artist` - 歌手表
- `content_album` - 专辑表
- `content_song` - 歌曲表
- `content_lyrics` - 歌词表

### 播放器模块 (player_*)
- `player_playlist` - 播放列表表
- `player_playlist_song` - 播放列表歌曲关联表
- `player_favorite` - 收藏表
- `player_play_history` - 播放历史表

### 社区模块 (music_*/app_*)
- `music_song_comment` - 歌曲评论表
- `music_lyrics_share` - 歌词分享表
- `app_feedback` - 用户反馈表

## 迁移脚本命名规范

迁移脚本使用以下命名格式：
```
NNN_description.sql
```
- `NNN`: 三位数字序号，表示执行顺序
- `description`: 简短描述，使用下划线分隔

## 注意事项

1. 执行迁移脚本前请先备份数据库
2. 迁移脚本应按序号顺序执行
3. `legacy/` 目录中的文件仅供历史参考，不建议使用
