# Validation and device acceptance

## Status of this delivery

- Source implementation: prepared.
- XML and project component/resource wiring: checked with `python3 tools/check_project.py`.
- Python helper syntax: checked locally.
- Kotlin compilation, Android lint and JUnit execution: run by the [GitHub build workflow](https://github.com/haileynre/motogtt/actions). Open the latest run to check its result.
- APK assembly and signature validation: run by that workflow; APK artifacts are published only after the checks pass.
- GitHub workflow: installed and triggered on pushes to `main`, with manual runs also supported.
- Emulator or physical-device test: **not run**.

The source prototype is not hardware-verified yet. Even after a successful build, physical TikTok behavior must be tested on the Moto and watch.

## Automated checks in the build workflow

`ControlsTest` verifies the requested inverted watch/phone swipe directions, ignores short/horizontal movements, checks that a like consists of two separate taps at one point, ensures status requests never touch the screen, and rejects old/future timestamps. These tests do not simulate TikTok or Android's gesture dispatcher.

The workflow runs Android lint and builds both modules. The APK check then verifies matching package IDs and signing certificates, which are required by Data Layer. Compiling and passing these checks still does not test watch/phone messaging on paired devices.

## Hardware acceptance checklist

Record the Moto model year, Android version, Galaxy Watch OS version, TikTok version and APK build/run number before starting.

| Test | Expected result | Recorded result |
| --- | --- | --- |
| No accessibility permission | Watch shows setup message; no phone touches. | Not tested |
| Service enabled but session disabled | Watch asks to enable remote; no phone touches. | Not tested |
| TikTok full-screen feed + enabled session | Watch reports ready. | Not tested |
| Swipe watch down once | One upward phone swipe; next video if supported by feed. | Not tested |
| Swipe watch up once | One downward phone swipe; previous video if available. | Not tested |
| Double-tap watch once | One two-tap phone gesture; intended video's like state checked visually. | Not tested |
| Single-tap watch | No command. | Not tested |
| Short or horizontal watch motion | No command. | Not tested |
| Touchscreen drag upward/downward slowly | Same mapping as a quick swipe after threshold. | Not tested |
| Two fingers touch watch | No command for that multi-pointer gesture. | Not tested |
| Other phone app foreground | Command blocked; other app unchanged. | Not tested |
| Keyboard or system window in front | Command blocked. | Not tested |
| Comments/share panel in TikTok | User closes panel before remote use; do not assume detection. | Not tested |
| Lock phone during session | Session ends; requires manual re-enable after unlocking. | Not tested |
| Disable session manually | Later commands blocked. | Not tested |
| Wait 30 minutes | Session expires; commands blocked. | Not tested |
| Disconnect/reconnect Bluetooth | No accumulated/replayed gesture queue; inspect phone on timeout. | Not tested |
| Kill/restart phone app process | Session disabled until manually enabled. | Not tested |
| Watch sleeps, then resumes | Controls work only after app is awake/foreground; fresh status. | Not tested |
| Replay identical command ID | Duplicate does not dispatch again within current deduplication window. | Not tested |

There is a short interval between foreground validation and touch dispatch during which the screen could change. Keep the phone on the video feed and avoid simultaneous input during testing.
