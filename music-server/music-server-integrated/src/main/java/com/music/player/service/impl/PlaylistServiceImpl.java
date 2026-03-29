package com.music.player.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.music.player.entity.Playlist;
import com.music.player.entity.PlaylistSong;
import com.music.player.mapper.PlaylistMapper;
import com.music.player.mapper.PlaylistSongMapper;
import com.music.player.service.PlaylistService;
import com.music.player.dto.PlaylistDTO;
import com.music.player.dto.PlaylistQueryDTO;
import com.music.common.core.domain.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 播放列表服务实现
 */
@Service
public class PlaylistServiceImpl extends ServiceImpl<PlaylistMapper, Playlist> implements PlaylistService {

    @Resource
    private PlaylistMapper playlistMapper;

    @Resource
    private PlaylistSongMapper playlistSongMapper;

    @Override
    @Transactional
    public Long createPlaylist(PlaylistDTO dto) {
        Playlist playlist = new Playlist();
        playlist.setUserId(dto.getUserId());
        playlist.setName(dto.getName());
        playlist.setCoverImage(dto.getCoverImage());
        playlist.setDescription(dto.getDescription());
        playlist.setIsPublic(dto.getIsPublic());
        save(playlist);
        return playlist.getId();
    }

    @Override
    @Transactional
    public void updatePlaylist(PlaylistDTO dto) {
        Playlist playlist = getById(dto.getId());
        if (playlist != null) {
            playlist.setName(dto.getName());
            playlist.setCoverImage(dto.getCoverImage());
            playlist.setDescription(dto.getDescription());
            playlist.setIsPublic(dto.getIsPublic());
            updateById(playlist);
        }
    }

    @Override
    @Transactional
    public void deletePlaylist(Long id) {
        // 先删除播放列表中的歌曲
        LambdaQueryWrapper<PlaylistSong> songWrapper = new LambdaQueryWrapper<>();
        songWrapper.eq(PlaylistSong::getPlaylistId, id);
        playlistSongMapper.delete(songWrapper);
        // 再删除播放列表
        removeById(id);
    }

    @Override
    public PlaylistDTO getPlaylistDetail(Long id) {
        Playlist playlist = getById(id);
        if (playlist == null) {
            return null;
        }
        return convertToDTO(playlist);
    }

    @Override
    public PageResult<PlaylistDTO> pagePlaylist(PlaylistQueryDTO queryDTO) {
        LambdaQueryWrapper<Playlist> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getUserId() != null) {
            wrapper.eq(Playlist::getUserId, queryDTO.getUserId());
        }
        if (queryDTO.getKeyword() != null) {
            wrapper.like(Playlist::getName, queryDTO.getKeyword());
        }
        if (queryDTO.getIsPublic() != null) {
            wrapper.eq(Playlist::getIsPublic, queryDTO.getIsPublic());
        }
        wrapper.orderByDesc(Playlist::getCreateTime);

        Page<Playlist> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        Page<Playlist> result = page(page, wrapper);

        List<PlaylistDTO> list = result.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new PageResult<>(list, result.getTotal(), result.getSize(), result.getCurrent(), result.getPages());
    }

    @Override
    public List<PlaylistDTO> getUserPlaylists(Long userId) {
        LambdaQueryWrapper<Playlist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Playlist::getUserId, userId);
        wrapper.orderByDesc(Playlist::getUpdateTime);
        List<Playlist> playlists = list(wrapper);
        return playlists.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<PlaylistDTO> getPublicPlaylists(int limit) {
        LambdaQueryWrapper<Playlist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Playlist::getIsPublic, 1);
        wrapper.orderByDesc(Playlist::getPlayCount).orderByDesc(Playlist::getCreateTime);
        wrapper.last("LIMIT " + limit);
        List<Playlist> playlists = list(wrapper);
        return playlists.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addSongToPlaylist(Long playlistId, Long songId) {
        // 检查是否已存在
        LambdaQueryWrapper<PlaylistSong> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlaylistSong::getPlaylistId, playlistId);
        wrapper.eq(PlaylistSong::getSongId, songId);
        if (playlistSongMapper.selectCount(wrapper) == 0) {
            PlaylistSong playlistSong = new PlaylistSong();
            playlistSong.setPlaylistId(playlistId);
            playlistSong.setSongId(songId);
            // 计算排序值
            Integer maxSort = playlistSongMapper.selectMaxSort(playlistId);
            playlistSong.setSortOrder(maxSort != null ? maxSort + 1 : 1);
            playlistSongMapper.insert(playlistSong);
            // 更新播放列表歌曲数量
            updateSongCount(playlistId);
        }
    }

    @Override
    @Transactional
    public void removeSongFromPlaylist(Long playlistId, Long songId) {
        LambdaQueryWrapper<PlaylistSong> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlaylistSong::getPlaylistId, playlistId);
        wrapper.eq(PlaylistSong::getSongId, songId);
        playlistSongMapper.delete(wrapper);
        // 更新播放列表歌曲数量
        updateSongCount(playlistId);
    }

    @Override
    @Transactional
    public void clearPlaylistSongs(Long playlistId) {
        LambdaQueryWrapper<PlaylistSong> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlaylistSong::getPlaylistId, playlistId);
        playlistSongMapper.delete(wrapper);
        updateSongCount(playlistId);
    }

    @Override
    public List<Long> getPlaylistSongs(Long playlistId) {
        LambdaQueryWrapper<PlaylistSong> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlaylistSong::getPlaylistId, playlistId);
        wrapper.orderByAsc(PlaylistSong::getSortOrder);
        List<PlaylistSong> playlistSongs = playlistSongMapper.selectList(wrapper);
        return playlistSongs.stream().map(PlaylistSong::getSongId).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void incrementPlayCount(Long playlistId) {
        Playlist playlist = getById(playlistId);
        if (playlist != null) {
            playlist.setPlayCount(playlist.getPlayCount() + 1);
            updateById(playlist);
        }
    }

    /**
     * 更新播放列表歌曲数量
     */
    private void updateSongCount(Long playlistId) {
        LambdaQueryWrapper<PlaylistSong> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlaylistSong::getPlaylistId, playlistId);
        int count = (playlistSongMapper.selectCount(wrapper)).intValue();
        Playlist playlist = getById(playlistId);
        if (playlist != null) {
            playlist.setSongCount(count);
            updateById(playlist);
        }
    }

    /**
     * 转换为DTO
     */
    private PlaylistDTO convertToDTO(Playlist playlist) {
        PlaylistDTO dto = new PlaylistDTO();
        dto.setId(playlist.getId());
        dto.setUserId(playlist.getUserId());
        dto.setName(playlist.getName());
        dto.setCoverImage(playlist.getCoverImage());
        dto.setDescription(playlist.getDescription());
        dto.setIsPublic(playlist.getIsPublic());
        dto.setSongCount(playlist.getSongCount());
        dto.setPlayCount(playlist.getPlayCount());
        dto.setCreateTime(playlist.getCreateTime());
        dto.setUpdateTime(playlist.getUpdateTime());
        return dto;
    }
}
