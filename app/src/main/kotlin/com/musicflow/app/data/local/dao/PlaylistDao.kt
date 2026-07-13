package com.musicflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.musicflow.app.data.local.entity.PlaylistEntity
import com.musicflow.app.data.local.entity.PlaylistTrackMap
import com.musicflow.app.data.local.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for playlist operations.
 */
@Dao
interface PlaylistDao {

    // ── Playlist CRUD ──────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPlaylist(playlist: PlaylistEntity): Long

    @Query("SELECT * FROM playlists ORDER BY created_at DESC")
    fun observeAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists ORDER BY created_at DESC")
    suspend fun getAllPlaylists(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE id = :playlistId LIMIT 1")
    suspend fun getPlaylistById(playlistId: Long): PlaylistEntity?

    @Query("UPDATE playlists SET name = :name WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    // ── Playlist Tracks ────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrackToPlaylist(map: PlaylistTrackMap)

    @Query("DELETE FROM playlist_track_map WHERE playlist_id = :playlistId AND song_id = :songId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, songId: String)

    @Query("SELECT song_id FROM playlist_track_map WHERE playlist_id = :playlistId ORDER BY position ASC")
    suspend fun getTrackIdsInPlaylist(playlistId: Long): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM playlist_track_map WHERE playlist_id = :playlistId AND song_id = :songId)")
    suspend fun isTrackInPlaylist(playlistId: Long, songId: String): Boolean

    @Query("SELECT COUNT(*) FROM playlist_track_map WHERE playlist_id = :playlistId")
    suspend fun getPlaylistTrackCount(playlistId: Long): Int

    @Transaction
    suspend fun addTrackToPlaylistAtomic(playlistId: Long, songId: String) {
        val count = getPlaylistTrackCount(playlistId)
        addTrackToPlaylist(PlaylistTrackMap(playlistId = playlistId, songId = songId, position = count))
    }

    // ── Combined queries ───────────────────────────────────────────

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN playlist_track_map ptm ON t.song_id = ptm.song_id
        WHERE ptm.playlist_id = :playlistId
        ORDER BY ptm.position ASC
    """)
    fun observePlaylistTracks(playlistId: Long): Flow<List<TrackEntity>>

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN playlist_track_map ptm ON t.song_id = ptm.song_id
        WHERE ptm.playlist_id = :playlistId
        ORDER BY ptm.position ASC
    """)
    suspend fun getPlaylistTracks(playlistId: Long): List<TrackEntity>
}
