package com.music.player.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.music.player.entity.LyricsShare;
import com.music.player.mapper.LyricsShareMapper;
import com.music.player.service.LyricsShareService;
import com.music.player.dto.LyricsShareDTO;
import com.music.common.core.domain.PageResult;
import com.music.system.entity.User;
import com.music.system.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 歌词分享服务实现
 */
@Service
public class LyricsShareServiceImpl extends ServiceImpl<LyricsShareMapper, LyricsShare> implements LyricsShareService {

    @Resource
    private LyricsShareMapper lyricsShareMapper;

    @Resource
    private UserMapper userMapper;

    @Override
    @Transactional
    public Long createShare(LyricsShareDTO dto) {
        LyricsShare share = new LyricsShare();
        share.setLyricsId(dto.getLyricsId());
        share.setUserId(dto.getUserId());
        share.setShareType(dto.getShareType());
        share.setDeleted(0);
        save(share);
        return share.getId();
    }

    @Override
    @Transactional
    public void deleteShare(Long shareId, Long userId) {
        LyricsShare share = getById(shareId);
        if (share == null) {
            throw new RuntimeException("分享不存在");
        }
        // 检查权限：分享者或管理员
        if (!share.getUserId().equals(userId)) {
            // 这里可以添加管理员检查，暂时只允许分享者删除
            throw new RuntimeException("无权删除此分享");
        }
        share.setDeleted(1);
        updateById(share);
    }

    @Override
    public PageResult<LyricsShareDTO> getUserShares(Long userId, int page, int size) {
        LambdaQueryWrapper<LyricsShare> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LyricsShare::getUserId, userId);
        wrapper.eq(LyricsShare::getDeleted, 0);
        wrapper.orderByDesc(LyricsShare::getShareTime);

        Page<LyricsShare> pageObj = new Page<>(page, size);
        Page<LyricsShare> result = page(pageObj, wrapper);

        List<LyricsShareDTO> list = result.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new PageResult<>(list, result.getTotal(), result.getSize(), result.getCurrent(), result.getPages());
    }

    @Override
    public PageResult<LyricsShareDTO> getLyricsShares(Long lyricsId, int page, int size) {
        LambdaQueryWrapper<LyricsShare> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LyricsShare::getLyricsId, lyricsId);
        wrapper.eq(LyricsShare::getDeleted, 0);
        wrapper.orderByDesc(LyricsShare::getShareTime);

        Page<LyricsShare> pageObj = new Page<>(page, size);
        Page<LyricsShare> result = page(pageObj, wrapper);

        List<LyricsShareDTO> list = result.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new PageResult<>(list, result.getTotal(), result.getSize(), result.getCurrent(), result.getPages());
    }

    @Override
    public LyricsShareDTO getShareDetail(Long shareId) {
        LyricsShare share = getById(shareId);
        if (share == null || share.getDeleted() == 1) {
            throw new RuntimeException("分享不存在");
        }
        return convertToDTO(share);
    }

    /**
     * 转换为DTO，包含用户信息
     */
    private LyricsShareDTO convertToDTO(LyricsShare share) {
        LyricsShareDTO dto = new LyricsShareDTO();
        dto.setId(share.getId());
        dto.setLyricsId(share.getLyricsId());
        dto.setUserId(share.getUserId());
        dto.setShareType(share.getShareType());
        dto.setCreateTime(share.getShareTime());

        // 查询用户信息
        User user = userMapper.selectById(share.getUserId());
        if (user != null) {
            dto.setUsername(user.getUsername());
            dto.setNickname(user.getNickname());
            dto.setAvatar(user.getAvatar());
        }

        return dto;
    }
}