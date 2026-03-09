package com.music.content.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.music.api.dto.ArtistDTO;
import com.music.api.vo.ArtistVO;
import com.music.content.entity.Artist;

public interface ArtistService extends IService<Artist> {
    
    Page<ArtistVO> pageList(String keyword, int current, int size);
    
    ArtistVO getDetail(Long id);
    
    Long create(ArtistDTO dto);
    
    void update(ArtistDTO dto);
    
    void delete(Long id);
    
    java.util.Map<String, Object> scanArtists();
}
