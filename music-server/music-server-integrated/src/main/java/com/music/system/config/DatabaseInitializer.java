package com.music.system.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements ApplicationRunner {
    
    private final JdbcTemplate jdbcTemplate;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("数据库初始化器开始执行...");
        try {
            log.info("正在初始化数据库...");
            
            createDatabase();
            createUserTable();
            createArtistTable();
            createAlbumTable();
            createSongTable();
            createSongArtistTable(); // 添加歌曲歌手关联表
            createLyricsTable();
            createPlaylistTable();
            createPlaylistSongTable();
            createFavoriteTable();
            createPlayHistoryTable();
            createSongCommentTable();
            createLyricsShareTable();
            createFeedbackTable();
            createUserStatsTables(); // 添加用户统计表创建
            insertAdminUser();
            
            log.info("数据库初始化完成！");
        } catch (Exception e) {
            log.error("数据库初始化失败", e);
        }
        log.info("数据库初始化器执行完成");
    }
    
    private void createDatabase() {
        try {
            jdbcTemplate.execute("CREATE DATABASE IF NOT EXISTS music_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            jdbcTemplate.execute("USE music_db");
            log.info("数据库 music_db 创建成功");
        } catch (Exception e) {
            log.warn("数据库可能已存在: {}", e.getMessage());
        }
    }
    
    private void createUserTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS sys_user (
                id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
                username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
                password VARCHAR(100) NOT NULL COMMENT '密码',
                nickname VARCHAR(50) COMMENT '昵称',
                avatar VARCHAR(255) COMMENT '头像',
                phone VARCHAR(20) COMMENT '手机号',
                email VARCHAR(100) COMMENT '邮箱',
                status INT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
                role INT DEFAULT 0 COMMENT '角色：0-普通用户，1-管理员',
                last_login_time DATETIME COMMENT '最后登录时间',
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                deleted INT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除'
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表'
            """;
        jdbcTemplate.execute(sql);
        log.info("用户表创建成功");
    }
    
    private void createArtistTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS content_artist (
                id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '歌手ID',
                name VARCHAR(100) NOT NULL COMMENT '歌手名称',
                name_en VARCHAR(100) COMMENT '歌手英文名称',
                avatar VARCHAR(255) COMMENT '头像URL',
                description TEXT COMMENT '简介',
                region VARCHAR(50) COMMENT '地区',
                type INT COMMENT '类型',
                sort_order INT COMMENT '排序',
                status INT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                deleted INT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除'
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='歌手表'
            """;
        jdbcTemplate.execute(sql);
        
        // 添加缺失的字段
        addColumnIfNotExists("content_artist", "name_en", "VARCHAR(100) COMMENT '歌手英文名称'");
        addColumnIfNotExists("content_artist", "avatar", "VARCHAR(255) COMMENT '头像URL'");
        addColumnIfNotExists("content_artist", "description", "TEXT COMMENT '简介'");
        addColumnIfNotExists("content_artist", "region", "VARCHAR(50) COMMENT '地区'");
        addColumnIfNotExists("content_artist", "type", "INT COMMENT '类型'");
        addColumnIfNotExists("content_artist", "sort_order", "INT COMMENT '排序'");
        addColumnIfNotExists("content_artist", "status", "INT DEFAULT 1 COMMENT '状态：0-禁用，1-启用'");
        
        log.info("歌手表创建成功");
    }
    
    private void addColumnIfNotExists(String tableName, String columnName, String columnDefinition) {
        try {
            // 检查字段是否存在
            String checkSql = "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, tableName, columnName);
            
            if (count == 0) {
                // 字段不存在，添加字段
                // 对于 NOT NULL 字段，添加默认值
                String alterSql = String.format("ALTER TABLE %s ADD COLUMN %s %s", tableName, columnName, columnDefinition.replaceAll("COMMENT '.*'", ""));
                // 如果是 NOT NULL 字段，添加默认值
                if (columnDefinition.contains("NOT NULL") && !columnDefinition.contains("DEFAULT")) {
                    if (columnDefinition.contains("VARCHAR")) {
                        alterSql = alterSql.replace("NOT NULL", "NOT NULL DEFAULT ''");
                    } else if (columnDefinition.contains("INT")) {
                        alterSql = alterSql.replace("NOT NULL", "NOT NULL DEFAULT 0");
                    } else if (columnDefinition.contains("BIGINT")) {
                        alterSql = alterSql.replace("NOT NULL", "NOT NULL DEFAULT 0");
                    }
                }
                jdbcTemplate.execute(alterSql);
                log.info("向表 {} 添加字段 {}", tableName, columnName);
            }
        } catch (Exception e) {
            log.warn("向表 {} 添加字段 {} 失败: {}", tableName, columnName, e.getMessage());
        }
    }
    
    private void createAlbumTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS content_album (
                id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '专辑ID',
                artist_id BIGINT NOT NULL COMMENT '歌手ID',
                name VARCHAR(100) NOT NULL COMMENT '专辑名称',
                folder_path TEXT COMMENT '专辑所在路径',
                cover_image TEXT COMMENT '封面URL',
                release_date DATE COMMENT '发行日期',
                description TEXT COMMENT '简介',
                album_type INT COMMENT '专辑类型',
                sort_order INT COMMENT '排序',
                status INT DEFAULT 1 COMMENT '状态',
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                deleted INT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除'
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专辑表'
            """;
        jdbcTemplate.execute(sql);
        
        // 添加缺失的字段
        addColumnIfNotExists("content_album", "folder_path", "TEXT COMMENT '专辑所在路径'");
        addColumnIfNotExists("content_album", "cover_image", "TEXT COMMENT '封面URL'");
        addColumnIfNotExists("content_album", "release_date", "DATE COMMENT '发行日期'");
        addColumnIfNotExists("content_album", "description", "TEXT COMMENT '简介'");
        addColumnIfNotExists("content_album", "album_type", "INT COMMENT '专辑类型'");
        addColumnIfNotExists("content_album", "sort_order", "INT COMMENT '排序'");
        addColumnIfNotExists("content_album", "status", "INT DEFAULT 1 COMMENT '状态'");
        
        // 修改字段类型，确保能够存储更长的路径
        try {
            String alterFolderPathSql = "ALTER TABLE content_album MODIFY COLUMN folder_path TEXT COMMENT '专辑所在路径'";
            jdbcTemplate.execute(alterFolderPathSql);
            String alterCoverImageSql = "ALTER TABLE content_album MODIFY COLUMN cover_image TEXT COMMENT '封面URL'";
            jdbcTemplate.execute(alterCoverImageSql);
            log.info("修改专辑表字段类型成功");
        } catch (Exception e) {
            log.warn("修改专辑表字段类型失败: {}", e.getMessage());
        }
        
        log.info("专辑表创建成功");
    }
    
    private void createSongTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS content_song (
                id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '歌曲ID',
                artist_id BIGINT NOT NULL COMMENT '歌手ID',
                album_id BIGINT NOT NULL COMMENT '专辑ID',
                title VARCHAR(100) NOT NULL COMMENT '歌曲名称',
                title_en VARCHAR(100) COMMENT '歌曲英文名称',
                file_path VARCHAR(255) NOT NULL COMMENT '文件路径',
                file_name VARCHAR(255) COMMENT '文件名',
                file_size BIGINT COMMENT '文件大小（字节）',
                duration INT COMMENT '时长（秒）',
                format VARCHAR(20) COMMENT '格式（WAV、FLAC、MP3等）',
                sample_rate INT COMMENT '采样率（Hz）',
                bit_depth INT COMMENT '位深（bit）',
                bit_rate INT COMMENT '比特率',
                channels INT COMMENT '声道数',
                track_number INT COMMENT '音轨号',
                disc_number INT COMMENT '光盘号',
                lyrics_id BIGINT COMMENT '歌词ID',
                has_lyrics INT DEFAULT 0 COMMENT '是否有歌词',
                play_count INT DEFAULT 0 COMMENT '播放次数',
                sort_order INT COMMENT '排序',
                status INT DEFAULT 1 COMMENT '状态',
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                deleted INT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除'
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='歌曲表'
            """;
        jdbcTemplate.execute(sql);
        
        // 添加缺失的字段
        addColumnIfNotExists("content_song", "title", "VARCHAR(100) NOT NULL COMMENT '歌曲名称'");
        addColumnIfNotExists("content_song", "title_en", "VARCHAR(100) COMMENT '歌曲英文名称'");
        addColumnIfNotExists("content_song", "file_path", "VARCHAR(255) NOT NULL COMMENT '文件路径'");
        addColumnIfNotExists("content_song", "file_name", "VARCHAR(255) COMMENT '文件名'");
        addColumnIfNotExists("content_song", "file_size", "BIGINT COMMENT '文件大小（字节）'");
        addColumnIfNotExists("content_song", "duration", "INT COMMENT '时长（秒）'");
        addColumnIfNotExists("content_song", "format", "VARCHAR(20) COMMENT '格式（WAV、FLAC、MP3等）'");
        addColumnIfNotExists("content_song", "sample_rate", "INT COMMENT '采样率（Hz）'");
        addColumnIfNotExists("content_song", "bit_depth", "INT COMMENT '位深（bit）'");
        addColumnIfNotExists("content_song", "bit_rate", "INT COMMENT '比特率'");
        addColumnIfNotExists("content_song", "channels", "INT COMMENT '声道数'");
        addColumnIfNotExists("content_song", "track_number", "INT COMMENT '音轨号'");
        addColumnIfNotExists("content_song", "disc_number", "INT COMMENT '光盘号'");
        addColumnIfNotExists("content_song", "lyrics_id", "BIGINT COMMENT '歌词ID'");
        addColumnIfNotExists("content_song", "has_lyrics", "INT DEFAULT 0 COMMENT '是否有歌词'");
        addColumnIfNotExists("content_song", "play_count", "INT DEFAULT 0 COMMENT '播放次数'");
        addColumnIfNotExists("content_song", "sort_order", "INT COMMENT '排序'");
        addColumnIfNotExists("content_song", "status", "INT DEFAULT 1 COMMENT '状态'");
        
        // 添加多歌手支持字段
        addColumnIfNotExists("content_song", "artist_ids", "JSON NULL COMMENT '歌手ID数组，用于存储多个歌手'");
        addColumnIfNotExists("content_song", "artist_names", "TEXT NULL COMMENT '歌手名称列表，用于展示'");
        
        // 强制确保字段存在（即使addColumnIfNotExists失败也要执行）
        ensureColumnExists("content_song", "artist_ids", "JSON NULL COMMENT '歌手ID数组，用于存储多个歌手'");
        ensureColumnExists("content_song", "artist_names", "TEXT NULL COMMENT '歌手名称列表，用于展示'");
        
        // 删除多余的 name 字段
        try {
            String checkNameColumnSql = "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'content_song' AND column_name = 'name'";
            Integer nameColumnCount = jdbcTemplate.queryForObject(checkNameColumnSql, Integer.class);
            if (nameColumnCount > 0) {
                String dropNameColumnSql = "ALTER TABLE content_song DROP COLUMN name";
                jdbcTemplate.execute(dropNameColumnSql);
                log.info("从表 content_song 删除多余的 name 字段");
            }
        } catch (Exception e) {
            log.warn("从表 content_song 删除多余的 name 字段失败: {}", e.getMessage());
        }
        
        log.info("歌曲表创建成功");
    }
    
    private void createSongArtistTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS content_song_artist (
                id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                song_id BIGINT NOT NULL COMMENT '歌曲ID',
                artist_id BIGINT NOT NULL COMMENT '歌手ID',
                sort_order INT DEFAULT 0 COMMENT '排序序号',
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                deleted INT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
                UNIQUE KEY uk_song_artist (song_id, artist_id),
                INDEX idx_song_id (song_id),
                INDEX idx_artist_id (artist_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='歌曲歌手关联表'
            """;
        jdbcTemplate.execute(sql);
        log.info("歌曲歌手关联表创建成功");
    }
    
    private void createLyricsTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS content_lyrics (
                id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '歌词ID',
                song_id BIGINT NOT NULL COMMENT '歌曲ID',
                lyrics_type INT DEFAULT 0 COMMENT '歌词类型',
                content TEXT NOT NULL COMMENT '歌词内容',
                translation TEXT COMMENT '翻译歌词',
                source VARCHAR(50) COMMENT '歌词来源',
                file_path TEXT COMMENT '歌词文件路径',
                source_url TEXT COMMENT '歌词来源链接',
                lyrics_offset DOUBLE DEFAULT 0 COMMENT '歌词偏移量（秒）',
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                deleted INT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除'
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='歌词表'
            """;
        jdbcTemplate.execute(sql);
        
        // 添加缺失的字段
        addColumnIfNotExists("content_lyrics", "lyrics_type", "INT DEFAULT 0 COMMENT '歌词类型'");
        addColumnIfNotExists("content_lyrics", "translation", "TEXT COMMENT '翻译歌词'");
        addColumnIfNotExists("content_lyrics", "source", "VARCHAR(50) COMMENT '歌词来源'");
        addColumnIfNotExists("content_lyrics", "file_path", "TEXT COMMENT '歌词文件路径'");
        addColumnIfNotExists("content_lyrics", "source_url", "TEXT COMMENT '歌词来源链接'");
        addColumnIfNotExists("content_lyrics", "lyrics_offset", "DOUBLE DEFAULT 0 COMMENT '歌词偏移量（秒）'");
        // 修改字段类型，确保能够存储更长的路径
        try {
            String alterSourceUrlSql = "ALTER TABLE content_lyrics MODIFY COLUMN source_url TEXT COMMENT '歌词来源链接'";
            jdbcTemplate.execute(alterSourceUrlSql);
            log.info("修改歌词表source_url字段类型成功");
        } catch (Exception e) {
            log.warn("修改歌词表source_url字段类型失败: {}", e.getMessage());
        }
        
        // 修改字段类型，确保能够存储更长的路径
        try {
            String alterFilePathSql = "ALTER TABLE content_lyrics MODIFY COLUMN file_path TEXT COMMENT '歌词文件路径'";
            jdbcTemplate.execute(alterFilePathSql);
            log.info("修改歌词表字段类型成功");
        } catch (Exception e) {
            log.warn("修改歌词表字段类型失败: {}", e.getMessage());
        }
        
        log.info("歌词表创建成功");
    }
    
    private void createPlaylistTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS music_playlist (
                id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '播放列表ID',
                user_id BIGINT NOT NULL COMMENT '用户ID',
                name VARCHAR(100) NOT NULL COMMENT '播放列表名称',
                description TEXT COMMENT '描述',
                cover_image VARCHAR(255) COMMENT '封面URL',
                is_public INT DEFAULT 1 COMMENT '是否公开：1-公开，0-私有',
                song_count INT DEFAULT 0 COMMENT '歌曲数量',
                play_count INT DEFAULT 0 COMMENT '播放次数',
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                deleted INT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除'
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='播放列表表'
            """;
        jdbcTemplate.execute(sql);
        
        // 添加缺失的字段
        addColumnIfNotExists("music_playlist", "cover_image", "VARCHAR(255) COMMENT '封面URL'");
        addColumnIfNotExists("music_playlist", "description", "TEXT COMMENT '描述'");
        addColumnIfNotExists("music_playlist", "is_public", "INT DEFAULT 1 COMMENT '是否公开：1-公开，0-私有'");
        addColumnIfNotExists("music_playlist", "song_count", "INT DEFAULT 0 COMMENT '歌曲数量'");
        addColumnIfNotExists("music_playlist", "play_count", "INT DEFAULT 0 COMMENT '播放次数'");
        
        log.info("播放列表表创建成功");
    }
    
    private void createPlaylistSongTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS music_playlist_song (
                id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID',
                playlist_id BIGINT NOT NULL COMMENT '播放列表ID',
                song_id BIGINT NOT NULL COMMENT '歌曲ID',
                sort_order INT COMMENT '排序序号',
                add_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
                deleted INT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除'
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='播放列表歌曲关联表'
            """;
        jdbcTemplate.execute(sql);
        
        // 添加缺失的字段
        addColumnIfNotExists("music_playlist_song", "sort_order", "INT COMMENT '排序序号'");
        
        log.info("播放列表歌曲关联表创建成功");
    }
    
    private void createFavoriteTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS music_favorite (
                id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '收藏ID',
                user_id BIGINT NOT NULL COMMENT '用户ID',
                favorite_type INT NOT NULL COMMENT '收藏类型：1-歌曲，2-专辑，3-歌手，4-播放列表',
                target_id BIGINT NOT NULL COMMENT '目标ID（歌曲ID、专辑ID或歌手ID）',
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表'
            """;
        jdbcTemplate.execute(sql);
        log.info("收藏表创建成功");
    }
    
    private void createPlayHistoryTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS music_play_history (
                id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '历史ID',
                user_id BIGINT NOT NULL COMMENT '用户ID',
                song_id BIGINT NOT NULL COMMENT '歌曲ID',
                play_duration INT COMMENT '播放时长（秒）',
                play_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '播放时间',
                deleted INT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除'
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='播放历史表'
            """;
        jdbcTemplate.execute(sql);
        log.info("播放历史表创建成功");
    }
    
    private void createSongCommentTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS music_song_comment (
                id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '评论ID',
                song_id BIGINT NOT NULL COMMENT '歌曲ID',
                user_id BIGINT NOT NULL COMMENT '用户ID',
                content TEXT NOT NULL COMMENT '评论内容',
                like_count INT DEFAULT 0 COMMENT '点赞数',
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                deleted INT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
                INDEX idx_song_id (song_id),
                INDEX idx_user_id (user_id),
                INDEX idx_create_time (create_time)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='歌曲评论表'
            """;
        jdbcTemplate.execute(sql);
        log.info("歌曲评论表创建成功");
    }
    
    private void createLyricsShareTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS music_lyrics_share (
                id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分享ID',
                lyrics_id BIGINT NOT NULL COMMENT '歌词ID',
                user_id BIGINT NOT NULL COMMENT '用户ID',
                share_type VARCHAR(20) COMMENT '分享类型：text/image/link',
                share_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '分享时间',
                deleted INT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
                INDEX idx_lyrics_id (lyrics_id),
                INDEX idx_user_id (user_id),
                INDEX idx_share_time (share_time)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='歌词分享表'
            """;
        jdbcTemplate.execute(sql);
        log.info("歌词分享表创建成功");
    }
    
    private void createFeedbackTable() {
        String sql = """
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
                deleted INT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
                INDEX idx_user_id (user_id),
                INDEX idx_status (status),
                INDEX idx_type (type),
                INDEX idx_create_time (create_time)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用反馈表'
            """;
        jdbcTemplate.execute(sql);
        log.info("应用反馈表创建成功");
    }
    
    private void createUserSettingTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS sys_user_setting (
                id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '设置ID',
                user_id BIGINT NOT NULL COMMENT '用户ID',
                setting_key VARCHAR(100) NOT NULL COMMENT '设置键',
                setting_value TEXT COMMENT '设置值',
                setting_type VARCHAR(50) COMMENT '设置类型',
                description VARCHAR(255) COMMENT '描述',
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                deleted INT DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
                UNIQUE KEY uk_user_setting (user_id, setting_key)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户设置表'
            """;
        jdbcTemplate.execute(sql);
        log.info("用户设置表创建成功");
    }
    
    private void createUserStatsTables() {
        // 创建用户统计表
        String userStatsSql = """
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
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户统计表'
            """;
        jdbcTemplate.execute(userStatsSql);
        log.info("用户统计表创建成功");
        
        // 创建用户会话表
        String userSessionSql = """
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
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户会话表'
            """;
        jdbcTemplate.execute(userSessionSql);
        log.info("用户会话表创建成功");
    }
    
    private void insertAdminUser() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE username = 'admin'", 
                Integer.class
            );
            
            if (count == null || count == 0) {
                String sql = """
                    INSERT INTO sys_user (username, password, nickname, status, role) VALUES 
                    ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 1, 1)
                    """;
                jdbcTemplate.execute(sql);
                log.info("管理员用户创建成功（用户名：admin，密码：123456）");
            } else {
                log.info("管理员用户已存在");
            }
        } catch (Exception e) {
            log.warn("管理员用户可能已存在: {}", e.getMessage());
        }
    }
    
    /**
     * 强制确保列存在，如果不存在则添加
     */
    private void ensureColumnExists(String tableName, String columnName, String columnDefinition) {
        try {
            String checkSql = String.format(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = '%s' AND column_name = '%s'",
                tableName, columnName
            );
            
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class);
            
            if (count == null || count == 0) {
                String addSql = String.format(
                    "ALTER TABLE %s ADD COLUMN %s %s",
                    tableName, columnName, columnDefinition
                );
                jdbcTemplate.execute(addSql);
                log.info("成功添加字段 {}.{}", tableName, columnName);
            } else {
                log.debug("字段 {}.{} 已存在", tableName, columnName);
            }
        } catch (Exception e) {
            log.error("确保字段存在时出错: {}.{}", tableName, columnName, e);
        }
    }
}
