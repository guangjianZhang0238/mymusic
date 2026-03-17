package com.music.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedSongDao {

    @Query("SELECT * FROM cached_songs ORDER BY cachedAt DESC")
    fun getAllCachedSongs(): Flow<List<CachedSongEntity>>

    @Query("SELECT * FROM cached_songs WHERE songId = :songId LIMIT 1")
    fun getCachedSong(songId: Long): CachedSongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(song: CachedSongEntity): Long

    @Query("DELETE FROM cached_songs WHERE songId = :songId")
    fun deleteBySongId(songId: Long): Int

    @Query("DELETE FROM cached_songs")
    fun clearAll(): Int

    @Query("SELECT COUNT(*) FROM cached_songs")
    fun getCount(): Int

    @Query("SELECT * FROM cached_songs ORDER BY cachedAt ASC LIMIT 1")
    fun getOldest(): CachedSongEntity?
}

