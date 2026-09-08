<div align="center">
<img alt="FacePlugin" src="https://avatars.githubusercontent.com/u/160751046?s=200&v=4" width="200"/>
</div>

#### 🌐 Company Site - [Here](https://faceplugin.com)
#### 🤗 Hugging Face - [Here](https://huggingface.co/FacePlugin-Ltd)
#### 🛟 Help Center - [Here](https://doc.faceplugin.com)
#### 🐳 Docker Hub - [Here](https://hub.docker.com/u/faceplugin)

# FacePlugin Face Liveness Detection SDK — Android (Fully On-Premise)

> **Ready in ~10 minutes (after AAR download):**
> Drop `facelivenessdk.aar` into `libfacesdk/` → Run on a phone
> Jump: [Quick start](#quick-start-checklist) · [Get the AAR](#get-the-aar-libfacesdk) · [Run the demo](#run-the-demo) · [Setup](#setup-on-your-own-app) · [About SDK](#about-sdk)

## Quick start checklist

- [ ] Clone `https://github.com/Faceplugin-ltd/FaceLivenessDetection-Android`
- [ ] Download `facelivenessdk.aar` from [Google Drive](#get-the-aar-libfacesdk)
- [ ] Place it in `libfacesdk/` (next to `libfacesdk/build.gradle`)
- [ ] Open this folder in Android Studio → Run on a **physical** phone
- [ ] Home status shows **Ready** → **Liveness / Settings / About** unlock

> **Your own app?** Skip to [Setup on your own app](#setup-on-your-own-app). Full API: [doc.faceplugin.com](https://doc.faceplugin.com).

## Introduction

FacePlugin **Face Liveness Detection SDK for Android** is a fully on-device anti-spoofing engine for KYC and remote identity verification. It detects printed photos, video replay, 3D masks, and deepfake-style attacks — no extra hardware, no cloud.

All processing stays on the phone. **No** biometric data leaves the device.

This repository is the **Android demo app**. Runtime is `libfacesdk/facelivenessdk.aar` (download from Google Drive). No other FacePlugin repository is required.

### Main Functionalities

| Demo tile | What it does |
| --------- | ------------ |
| **Liveness** | Live camera anti-spoofing with face box + track / liveness / luminance HUD |
| **Settings** | Camera lens (front / back), liveness threshold |
| **About** | FacePlugin Face Liveness SDK — on-device anti-spoofing |

### Product List

| Platform | Repository |
|----------|------------|
| Android (Recognition) | [FaceRecognition-Android](https://github.com/Faceplugin-ltd/FaceRecognition-Android) |
| iOS (Recognition) | [FaceRecognition-iOS](https://github.com/Faceplugin-ltd/FaceRecognition-iOS) |
| React Native (Recognition) | [FaceRecognition-React-Native](https://github.com/Faceplugin-ltd/FaceRecognition-React-Native) |
| Flutter (Recognition) | [FaceRecognition-Flutter](https://github.com/Faceplugin-ltd/FaceRecognition-Flutter) |
| Ionic Capacitor (Recognition) | [FaceRecognition-Ionic-Capacitor](https://github.com/Faceplugin-ltd/FaceRecognition-Ionic-Capacitor) |
| Ionic Cordova (Recognition) | [FaceRecognition-Ionic-Cordova](https://github.com/Faceplugin-ltd/FaceRecognition-Ionic-Cordova) |
| Windows (Recognition) | [FaceRecognition-Windows](https://github.com/Faceplugin-ltd/FaceRecognition-Windows) |
| Linux / Docker (Recognition) | [FaceRecognition-Docker](https://github.com/Faceplugin-ltd/FaceRecognition-Docker) |
| **Android (Liveness)** | **[FaceLivenessDetection-Android](https://github.com/Faceplugin-ltd/FaceLivenessDetection-Android)** (**this repo**) |
| iOS (Liveness) | [FaceLivenessDetection-iOS](https://github.com/Faceplugin-ltd/FaceLivenessDetection-iOS) |
| Windows (Liveness) | [FaceLivenessDetection-Windows](https://github.com/Faceplugin-ltd/FaceLivenessDetection-Windows) |
| Linux / Docker (Liveness) | [FaceLivenessDetection-Docker](https://github.com/Faceplugin-ltd/FaceLivenessDetection-Docker) |


## Before you start

| Step | What you need |
| ---- | ------------- |
| 1 | Android Studio + a **real device** (emulator is not recommended) |
| 2 | `facelivenessdk.aar` in `./libfacesdk/` — see [Get the AAR](#get-the-aar-libfacesdk) |
| 3 | Demo license is already in the repo for `com.faceplugin.faceliveness`. Request a new key only if you change `applicationId` — see [SDK License](#sdk-license) |

### System requirements

| Item | Minimum | Recommended |
| ---- | ------- | ----------- |
| Android | API 24 (7.0) | API 29 (10) or newer |
| ABI | `arm64-v8a`, `armeabi-v7a` | `arm64-v8a` |
| Camera | Front or rear | Physical device |

## Get the AAR (`libfacesdk`)

`libfacesdk/facelivenessdk.aar` is empty on GitHub because the binary is too large.

### Where to download

**[FaceLivenessSDK Android (Google Drive)](https://drive.google.com/drive/folders/1x3jt02f-YHsk4WD_QlnKJSx5uQ5Ds4xH)**

### How to place it

```bash
git clone https://github.com/Faceplugin-ltd/FaceLivenessDetection-Android.git
cd FaceLivenessDetection-Android
```

Download `facelivenessdk.aar` and put it here:

```text
FaceLivenessDetection-Android/
└── libfacesdk/
    ├── build.gradle
    └── facelivenessdk.aar
```

## Run the demo

1. Open **this** folder in Android Studio.
2. Run on a device. The demo already has a valid `LICENSE_KEY` for `com.faceplugin.faceliveness`.

Keep `applicationId` **`com.faceplugin.faceliveness`** for the included license.

### Screenshots

| Home | Liveness | Settings | About |
| ---- | -------- | -------- | ----- |
| <p align="center"><img src="https://raw.githubusercontent.com/Faceplugin-ltd/faceplugin-assets/main/screenshots/face-liveness/mobile/home.png" alt="FacePlugin Face Liveness — Home with Liveness, Settings, About" width="220"/></p> | <p align="center"><img src="https://raw.githubusercontent.com/Faceplugin-ltd/faceplugin-assets/main/screenshots/face-liveness/mobile/liveness.png" alt="FacePlugin Face Liveness — live camera anti-spoofing with face box and metrics" width="220"/></p> | <p align="center"><img src="https://raw.githubusercontent.com/Faceplugin-ltd/faceplugin-assets/main/screenshots/face-liveness/mobile/settings.png" alt="FacePlugin Face Liveness — Settings for camera lens and liveness threshold" width="220"/></p> | <p align="center"><img src="https://raw.githubusercontent.com/Faceplugin-ltd/faceplugin-assets/main/screenshots/face-liveness/mobile/about.png" alt="FacePlugin Face Liveness SDK — About, on-device anti-spoofing" width="220"/></p> |

## SDK License

Licenses are **offline** and bound to your `applicationId`.

The sample app already includes a valid key for `com.faceplugin.faceliveness`. You only need a new key if you use a different `applicationId`.

### How to get a license

The code below shows how to use the license:

[https://github.com/Faceplugin-ltd/FaceLivenessDetection-Android/blob/96512bbf0839b416986a944888229a037ad73739/app/src/main/java/com/faceplugin/faceliveness/ui/MainActivity.kt#L20-L21](https://github.com/Faceplugin-ltd/FaceLivenessDetection-Android/blob/96512bbf0839b416986a944888229a037ad73739/app/src/main/java/com/faceplugin/faceliveness/ui/MainActivity.kt#L20-L21)

[https://github.com/Faceplugin-ltd/FaceLivenessDetection-Android/blob/96512bbf0839b416986a944888229a037ad73739/app/src/main/java/com/faceplugin/faceliveness/ui/MainActivity.kt#L50-L59](https://github.com/Faceplugin-ltd/FaceLivenessDetection-Android/blob/96512bbf0839b416986a944888229a037ad73739/app/src/main/java/com/faceplugin/faceliveness/ui/MainActivity.kt#L50-L59)

Please [contact us](#contact) to get a license for **your own app**.

### License capabilities

After activation, `FaceLivenessSDK.getLicenseStatus()` (and `LicenseStatus.current()` in the demo kit) reports what the key unlocks from the `license_level` field (0 / 1 / 2):

- **Liveness only** / **Recognition + Liveness** — Liveness tile
- **Recognition only** — liveness stays unavailable on this App
- **Not licensed** — tile stays locked until you activate

## Setup on your own app

Minimal integration (details: [doc.faceplugin.com](https://doc.faceplugin.com)):

1. Copy `libfacesdk/` into your project and place `facelivenessdk.aar` inside it.
2. `settings.gradle`: `include ':libfacesdk'`
3. `app/build.gradle`: `implementation project(':libfacesdk')`, `minSdk 24`, `abiFilters 'arm64-v8a', 'armeabi-v7a'`, and `packaging { jniLibs { useLegacyPackaging = true } }`
4. Add `CAMERA` permission.
5. On a **background** thread: `FaceLivenessSDK.setActivation(context, "FP1.…")` → `FaceLivenessSDK.init(context)` (`0` = success).

Optional: copy `app/.../kit/` (`FaceLivenessClient`) for demo-style threading / camera helpers.

Request a license for **your** `applicationId`, not the demo’s.

## About SDK

Public class: `com.faceplugin.facelivenessdk.FaceLivenessSDK`. Call **once per process** on a background thread: `setActivation` → `init`. Serialize native calls. Full reference: [doc.faceplugin.com](https://doc.faceplugin.com).

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
<a target="_blank" href="https://wa.me/+14692784822"><img src="https://img.shields.io/badge/whatsapp-faceplugin-blue.svg?logo=whatsapp" alt="faceplugin.com"></a>
</div>
