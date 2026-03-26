package com.music.app.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.music.app.vo.AppAlbumVO;
import com.music.app.vo.AppArtistVO;
import com.music.app.vo.AppSongVO;
import com.music.app.vo.SearchSuggestionVO;

import java.util.List;

public interface AppMusicService {

    Page<AppSongVO> pageSongs(Integer current, Integer size, String keyword, Long albumId, Long artistId);

    List<AppSongVO> hotSongs();

    /**
     * 获取热门歌手（按“歌手参与歌曲数 songCount”口径排序）
     * @param limit 返回数量上限
     */
    List<AppArtistVO> hotArtists(int limit);

    Page<AppAlbumVO> pageAlbums(Integer current, Integer size, String keyword, Long artistId);

    Page<AppArtistVO> pageArtists(Integer current, Integer size, String keyword);

    List<AppSongVO> getSongsByIds(List<Long> ids);

    AppAlbumVO getAlbumDetail(Long albumId);

    AppArtistVO getArtistDetail(Long artistId);
    
    /**
     * 获取歌手热门歌曲（按播放量降序，最夐20首）
     */
    List<AppSongVO> getArtistTopSongs(Long artistId, int limit);

    /**
     * 根据拼音首字母获取搜索联想结果
     * @param keyword 拼音首字母关键字
     * @param limit 返回结果数量限制
     * @return 搜索联想结果列表
     */
    List<SearchSuggestionVO> getSuggestions(String keyword, int limit);
}
