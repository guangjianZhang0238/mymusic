package com.music.content.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.music.api.dto.AlbumDTO;
import com.music.api.dto.SwitchAlbumArtistDTO;
import com.music.api.vo.AlbumVO;
import com.music.api.vo.SwitchAlbumArtistResultVO;
import com.music.content.entity.Album;

import java.util.List;

public interface AlbumService extends IService<Album> {
    
    Page<AlbumVO> pageList(String keyword, Long artistId, int current, int size);
    
    AlbumVO getDetail(Long id);
    
    Long create(AlbumDTO dto);
    
    void update(AlbumDTO dto);
    
    void delete(Long id);

    /**
     * 将一批歌曲收录到指定专辑中（不会复制歌曲，只建立关联关系）
     */
    void bindSongs(Long albumId, List<Long> songIds);

    /**
     * 切换专辑歌手（迁移专辑文件夹并同步路径）
     */
    SwitchAlbumArtistResultVO switchArtist(Long albumId, SwitchAlbumArtistDTO dto);
}
