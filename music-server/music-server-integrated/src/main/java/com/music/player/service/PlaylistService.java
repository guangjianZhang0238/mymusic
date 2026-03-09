package com.music.player.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.music.player.entity.Playlist;
import com.music.player.dto.PlaylistDTO;
import com.music.player.dto.PlaylistQueryDTO;
import com.music.common.core.domain.PageResult;

import java.util.List;

/**
 * 播放列表服务接口
 */
public interface PlaylistService extends IService<Playlist> {

    /**
     * 创建播放列表
     */
    Long createPlaylist(PlaylistDTO dto);

    /**
     * 更新播放列表
     */
    void updatePlaylist(PlaylistDTO dto);

    /**
     * 删除播放列表
     */
    void deletePlaylist(Long id);

    /**
     * 获取播放列表详情
     */
    PlaylistDTO getPlaylistDetail(Long id);

    /**
     * 分页查询播放列表
     */
    PageResult<PlaylistDTO> pagePlaylist(PlaylistQueryDTO queryDTO);

    /**
     * 获取用户的播放列表
     */
    List<PlaylistDTO> getUserPlaylists(Long userId);

    /**
     * 获取公开的播放列表
     */
    List<PlaylistDTO> getPublicPlaylists(int limit);

    /**
     * 向播放列表添加歌曲
     */
    void addSongToPlaylist(Long playlistId, Long songId);

    /**
     * 从播放列表移除歌曲
     */
    void removeSongFromPlaylist(Long playlistId, Long songId);

    /**
     * 获取播放列表中的歌曲
     */
    List<Long> getPlaylistSongs(Long playlistId);

    /**
     * 增加播放列表播放次数
     */
    void incrementPlayCount(Long playlistId);
}
