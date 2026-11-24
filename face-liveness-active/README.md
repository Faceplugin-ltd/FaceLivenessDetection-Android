# face-liveness-active

The guided active-liveness screen, as a drop-in activity.

One attempt runs in three stages, in this order:

1. **Align** — one face, square to the camera.
2. **Passive anti-spoofing gate** — the face is scored and a few frames are averaged. Below the
   threshold the attempt ends here and **no challenge is asked**: a printed photo or a replayed
   video is never invited to blink.
3. **Challenges** — blink, smile, turn left or right, look up or down, move closer, in a shuffled
   order.

Losing the face for about a second starts the attempt over from stage one, the gate included, so no
attempt can be half-completed by one face and finished by another.

While a face is present but cannot be scored, the screen says what is wrong with it — too far, too
close, off centre, turned away, too dark, or well framed and still unscorable — instead of asking
the user to keep holding still. `ActiveLivenessSession.Notice` is that diagnosis.

```kotlin
val launcher = registerForActivityResult(StartActivityForResult()) { outcome ->
    val result = ActiveLivenessActivity.result(outcome.data)
    if (result?.passed == true) { /* live person */ }
}

launcher.launch(
    ActiveLivenessActivity.intent(
        this,
        ActiveLivenessOptions(count = 3, livenessThreshold = 0.5f),
    ),
)
```

| Type | What it is |
| ---- | ---------- |
| `ActiveLivenessActivity` | The screen: camera, face frame, progress ring, instructions, result card |
| `ActiveLivenessOptions` | Challenge pool, how many are asked, random order, and every threshold |
| `ActiveLivenessSession` | The state machine, usable on its own if you draw your own screen |
| `ActiveLivenessResult` | Pass/fail, why it failed, the passive score, and each check's outcome |
| `ActiveCheck` | One challenge: its label, hint and icon |
| `FaceFrameOverlayView` | The scrim, face cut-out and progress ring |

`FaceEngine.start` has to have succeeded before the screen opens. Every resource this module ships
is prefixed `fp_al_`, so a host app's own names cannot collide.
