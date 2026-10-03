# wonderPlay

An independent, open-source Android music player under development. **wonderPlay is the app name; VEYRA was only the working project name.**

> **Incomplete development checkpoint — not a working release.**
> The project does not currently compile. No APK has been generated or published, and no test suite has passed. This checkpoint preserves the work completed so far without presenting it as a finished application.

## What is present

- Android/Kotlin/Compose project, pinned dependency catalog, Gradle wrapper with verified distribution checksum, manifest and original vector launcher icon.
- Provider-independent domain models and library/player contracts.
- Room database, library repository, DataStore preferences and track serialization code.
- Metadata normalization, music relevance ranking and network-client code.
- Partial Media3 playback service, stream-resolution layer and track metadata serialization.
- Compose design system, reusable controls, haptics, library/playlist and discovery screen code.
- App bootstrap and ViewModel orchestration that reference components still to be implemented.
- One instrumented playback test source, not yet executed.
- MIT license, bundled third-party license text, privacy/design notes and build verification script.

These files are implementation work, **not independently verified finished features**.

## What remains

1. Implement the source registry, Audius/local providers and artwork resolver.
2. Implement the MediaController-facing player controller.
3. Finish the root navigation, Now Playing, queue and settings screens.
4. Resolve integration errors and obtain a successful full build.
5. Complete and execute unit, database, playback and Compose tests; run lint.
6. Inspect real screens and validate playback/background behavior on an emulator/device.
7. Build and verify a signed release APK, then publish a GitHub release.

See [docs/QA.md](docs/QA.md) for the actual validation status and [docs/IMPLEMENTATION_CONTRACT.md](docs/IMPLEMENTATION_CONTRACT.md) for the component interfaces.

## Intended music-source boundary

The design uses publicly streamable Audius tracks and user-selected local files. YouTube Music is intended as an explicit external app/browser handoff, **not in-app YouTube audio extraction**. Anonymous Audius search and a ranged streaming request were checked during development; this does not mean app integration is complete. No API key or cloud inference is planned.

wonderPlay is unaffiliated with Audius, YouTube, Google, Apple or Spotify. Source availability is not guaranteed, and the Audius catalog is not equivalent to YouTube Music's catalog.

## Development setup

JDK 17, Android SDK platform 36 and Build Tools 35.0.0 or later are required. Set `ANDROID_HOME` or supply `sdk.dir` in an untracked `local.properties` file. The wrapper uses Gradle 8.13, with AGP 8.13.2 and Kotlin 2.3.0.

```sh
./gradlew :app:compileDebugKotlin
# Currently expected to fail: required implementation classes are missing.
```

After the missing implementations are completed, intended validation commands are:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew connectedDebugAndroidTest
```

The manual-only CI workflow template is saved at `docs/ci/android.yml`. The existing GitHub credential cannot publish workflow files, so it is preserved as a template rather than an active workflow. Once authorized, move it to `.github/workflows/android.yml`.

## Release signing

The release build configuration reads `WONDERPLAY_KEYSTORE`, `WONDERPLAY_KEY_ALIAS`, `WONDERPLAY_STORE_PASSWORD`, and `WONDERPLAY_KEY_PASSWORD` from the environment. Without these, release output is unsigned. The dedicated signing key and credentials are stored privately outside this repository and are **not committed**. They must be preserved to sign compatible future updates.

## License and documentation

Original code and vector artwork: [MIT](LICENSE). See [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md). [PRIVACY.md](PRIVACY.md) and [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) describe the intended behavior; they must be checked against the completed implementation before a release.
