package com.music.content.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.music.api.dto.AlbumDTO;
import com.music.api.vo.AlbumVO;
import com.music.content.entity.Album;

public interface AlbumService extends IService<Album> {
    
    Page<AlbumVO> pageList(String keyword, Long artistId, int current, int size);
    
    AlbumVO getDetail(Long id);
    
    Long create(AlbumDTO dto);
    
    void update(AlbumDTO dto);
    
    void delete(Long id);
}
