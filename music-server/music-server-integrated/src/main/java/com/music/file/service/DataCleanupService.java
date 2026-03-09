package com.music.file.service;

import java.util.List;

/**
 * 数据清理服务
 * 清理孤立的歌曲、歌词、专辑数据
 */
public interface DataCleanupService {

    /**
     * 执行完整的数据清理
     * 按顺序：1.清理歌曲表 2.清理歌词表 3.清理专辑表
     * @param progressCallback 进度回调
     * @return 清理结果
     */
    CleanupResult performCleanup(ProgressCallback progressCallback);

    /**
     * 清理歌曲表：删除file_path指向的文件不存在的歌曲记录
     * @param progressCallback 进度回调
     * @return 被删除的歌曲数量
     */
    int cleanupOrphanedSongs(ProgressCallback progressCallback);

    /**
     * 清理歌词表：删除对应歌曲不存在的歌词记录
     * @param progressCallback 进度回调
     * @return 被删除的歌词数量
     */
    int cleanupOrphanedLyrics(ProgressCallback progressCallback);

    /**
     * 清理专辑表：删除清理后没有歌曲的空专辑
     * @param progressCallback 进度回调
     * @return 被删除的专辑数量
     */
    int cleanupEmptyAlbums(ProgressCallback progressCallback);

    /**
     * 进度回调接口
     */
    interface ProgressCallback {
        void onProgress(int percent, String message);
    }

    /**
     * 清理结果
     */
    class CleanupResult {
        private int deletedSongs;
        private int deletedLyrics;
        private int deletedAlbums;
        private final List<String> details;
        private boolean success;
        private String errorMessage;

        public CleanupResult(List<String> details) {
            this.details = details;
            this.success = true;
        }

        public int getDeletedSongs() { return deletedSongs; }
        public void setDeletedSongs(int deletedSongs) { this.deletedSongs = deletedSongs; }

        public int getDeletedLyrics() { return deletedLyrics; }
        public void setDeletedLyrics(int deletedLyrics) { this.deletedLyrics = deletedLyrics; }

        public int getDeletedAlbums() { return deletedAlbums; }
        public void setDeletedAlbums(int deletedAlbums) { this.deletedAlbums = deletedAlbums; }

        public List<String> getDetails() { return details; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}
