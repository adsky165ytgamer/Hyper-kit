# HyperKit

Android app ecosystem. Milestone 1: app shell + classroom whiteboard built on DrawBox (the Whiteboard UPS).
Architecture and terminology: `docs/ARCHITECTURE.md`. File reference: `docs/FILE_REFERENCE.md`.

## Build
Requirements (dictated by DrawBox 2.1.0's own build, see ARCHITECTURE §3):

| | Version |
|---|---|
| JDK | 21 |
| Gradle (wrapper) | 9.7.1 |
| Android Gradle Plugin | 9.2.1 |
| Kotlin | 2.4.10 |
| Compose Multiplatform | 1.11.1 (Material 3 `1.11.0-alpha07`) |
| compileSdk / minSdk / targetSdk | 37 / 24 / 35 |

You need the Android SDK **Platform 37** installed and `ANDROID_HOME` (or `local.properties` with `sdk.dir=`).

```
./gradlew :app:assembleDebug
```
APK: `app/build/outputs/apk/debug/`.

## Status
Written and statically reviewed against DrawBox's source and sample app. **Not yet compiled**:
the authoring environment had no Android SDK and no network. The first `assembleDebug` on your machine is the real verification.

## Known first-build risks (check these first)
1. **Material 3 Expressive API names** in `HyperKitTheme.kt` (`MaterialExpressiveTheme`, `MotionScheme.expressive()`) against Material3 `1.11.0-alpha07`.
2. **DrawBox publishing plugins** (`vanniktech`, `dokka`, `spotless`, `signing`) are applied by the untouched UPS build files; they must resolve from the plugin repositories.
3. **iOS / JS / wasm targets** declared by the DrawBox modules are configured but never built for `:app:assembleDebug`; Gradle may print "target disabled on this host" warnings.
4. `kotlinx-coroutines-android` was added to the catalog; the rest of the catalog is DrawBox's, verbatim.
