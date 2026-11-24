# Publishing

Two artifacts go to Maven Central under the `com.faceplugin` group:

| Artifact | What it is | Size |
| -------- | ---------- | ---- |
| `com.faceplugin:face-liveness-kit` | Engine lifecycle, serialized calls, camera frames, per-frame face attributes | small |
| `com.faceplugin:face-liveness-active` | The guided active-liveness screen with its anti-spoofing gate | small |

Each one ships the AAR, a sources jar and a Dokka javadoc jar, which is what Maven Central
validation requires.

`face-liveness-active` depends on the kit, so one dependency line pulls what a customer needs.

> **The engine is not published from here.** `libs/facerecognitionsdk.aar` is the same binary the
> recognition product ships, and it is published once, as
> `com.faceplugin:face-recognition-sdk`, from
> [Face-Recognition-LivenessDetection-Android](https://github.com/Faceplugin-ltd/FaceRecognition-LivenessDetection-Android).
> `face-liveness-kit` declares that coordinate as a runtime dependency in its POM
> (`publishDependsOnEngine`), so a consumer still gets the engine from one line — there is just one
> copy of the ~78 MB binary on Central instead of two. If you ever need a liveness-only engine
> coordinate, change `ENGINE_ARTIFACT_ID` in [`gradle/publish.gradle`](gradle/publish.gradle) and
> publish it from the SDK repository, not from here.

## One-time setup

1. **A namespace.** Register `com.faceplugin` at [central.sonatype.com](https://central.sonatype.com)
   and verify the domain. You get a username and password token.
2. **A signing key.** Maven Central requires every file to be signed:

   ```powershell
   gpg --full-generate-key
   ```

   ```powershell
   gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
   ```

   ```powershell
   gpg --armor --export-secret-keys YOUR_KEY_ID | Set-Content -Encoding ascii signing-key.asc
   ```

3. **Credentials**, in `~/.gradle/gradle.properties` — never in this repository:

   ```properties
   centralUsername=<token username>
   centralPassword=<token password>
   signingKey=-----BEGIN PGP PRIVATE KEY BLOCK-----\n...\n-----END PGP PRIVATE KEY BLOCK-----
   signingPassword=<key passphrase>
   ```

   `signingKey` is the whole armoured block with `\n` for line breaks. CI reads the same names from
   the environment as `CENTRAL_USERNAME`, `CENTRAL_PASSWORD`, `SIGNING_KEY`, `SIGNING_PASSWORD`.

## Releasing

1. Set the version in `gradle.properties`:

   ```properties
   faceplugin.version=1.0.0
   ```

2. Make sure `libs/facerecognitionsdk.aar` is the build the libraries were tested against. From
   Face-Recognition-LivenessDetection-Android-LX-SDK:

   ```powershell
   python scripts/export.py --app <path to Face-LivenessDetection-Android>
   ```

3. Dry run into `build/staging-deploy/` first:

   ```powershell
   .\gradlew.bat publishReleasePublicationToLocalStagingRepository
   ```

4. Then publish:

   ```powershell
   .\gradlew.bat publishReleasePublicationToCentralRepository
   ```

5. Release the staging repository at [central.sonatype.com](https://central.sonatype.com).

## What a customer writes

```gradle
dependencies {
    implementation 'com.faceplugin:face-liveness-active:1.0.0'
}
```

That brings in `face-liveness-kit` and, through the kit's POM, the engine.
