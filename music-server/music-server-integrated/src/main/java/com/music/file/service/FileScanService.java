package com.music.file.service;

import java.util.List;

/**
 * 文件扫描服务
 * 用于扫描base-path文件夹，分析歌曲文件，提取歌曲信息
 */
public interface FileScanService {
    
    /**
     * 扫描歌曲库
     * 扫描base-path文件夹，分析歌曲文件，提取歌曲信息
     * 对于MySQL中没有的歌曲，在MySQL中添加相关数据
     * @return 扫描结果，包括新增的歌曲数量，更新的歌曲数量等
     */
    ScanResult scanMusicLibrary();
    
    /**
     * 扫描结果
     */
    interface ScanResult {
        /**
         * 获取新增的歌曲数量
         * @return 新增的歌曲数量
         */
        int getAddedSongs();
        
        /**
         * 获取更新的歌曲数量
         * @return 更新的歌曲数量
         */
        int getUpdatedSongs();
        
        /**
         * 获取跳过的歌曲数量
         * @return 跳过的歌曲数量
         */
        int getSkippedSongs();
        
        /**
         * 获取错误信息列表
         * @return 错误信息列表
         */
        List<String> getErrors();
    }
}