package com.music.file.service;

import com.music.api.vo.ArtistVO;
import com.music.api.vo.AlbumVO;

import java.util.List;

/**
 * 音乐元数据服务接口
 * 提供歌手和专辑的自动匹配、创建功能
 */
public interface MusicMetadataService {
    
    /**
     * 根据关键字搜索匹配的歌手
     * @param keyword 搜索关键字
     * @param limit 返回结果数量限制
     * @return 匹配的歌手列表
     */
    List<ArtistVO> searchArtists(String keyword, int limit);
    
    /**
     * 根据歌手ID和关键字搜索匹配的专辑
     * @param artistId 歌手ID
     * @param keyword 搜索关键字
     * @param limit 返回结果数量限制
     * @return 匹配的专辑列表
     */
    List<AlbumVO> searchAlbums(Long artistId, String keyword, int limit);
    
    /**
     * 自动匹配或创建歌手
     * @param artistName 歌手名称
     * @return 歌手信息
     */
    ArtistVO autoMatchOrCreateArtist(String artistName);
    
    /**
     * 自动匹配或创建专辑
     * @param artistId 歌手ID
     * @param albumName 专辑名称
     * @return 专辑信息
     */
    AlbumVO autoMatchOrCreateAlbum(Long artistId, String albumName);
    
    /**
     * 确保歌手目录存在
     * @param artistName 歌手名称
     * @return 目录路径
     */
    String ensureArtistDirectoryExists(String artistName);
    
    /**
     * 确保专辑目录存在
     * @param artistName 歌手名称
     * @param albumName 专辑名称
     * @return 目录路径
     */
    String ensureAlbumDirectoryExists(String artistName, String albumName);
}