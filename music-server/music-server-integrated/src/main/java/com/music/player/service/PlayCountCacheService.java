package com.music.player.service;

import com.music.content.mapper.SongMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 歌曲播放量缓存服务：
 * 1) 播放时先写 Redis
 * 2) 定时批量同步到数据库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlayCountCacheService {

    private static final String SONG_KEY_PREFIX = "music:playcount:song:";
    private static final String TOTAL_PENDING_KEY = "music:playcount:pending:total";

    private final StringRedisTemplate stringRedisTemplate;
    private final SongMapper songMapper;

    public void incrementSongPlayCount(Long songId) {
        if (songId == null) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().increment(SONG_KEY_PREFIX + songId, 1L);
            stringRedisTemplate.opsForValue().increment(TOTAL_PENDING_KEY, 1L);
        } catch (Exception e) {
            // Redis 不可用时降级为直接落库，避免播放量丢失
            log.error("Redis 累加播放量失败，降级直写数据库，songId={}", songId, e);
            songMapper.incrementPlayCountById(songId, 1L);
        }
    }

    public long getPendingTotalPlayCount() {
        try {
            String value = stringRedisTemplate.opsForValue().get(TOTAL_PENDING_KEY);
            return value == null ? 0L : Long.parseLong(value);
        } catch (Exception e) {
            log.warn("读取 Redis 待同步播放量失败", e);
            return 0L;
        }
    }

    @Scheduled(fixedDelayString = "${music.play-count.sync-interval-ms:60000}")
    @Transactional(rollbackFor = Exception.class)
    public void flushPendingPlayCountsToDb() {
        try {
            Set<String> keys = stringRedisTemplate.keys(SONG_KEY_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                return;
            }

            long flushedTotal = 0L;
            for (String key : keys) {
                if (key == null) {
                    continue;
                }

                String deltaStr = stringRedisTemplate.opsForValue().get(key);
                if (deltaStr == null) {
                    continue;
                }

                long delta;
                try {
                    delta = Long.parseLong(deltaStr);
                } catch (NumberFormatException ex) {
                    log.warn("播放量缓存值异常，key={}, value={}", key, deltaStr);
                    continue;
                }

                if (delta <= 0) {
                    continue;
                }

                Long songId;
                try {
                    songId = Long.parseLong(key.substring(SONG_KEY_PREFIX.length()));
                } catch (Exception ex) {
                    log.warn("解析歌曲ID失败，key={}", key);
                    continue;
                }

                int affected = songMapper.incrementPlayCountById(songId, delta);
                if (affected > 0) {
                    stringRedisTemplate.delete(key);
                    flushedTotal += delta;
                }
            }

            if (flushedTotal > 0) {
                stringRedisTemplate.opsForValue().increment(TOTAL_PENDING_KEY, -flushedTotal);
                log.info("播放量同步完成，本次落库增量={}", flushedTotal);
            }
        } catch (Exception e) {
            log.error("播放量定时同步失败", e);
        }
    }
}
