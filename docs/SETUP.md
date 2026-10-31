# Device installation

These instructions require APKs built by the included workflow or Android Studio. The source ZIP itself is not installable.

## Moto G Stylus

1. If using Android Studio, enable Developer options and USB debugging on the Moto. Connect with a data-capable USB cable, unlock it, and accept its computer authorization prompt.
2. Select the phone device with the `phone` module in Android Studio, then Run. For a prebuilt APK, you can use ADB:

```sh
adb devices
adb -s PHONE_SERIAL install -r phone-debug.apk
```

Replace `PHONE_SERIAL` with the actual device ID from `adb devices`; point the install command at the downloaded APK's actual path. Alternatively, open the phone APK on the Moto and follow Android's installation prompts.

3. Open WristFlick. Select **Set up accessibility**, read the disclosure and continue to Settings. Find WristFlick under Accessibility's downloaded/installed apps or services and enable it. The exact menu name depends on the Moto's Android version.
4. If Android says the setting is restricted after sideloading, review [Google's restricted-settings guidance](https://support.google.com/android/answer/12623953). For this project, if you trust the build, open **Settings > Apps > WristFlick > More > Allow restricted settings** where that option exists, then return to Accessibility. Do not assume that every Android version has identical menus.
5. Return to WristFlick, enable a 30-minute session, then open TikTok's full-screen feed.

## Galaxy Watch 8

1. Pair the watch with the Moto in Galaxy Wearable. This pairs the watch for normal use. ADB pairing below separately authorizes your development computer.
2. Put the computer and watch on the same Wi-Fi network.
3. On the watch, open **Settings > About watch > Software information** and tap **Software version** repeatedly to enable Developer options. Samsung's documented procedure uses five taps; menu wording can vary with One UI.
4. In Developer options enable **ADB debugging** and **Wireless debugging**, where shown.
5. In Wireless debugging select **Pair new device**. Android Studio's **Pair Devices Using Wi-Fi** can use the shown pairing code. With ADB manually:

```sh
adb pair WATCH_IP:PAIRING_PORT
adb connect WATCH_IP:DEBUG_PORT
adb devices
adb -s WATCH_IP:DEBUG_PORT install -r wear-debug.apk
```

Use the actual IP/ports from the watch. **The pairing port and the normal debugging port can differ.** Enter the code when ADB prompts. Do not use the placeholder text literally.

6. Open WristFlick from the watch's app launcher. With TikTok open on the phone and a session enabled, the status should become ready.
7. After installation/testing, turn off wireless debugging when you no longer need it. The app's normal watch-to-phone controls do not use ADB.

## Troubleshooting

| Watch result | What to check |
| --- | --- |
| Connect phone · install companion | Both WristFlick apps are installed, Bluetooth is connected, and both APKs came from the same build/signing key. |
| Enable accessibility on phone | Enable WristFlick's accessibility service on the Moto. |
| Enable remote on phone | Open the phone app and start a 30-minute session. Re-enable after screen lock or process restart. |
| Open TikTok's video feed | Keep TikTok in front on the unlocked phone. Unsupported package variants are blocked. |
| Request expired | Enable automatic date/time on both devices and check connectivity. |
| No reply · check phone | A command may have happened without its reply arriving. Inspect the phone before repeating it. |
| Gesture cancelled | Avoid touching the phone while the remote performs a gesture; check for conflicting accessibility services or overlays. |
| Gesture completes but wrong TikTok behavior | Close comments/menus and test on a standard full-screen video. Geometry in `common/Controls.kt` may need tuning for this device/UI. |
| Android refuses an APK update | A cloud rebuild can generate a new debug signing key. Uninstall only WristFlick from both devices, then install the matching new APK pair and repeat setup. |

The previous video may be unavailable in some feed states. Double-tap is implemented as TikTok's normal video double-tap; its effect depends on the current TikTok screen. There is no independent confirmation of a like.
