package com.wonderplay

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil3.imageLoader
import com.wonderplay.domain.*
import com.wonderplay.player.PlayerController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Presentation state contains only metadata; audio lifecycle belongs to the service. */
data class UiState(
    val query: String = "",
    val searchTracks: List<Track> = emptyList(),
    val searching: Boolean = false,
    val searchError: String? = null,
    val hasMore: Boolean = false,
    val collection: MusicCollection? = null,
    val artist: Artist? = null,
    val detailLoading: Boolean = false,
    val message: String? = null,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as WonderPlayApp).container
    private val library = container.library
    private val sources = container.sources
    val player = PlayerController(application, library, sources)
    private val mutableUi = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = mutableUi.asStateFlow()
    val settings = library.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val favorites = library.favorites.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val history = library.history.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val localTracks = library.localTracks.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val playlists = library.playlists.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val recentSearches = library.recentSearches.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private var searchJob: Job? = null
    private var detailJob: Job? = null
    private var searchGeneration = 0
    private var remoteOffset = 0

    init { player.connect() }

    fun search(query: String) {
        searchJob?.cancel()
        val generation = ++searchGeneration
        remoteOffset = 0
        mutableUi.update { it.copy(query = query, searchTracks = emptyList(), searching = query.isNotBlank(), searchError = null, hasMore = false) }
        if (query.isBlank()) return
        searchJob = viewModelScope.launch {
            delay(250)
            runSearch(query.trim(), generation, append = false)
        }
    }

    fun retrySearch() { search(mutableUi.value.query) }

    fun loadMore() {
        val current = mutableUi.value
        if (current.searching || !current.hasMore || current.query.isBlank()) return
        val generation = searchGeneration
        mutableUi.update { it.copy(searching = true) }
        searchJob = viewModelScope.launch { runSearch(current.query.trim(), generation, append = true) }
    }

    private suspend fun runSearch(query: String, generation: Int, append: Boolean) {
        try {
            val result = sources.search(query, if (append) remoteOffset else 0)
            if (generation != searchGeneration) return
            // Provider pages are 30 entries, even if ranking filters some candidates.
            remoteOffset += 30
            mutableUi.update { state ->
                state.copy(searchTracks = (if (append) state.searchTracks + result.tracks else result.tracks).distinctBy { it.id }, searching = false, searchError = null, hasMore = result.hasMore)
            }
            if (!append && result.tracks.isNotEmpty()) library.addSearch(query)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            if (generation != searchGeneration) return
            debugLog("Search failed", error)
            mutableUi.update { it.copy(searching = false, searchError = error.userMessage("Music source unavailable. Check your connection and try again.")) }
        }
    }

    fun toggleFavorite(track: Track) = mutate { library.toggleFavorite(track) }
    fun createPlaylist(name: String) = mutate {
        val clean = name.trim().take(80)
        if (clean.isEmpty()) { message("Give your playlist a name."); return@mutate }
        library.createPlaylist(clean)
        message("Playlist created")
    }
    fun renamePlaylist(id: Long, name: String) = mutate {
        val clean = name.trim().take(80)
        if (clean.isNotEmpty()) library.renamePlaylist(id, clean)
    }
    fun deletePlaylist(id: Long) = mutate { library.deletePlaylist(id) }
    fun addToPlaylist(id: Long, track: Track) = mutate { library.addToPlaylist(id, track); message("Added to playlist") }
    fun removeFromPlaylist(id: Long, trackId: String) = mutate { library.removeFromPlaylist(id, trackId) }
    fun movePlaylistTrack(id: Long, from: Int, to: Int) = mutate { library.movePlaylistTrack(id, from, to) }
    fun removeLocal(track: Track) = mutate { library.removeLocalTrack(track.id) }

    fun importLocal(uris: List<Uri>) = mutate {
        var added = 0
        var failed = 0
        for (uri in uris.distinct()) {
            try {
                getApplication<Application>().contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val track = sources.importLocal(uri)
                library.saveLocalTrack(track)
                added++
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { debugLog("Import failed", error); failed++ }
        }
        message(when {
            failed == 0 -> "$added ${if (added == 1) "track" else "tracks"} added to your library"
            added == 0 -> "Couldn’t read those audio files. Choose files available on this device."
            else -> "$added tracks added. $failed couldn’t be read."
        })
    }

    fun openArtist(track: Track) {
        detailJob?.cancel()
        mutableUi.update { it.copy(collection = null, artist = null, detailLoading = true) }
        detailJob = viewModelScope.launch {
            try {
                val artist = sources.getArtist(track)
                mutableUi.update { it.copy(artist = artist, detailLoading = false) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                debugLog("Artist unavailable", error)
                mutableUi.update { it.copy(detailLoading = false, message = error.userMessage("Couldn’t open this artist. Try again.")) }
            }
        }
    }

    fun openAlbum(track: Track) {
        detailJob?.cancel()
        mutableUi.update { it.copy(collection = null, artist = null, detailLoading = true) }
        detailJob = viewModelScope.launch {
            try {
                val album = sources.getAlbum(track)
                mutableUi.update { it.copy(collection = album, detailLoading = false) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                debugLog("Album unavailable", error)
                mutableUi.update { it.copy(detailLoading = false, message = error.userMessage("Couldn’t open this album. Try again.")) }
            }
        }
    }

    fun closeDetail() {
        detailJob?.cancel()
        mutableUi.update { it.copy(collection = null, artist = null, detailLoading = false) }
    }
    fun updateSettings(value: AppSettings) = mutate { library.updateSettings(value) }
    fun clearHistory() = mutate { library.clearHistory(); message("Listening history cleared") }
    fun clearSearches() = mutate { library.clearSearches() }
    fun clearArtworkCache() = mutate {
        val loader = getApplication<Application>().imageLoader
        loader.memoryCache?.clear()
        withContext(Dispatchers.IO) { loader.diskCache?.clear() }
        message("Artwork cache cleared")
    }
    fun dismissMessage() { mutableUi.update { it.copy(message = null) } }
    private fun message(value: String) { mutableUi.update { it.copy(message = value) } }
    private fun mutate(block: suspend () -> Unit) { viewModelScope.launch {
        try { block() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { debugLog("Library operation failed", error); message(error.userMessage("Couldn’t save that change. Please try again.")) }
    } }
    private fun Exception.userMessage(fallback: String): String = if (this is SourceException) message ?: fallback else fallback
    private fun debugLog(message: String, error: Exception) { if (BuildConfig.DEBUG) Log.w("wonderPlay", message, error) }
    override fun onCleared() { player.release(); super.onCleared() }
}
