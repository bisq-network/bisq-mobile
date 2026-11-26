# Bisq Easy on F-Droid

Bisq Easy (`network.bisq.mobile.node`) ships on F-Droid through the reproducible-builds path: F-Droid builds each release from its tag, and publishes our signed APKs from the GitHub release when its own build matches them byte for byte. F-Droid users and GitHub users therefore get the same signature and can move between the two installs.

- Recipe for fdroiddata: [`network.bisq.mobile.node.yml`](network.bisq.mobile.node.yml) (goes to `metadata/network.bisq.mobile.node.yml` in fdroiddata)
- Store listing: `apps/nodeApp/fastlane/metadata/android/en-US/` (F-Droid reads it from the build's `subdir`)
- Build rules for a reproducible release: [`docs/android.md`](../android.md#building-what-the-tag-says)
- CI check: `.github/workflows/fdroid.yml` builds every `anode_*` tag unsigned on Linux, the same way as the release

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
4. Fork fdroiddata, add the recipe on a branch named `network.bisq.mobile.node`, and run locally: `fdroid readmeta`, `fdroid rewritemeta network.bisq.mobile.node`, `fdroid lint network.bisq.mobile.node`, `fdroid checkupdates --allow-dirty network.bisq.mobile.node`, `fdroid build -v -l network.bisq.mobile.node`. The merge request needs a green pipeline and at least one working build.
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
