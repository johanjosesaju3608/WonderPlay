# Development verification ledger

- [x] Audius and local-file providers, artwork fallback and metadata/parser tests
- [x] MediaController integration and playback-state tests
- [x] Navigation, player/queue, settings and interaction tests
- [x] Debug/release compilation, 16 unit tests and lint
- [x] Emulator inspection and 3 integration/UI tests
- [x] Signed release APK verification and installation

See QA.md for executed checks and remaining physical-device validation. The product is named wonderPlay throughout public source and documentation.

1.0.1 adds YouTube Music song search and public in-app audio playback via NewPipe, optional cover-derived colors, the supplied logo, bounded expansion animation and increased streaming buffer headroom.

## 1.0.3
- [x] Synced/plain lyrics and provider fallback, full-screen lyrics and four-line preview
- [x] Live featured playlists with canonical music metadata and bounded pagination
- [x] Automatic dynamic colors, Audius removal, high-resolution system artwork
- [x] 28 unit / 9 emulator checks, lint, APK verification and update over 1.0.2
- [ ] Physical Vivo Origin Island recognition (not certified by emulator checks)

## 1.0.4
- [x] Disable lyrics lookup and UI for local files
- [x] Search albums and official featured playlists, rank exact names first
- [x] Floating themed navigation with accessible selected pill
- [x] Additional high-resolution cover formats and safe video-artwork fallback
- [x] Anchored shuffle/playback-order queue, actual navigation and repeat tests
- [x] 34 unit tests, 10 emulator tests, lint, signed APK checks and update over 1.0.3
