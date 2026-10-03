# Privacy and source behavior


wonderPlay has no account service, telemetry endpoint, ad SDK, cloud inference, or developer-operated backend.

## Data on your device

Room stores track metadata, favorites, playlists and their order, listening history, recent searches, selected local-file URIs, and the saved queue/position. DataStore stores preferences. Coil caches artwork in memory and the app cache. Ordinary ExoPlayer buffering supports streaming; there is no download manager or permanent stream cache.

Library data is private app storage. Android cloud backup is disabled. Clearing app data or uninstalling deletes it. Clearing listening history and recent searches is available in the app. Choosing local files grants read access through Android's Storage Access Framework; files are not uploaded or copied into a music download directory.

## Network requests

- Audius discovery endpoints (`api.audius.co` and discovered Audius network nodes): search text, track/artist/playlist identifiers, public track metadata, and streaming requests. Public media URLs may redirect to Audius-operated storage hosts.
- Artwork hosts returned by the public provider: requests for cover images displayed in the app.
- MusicBrainz (`musicbrainz.org`), where canonical artwork matching is requested: normalized artist/song text. Lookups are bounded and rate limited.
- Cover Art Archive (`coverartarchive.org`, potentially redirecting to `archive.org` infrastructure): release IDs and cover image requests after a metadata match.
- YouTube Music (`music.youtube.com`): only an explicit external handoff; the app/browser you open has its own privacy terms.

These providers receive ordinary HTTP information, including the user's IP address. wonderPlay adds no persistent device identifier. Third-party providers have independent policies and availability. Search does not use any remote AI model.

## Access and limits

Only publicly streamable, non-gated source audio is accepted. No login credentials, source access tokens, payment access, DRM keys, or YouTube media extraction are collected or implemented. The app does not impersonate any service's official app.

Wi-Fi-only restrictions are enforced for remote playback. Browsing/search and artwork requests still use the available connection. They are not an app-wide firewall. Local files remain playable without networking.

## Permissions

- Internet and network state: search, artwork, streaming, and checking playback network preferences.
- Foreground media playback and wake lock: continuous playback and Android media controls.
- Vibration: optional tactile feedback.

There is no microphone, camera, location, contacts, phone state, broad file access or advertising ID permission. Media-session notifications use Android's media notification handling without an unrelated onboarding permission wall.
