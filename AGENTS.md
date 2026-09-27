# AGENTS.md — FUTO Keyboard (fork of LatinIME)

## Build environment (this workspace)

- JDK 21 portable: `~/tools/jdk-21.0.12.1+1` (export `JAVA_HOME`)
- Android SDK: `~/tools/android-sdk` (export `ANDROID_HOME`; `local.properties` already has `sdk.dir`)
- Gradle wrapper 8.14.3, AGP 8.10.1, Kotlin 2.1.0. First builds are slow (NDK/CMake native, ML model binding).
- No KVM available → no emulator; rely on JVM unit tests + CI.

## Build & test commands

```bash
./gradlew :voiceinput-shared:testDebugUnitTest     # JVM unit tests (fast)
./gradlew :compileStableDebugKotlin                # compile app (slow, ~10 min)
./gradlew processStableReleaseManifestForPackage processApplicationManifestPlaystoreReleaseForBundle  # fast manifest merge
```

## Critical CI constraint — INTERNET permission

`.gitlab-ci.yml` fails the build if `android.permission.INTERNET` appears in the merged manifests of
**stableRelease** and **playstoreRelease**. `java/AndroidManifest.xml` therefore keeps
`<uses-permission android:name="android.permission.INTERNET" tools:node="remove"/>`.
Never remove that line. `ACCESS_NETWORK_STATE` is allowed and used for connectivity detection.

Verify locally (fast, reproduces the CI check):

```bash
grep -q "android.permission.INTERNET" build/intermediates/packaged_manifests/stableRelease/processStableReleaseManifestForPackage/AndroidManifest.xml && echo FAIL || echo OK
```

## Module layout

- `:voiceinput-shared` — voice input library (AudioRecognizer, RecognizerView, Whisper/GGML, `engine/` speech engine abstraction, `util/`)
- `java/` — main app module (settings UI in `java/src/org/futo/inputmethod/latin/uix/settings/`, actions in `.../uix/actions/`)
- Flavors: `stable`, `playstore`, `unstable`. Settings are `SettingsKey<T>` in `uix/Settings.kt` + per-page keys (`VoiceInputSettingKeys.kt`); UI uses `userSettingToggleDataStore` / `DropDownPickerSettingItem` from `settings/Components.kt`.
- Submodules: `libs`, `voiceinput-shared/src/main/ml` (models) — `git submodule update --init --recursive` after clone.

## Voice recognition engines (added 2026-08)

`voiceinput-shared/src/main/java/org/futo/voiceinput/shared/engine/`:
- `SpeechEngine` interface implemented by `AudioRecognizer` (local Whisper) and `GoogleSpeechRecognizer` (android.speech.SpeechRecognizer → Google online)
- `SpeechEngineSelection.kt` — `SpeechEngineMode` (auto/online/offline), `selectSpeechEngine()`, `shouldFallBackToLocal()`; unit-tested in `voiceinput-shared/src/test/`
- `RecognizerView` routes sessions and falls back to the local model on recoverable online failures
- Settings keys: `VOICE_INPUT_ENGINE_MODE`, `VOICE_INPUT_ONLINE_*` in `VoiceInputSettingKeys.kt`

## Conventions

- Git identity pre-configured (openhands). Co-authored-by: openhands <openhands@all-hands.dev> in commits.
- Do not push to `master`; feature branches + PR only.