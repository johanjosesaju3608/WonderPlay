# Release validation

Validated on 2026-10-03 with JDK 17, Gradle 8.13 and an Android 15 / API 35 ARM64 emulator.

- `testDebugUnitTest`: 16 tests passed, covering metadata normalization/ranking, queue state, Room library transactions and Audius response parsing.
- `lintDebug` and `lintRelease`: passed without errors.
- `assembleDebug`, `assembleDebugAndroidTest` and `assembleRelease`: passed.
- Android instrumentation: 3 tests passed, covering actual Media3 local playback, seeking, pause, error/recovery, playlist/settings navigation and search state.
- Signed, minified release APK installed and launched successfully on the emulator.
- Release signing verified with `apksigner`; ZIP alignment verified including the 16 KB alignment check.
- Live Audius search returned real track metadata in the release app.
- The signed release streamed a public Audius track successfully: MediaSession reported PLAYING with an advancing position, and continued playing after the app was backgrounded.

## Boundaries

These checks do not certify every device or every upstream track. No physical Galaxy S25 was available. Samsung background restrictions, Bluetooth routing, haptic feel, battery consumption and 120 Hz frame pacing need physical-device testing. Document-provider behavior also varies by device/provider.

Audius supplies the public streaming catalog; local files play through user-selected document URIs. YouTube Music is an external search handoff, not an in-app playback source. No offline remote downloads, lossless guarantee, trained neural recommendation model, crossfade or loudness normalization is shipped.
