<div align="center">
<img alt="FacePlugin" src="https://raw.githubusercontent.com/Faceplugin-ltd/faceplugin-assets/main/brand/logo.png" width="400"/>
</div>

#### 🌐 Company Site - [Here](https://faceplugin.com)
#### 🤗 Hugging Face - [Here](https://huggingface.co/FacePlugin-Ltd)
#### 🛟 Help Center - [Here](https://doc.faceplugin.com)
#### 🐳 Docker Hub - [Here](https://hub.docker.com/u/faceplugin)

# FacePlugin Face Liveness Detection — Android (Fully On-Premise)

> **Ready in ~10 minutes (after the AAR download):**
> Drop `facerecognitionsdk.aar` into `libs/` → Run on a phone
> Jump: [Quick start](#quick-start-checklist) · [Get the library](#get-the-library-libsfacerecognitionsdkaar) · [Run the demo](#run-the-demo) · [Active liveness](#active-liveness) · [Setup](#setup-on-your-own-app) · [About SDK](#about-sdk)

## Quick start checklist

- [ ] Clone `FaceLivenessDetection-Android`
- [ ] Download `facerecognitionsdk.aar` — see [Get the library](#get-the-library-libsfacerecognitionsdkaar)
- [ ] Place it in `libs/` (next to `libs/README.md`)
- [ ] Open this folder in Android Studio → Run on a **physical** phone
- [ ] Home status shows **Ready** → **Active liveness / Passive liveness** unlock

> **Your own app?** Skip to [Setup on your own app](#setup-on-your-own-app). Full API: [doc.faceplugin.com](https://doc.faceplugin.com).

## Introduction

FacePlugin **Face Liveness Detection SDK for Android** is a fully on-device anti-spoofing engine for KYC and remote identity verification. It detects printed photos, video replay, 3D masks, and deepfake-style attacks — no extra hardware, no cloud.

It runs liveness two ways, and this demo app ships both:

- **Passive** — the engine scores a single frame. Nothing is asked of the user.
- **Active** — the face is scored for spoofing **first**, and only a face that clears that gate is asked for a short sequence of actions (blink, smile, turn your head). A recorded video cannot answer a sequence it has never seen, so the two together are the stronger check against replay.

All processing stays on the phone. **No** biometric data leaves the device.

This repository is the whole product: the **engine** (`libs/facerecognitionsdk.aar`), two **helper libraries** you add to your project, and a **demo app** built from those same libraries — so what you read here is what the demo runs. The engine is built by **Face-Recognition-LivenessDetection-Android-LX-SDK**, the same build the [Face-Recognition-LivenessDetection-Android](https://github.com/Faceplugin-ltd/FaceRecognition-LivenessDetection-Android) demo uses.

### What is in here

| Module | What it is |
| ------ | ---------- |
| [`face-liveness-kit`](face-liveness-kit/README.md) | Engine lifecycle, serialized SDK calls, camera frames, per-frame face attributes |
| [`face-liveness-active`](face-liveness-active/README.md) | The guided active-liveness screen: anti-spoofing gate, then the challenges, returned as a result |
| `app` | The demo, built from those two libraries |
| `libs/` | `facerecognitionsdk.aar`, the engine |

Publishing the two libraries to Maven Central: [PUBLISHING.md](PUBLISHING.md).

### Main Functionalities

| Demo tile | What it does |
| --------- | ------------ |
| **Active liveness** | Anti-spoofing gate, then a guided challenge sequence — face frame, progress ring, per-step instructions, and a pass/fail result listing every check |
| **Passive liveness** | Live camera anti-spoofing with a face box and a liveness / quality / pose HUD |
| **Settings** | Camera lens, the challenge pool and how many are asked, random order, engine thresholds, passive score threshold |
| **About** | FacePlugin Face Liveness SDK — on-device anti-spoofing |

### Product List

| Platform | Repository |
| -------- | ---------- |
| **Android** | **[FaceLivenessDetection-Android](https://github.com/Faceplugin-ltd/FaceLivenessDetection-Android)** **(this repo)** |
| iOS | [FaceLivenessDetection-iOS](https://github.com/Faceplugin-ltd/FaceLivenessDetection-iOS) |
| React Native | [FaceLivenessDetection-ReactNative](https://github.com/Faceplugin-ltd/FaceLivenessDetection-ReactNative) |
| Flutter | [FaceLivenessDetection-Flutter](https://github.com/Faceplugin-ltd/FaceLivenessDetection-Flutter) |
| .NET MAUI | [FaceLivenessDetection-DotNet-MAUI](https://github.com/Faceplugin-ltd/FaceLivenessDetection-DotNet-MAUI) |
| Ionic Cordova | [FaceLivenessDetection-Ionic-Cordova](https://github.com/Faceplugin-ltd/FaceLivenessDetection-Ionic-Cordova) |
| Ionic Capacitor | [FaceLivenessDetection-Ionic-Capacitor](https://github.com/Faceplugin-ltd/FaceLivenessDetection-Ionic-Capacitor) |
| Windows | [FaceLivenessDetection-Windows](https://github.com/Faceplugin-ltd/FaceLivenessDetection-Windows) |
| Linux | [FaceLivenessDetection-Linux](https://github.com/Faceplugin-ltd/FaceLivenessDetection-Linux) |
### Related products

- **Face Recognition + Liveness — Android** — [FaceRecognition-LivenessDetection-Android](https://github.com/Faceplugin-ltd/FaceRecognition-LivenessDetection-Android) — recognition + liveness + attributes
- **Face Recognition + Liveness — iOS** — [FaceRecognition-LivenessDetection-iOS](https://github.com/Faceplugin-ltd/FaceRecognition-LivenessDetection-iOS) — recognition + liveness + attributes
- **Face Recognition — Windows** — [FaceRecognition-Windows](https://github.com/Faceplugin-ltd/FaceRecognition-Windows) — on-premise server
- **Face Recognition — Docker** — [FaceRecognition-Docker](https://github.com/Faceplugin-ltd/FaceRecognition-Docker) — on-premise server


## Before you start

| Step | What you need |
| ---- | ------------- |
| 1 | Android Studio + a **real device** (emulator is not recommended) |
| 2 | `facerecognitionsdk.aar` in `./libs/` — see [Get the library](#get-the-library-libsfacerecognitionsdkaar) |
| 3 | Demo license is already in the repo for `com.faceplugin.faceliveness`. Request a new key only if you change `applicationId` — see [SDK License](#sdk-license) |

### System requirements

| Item | Minimum | Recommended |
| ---- | ------- | ----------- |
| Android | API 24 (7.0) | API 29 (10) or newer |
| ABI | `arm64-v8a`, `armeabi-v7a` | `arm64-v8a` |
| Camera | Front or rear | Physical device |

## Get the library (`libs/facerecognitionsdk.aar`)

`libs/facerecognitionsdk.aar` is empty on GitHub because the binary is too large. Gradle fails fast
when the file is missing.

### Where to get it

**[Download the engine from Releases](https://github.com/Faceplugin-ltd/FaceRecognition-LivenessDetection-Android/releases/tag/latest)**

Inside FacePlugin, the same file comes straight out of the SDK repository:

```bash
# from Face-Recognition-LivenessDetection-Android-LX-SDK
python scripts/export.py --app ../../APPs/Face-LivenessDetection-Android
```

### How to place it

```bash
git clone https://github.com/Faceplugin-ltd/FaceLivenessDetection-Android.git
cd FaceLivenessDetection-Android
```

Put `facerecognitionsdk.aar` **here** — not in a nested folder:

```text
FaceLivenessDetection-Android/
├── libs/
│   ├── README.md
│   └── facerecognitionsdk.aar
├── face-liveness-kit/
├── face-liveness-active/
└── app/
```

The engine is a large binary, so use [ABI splits or an app bundle](https://developer.android.com/studio/build/configure-apk-splits) for release builds.

## Run the demo

1. Open **this** folder in Android Studio.
2. Run on a device. The demo already has a valid `LICENSE_KEY` for `com.faceplugin.faceliveness`.

Keep `applicationId` **`com.faceplugin.faceliveness`** for the included license.

### Screenshots

| Active liveness(Result) | Passive liveness |
| ----------------- | ------------------ |
| <p align="center"><img src="https://raw.githubusercontent.com/Faceplugin-ltd/faceplugin-assets/main/screenshots/face-liveness/mobile/active_result.png" alt="FacePlugin Face Liveness — Active liveness result: anti-spoofing gate and challenges passed" width="220"/></p> | <p align="center"><img src="https://raw.githubusercontent.com/Faceplugin-ltd/faceplugin-assets/main/screenshots/face-liveness/mobile/passive.png" alt="FacePlugin Face Liveness — Passive liveness: live camera anti-spoofing with liveness, quality and pose" width="220"/></p> |

## Active liveness

Open **Active liveness** on the home screen. One attempt runs in three stages, in this order:

1. **Align** — one face, framed well enough to be scored.
2. **Passive anti-spoofing gate** — the face is scored and a few frames are averaged. Below the
   threshold the attempt ends here and **no challenge is asked**: a printed photo or a replayed
   video is never invited to blink.
3. **Challenges** — a shuffled sequence of actions over the face frame, with a progress ring and
   per-step instructions.

It ends on a result card listing the gate and every challenge with its outcome, how long the attempt
took, and the passive score the gate measured.

**Losing the face starts the attempt over.** Gone from the frame for about a second and everything
resets to stage one, the gate included, so no attempt can be half-completed by one face and finished
by another. The screen says so, and `ActiveLivenessResult` is only ever produced for an attempt run
end to end by one face.

### When a face cannot be checked

A face in front of the camera is not always one the engine can score. Rather than leaving the user
holding still against a frame that will never pass, the screen names what is wrong:

| On screen | What the frame shows |
| --------- | -------------------- |
| Move closer | The face fills less of the frame than **Minimum face size** |
| Move back a little | The face runs past the edge of the frame, or nearly fills it |
| Move your face into the middle | The face is well off centre |
| Look straight at the camera | Yaw, pitch or roll is beyond **Start alignment tolerance** |
| Too dark or blurry | The engine's own `ImageQuality` is below **Minimum image quality** |
| Cannot read your face | Well framed, and the engine still returned no liveness score |
| Only one face, please | More than one face is in the frame |

These are checked in the order a person would fix them, and only while the face is being framed and
scored — a challenge deliberately turns the head away, so the pose and quality checks step aside
once the challenges start. Both thresholds are editable under **Settings → Active liveness**.

### Using it in your app

The screen is [`face-liveness-active`](face-liveness-active/README.md) — one activity, launched for
a result:

```kotlin
val launcher = registerForActivityResult(StartActivityForResult()) { outcome ->
    val result = ActiveLivenessActivity.result(outcome.data)
    when {
        result == null -> { /* closed before it finished */ }
        result.passed -> { /* live person */ }
        !result.passivePassed -> { /* turned away at the anti-spoofing gate */ }
        else -> { /* a challenge was not completed: result.failedCheck */ }
    }
}

launcher.launch(
    ActiveLivenessActivity.intent(
        this,
        ActiveLivenessOptions(count = 3, livenessThreshold = 0.5f),
    ),
)
```

`FaceEngine.start` has to have succeeded first — see [Setup](#setup-on-your-own-app).

### How the checks are judged

The engine scores a frame; it does not sequence anything. So the sequence lives in
`ActiveLivenessSession`, over the per-frame attributes the engine reports:

```kotlin
val options = FaceRecognitionSDK.DetectOptions()
options.liveness = true    // passive anti-spoofing score -> the gate
options.pose = true        // yaw / pitch / roll, in degrees -> the turn and tilt challenges
options.eyes = true        // eyesOpen 0..1               -> blink
options.expression = true  // smile, mouthOpen 0..1       -> smile
val faces = FaceRecognitionSDK.detect(bitmap, options)
```

| Stage | What it waits for |
| ----- | ----------------- |
| Align | One face, framed and square to the camera — the table above is what it checks |
| Gate | The average of **passive samples** scores reaching the **score threshold** |
| Each challenge | The condition below, held for two frames running |
| Restart | The face out of frame for about a second, at any stage past aligning |
| Fail | **Frames allowed per stage** elapse without the condition being met |

| Challenge | Satisfied when |
| --------- | -------------- |
| Blink | `eyesOpen` drops to the eye-closed threshold and recovers, **Blinks required** times |
| Smile | `smile` reaches the smile threshold |
| Turn left / right | `yaw` passes the turn angle in that direction |
| Look up / down | `pitch` passes the tilt angle in that direction |
| Move closer | The face box grows by the move-closer fraction from where the challenge started |

Turning the wrong way is answered with a nudge rather than a silent failure, and every threshold
above is editable under **Settings → Active liveness**.

> **Head-pose direction.** `yaw` and `pitch` are the engine's `Pan` and `Tilt` in degrees, and which
> way is positive is a property of the engine build. If a turn challenge accepts the opposite
> direction on your device, flip **Settings → Invert left / right** (or **Invert up / down**) — no
> rebuild needed.

### Replay resistance

The gate runs before the challenges, and the sequence is shuffled on each attempt
(**Settings → Random order**), so the challenges and their order are not known ahead of time. Keep
both on in production: the result card reports the passive score and each challenge separately.

## SDK License

Licenses are **offline** and bound to your `applicationId`.

The engine reads the key against your package name, so a key issued for one `applicationId` does not
activate another. The demo ships the key for `com.faceplugin.faceliveness`.

> **This build changed engines.** The app now runs on `facerecognitionsdk.aar`, and that runtime
> checks licences with its own library. If the home status shows **Invalid license!** or
> **No activated!** on a clean install, the bundled key predates the change —
> [contact us](#contact) for one issued for `com.faceplugin.faceliveness` on this runtime.

### How to get a license

The demo holds its key in [`MainActivity.kt`](app/src/main/java/com/faceplugin/faceliveness/ui/MainActivity.kt)
and hands it to `FaceEngine.start`, which activates and initialises the engine on a background
thread:

```kotlin
FaceEngine.start(this, LICENSE_KEY) { code ->
    // code == FaceRecognitionSDK.SDK_SUCCESS once the engine is ready
}
```

Please [contact us](#contact) to get a license for **your own app**.

### License capabilities

After activation, `FaceRecognitionSDK.license()` (and `FaceEngine.licenseStatus()` in the demo kit) reports what the key unlocks:

- **Liveness only** / **Recognition + Liveness** — both liveness tiles unlock
- **Recognition only** — liveness stays unavailable in this app
- **Not licensed** — the tiles stay locked until you activate

## Setup on your own app

Minimal integration (details: [doc.faceplugin.com](https://doc.faceplugin.com)):

1. Copy `face-liveness-kit/` and `face-liveness-active/` into your project and put
   `facerecognitionsdk.aar` in `libs/`.
2. `settings.gradle`: `include ':face-liveness-kit'`, `include ':face-liveness-active'`
3. `app/build.gradle`:

   ```gradle
   implementation project(':face-liveness-active')
   implementation files(rootProject.file('libs/facerecognitionsdk.aar'))
   ```

   with `minSdk 24`, `abiFilters 'arm64-v8a', 'armeabi-v7a'`, and
   `packaging { jniLibs { useLegacyPackaging = true } }`.
4. Add the `CAMERA` permission.
5. Start the engine once, off the main thread — `FaceEngine.start` does the activation and the init
   for you:

   ```kotlin
   FaceEngine.start(context, "YOUR_LICENSE_KEY") { code ->
       // code == FaceRecognitionSDK.SDK_SUCCESS once the engine is ready
   }
   ```

6. Passive: `FaceEngine.detect(bitmap, DetectOptions.PASSIVE)` per frame. Active: launch
   `ActiveLivenessActivity` for a result — see [Active liveness](#active-liveness).

Prefer Maven Central to copying the modules? See [PUBLISHING.md](PUBLISHING.md).

Request a license for **your** `applicationId`, not the demo’s.

## About SDK

Public class: `com.faceplugin.facerecognitionsdk.FaceRecognitionSDK`, from `libs/facerecognitionsdk.aar`. Activate once at start-up, then call the engine. The first start unpacks the on-device model pack, so it takes a few seconds. Call `activate` and `init` from a background thread, and keep every later call on one thread — the engine is not concurrent. Full reference: [doc.faceplugin.com](https://doc.faceplugin.com).

| Code | Meaning |
| ---- | ------- |
| 0 | Success |
| 1 | License invalid |
| 2 | License expired |
| 3 | Not activated |
| 4 | Init failed |

## Contact

<div align="left">
<a target="_blank" href="mailto:info@faceplugin.com"><img src="https://img.shields.io/badge/email-info@faceplugin.com-blue.svg?logo=gmail" alt="faceplugin.com"></a>&emsp;
<a target="_blank" href="https://t.me/FacePluginSupport"><img src="https://img.shields.io/badge/telegram-@FacePluginSupport-blue.svg?logo=telegram" alt="Telegram @FacePluginSupport"></a>&emsp;
<a target="_blank" href="https://wa.me/+14692784822"><img src="https://img.shields.io/badge/whatsapp-faceplugin-blue.svg?logo=whatsapp" alt="faceplugin.com"></a>
</div>
