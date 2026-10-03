package com.wonderplay.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.wonderplay.domain.*
import com.wonderplay.source.SourceRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@androidx.annotation.OptIn(UnstableApi::class)
class PlayerController(private val context:Context, @Suppress("UNUSED_PARAMETER") library:LibraryStore, @Suppress("UNUSED_PARAMETER") sources:SourceRegistry) {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val mutable=MutableStateFlow(PlayerState())
    val state:StateFlow<PlayerState> = mutable.asStateFlow()
    private var future:ListenableFuture<MediaController>?=null
    private var controller:MediaController?=null
    private val ready=CompletableDeferred<MediaController>()
    private var selection=0
    private var closed=false
    private val listener=object:Player.Listener {
        override fun onEvents(player:Player, events:Player.Events) { publish() }
    }
    fun connect() {
        if(future!=null || closed) return
        val pending=MediaController.Builder(context,SessionToken(context,ComponentName(context,PlaybackService::class.java))).buildAsync()
        future=pending
        pending.addListener({
            if(!closed) try {
                val player=pending.get(); controller=player; player.addListener(listener); ready.complete(player); publish()
            } catch(_:Exception) { mutable.update { it.copy(phase=PlaybackPhase.ERROR,error="Couldn’t connect to playback. Reopen wonderPlay.") }; ready.completeExceptionally(IllegalStateException("Player connection failed")) }
        },ContextCompat.getMainExecutor(context))
        scope.launch { ResolutionState.values.collect { publish() } }
        scope.launch { while(isActive) { delay(250); if(controller?.isPlaying==true) { val p=controller!!; mutable.update { it.copy(positionMs=p.currentPosition.coerceAtLeast(0),bufferedMs=p.bufferedPosition.coerceAtLeast(0)) } } } }
    }
    private fun command(action:(MediaController)->Unit) {
        connect()
        scope.launch { try { action(ready.await()); publish() } catch(cancelled:CancellationException) { throw cancelled } catch(_:Exception) { mutable.update { it.copy(phase=PlaybackPhase.ERROR,error="Couldn’t update playback. Try again.") } } }
    }
    fun play(tracks:List<Track>,index:Int=0) {
        if(tracks.isEmpty()) return
        val chosen=index.coerceIn(tracks.indices); val token=++selection
        mutable.update { it.copy(queue=tracks.toList(),index=chosen,positionMs=0,durationMs=tracks[chosen].durationMs,phase=PlaybackPhase.RESOLVING,error=null) }
        command { if(token==selection) { it.setMediaItems(tracks.map { track -> TrackMediaCodec.item(track) },chosen,0); it.prepare(); it.play() } }
    }
    fun togglePlayPause() = command {
        if(it.playWhenReady && it.playbackState!=Player.STATE_ENDED && it.playerError==null) it.pause()
        else { if(it.playbackState==Player.STATE_ENDED) it.seekToDefaultPosition(); if(it.playbackState==Player.STATE_IDLE || it.playerError!=null) it.prepare(); it.play() }
    }
    fun seekTo(positionMs:Long) = command { it.seekTo(positionMs.coerceAtLeast(0).let { value -> if(it.duration>0) value.coerceAtMost(it.duration) else value }) }
    fun next() = command { if(it.hasNextMediaItem()) { it.seekToNextMediaItem(); it.prepare(); it.play() } }
    fun previous() = command { if(it.currentPosition>3000 || !it.hasPreviousMediaItem()) it.seekTo(0) else it.seekToPreviousMediaItem() }
    /** Gesture navigation never restarts the current track at a queue boundary. */
    fun skipFromGesture(previous:Boolean,onUnavailable:()->Unit) = command {
        if(if(previous) it.hasPreviousMediaItem() else it.hasNextMediaItem()) {
            if(previous) it.seekToPreviousMediaItem() else it.seekToNextMediaItem()
            it.prepare()
        } else onUnavailable()
    }
    fun setShuffle(enabled:Boolean) = command { it.shuffleModeEnabled=enabled }
    fun setRepeat(mode:RepeatMode) = command { it.repeatMode=when(mode) { RepeatMode.OFF->Player.REPEAT_MODE_OFF; RepeatMode.ALL->Player.REPEAT_MODE_ALL; RepeatMode.ONE->Player.REPEAT_MODE_ONE } }
    fun addNext(track:Track) = command { it.addMediaItem(if(it.mediaItemCount==0) 0 else it.currentMediaItemIndex+1,TrackMediaCodec.item(track)) }
    fun enqueue(track:Track) = command { it.addMediaItem(TrackMediaCodec.item(track)) }
    fun remove(index:Int) = command { if(index in 0 until it.mediaItemCount) it.removeMediaItem(index) }
    fun move(from:Int,to:Int) = command { if(from in 0 until it.mediaItemCount && to in 0 until it.mediaItemCount && from!=to) it.moveMediaItem(from,to) }
    fun clearQueue() { selection++; command { it.stop(); it.clearMediaItems() } }
    fun playIndex(index:Int) = command { if(index in 0 until it.mediaItemCount) { it.seekTo(index,0); it.prepare(); it.play() } }
    fun retry() = command {
        val current=it.currentMediaItem?.let(TrackMediaCodec::track) ?: return@command
        val position=it.currentPosition.coerceAtLeast(0); val index=it.currentMediaItemIndex
        // New occurrence invalidates any failed/expired source resolution.
        it.replaceMediaItem(index,TrackMediaCodec.item(current)); it.seekTo(index,position); it.prepare(); it.play()
    }
    private fun publish() {
        val player=controller ?: return
        val items=(0 until player.mediaItemCount).map { player.getMediaItemAt(it) }
        val tracks=items.mapNotNull(TrackMediaCodec::track)
        val index=if(tracks.isEmpty()) -1 else player.currentMediaItemIndex.coerceIn(tracks.indices)
        val info=ResolutionState.values.value[player.currentMediaItem?.mediaId]
        val failed=player.playerError
        val error=if(failed==null) null else info?.error ?: when(failed.errorCode) {
            2001,2002 -> "Connection interrupted. Check your connection and retry."
            2005 -> "This file is unavailable. Choose it again or try another track."
            2004 -> "This track is unavailable from its source. Try again or choose another."
            else -> "Couldn’t start this track. Try again or choose another."
        }
        mutable.value=PlayerState(tracks,index,mapPhase(player.playbackState,player.isPlaying,tracks.isNotEmpty(),failed!=null,info?.resolving==true && player.playWhenReady),
            player.isPlaying,player.currentPosition.coerceAtLeast(0),player.duration.takeIf { it!=C.TIME_UNSET && it>0 } ?: tracks.getOrNull(index)?.durationMs ?: 0,
            player.bufferedPosition.coerceAtLeast(0),player.shuffleModeEnabled,when(player.repeatMode) { Player.REPEAT_MODE_ALL->RepeatMode.ALL; Player.REPEAT_MODE_ONE->RepeatMode.ONE; else->RepeatMode.OFF },error,info?.quality ?: "Source quality", playbackOrder = buildList {
                val timeline = player.currentTimeline
                var next = timeline.getFirstWindowIndex(player.shuffleModeEnabled)
                while (next != C.INDEX_UNSET && size < tracks.size) {
                    add(next); next = timeline.getNextWindowIndex(next, Player.REPEAT_MODE_OFF, player.shuffleModeEnabled)
                }
            })
    }
    fun release() { closed=true; controller?.removeListener(listener); scope.cancel(); future?.let(MediaController::releaseFuture); controller=null }
}
internal fun mapPhase(nativeState:Int,playing:Boolean,hasItem:Boolean,error:Boolean,resolving:Boolean):PlaybackPhase = when {
    error->PlaybackPhase.ERROR
    !hasItem->PlaybackPhase.IDLE
    resolving->PlaybackPhase.RESOLVING
    nativeState==Player.STATE_BUFFERING->PlaybackPhase.BUFFERING
    nativeState==Player.STATE_ENDED->PlaybackPhase.ENDED
    playing->PlaybackPhase.PLAYING
    else->PlaybackPhase.PAUSED
}
