package com.wonderplay.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tracks")
data class TrackEntity(@PrimaryKey val id: String, val payload: String)

@Entity(tableName = "favorites", foreignKeys = [ForeignKey(entity = TrackEntity::class, parentColumns = ["id"], childColumns = ["trackId"], onDelete = ForeignKey.CASCADE)])
data class FavoriteEntity(@PrimaryKey val trackId: String, val addedAt: Long)

@Entity(tableName = "history", foreignKeys = [ForeignKey(entity = TrackEntity::class, parentColumns = ["id"], childColumns = ["trackId"], onDelete = ForeignKey.CASCADE)])
data class HistoryEntity(@PrimaryKey val trackId: String, val playedAt: Long)

@Entity(tableName = "local_tracks", foreignKeys = [ForeignKey(entity = TrackEntity::class, parentColumns = ["id"], childColumns = ["trackId"], onDelete = ForeignKey.CASCADE)])
data class LocalEntity(@PrimaryKey val trackId: String, val addedAt: Long)

@Entity(tableName = "playlists")
data class PlaylistEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val createdAt: Long)

@Entity(tableName = "playlist_tracks", primaryKeys = ["playlistId", "trackId"], indices = [Index("trackId")], foreignKeys = [
    ForeignKey(entity = PlaylistEntity::class, parentColumns = ["id"], childColumns = ["playlistId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = TrackEntity::class, parentColumns = ["id"], childColumns = ["trackId"], onDelete = ForeignKey.CASCADE),
])
data class PlaylistTrackEntity(val playlistId: Long, val trackId: String, val position: Int)

data class PlaylistTrackRow(val playlistId: Long, val payload: String)

@Entity(tableName = "queue_items")
data class QueueEntity(@PrimaryKey val position: Int, val payload: String)

@Entity(tableName = "queue_state")
data class QueueStateEntity(@PrimaryKey val id: Int = 0, val trackIndex: Int, val positionMs: Long)

@Dao
interface LibraryDao {
    @Upsert suspend fun putTrack(value: TrackEntity)
    @Query("DELETE FROM tracks WHERE id = :trackId") suspend fun deleteTrack(trackId: String)
    @Query("SELECT t.* FROM tracks t INNER JOIN favorites f ON t.id = f.trackId ORDER BY f.addedAt DESC, t.id") fun favorites(): Flow<List<TrackEntity>>
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE trackId = :id)") suspend fun isFavorite(id: String): Boolean
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putFavorite(value: FavoriteEntity)
    @Query("DELETE FROM favorites WHERE trackId = :id") suspend fun deleteFavorite(id: String)
    @Query("SELECT t.* FROM tracks t INNER JOIN history h ON t.id = h.trackId ORDER BY h.playedAt DESC, t.id LIMIT 200") fun history(): Flow<List<TrackEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putHistory(value: HistoryEntity)
    @Query("DELETE FROM history WHERE trackId NOT IN (SELECT trackId FROM history ORDER BY playedAt DESC LIMIT 200)") suspend fun trimHistory()
    @Query("DELETE FROM history") suspend fun clearHistory()
    @Query("SELECT t.* FROM tracks t INNER JOIN local_tracks l ON t.id = l.trackId ORDER BY l.addedAt DESC, t.id") fun localTracks(): Flow<List<TrackEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putLocal(value: LocalEntity)
    @Insert suspend fun createPlaylist(value: PlaylistEntity): Long
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC, id DESC") fun playlists(): Flow<List<PlaylistEntity>>
    @Query("SELECT EXISTS(SELECT 1 FROM playlists WHERE id = :id)") suspend fun playlistExists(id: Long): Boolean
    @Query("UPDATE playlists SET name = :name WHERE id = :id") suspend fun renamePlaylist(id: Long, name: String)
    @Query("DELETE FROM playlists WHERE id = :id") suspend fun deletePlaylist(id: Long)
    @Query("SELECT p.playlistId, t.payload FROM playlist_tracks p INNER JOIN tracks t ON t.id = p.trackId ORDER BY p.playlistId, p.position, p.trackId") fun playlistTracks(): Flow<List<PlaylistTrackRow>>
    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :id ORDER BY position, trackId") suspend fun playlistEntries(id: Long): List<PlaylistTrackEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addPlaylistTrack(value: PlaylistTrackEntity)
    @Query("DELETE FROM playlist_tracks WHERE playlistId = :id AND trackId = :trackId") suspend fun removePlaylistTrack(id: Long, trackId: String)
    @Query("UPDATE playlist_tracks SET position = :position WHERE playlistId = :id AND trackId = :trackId") suspend fun setPlaylistPosition(id: Long, trackId: String, position: Int)
    @Query("DELETE FROM queue_items") suspend fun clearQueue()
    @Insert suspend fun putQueue(values: List<QueueEntity>)
    @Upsert suspend fun putQueueState(value: QueueStateEntity)
    @Query("SELECT * FROM queue_items ORDER BY position") suspend fun queue(): List<QueueEntity>
    @Query("SELECT * FROM queue_state WHERE id = 0") suspend fun queueState(): QueueStateEntity?
}

@Database(entities = [TrackEntity::class, FavoriteEntity::class, HistoryEntity::class, LocalEntity::class, PlaylistEntity::class, PlaylistTrackEntity::class, QueueEntity::class, QueueStateEntity::class], version = 1, exportSchema = true)
abstract class LibraryDatabase : RoomDatabase() { abstract fun libraryDao(): LibraryDao }
