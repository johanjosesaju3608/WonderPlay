package com.wonderplay.data
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.wonderplay.domain.Track
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class LibraryRepositoryTest {
    private lateinit var db:LibraryDatabase
    private lateinit var repo:LibraryRepository
    private lateinit var scope:CoroutineScope
    private val one=Track("local:one","One","Artist",source="local",streamUrl="content://test/one")
    private val two=one.copy(id="local:two",title="Two")
    @Before fun setup() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        db=Room.inMemoryDatabaseBuilder(context,LibraryDatabase::class.java).allowMainThreadQueries().build()
        scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
        val store=PreferenceDataStoreFactory.create(scope=scope,produceFile={File(context.cacheDir,"${java.util.UUID.randomUUID()}.preferences_pb")})
        repo=LibraryRepository(context,db,store)
    }
    @After fun close(){scope.cancel();db.close()}
    @Test fun rapidFavoriteTogglesAreSerialized()= runBlocking {
        coroutineScope { repeat(20){launch(Dispatchers.Default){repo.toggleFavorite(one)}} }
        assertTrue(repo.favorites.first().isEmpty())
        repo.toggleFavorite(one); assertEquals(one,repo.favorites.first().single())
    }
    @Test fun playlistDuplicatesReorderRenameAndDelete()= runBlocking {
        val id=repo.createPlaylist("Drive")
        repo.addToPlaylist(id,one);repo.addToPlaylist(id,two);repo.addToPlaylist(id,one)
        assertEquals(2,db.libraryDao().playlistEntries(id).size)
        repo.movePlaylistTrack(id,0,1)
        assertEquals(listOf(two.id,one.id),db.libraryDao().playlistEntries(id).map{it.trackId})
        repo.renamePlaylist(id,"Night drive")
        assertEquals("Night drive",repo.playlists.first().single().name)
        repo.removeFromPlaylist(id,two.id)
        assertEquals(listOf(one.id),db.libraryDao().playlistEntries(id).map{it.trackId})
        repo.deletePlaylist(id);assertTrue(db.libraryDao().playlistEntries(id).isEmpty())
    }
    @Test fun queueRestoresDuplicateOccurrencesAndPosition()= runBlocking {
        repo.saveQueue(listOf(one,two,one),2,4000)
        val queue=repo.restoreQueue()
        assertEquals(listOf(one,two,one),queue.first);assertEquals(2,queue.second);assertEquals(4000L,queue.third)
        repo.saveQueue(emptyList(),99,-1);assertEquals(Triple(emptyList<Track>(),-1,0L),repo.restoreQueue())
    }
    @Test fun clearingHistoryPreservesFavoriteAndSearchesDeduplicate()= runBlocking {
        repo.toggleFavorite(one);repo.recordPlay(one);repo.clearHistory()
        assertTrue(repo.history.first().isEmpty());assertEquals(1,repo.favorites.first().size)
        repo.addSearch(" Night ");repo.addSearch("night");assertEquals(listOf("night"),repo.recentSearches.first())
    }
    @Test fun removingLocalTrackCascadesMembership()= runBlocking {
        repo.saveLocalTrack(one);repo.toggleFavorite(one);val id=repo.createPlaylist("Local");repo.addToPlaylist(id,one)
        repo.removeLocalTrack(one.id)
        assertTrue(repo.localTracks.first().isEmpty());assertTrue(repo.favorites.first().isEmpty());assertTrue(db.libraryDao().playlistEntries(id).isEmpty())
    }
}
