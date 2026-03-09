package com.music.player.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaybackPlaylistService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String PLAYBACK_PLAYLIST_KEY_PREFIX = "playback:playlist:";
    private static final String PLAYBACK_INDEX_KEY_PREFIX = "playback:index:";
    private static final long EXPIRE_DAYS = 30; // 30天过期

    private String getPlaylistKey(Long userId) {
        return PLAYBACK_PLAYLIST_KEY_PREFIX + userId;
    }

    private String getIndexKey(Long userId) {
        return PLAYBACK_INDEX_KEY_PREFIX + userId;
    }

    public void savePlaybackPlaylist(Long userId, List<Long> songIds, Integer currentIndex) {
        try {
            String playlistKey = getPlaylistKey(userId);
            String indexKey = getIndexKey(userId);
            
            // 保存播放列表
            String playlistJson = objectMapper.writeValueAsString(songIds);
            redisTemplate.opsForValue().set(playlistKey, playlistJson, EXPIRE_DAYS, TimeUnit.DAYS);
            
            // 保存当前索引
            redisTemplate.opsForValue().set(indexKey, currentIndex, EXPIRE_DAYS, TimeUnit.DAYS);
            
            log.info("保存播放列表成功，用户ID: {}, 歌曲数量: {}, 当前索引: {}", userId, songIds.size(), currentIndex);
        } catch (Exception e) {
            log.error("保存播放列表失败，用户ID: {}", userId, e);
        }
    }

    public List<Long> getPlaybackPlaylist(Long userId) {
        try {
            String playlistKey = getPlaylistKey(userId);
            Object value = redisTemplate.opsForValue().get(playlistKey);
            
            if (value != null) {
                String playlistJson = value.toString();
                return objectMapper.readValue(playlistJson, new TypeReference<List<Long>>() {});
            }
        } catch (Exception e) {
            log.error("获取播放列表失败，用户ID: {}", userId, e);
        }
        return new ArrayList<>();
    }

    public Integer getPlaybackIndex(Long userId) {
        try {
            String indexKey = getIndexKey(userId);
            Object value = redisTemplate.opsForValue().get(indexKey);
            
            if (value != null) {
                return Integer.parseInt(value.toString());
            }
        } catch (Exception e) {
            log.error("获取播放索引失败，用户ID: {}", userId, e);
        }
        return 0;
    }

    public void clearPlaybackPlaylist(Long userId) {
        try {
            String playlistKey = getPlaylistKey(userId);
            String indexKey = getIndexKey(userId);
            
            redisTemplate.delete(playlistKey);
            redisTemplate.delete(indexKey);
            
            log.info("清空播放列表成功，用户ID: {}", userId);
        } catch (Exception e) {
            log.error("清空播放列表失败，用户ID: {}", userId, e);
        }
    }
}
