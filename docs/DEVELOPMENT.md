# Development guide

For app features and screenshots, see the [main README](../README.md). This guide
covers building, configuration, and APK releases.

## Build requirements

Open the repository root in Android Studio. Install JDK 25, Android SDK platform
`android-37.0`, build tools `36.0.0`, and platform tools. Set `JAVA_HOME` and
`ANDROID_HOME`, or point `sdk.dir` in ignored `local.properties` at your SDK.
Install Python 3.11 or newer as `python` on your PATH. Set the `PYTHON` environment
variable to the interpreter path if it has a different name or location.

The wrapper provides Gradle 9.6.0. The project uses Android Gradle Plugin 9.5.0
with built-in Kotlin support. Devices require Android 7.0 (API 24) or newer;
the target SDK is 37.

## Build and check

From the repository root:

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --max-workers=2
```

On Windows, use `./gradlew.bat`. The JVM/Robolectric checks do not require an
emulator. These commands do not start an emulator or install the app.

The build writes the debug APK to `app/build/outputs/apk/debug/app-debug.apk`.

## Firebase configuration

Firebase is optional. A clean checkout builds and runs without a Firebase
project. To configure Firebase, put your Android client configuration in
ignored `app/google-services.json`. Forks should use their own Firebase project.
Never commit service-account credentials or signing material.

Signed releases can use the optional `GOOGLE_SERVICES_JSON` repository secret.
Crashlytics collection is enabled in release builds and disabled in debug builds.
The Crashlytics Gradle plugin uploads R8 mappings for release builds.

## GitHub Actions

- Branch pushes and pull requests run tests, lint, and a debug build. The workflow
  uploads the APK and reports; GitHub sign-in and repository access are required.
- Version tags trigger an optimized, signed APK release with signature validation,
  SHA-256 checksums, and license notices.
- The tag must match `versionName` in `version.properties`. Increase `versionCode`
  for each released update so Android accepts it as an upgrade.
- The release workflow can also be run manually for an existing version tag.

## Release signing

Use the existing release key for every update. Local builds read signing details
from ignored `keystore.properties`, which points to `.signing/release.p12`.
Back up both files together in a secure location outside the checkout.

For a new fork with its own signing identity, run
`./scripts/New-ReleaseKey.ps1` in PowerShell with `JAVA_HOME` set. The script
refuses to replace existing signing files.

The release workflow needs these repository Actions secrets:

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded release keystore |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Signing alias |
| `ANDROID_KEY_PASSWORD` | Signing-key password |
| `GOOGLE_SERVICES_JSON` | Optional Android Firebase client JSON |

To configure them from local signing files, install PyNaCl and run:

```sh
python -m pip install PyNaCl
python scripts/configure_release.py --repo OWNER/REPO
```

The script uses `GH_TOKEN` or the Git credential helper and encrypts values before
upload. GitHub stores these secrets but does not return their values.

## Publishing a release

1. Increase `versionCode` and set `versionName` in `version.properties`.
2. Run the checks above, then commit and push the changes.
3. Create and push a tag matching `versionName`. For version `1.0.1`:

```sh
git tag -a v1.0.1 -m "GNT Reader 1.0.1"
git push origin v1.0.1
```

The workflow publishes a GitHub Release containing the signed APK, SHA-256
checksum, license, and third-party notices. The workflow saves test reports and
R8 mappings as Actions artifacts. Releases in a private repository require repository
access to download.

If a run fails, fix the problem and rerun **Release APK** for the existing tag.
Once a version is published, use a new version and tag for further changes.

To build a signed APK locally with `keystore.properties` configured:

```sh
./gradlew :app:assembleRelease :app:lintRelease --max-workers=2
```

The build writes the APK to `app/build/outputs/apk/release/app-release.apk`.

## Repository layout

| Path | Purpose |
| --- | --- |
| `app/` | Android app, resources, Room schemas, bundled database, and tests |
| `gradle/`, `gradlew*` | Gradle wrapper |
| `.github/` | CI and signed APK release workflows |
| `scripts/` | Local signing-key and encrypted GitHub secret setup |
| `tools/corpus/` | Source data and Python database tools |
| `tools/vocabulary/` | Vocabulary validation and database compilation |
| `vocabulary/` | Built-in vocabulary lists and contribution instructions |
| `docs/images/` | App screenshots used in the README |

## Corpus and personal data

The build combines `tools/corpus/base.db` with the JSON files in `vocabulary/`
to generate `app/build/generated/vocabularyAssets/database/sblgnt_data.db`.
Gradle tracks the inputs and regenerates the asset when a list changes or is
removed. The app copies this asset into `corpus.db` and stores personal vocabulary
separately in `user_vocabulary.db`.
Room exports database schemas under `app/schemas`.

Android backup includes personal vocabulary and DataStore preferences. Vocabulary
Settings also supports JSON export/import through the system file picker. Imports
validate corpus membership, merge duplicates, and retain the earliest timestamp.
Portable vocabulary files contain word-list entries and timestamps; they do not
include reading preferences.

Corpus regeneration instructions are in [tools/corpus/README.md](../tools/corpus/README.md).
To contribute a vocabulary list, follow [vocabulary/README.md](../vocabulary/README.md).
Review data and font terms in [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md)
before redistributing the assets.
