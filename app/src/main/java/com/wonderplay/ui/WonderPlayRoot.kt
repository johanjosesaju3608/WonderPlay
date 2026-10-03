package com.wonderplay.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wonderplay.AppViewModel
import com.wonderplay.domain.*
import com.wonderplay.source.YouTubeMusicSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WonderPlayRoot(viewModel:AppViewModel) {
    val vm=viewModel
    val settings by vm.settings.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val player by vm.player.state.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val locals by vm.localTracks.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val recent by vm.recentSearches.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf("Home") }
    var libraryRoute by rememberSaveable { mutableStateOf("all") }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var queue by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf<Track?>(null) }
    var addTrack by remember { mutableStateOf<Track?>(null) }
    var collection by remember { mutableStateOf<MusicCollection?>(null) }
    var newPlaylist by remember { mutableStateOf(false) }
    val context=LocalContext.current
    val view=LocalView.current
    val haptics=remember(view,settings.haptics) { HapticsController(view,settings.haptics) }
    val snackbar=remember { SnackbarHostState() }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { vm.importLocal(it) }
    val import={ picker.launch(arrayOf("audio/*")) }
    val search={ tab="Search"; libraryRoute="all"; collection=null; vm.closeDetail() }
    fun open(uri:Uri) { try { context.startActivity(Intent(Intent.ACTION_VIEW,uri)) } catch(_:android.content.ActivityNotFoundException) { Toast.makeText(context,"No app can open this link.",Toast.LENGTH_SHORT).show() } }
    LaunchedEffect(ui.message) { ui.message?.let { snackbar.showSnackbar(it); vm.dismissMessage() } }
    LaunchedEffect(player.current) { if(player.current==null) { expanded=false; queue=false } }
    val detail=collection ?: ui.collection ?: ui.artist?.let { MusicCollection(it.id,it.name,"Artist · ${it.tracks.size} tracks",it.artworkUrl,it.tracks) }
    BackHandler(enabled=expanded || showSettings || detail!=null || ui.detailLoading || libraryRoute!="all" || tab!="Home") {
        when { expanded -> expanded=false; showSettings -> showSettings=false; detail!=null || ui.detailLoading -> { collection=null; vm.closeDetail() }; libraryRoute!="all" -> libraryRoute="all"; else -> tab="Home" }
    }
    WonderTheme(settings, player.current?.artworkUrl) {
        CompositionLocalProvider(LocalWonderHaptics provides haptics) {
            Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
                Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f)) {
                            when {
                                showSettings -> SettingsScreen(settings,vm) { showSettings=false }
                                detail!=null -> CollectionScreen(detail,vm,{ collection=null; vm.closeDetail() },{ menu=it },player.current?.id)
                                ui.detailLoading -> Column { ScreenHeader("Opening music",onBack=vm::closeDetail); LinearProgressIndicator(Modifier.fillMaxWidth().padding(Space.page)) }
                                tab=="Home" -> HomeScreen(vm,history,favorites,locals,search,import,{showSettings=true},{libraryRoute=it;tab="Library"},{menu=it},player.current?.id)
                                tab=="Search" -> SearchScreen(vm,ui.query,ui.searchTracks,ui.searching,ui.searchError,ui.hasMore,recent,favorites,player.current?.id,{menu=it},{open(YouTubeMusicSource.searchUrl(it))})
                                else -> LibraryScreen(vm,favorites,history,locals,playlists,libraryRoute,{libraryRoute=it},import,search,{collection=it},{menu=it},player.current?.id)
                            }
                        }
                        if(player.current!=null) Spacer(Modifier.height(84.dp))
                        Spacer(Modifier.height(88.dp))
                    }
                    FloatingNavigation(tab, { label -> haptics.perform(HapticEvent.SELECT); tab=label; showSettings=false; collection=null; vm.closeDetail() }, Modifier.align(Alignment.BottomCenter))
                    player.current?.let { PlayerSurface(player,vm,expanded,{expanded=it},favorites.any { t->t.id==it.id },{vm.toggleFavorite(it)},{queue=true},{menu=it}) }
                    SnackbarHost(snackbar,Modifier.align(Alignment.BottomCenter).padding(bottom=if(player.current!=null) 152.dp else 72.dp))
                }
            }
            if(queue) ModalBottomSheet(onDismissRequest={queue=false},containerColor=MaterialTheme.colorScheme.surface) { QueueSheet(player,vm,{queue=false}) }
            menu?.let { track ->
                ModalBottomSheet(onDismissRequest={menu=null},containerColor=MaterialTheme.colorScheme.surface) {
                    TrackRow(track,{vm.player.play(listOf(track));menu=null},{menu=null})
                    ActionRow(if(favorites.any { it.id==track.id }) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,"${if(favorites.any { it.id==track.id }) "Remove from" else "Add to"} favorites") { vm.toggleFavorite(track); menu=null }
                    ActionRow(Icons.Rounded.PlaylistPlay,"Play next") { vm.player.addNext(track); menu=null }
                    ActionRow(Icons.Rounded.QueueMusic,"Add to queue") { vm.player.enqueue(track); menu=null }
                    ActionRow(Icons.Rounded.PlaylistAdd,"Add to playlist") { addTrack=track; menu=null }
                    ActionRow(Icons.Rounded.Person,"View artist") { vm.openArtist(track); menu=null; expanded=false }
                    ActionRow(Icons.Rounded.Album,if(track.album.isBlank()) "Track details" else "View album") { vm.openAlbum(track); menu=null; expanded=false }
                    if(track.source=="local") ActionRow(Icons.Rounded.RemoveCircleOutline,"Remove from library","The file itself stays on your device") { vm.removeLocal(track); menu=null }
                    Spacer(Modifier.height(24.dp))
                }
            }
            addTrack?.let { track -> ModalBottomSheet(onDismissRequest={addTrack=null},containerColor=MaterialTheme.colorScheme.surface) {
                ScreenHeader("Add to playlist",track.title)
                LazyColumn(Modifier.heightIn(max=440.dp)) {
                    item { ActionRow(Icons.Rounded.Add,"New playlist") { newPlaylist=true } }
                    items(playlists,key={it.id}) { list -> ActionRow(Icons.Rounded.QueueMusic,list.name,"${list.tracks.size} tracks") { vm.addToPlaylist(list.id,track); addTrack=null } }
                }
                Spacer(Modifier.height(24.dp))
            } }
            if(newPlaylist) NameDialog("New playlist","Name","Create",onDismiss={newPlaylist=false}) { vm.createPlaylist(it,addTrack); newPlaylist=false; addTrack=null }
        }
    }
}
