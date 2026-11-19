# FatFlow

Native Android nutrition app. Daily calories come from Seymour Alpert’s fat-release ceiling, carbs cycle around tennis and lifting, and food logs stay on device.

Product spec: [`fatflow_android_app_specification.md`](fatflow_android_app_specification.md).

| | |
|---|---|
| Package | `com.fatflow.app` |
| Version | `0.1.0` |
| minSdk / targetSdk | 26 / 36 |
| Modules | `:engine` (JVM math) and `:app` (Compose UI, Room, Health Connect) |

## Layout

```
android/          Gradle project
  engine/         Metabolic engine and unit tests
  app/            Android app
.github/workflows/android-apk.yml
```

Screens: onboarding, Today, week planner, trajectory, check-in, food search, barcode scan, settings.

## Build locally

Needs JDK 21 and an Android SDK with platform `android-37` (or `android-37.0`).

```sh
cd android
echo "sdk.dir=$ANDROID_HOME" > local.properties   # skip if Android Studio already wrote it
./gradlew :engine:test
./gradlew :app:assembleDebug
```

Debug APK: `android/app/build/outputs/apk/debug/app-debug.apk`.

Install on a connected device:

```sh
./gradlew :app:installDebug
adb shell am start -n com.fatflow.app/.MainActivity
```

## CI

[`.github/workflows/android-apk.yml`](.github/workflows/android-apk.yml) runs on pushes and pull requests that touch `android/` or the workflow.

- `:engine:test`
- `:app:assembleDebug`
- Actions artifact `fatflow-debug-<sha>` (30 days)

A push to the default branch also publishes a GitHub Release:

- Tag `apk-<full sha>`
- Asset `fatflow-<short sha>.apk` (unsigned debug build)

Pull requests upload the artifact only. They do not create a release.
