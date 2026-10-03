# wonderPlay 1.0.1 validation

Validated on 2026-10-03 with JDK 17, Gradle 8.13 and an Android 15 / API 35 ARM64 emulator.

- 21 unit tests cover metadata/ranking, queue state, Room transactions, settings persistence, Audius and YouTube identities, expansion bounds and album-color contrast.
- Debug and release lint pass without errors; debug, instrumentation and minified signed release APKs build successfully.
- Four emulator tests cover actual local audio decoding/seek/pause/error/recovery, navigation, search recreation, color preference persistence, and five local mini-player expansion/collapse cycles followed by activity recreation.
- Live YouTube Music search returned mainstream recordings; in-app audio playback reported PLAYING with an advancing position.
- The signed 1.0.1 APK successfully updated an installed 1.0.0 APK.
- The signed release player was visually inspected with a real YouTube Music track, including the album-derived gradient and accents.
- Release signing uses the same certificate as 1.0.0. APK signing and ZIP/16 KB alignment checks pass.

## Crash and buffering changes

Expansion spring values are clamped to 0–1 before driving padding, sizing and opacity. This prevents invalid negative padding during overshoot. The full-screen blurred artwork layer has been replaced by a cover-colored gradient. The emulator expansion regression passes; the original user's device crash trace was not available, so physical-device confirmation remains necessary.

Audio buffering now targets 30–120 seconds, retaining the short 700 ms initial-play threshold and using 3 seconds before resuming after a stall. Stream transfers have bounded retries; unavailable source resolutions and missing local files report errors without exhausting those retries. A live Audius endpoint check took about 21 seconds and returned HTTP 522 from its upstream media host. Better buffering cannot eliminate that provider outage.

## Boundaries

No physical Galaxy S25 was available. Samsung background restrictions, Bluetooth routing, haptic feel, battery consumption and 120 Hz pacing need physical-device testing. Document-provider behavior varies.

YouTube Music is the default song-search catalog, using NewPipe Extractor for public audio. There is no account sync or paid-content access. Audius is optional in Settings; local files remain offline-capable. No offline remote downloads, lossless guarantee, trained neural recommendation model, crossfade or loudness normalization is shipped. Upstream changes and region restrictions can affect public playback.
