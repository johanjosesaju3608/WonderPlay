# wonderPlay 1.0.4 validation

- 34 unit tests passed: metadata/ranking, queue state, Room persistence, expansion bounds, dynamic-color contrast, high-resolution artwork URLs, system artwork metadata, lyric parsing/timing/matching and featured playlist/song parsing.
- 10 Android 15 emulator tests passed: playback/seek/pause/error recovery, library/settings/search flows, mini-player gestures, repeated expansion/recreation, four-line lyrics preview/full-screen opening, lyric-line seeking, plain lyrics and unavailable-lyrics states.
- Debug and release lint passed. Signed release APK passed signature and 16 KB alignment checks.
- Signed 1.0.4 installed successfully over the published 1.0.3 APK using the unchanged release key.
- Live signed-release checks: regional featured playlists loaded; a playlist opened with canonical song titles/artists and 100 tracks; song search, expanded player, LRCLIB synced preview and full-screen lyrics worked. YouTube audio played after retrying an upstream HTML-instead-of-JSON response. Android reported the wonderPlay media session active and PLAYING with advancing position/buffering.

## Lyrics

Local files are excluded from lyrics lookup and the lyrics panel. LRCLIB exact lookup is followed by title/artist/duration-checked search, then lyrics.ovh plain-lyrics fallback. Requests are cancellable when the track changes and successful results are cached only in memory, bounded to 40 tracks. LRC tests cover fractions, repeated stamps, offset tags, intros, backward seeking and invalid timestamps. Synced previews have four fixed rows. Full-screen lyrics follow playback, allow tapping a timestamped line to seek and support disabling following for manual browsing. Reduce motion disables transitions and animated following. Plain lyrics are labelled unsynced.

Unavailable results identify the available sources, not the entire internet. Provider failures are distinguished from confirmed missing responses. Retry and an external web-search button remain available. No lyrics are bundled in source/APK assets.

## System media controls

MediaSession receives the high-resolution (up to 1024 pixels) cover URL rather than the original 120-pixel Google thumbnail. Media3 decodes and shares artwork within Android's device-specific bitmap limits. The release exposes an active standard MediaSession, metadata, audio attributes and a MediaStyle notification. No physical Samsung or Vivo device was available for this release. In particular, Vivo Origin Island recognition has not been verified or claimed fixed; firmware-specific eligibility is outside emulator coverage.

## Limits

Featured playlists use the anonymous YouTube Music regional home feed and music playlist metadata. Lists are bounded to five pages and larger lists identify the loaded subset. No account sync or personalized signed-in feed is implemented. YouTube, LRCLIB and lyrics.ovh availability and upstream response changes can affect requests. Missing artwork uses the existing theme fallback. Dynamic colors are automatic and Audius is no longer a supported provider; saved library metadata is retained.

Wi-Fi-only applies to remote playback, not browsing, lyrics or artwork. No remote offline downloads, lossless guarantee, crossfade or loudness normalization is shipped. Physical-device Origin Island/Now Bar appearance, Bluetooth routes, haptics, battery behavior and refresh-rate performance require device testing.

## 1.0.4 checks

The new shuffle test selects a nonzero initial index, confirms it starts the shuffled playback order, visits every other queue occurrence exactly once, wraps on repeat-all and turns shuffle off without resetting an advanced position. The test waits for repeat-mode application before requesting navigation. Queue order is read from Media3's timeline, not the saved library order. The service anchors a newly shuffled/replaced queue to the current item. Existing four-way mini-player gestures, expansion and recreation remain covered.

Cover URL tests include `w120-h120` and `s120` music image formats, unrelated/local hosts, and opt-in high-resolution video candidates. The UI rejects small successful placeholder images and falls back to the original thumbnail; unverified video candidates are not substituted into system metadata.

Search tests cover official editorial IDs versus community/personal-mix IDs, featured-result parsing and exact collection-name ranking. Live checks verify Random Access Memories album search/opening and Bollywood Hitlist official-playlist search/opening. The floating bar has accessible Home/Search/Library tabs and remains separated from the mini-player.

An initial Android emulator run was aborted by a system crash. Verification was repeated in a fresh emulator session; the aborted run is not counted as passing.
