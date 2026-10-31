# WristFlick

A native Galaxy Watch gesture remote for TikTok on an Android phone, prepared for a Galaxy Watch 8 and Moto G Stylus.

**Source prototype. No APK has been built in this workspace and no physical-device test has been run.** The Android SDK and Gradle are absent here, and attempted dependency downloads returned HTTP 403. The included GitHub Actions workflow provides an executable build route in a separate Android-capable environment.

## Your controls

| On the watch | On the phone |
| --- | --- |
| Swipe up | Swipe down in TikTok to go to the previous video |
| Swipe down | Swipe up in TikTok to go to the next video |
| Double-tap the screen | Double-tap the video to like it |

Use these gestures **inside the open WristFlick watch app**. They are touchscreen gestures, not Samsung's finger-pinch or wrist gestures. The phone remains the video screen. There is no automatic scrolling.

## Produce installable APKs with GitHub Actions

You need a GitHub repository with Actions enabled. Nothing has been uploaded to GitHub automatically.

1. Unzip the project. Add the **contents of the WristFlick folder at the root of the repository**, including `.github/workflows/android.yml`. Do not nest them under another WristFlick folder. A Git client preserves the hidden `.github` folder; if using GitHub's web upload, check that the workflow file is present afterward.
2. Open the repository's **Actions** tab, select **Build phone and watch APKs**, then **Run workflow**. The manual button is available once the workflow is on the default branch. Pushes to `main` or `master` also trigger builds.
3. The workflow installs Java 17, Android SDK 35 and Gradle 8.11.1; runs project checks, unit tests and Android lint; assembles both APKs; and verifies their application IDs and signing certificates match.
4. If it succeeds, download the **WristFlick-debug-APKs** artifact from the run page. It contains `phone-debug.apk` and `wear-debug.apk` in their module folders.
5. Install the phone APK on the Moto and the watch APK on the Galaxy Watch. Watch installation uses Android Studio/ADB on a computer. See [device setup](docs/SETUP.md).

The workflow is supplied but has **not been run**. A successful future workflow run establishes that the build and checks passed; it does not establish TikTok compatibility on your devices.

Both APKs from one run use the same generated debug signing key, as required for watch/phone communication. Fresh cloud runs may use a different key: Android will then require uninstalling the prior prototype from **both devices** before installing the matching new pair. That resets WristFlick setup; it does not uninstall TikTok. For iterative development, use one local Android Studio installation with a consistent debug key.

## Build locally with Android Studio

1. Install Android Studio, Android SDK Platform 35 and Build Tools 35.0.0. Use JDK 17 for Gradle.
2. Run `python3 tools/bootstrap_wrapper.py` from this folder (Windows: `py tools\bootstrap_wrapper.py`). This downloads the official Gradle Wrapper JAR and verifies its published SHA-256 checksum. Python 3 is needed only for this bootstrap and helper scripts.
3. Open this folder in Android Studio and let Gradle sync. If needed, set `sdk.dir` in your local `local.properties` file or set `ANDROID_HOME` to your SDK directory.
4. Run the `phone` configuration on the Moto and `wear` on the watch. Alternatively, build from the command line:

```sh
./gradlew :common:testDebugUnitTest :phone:lintDebug :wear:lintDebug :phone:assembleDebug :wear:assembleDebug
```

On Windows use `gradlew.bat` instead of `./gradlew`.

## Use it

1. Pair the watch with your Moto using Galaxy Wearable as usual. The Moto needs Google Play services. Install the matching WristFlick app on each device.
2. On the Moto, open WristFlick, read the accessibility disclosure, then enable **WristFlick** in Android's Accessibility settings.
3. Return to the phone app and select **Enable remote for 30 minutes**.
4. Open TikTok's full-screen video feed on the unlocked phone. Close comments, keyboards, share panels and other menus.
5. Open WristFlick on the watch. It should show **Ready · TikTok in front**. Try one swipe, then check the phone. Test double-tap on a video you intend to like.
6. Disable the remote from the phone app when finished, or lock the phone to end the session. Re-enable the session after unlocking.

## Limits and privacy

- The prototype supports the TikTok package IDs `com.zhiliaoapp.musically` and `com.ss.android.ugc.trill`. TikTok Lite, browser playback and other package variants are not enabled.
- It checks the foreground package and window bounds before dispatching. It blocks screen-off/locked use, known other focused windows and keyboards. It does not understand every TikTok page or in-app sheet: you must keep the video feed open. Touch coordinates may need adjustment after device testing or TikTok UI changes.
- A green watch acknowledgement means **Android completed the touch gesture**. It does not prove that the video changed or that TikTok accepted the like. The app does not read video state or like state.
- One explicit watch gesture triggers one command. There are no automatic retries, scheduled actions, account bots, TikTok credentials or private API calls. A timeout can mean the touch happened but the reply was lost; inspect the phone before repeating it.
- Commands are accepted only from a currently nearby device running the same app ID and signing certificate. Bluetooth pairing is managed by the watch platform. Google's Data Layer is used for messaging, so this is not a claim that all transport always bypasses Google's infrastructure.
- Screen contents are not saved. The service accesses the active accessibility root's package and bounds; Android's accessibility permission itself is broader than those specific reads. There is no app backend, analytics SDK, camera/microphone access or media recording.
- Device clocks must agree within ten seconds. Old requests are rejected; use automatic date/time on both devices.
- The app supports Android 8+ on the phone and Android API 30+ on Wear OS. Exact Moto G Stylus model year and Android version have not been checked. The phone must already be compatible with Galaxy Watch 8 pairing.
- Screen timeout works normally on the watch. Wake the watch and reopen the app if necessary. Watch haptics follow system settings.
- This is a personal sideload prototype. Play Store publication would require separate accessibility-policy and distribution work.

## Project map

- `phone`: setup screen, expiring enable switch, Wear Data Layer receiver, guarded accessibility gesture service.
- `wear`: round-screen gesture pad, phone discovery, command/result messaging and haptic acknowledgement.
- `common`: gesture mapping, relative touch geometry, protocol and unit tests.
- `.github/workflows/android.yml`: builds, checks and downloadable APK artifacts.
- `docs/TESTING.md`: physical-device acceptance checklist and current validation status.

## Official references

- [Wear OS Data Layer and matching identity requirements](https://developer.android.com/training/wearables/data/overview)
- [Wear OS messaging](https://developer.android.com/training/wearables/data/client-types)
- [Accessibility gesture dispatch](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#dispatchGesture(android.accessibilityservice.GestureDescription,android.accessibilityservice.AccessibilityService.GestureResultCallback,android.os.Handler))
- [Watch debugging and wireless installation](https://developer.android.com/training/wearables/get-started/debugging)
- [Gradle 8.9 plugin compatibility](https://developer.android.com/build/releases/agp-8-9-0-release-notes)
- [Official Gradle distribution and wrapper checksums](https://gradle.org/release-checksums/)
- [GitHub Actions Gradle builds and artifacts](https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-gradle)
- [Android restricted settings](https://support.google.com/android/answer/12623953)
