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
    suspend fun getCachedSong(songId: Long): CachedSongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(song: CachedSongEntity)

    @Query("DELETE FROM cached_songs WHERE songId = :songId")
    suspend fun deleteBySongId(songId: Long)

    @Query("DELETE FROM cached_songs")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM cached_songs")
    suspend fun getCount(): Int

    @Query("SELECT * FROM cached_songs ORDER BY cachedAt ASC LIMIT 1")
    suspend fun getOldest(): CachedSongEntity?
}

