package com.music.app.vo;

import lombok.Data;

/**
 * 搜索联想结果VO
 */
@Data
public class SearchSuggestionVO {
    /**
     * 类型：1-歌曲，2-歌手，3-专辑
     */
    private Integer type;
    
    /**
     * ID
     */
    private Long id;
    
    /**
     * 名称
     */
    private String name;
    
    /**
     * 歌手名称（歌曲类型时）
     */
    private String artistName;
    
    /**
     * 专辑名称（歌曲类型时）
     */
    private String albumName;
    
    /**
     * 封面图片URL
     */
    private String coverImage;
    
    /**
     * 匹配的关键字
     */
    private String matchedKeyword;
}