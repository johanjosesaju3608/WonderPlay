# Third-party notices

Original wonderPlay source and vector identity are MIT licensed (see LICENSE). This document identifies direct runtime/build/test dependencies; their own copyright notices and license texts remain applicable. No copyrighted music or album-art catalog is bundled with the app. Material icons are used under Apache-2.0.

| Component | License | Upstream |
| --- | --- | --- |
| Kotlin and kotlinx.coroutines | Apache-2.0 | https://github.com/JetBrains/kotlin ; https://github.com/Kotlin/kotlinx.coroutines |
| AndroidX Core, Activity, Lifecycle, Compose, Material, Room, DataStore, Palette, SplashScreen | Apache-2.0 | https://android.googlesource.com/platform/frameworks/support/ |
| AndroidX Media3 / ExoPlayer | Apache-2.0 | https://github.com/androidx/media |
| Coil | Apache-2.0 | https://github.com/coil-kt/coil |
| OkHttp and Okio | Apache-2.0 | https://github.com/square/okhttp ; https://github.com/square/okio |
| Guava / ListenableFuture, Error Prone annotations | Apache-2.0 | https://github.com/google/guava ; https://github.com/google/error-prone |
| Android Gradle Plugin, KSP | Apache-2.0 | https://android.googlesource.com/platform/tools/base/ ; https://github.com/google/ksp |
| Gradle | Apache-2.0 | https://github.com/gradle/gradle |
| JUnit 4 (tests only) | EPL-1.0 | https://github.com/junit-team/junit4 |
| Robolectric (tests only) | MIT | https://github.com/robolectric/robolectric |
| Hamcrest (tests only) | BSD-3-Clause | https://github.com/hamcrest/JavaHamcrest |
| AndroidX Test / Espresso (tests only) | Apache-2.0 | https://github.com/android/android-test |

Audius's HTTP API is consumed directly; no Audius SDK or music files are bundled. MusicBrainz/Cover Art Archive metadata and artwork remain governed by their respective licenses and artwork owners' rights. Attribution does not imply endorsement or grant additional music redistribution rights.

Apache License 2.0 text: https://www.apache.org/licenses/LICENSE-2.0
Eclipse Public License 1.0 text: https://www.eclipse.org/legal/epl-v10.html

Runtime dependencies also carry their upstream META-INF notices in their published artifacts. Build-only and test-only components are not part of the release runtime. The dependency lock/catalog and Gradle dependency reports can be used to audit resolved versions.
