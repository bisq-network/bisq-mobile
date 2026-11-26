# Bisq Easy and Bisq Connect on F-Droid

Both apps take the same path. This page describes Bisq Easy, which goes first. [Bisq Connect](#bisq-connect) lists only where Connect differs.

Bisq Easy (`network.bisq.mobile.node`) ships on F-Droid through the reproducible-builds path: F-Droid builds each release from its tag, and publishes our signed APKs from the GitHub release when its own build matches them byte for byte. F-Droid users and GitHub users therefore get the same signature and can move between the two installs.

- Recipe for fdroiddata: [`network.bisq.mobile.node.yml`](network.bisq.mobile.node.yml) (goes to `metadata/network.bisq.mobile.node.yml` in fdroiddata)
- Store listing: `apps/nodeApp/fastlane/metadata/android/en-US/` (F-Droid reads it from the build's `subdir`)
- Build rules for a reproducible release: [`docs/android.md`](../android.md#building-what-the-tag-says)
- CI check: `.github/workflows/fdroid.yml` builds every `anode_*` tag unsigned on Linux, the same way as the release. A tag push runs the workflow as it is at that tag. The tag `anode_1.0.0` predates the fix that dropped `-Pabi`, so its own run reports armeabi-v7a as different; ignore that run. To check 1.0.0, dispatch the workflow from `main` with `apps=node` and `ref=anode_1.0.0`, which builds the tag's code with the current workflow.

## What is verified (1.0.0, commit `de6e8dd43`)

- A clean macOS build (Corretto 21.0.13) and the Linux CI build (Zulu 21.0.13) of the commit are byte-identical for all four ABI splits.
- `apksigcopier compare` (build-tools 34) of the signed release APKs against the unsigned CI build: match for arm64-v8a, x86 and x86_64. armeabi-v7a matches once both sides build with the same invocation (see below).
- The APKs carry no proprietary Google code (`scripts/verify-fdroid-apk.py`), and the dependency gate passes (`scripts/verify-fdroid-dependencies.py`).
- Signing certificate SHA-256: `20425c3acefd9aa7039f708e2cb36edf9043e7187d7066f706608a8a431f3841`.

## The one rule that is easy to break

Build with a plain `assembleRelease`, which writes the universal APK and the four splits, never with `-Pabi=...`. AGP's manifest merger formats the first output it writes slightly differently (a comment joined onto the previous line), which changes the source line numbers stored in the binary manifest. With the universal APK, the universal APK takes that; without it, armeabi-v7a does and no longer matches the published APK. The recipe and the CI workflow both follow the release invocation.

## Submission steps

1. Make sure the tag `anode_1.0.0` exists on bisq-network and points at `de6e8dd43`, the commit the APKs were built from (`META-INF/version-control-info.textproto` in the APK names it). Not the merge commit of the release PR.
2. Make sure the GitHub release `anode_1.0.0` holds the four split APKs under the exact names the recipe expects: `Bisq_Easy-1.0.0-<abi>-<versionCode>.apk`.
3. Open a Request For Packaging issue on gitlab.com/fdroid/rfp (first-time apps).
4. Fork fdroiddata, add the recipe on a branch named `network.bisq.mobile.node`. For 1.0.0 only, also copy `apps/nodeApp/fastlane/metadata/android/en-US/` from `main` to `metadata/network.bisq.mobile.node/en-US/` in fdroiddata: F-Droid reads the store listing from the checkout of the built tag, and `anode_1.0.0` predates the listing. Remove that copy from fdroiddata once a release tag carries the listing (1.0.1 on). Then run locally: `fdroid readmeta`, `fdroid rewritemeta network.bisq.mobile.node`, `fdroid lint network.bisq.mobile.node`, `fdroid checkupdates --allow-dirty network.bisq.mobile.node`, `fdroid build -v -l network.bisq.mobile.node`. The merge request needs a green pipeline and at least one working build.
5. Open the merge request. Expect weeks of review.

## Open points to check before submitting

- **JDK 21 on the build server.** The recipe installs `openjdk-21-jdk-headless` with `apt-get -t trixie`. Check which Debian release the current build server image runs and adjust the `sudo:` lines. The JDK vendor does not change the bytes (verified Corretto vs Zulu), the major version must be 21.
- **`gradle/verification-metadata.xml`.** It pins the checksums of the bisq2 jars from JitPack. Some recipes delete such files because F-Droid's build server mirrors Maven repositories. Keep it if the build passes with it; it is the only integrity check on the JitPack jars.
- **Tor binaries.** The app embeds tor through `io.github.rodvar.kmp-tor:resource-*` from Maven Central. They are built from pinned source (tor, OpenSSL, libevent, xz, zlib) in the CI of bisq-network/kmp-tor-resource, deterministically, with sha256 hashes pinned in the fork (see its `docs/DETERMINISTIC_BUILDS.md`). Reviewers may ask about prebuilt native libraries; Briar was accepted with the same argument.
- **Analytics.** Opt-in, off by default, sent over Tor to a GlitchTip server the Bisq project runs. The Sentry SDK is FOSS and is not in the F-Droid scanner's tracker list. No Tracking anti-feature is expected, but state it in the MR.
- **Gradle 9.8.1.** The build uses the wrapper version; check that fdroidserver's `gradlew-fdroid` knows it (it carries a checksum list).

## Every release after this one

- Keep the per-ABI APK names and the plain `assembleRelease` invocation.
- Add `apps/nodeApp/fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` for each of the four split version codes (at most 500 characters), before tagging.
- Tag the release commit itself. F-Droid's `UpdateCheckMode: Tags ^anode_` picks the tag up and reads `node.android.version` and `node.android.version.code` from `gradle.properties` at that tag.

## Bisq Connect

Bisq Connect (`network.bisq.mobile.client`) ships its `fdroid` flavor, which has no Firebase and therefore no relayed push (see [`docs/android.md`](../android.md#distribution-flavors-clientapp-only)). The first release prepared for it is `connect_1.0.0`. Submit it once Bisq Easy has been through review, so the reviewers' requests are applied to one app first.

- Recipe for fdroiddata: [`network.bisq.mobile.client.yml`](network.bisq.mobile.client.yml)
- Store listing: `apps/clientApp/fastlane/metadata/android/en-US/`, carried by the tag from 1.0.0 on, so no copy into fdroiddata is needed
- Signing certificate SHA-256: `88be0ec21c7b1627afd92dcebcd281b39900ba87695cd88870e8b3fd9bf94c22` (the Connect key, not the Bisq Easy one)

Where it differs from Bisq Easy:

- **The invocation is the per-ABI one.** Connect's release builds the fdroid flavor in its own Gradle run, `assembleFdroidRelease -Pabi=armeabi-v7a,arm64-v8a,x86,x86_64`, which writes the four splits and no universal APK (see [`docs/android.md`](../android.md#release-packaging)). The recipe and the `client` job of `.github/workflows/fdroid.yml` build the same way. The rule from Bisq Easy still holds in this form: every side builds the same set of outputs, because the first one written absorbs the manifest merger's different formatting (here armeabi-v7a).
- **No NDK and no JitPack.** Connect builds no native code of its own and embeds no bisq2 jars, so the recipe installs no NDK and `gradle/verification-metadata.xml` only covers ordinary Maven dependencies.
- **Asset names carry the flavor:** `Bisq_Connect-<version>-<abi>-<versionCode>-fdroid.apk`. They sit in the GitHub release next to the google APKs, which keep the undecorated names.
- **Tags:** `UpdateCheckMode` only accepts plain `connect_<version>` tags, so a suffixed tag (for example an `_rc1`) is never taken for a release.

### Release checklist (Connect)

1. Add `apps/clientApp/fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` for the four split version codes (`client.android.version.code * 1000 + 1..4`, at most 500 characters) before the release commit.
2. Tag `connect_<version>` on the `Release Bisq Connect <version>` commit itself, as for Bisq Easy.
3. Build the fdroid flavor from a clean checkout of the tag with the release invocation, signed, and upload the four `-fdroid` APKs to the GitHub release.
4. Check the tag's `client` job in `.github/workflows/fdroid.yml`: its unsigned APKs must match the published ones under `apksigcopier compare`.
