package com.music.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.music.system.entity.UserSetting;
import com.music.system.mapper.UserSettingMapper;
import com.music.system.service.UserSettingService;
import com.music.system.vo.UserSettingVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSettingServiceImpl extends ServiceImpl<UserSettingMapper, UserSetting> implements UserSettingService {

    private static final String USER_SETTING_KEY_PREFIX = "music:user:setting:";
    private static final String USER_SETTING_ALL_KEY_PREFIX = "music:user:setting:all:";
    private static final Duration CACHE_TTL = Duration.ofHours(6);

    private final UserSettingMapper userSettingMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public UserSettingVO getUserSetting(Long userId, String settingKey) {
        String cacheKey = buildUserSettingKey(userId, settingKey);

        // 1. 先查 Redis
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                if (cached.isEmpty()) {
                    return null;
                }
                return objectMapper.readValue(cached, UserSettingVO.class);
            }
        } catch (Exception e) {
            log.warn("读取用户设置缓存失败, userId={}, settingKey={}", userId, settingKey, e);
        }

        // 2. 回源 DB
        UserSetting setting = userSettingMapper.getByUserIdAndKey(userId, settingKey);
        UserSettingVO result = setting != null ? convertToVO(setting) : null;

        // 3. 回填 Redis（含空值占位）
        try {
            if (result == null) {
                stringRedisTemplate.opsForValue().set(cacheKey, "", CACHE_TTL);
            } else {
                stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result), CACHE_TTL);
            }
        } catch (Exception e) {
            log.warn("回填用户设置缓存失败, userId={}, settingKey={}", userId, settingKey, e);
        }

        return result;
    }

    @Override
    public List<UserSettingVO> getUserSettings(Long userId) {
        String cacheKey = buildUserAllSettingsKey(userId);

        // 1. 先查 Redis
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                if (cached.isEmpty()) {
                    return Collections.emptyList();
                }
                return objectMapper.readValue(cached, new TypeReference<List<UserSettingVO>>() {
                });
            }
        } catch (Exception e) {
            log.warn("读取用户全部设置缓存失败, userId={}", userId, e);
        }

        // 2. 回源 DB
        List<UserSetting> settings = userSettingMapper.getByUserId(userId);
        List<UserSettingVO> result = settings.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // 3. 回填 Redis
        try {
            if (result.isEmpty()) {
                stringRedisTemplate.opsForValue().set(cacheKey, "", CACHE_TTL);
            } else {
                stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result), CACHE_TTL);
            }
        } catch (Exception e) {
            log.warn("回填用户全部设置缓存失败, userId={}", userId, e);
        }

        return result;
    }

    @Override
    @Transactional
    public UserSettingVO saveOrUpdateSetting(Long userId, String settingKey, String settingValue, String settingType, String description) {
        UserSetting existingSetting = userSettingMapper.getByUserIdAndKey(userId, settingKey);

        UserSettingVO result;
        if (existingSetting != null) {
            // 更新现有设置
            existingSetting.setSettingValue(settingValue);
            existingSetting.setSettingType(settingType);
            existingSetting.setDescription(description);
            existingSetting.setUpdateTime(LocalDateTime.now());
            updateById(existingSetting);
            result = convertToVO(existingSetting);
        } else {
            // 创建新设置
            UserSetting newSetting = new UserSetting();
            newSetting.setUserId(userId);
            newSetting.setSettingKey(settingKey);
            newSetting.setSettingValue(settingValue);
            newSetting.setSettingType(settingType);
            newSetting.setDescription(description);
            save(newSetting);
            result = convertToVO(newSetting);
        }

        // 写后刷新 Redis 缓存
        refreshUserSettingCache(userId, settingKey, result);
        evictUserAllSettingsCache(userId);

        return result;
    }

    @Override
    @Transactional
    public void deleteUserSetting(Long userId, String settingKey) {
        LambdaQueryWrapper<UserSetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSetting::getUserId, userId);
        wrapper.eq(UserSetting::getSettingKey, settingKey);
        remove(wrapper);

        // 删除后同步缓存
        evictUserSettingCache(userId, settingKey);
        evictUserAllSettingsCache(userId);
    }

    @Override
    @Transactional
    public void batchSaveSettings(Long userId, List<UserSettingVO> settings) {
        for (UserSettingVO settingVO : settings) {
            saveOrUpdateSetting(userId, settingVO.getSettingKey(), settingVO.getSettingValue(),
                    settingVO.getSettingType(), settingVO.getDescription());
        }
        evictUserAllSettingsCache(userId);
    }

    private UserSettingVO convertToVO(UserSetting setting) {
        UserSettingVO vo = new UserSettingVO();
        BeanUtils.copyProperties(setting, vo);
        return vo;
    }

    private String buildUserSettingKey(Long userId, String settingKey) {
        return USER_SETTING_KEY_PREFIX + userId + ":" + settingKey;
    }

    private String buildUserAllSettingsKey(Long userId) {
        return USER_SETTING_ALL_KEY_PREFIX + userId;
    }

    private void refreshUserSettingCache(Long userId, String settingKey, UserSettingVO value) {
        try {
            stringRedisTemplate.opsForValue().set(
                    buildUserSettingKey(userId, settingKey),
                    objectMapper.writeValueAsString(value),
                    CACHE_TTL
            );
        } catch (Exception e) {
            log.warn("刷新用户设置缓存失败, userId={}, settingKey={}", userId, settingKey, e);
        }
    }

    private void evictUserSettingCache(Long userId, String settingKey) {
        try {
            stringRedisTemplate.delete(buildUserSettingKey(userId, settingKey));
        } catch (Exception e) {
            log.warn("删除用户设置缓存失败, userId={}, settingKey={}", userId, settingKey, e);
        }
    }

    private void evictUserAllSettingsCache(Long userId) {
        try {
            stringRedisTemplate.delete(buildUserAllSettingsKey(userId));
        } catch (Exception e) {
            log.warn("删除用户全部设置缓存失败, userId={}", userId, e);
        }
    }
}
