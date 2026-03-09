package com.music.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.music.content.entity.Album;
import com.music.content.entity.Lyrics;
import com.music.content.entity.Song;
import com.music.content.mapper.AlbumMapper;
import com.music.content.mapper.LyricsMapper;
import com.music.content.mapper.SongMapper;
import com.music.file.config.StorageConfig;
import com.music.file.service.DataCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据清理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataCleanupServiceImpl implements DataCleanupService {

    private final SongMapper songMapper;
    private final LyricsMapper lyricsMapper;
    private final AlbumMapper albumMapper;
    private final StorageConfig storageConfig;

    @Override
    public CleanupResult performCleanup(ProgressCallback progressCallback) {
        List<String> details = new ArrayList<>();
        CleanupResult result = new CleanupResult(details);

        try {
            // 第一步：清理歌曲表（10% -> 40%）
            progressCallback.onProgress(10, "第一步：正在扫描歌曲表，检查文件是否存在...");
            int deletedSongs = cleanupOrphanedSongs(progressCallback);
            result.setDeletedSongs(deletedSongs);
            details.add(String.format("歌曲清理完成：共删除 %d 条无对应文件的歌曲记录", deletedSongs));

            // 第二步：清理歌词表（40% -> 70%）
            progressCallback.onProgress(40, "第二步：正在扫描歌词表，检查歌词对应的歌曲是否存在...");
            int deletedLyrics = cleanupOrphanedLyrics(progressCallback);
            result.setDeletedLyrics(deletedLyrics);
            details.add(String.format("歌词清理完成：共删除 %d 条孤立歌词记录", deletedLyrics));

            // 第三步：清理专辑表（70% -> 100%）
            progressCallback.onProgress(70, "第三步：正在扫描专辑表，检查专辑下是否有歌曲...");
            int deletedAlbums = cleanupEmptyAlbums(progressCallback);
            result.setDeletedAlbums(deletedAlbums);
            details.add(String.format("专辑清理完成：共删除 %d 个空专辑记录", deletedAlbums));

            result.setSuccess(true);
            log.info("数据清理完成：删除歌曲 {} 条，歌词 {} 条，专辑 {} 条",
                    deletedSongs, deletedLyrics, deletedAlbums);

        } catch (Exception e) {
            log.error("数据清理失败: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    @Override
    public int cleanupOrphanedSongs(ProgressCallback progressCallback) {
        String basePath = storageConfig.getBasePath();
        List<Song> allSongs = songMapper.selectList(null);
        int total = allSongs.size();
        int deleted = 0;
        int processed = 0;

        log.info("开始清理歌曲表，共 {} 条记录", total);

        for (Song song : allSongs) {
            processed++;
            // 每处理50条更新一次进度（10% ~ 40% 区间）
            if (processed % 50 == 0 || processed == total) {
                int percent = 10 + (int) ((double) processed / total * 30);
                progressCallback.onProgress(percent,
                        String.format("正在检查歌曲文件 (%d/%d)，已删除 %d 条...", processed, total, deleted));
            }

            String filePath = song.getFilePath();
            if (!StringUtils.hasText(filePath)) {
                // 文件路径为空，直接删除
                songMapper.deleteById(song.getId());
                deleted++;
                log.info("删除文件路径为空的歌曲记录: id={}, title={}", song.getId(), song.getTitle());
                continue;
            }

            // 构建绝对路径
            File file;
            if (new File(filePath).isAbsolute()) {
                file = new File(filePath);
            } else {
                file = new File(basePath, filePath);
            }

            if (!file.exists() || !file.isFile()) {
                songMapper.deleteById(song.getId());
                deleted++;
                log.info("删除文件不存在的歌曲记录: id={}, title={}, filePath={}",
                        song.getId(), song.getTitle(), filePath);
            }
        }

        log.info("歌曲表清理完成：共检查 {} 条，删除 {} 条", total, deleted);
        return deleted;
    }

    @Override
    public int cleanupOrphanedLyrics(ProgressCallback progressCallback) {
        // 获取所有仍然存在的歌曲ID集合
        List<Song> remainingSongs = songMapper.selectList(null);
        Set<Long> existingSongIds = remainingSongs.stream()
                .map(Song::getId)
                .collect(Collectors.toSet());

        List<Lyrics> allLyrics = lyricsMapper.selectList(null);
        int total = allLyrics.size();
        int deleted = 0;
        int processed = 0;

        log.info("开始清理歌词表，共 {} 条记录，当前有效歌曲 {} 首", total, existingSongIds.size());

        for (Lyrics lyrics : allLyrics) {
            processed++;
            if (processed % 50 == 0 || processed == total) {
                int percent = 40 + (int) ((double) processed / total * 30);
                progressCallback.onProgress(percent,
                        String.format("正在检查歌词记录 (%d/%d)，已删除 %d 条...", processed, total, deleted));
            }

            Long songId = lyrics.getSongId();
            // 歌词没有对应歌曲ID，或歌曲ID对应的歌曲已被删除
            if (songId == null || !existingSongIds.contains(songId)) {
                lyricsMapper.deleteById(lyrics.getId());
                deleted++;
                log.info("删除孤立歌词记录: id={}, songId={}", lyrics.getId(), songId);
            }
        }

        log.info("歌词表清理完成：共检查 {} 条，删除 {} 条", total, deleted);
        return deleted;
    }

    @Override
    public int cleanupEmptyAlbums(ProgressCallback progressCallback) {
        List<Album> allAlbums = albumMapper.selectList(null);
        int total = allAlbums.size();
        int deleted = 0;
        int processed = 0;

        log.info("开始清理专辑表，共 {} 条记录", total);

        for (Album album : allAlbums) {
            processed++;
            if (processed % 20 == 0 || processed == total) {
                int percent = 70 + (int) ((double) processed / total * 28);
                progressCallback.onProgress(percent,
                        String.format("正在检查专辑 (%d/%d)，已删除 %d 个...", processed, total, deleted));
            }

            // 查询该专辑下是否还有歌曲
            LambdaQueryWrapper<Song> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Song::getAlbumId, album.getId());
            Long songCount = songMapper.selectCount(wrapper);

            if (songCount == 0) {
                albumMapper.deleteById(album.getId());
                deleted++;
                log.info("删除空专辑记录: id={}, name={}", album.getId(), album.getName());
            }
        }

        log.info("专辑表清理完成：共检查 {} 条，删除 {} 条", total, deleted);
        return deleted;
    }
}
