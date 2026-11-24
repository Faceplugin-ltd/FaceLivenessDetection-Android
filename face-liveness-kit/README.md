# face-liveness-kit

The engine seam: activation, the lock that serializes every SDK call, the engine thread, camera
frames, and the per-frame face attributes the liveness checks are built on.

```kotlin
// once per process, off the main thread
FaceEngine.start(context, LICENSE_KEY) { code -> /* 0 == ready */ }

// per frame, on FaceEngine.worker
val faces = FaceEngine.detect(bitmap, DetectOptions.PASSIVE)
val live = faces.firstOrNull()?.liveness   // 0..1, NaN when not measured
```

| Type | What it is |
| ---- | ---------- |
| `FaceEngine` | Activation, init, `detect`, the call lock and the engine worker |
| `DetectOptions` | What to ask the engine for; `PASSIVE`, `ACTIVE` and `ACTIVE_FAST` are the presets this product uses |
| `DetectedFace` | One face: bounds, passive score, quality, yaw/pitch/roll, eye openness, smile, mouth openness |
| `LicenseStatus` | What the activated key unlocks |
| `CameraFrameUtils` | CameraX `ImageProxy` → an upright bitmap the engine can read |
| `CameraPreview` | Binds preview + analysis in one call |

The engine itself is `libs/facerecognitionsdk.aar`, which this module compiles against but does not
package. See the root [README](../README.md).
