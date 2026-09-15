# Android platform constraints

What the Android API floor rules out, and the workarounds already in place. Read this before adding
a dependency or an API call that assumes a modern JDK.

---

## Release packaging

ABI splits are enabled for release APKs ([build-logic/AppArtifactsPlugin](../build-logic/src/main/kotlin/network/bisq/gradle/AppArtifactsPlugin.kt)), and AGP cannot build split APKs and an app bundle in one invocation, so a release is two Gradle runs per app. clientApp's tasks carry its distribution flavor (see below); nodeApp has no flavors and keeps the plain names:

```bash
# Bisq Connect (clientApp). `google` is the flavor that ships to Play and GitHub.
./gradlew apps:clientApp:clean apps:clientApp:bundleGoogleRelease --info && ./gradlew apps:clientApp:assembleGoogleRelease --info

# Bisq Connect, fdroid flavor. Per-ABI only: F-Droid takes no AAB, and naming the ABIs also
# drops the universal APK. Signed like everything else, because F-Droid's reproducible-builds
# path verifies its own rebuild against these and then redistributes them.
./gradlew apps:clientApp:assembleFdroidRelease -Pabi=armeabi-v7a,arm64-v8a,x86,x86_64 --info

# Bisq Easy (nodeApp)
./gradlew apps:nodeApp:clean apps:nodeApp:bundleRelease --info && ./gradlew apps:nodeApp:assembleRelease --info
```

These fdroid APKs are signed, and they belong in the GitHub release next to the google ones:
they are the reference F-Droid compares its own rebuild against. Do not confuse them with what
`.github/workflows/fdroid.yml` uploads. That workflow passes `-PallowUnsignedRelease=true` and
publishes `fdroid-connect-unsigned`, which exists to check that the build still produces what we
expect; it is never the artifact F-Droid verifies and nothing signs it.

Publishing them is what makes the reproducible-builds path available at all. Without a reference
binary, F-Droid does what it does for most apps, which is build from source and distribute under
its own signing key. That is a different distribution arrangement rather than a failure mode, and
it is the one this setup is deliberately avoiding, since it would leave F-Droid users unable to
move between a GitHub download and an F-Droid install.

`clean` belongs only to the first run — the second reuses the compiled code and just packages the APKs. Combining `bundleRelease` and `assembleRelease` in one invocation fails at configuration time with a message repeating the two commands above.

clientApp still answers to the unqualified `assembleRelease` / `bundleRelease`, but they are aggregates that build *every* flavor, so a release run would also produce an fdroid AAB nobody wants. Name the flavor.

Outputs:

- `apps/clientApp/build/outputs/bundle/googleRelease/`, `apps/nodeApp/build/outputs/bundle/release/` — one AAB for Google Play, carrying all four ABIs (Play derives per-device splits itself).
- `apps/clientApp/build/outputs/apk/google/release/`, `apps/nodeApp/build/outputs/apk/release/` — five APKs for the GitHub release: `universal` plus one per ABI. All five are uploaded; the universal stays the sideloading default.
- `apps/clientApp/build/outputs/apk/fdroid/release/` — four APKs, one per ABI and no universal, uploaded alongside them for F-Droid to verify against.

Version codes are `base × 1000 + ABI ordinal` (universal = 0, then armeabi-v7a/arm64-v8a/x86/x86_64 = 1–4), where `base` is the app's version code from [gradle.properties](../gradle.properties). The ordinals are permanent — changing one would rewrite the version code of an already published ABI — and the scheme itself is one-way on Google Play, which only accepts increasing version codes.

Escape hatches: `-PabiSplits=false` restores the previous single-universal-APK behaviour (and lets one invocation build APK + AAB together again); `-Pabi=arm64-v8a` builds just that split, debug builds included.

---

## Distribution flavors (clientApp only)

`google` is what ships to Google Play and as the sideloadable GitHub APK. `fdroid` exists because
F-Droid's inclusion policy rejects proprietary dependencies outright rather than flagging them, and
Firebase Cloud Messaging is one. The flavors differ in exactly one thing, the push transport:

| | `google` | `fdroid` |
|---|---|---|
| Relayed push | FCM, opt-in, off by default | none |
| Background delivery | FCM plus the local foreground service | local foreground service only |
| Settings opt-in | shown | shown, disabled, with an explanation |

`fdroid` binds an `UnsupportedPushNotificationTokenProvider`, which reports
`PushNotificationTokenProvider.isSupported = false`. That travels up through
`PushNotificationServiceFacade.isRelayedPushSupported` into
`SettingsUiState.isRelayedPushSupported`, which disables the switch and swaps the tail of the
section for `mobile.pushNotifications.settings.unsupportedOnThisBuild`. Meanwhile
`ClientPushNotificationServiceFacade.activate()` short-circuits, so nothing registers with the
trusted node.

The setting is disabled rather than removed because a setting that simply is not there reads as a
bug to anyone who has seen it documented. Keep that string free of any nudge toward the Play or
GitHub build: F-Droid treats pointing users at a non-free variant as promoting non-free software.

Flavor sources use AGP's layout, not the `src/androidMain` spelling the rest of the module uses:

```
src/google/kotlin/          src/fdroid/kotlin/          src/testGoogle/kotlin/
src/google/AndroidManifest.xml
```

The KMP plugin advertises `src/androidGoogle` as the equivalent and does compile Kotlin from it, but
the variant manifest merger reads only AGP's path, so a flavor manifest under the KMP spelling is
dropped with no error, which would leave the FCM service undeclared in a build that still bundles
FCM. Keep both halves of a flavor in one directory.

Android Studio selects the first variant alphabetically, so a fresh sync lands on `fdroidDebug`.
Switch to `googleDebug` in the Build Variants panel when working on anything push-related.

`google-services.json` stays at the module root, but Google's plugin creates a task per variant
and resolves the json for every one of them, with no per-variant switch. The module's build script
therefore points the non-Google variants at an empty json list with
`missingGoogleServicesStrategy = IGNORE`, so their task runs and produces nothing. Without it an
fdroid APK built on a machine that has the json picks up `google_app_id` / `google_api_key` /
`gcm_defaultSenderId` string resources that nothing reads. Disabling the task instead does not
work: AGP merges its output folder whether or not it ran, so the previous build's resources would
still reach the APK.

Artifact names: `google` APKs keep the undecorated `Bisq_Connect-<version>-<abi>-<versionCode>.apk`
so published assets and the reproducible-build metadata pinning them are unaffected, and fdroid
APKs carry the flavor as a trailing segment:
`Bisq_Connect-<version>-<abi>-<versionCode>-fdroid.apk`. That split is
[AppArtifactsExtension.primaryFlavor](../build-logic/src/main/kotlin/network/bisq/gradle/AppArtifactsPlugin.kt).
AABs follow the same rule, `BisqConnect-<version>_<versionCode>-release.aab` for `google` and
`-release-fdroid.aab` for the other. AGP appends the flavor and build type to `archivesName`
itself, so adding a second flavor would otherwise have renamed the Play artifact; the plugin
rewrites the BUNDLE artifact's output location to undo that.

Two consequences of the flavors sharing an `applicationId` and a version code, both deliberate:

- **Play is the only install users cannot cross-grade.** We publish to F-Droid through its
  reproducible-builds path: F-Droid rebuilds the fdroid flavor, checks the result against the APK
  we published, and then distributes ours, carrying our signature. A GitHub download and an
  F-Droid install are therefore the same certificate and users can move between them. The Play
  build is the odd one out, because the artifact we upload there is an AAB and Play necessarily
  re-signs it with its own app signing key. Moving between Play and either of the others means
  uninstalling first, which loses app data.
- **The shared version codes are not a bug to fix.** Each flavor keeps the `base × 1000 + ABI
  ordinal` code, so `google` and `fdroid` builds of one release carry identical codes. Nothing
  compares them: Play only ever sees `google`, and F-Droid tracks the fdroid flavor in its own
  index. Offsetting one flavor would gain nothing and would break the mapping between a version
  code and its release.

The reproducible-builds path is also why the fdroid APKs carry `-fdroid` in their file name: the
release has to publish both flavors side by side for F-Droid to verify against, and the two would
otherwise be indistinguishable.

One constraint that path imposes on the build: **`BuildConfig.BUILD_COMMIT` is read from git**, so
F-Droid has to build from a git checkout. It does, but a source tarball would resolve the field to
`"unknown"`, changing the dex and failing verification for a reason that looks nothing like its
cause. Anything else added to `BuildConfig` has to be a function of the source alone for the same
reason; that is why the wall-clock `BUILD_TS` it replaced had to go.

---

## API floor per app

| App | `minSdk` | Core library desugaring |
|-----|----------|-------------------------|
| `:apps:clientApp` (Bisq Connect) | 24 | No |
| `:apps:nodeApp` (Bisq Easy Node) | 33 | Yes — needed by the bisq2 jars |

Values live in [gradle/libs.versions.toml](../gradle/libs.versions.toml) (`android-minSdk`,
`android-node-minSdk`).

---

## `java.time` is off limits in shared code

`java.time` only exists from API 26. clientApp runs from API 24 without desugaring, so any code path
that resolves a `java.time` class dies on API 24 and 25 devices:

```text
java.lang.NoClassDefFoundError: Failed resolution of: Ljava/time/LocalDateTime;
    at kotlinx.datetime.LocalDateTime.<clinit>(LocalDateTimeJvm.kt:103)
    at network.bisq.mobile.domain.utils.DateUtils.<clinit>(DateUtils.kt:79)
```

kotlinx-datetime maps straight onto `java.time` on Android, which is what produced the crash above.
It was removed from the project rather than papered over with desugaring. A resolution guard in
[apps/clientApp/build.gradle.kts](../apps/clientApp/build.gradle.kts) fails any clientApp Android
build in which it reappears, directly or transitively. iOS configurations are exempt: there
kotlinx-datetime compiles to native code (no `java.time`), and Compose material3's ios variant
legitimately depends on it.

Anything on the clientApp classpath is in scope, not just first-party code. Third-party libraries
that reference `java.time` are only safe when they gate those paths behind an API level check, as
`androidx.compose.material3` does with `CalendarModelImpl` vs `LegacyCalendarModelImpl`.

To audit a build:

```bash
./gradlew :apps:clientApp:assembleDebug
cd $(mktemp -d) && unzip -q <path-to>/Bisq_Connect-*-debug-universal-*.apk 'classes*.dex'
grep -al 'java/time' classes*.dex   # then disassemble with build-tools/dexdump to find the owner
```

---

## Date handling: `DateUtils`

[`DateUtils`](../shared/domain/src/commonMain/kotlin/network/bisq/mobile/domain/utils/DateUtils.kt)
is the replacement, and the only date API shared code should use.

- Common code does plain epoch-millis arithmetic — no calendar library.
- Calendar formatting is delegated to `expect` / `actual` functions in
  [`PlatformDomainAbstractions`](../shared/domain/src/commonMain/kotlin/network/bisq/mobile/data/utils/PlatformDomainAbstractions.kt):
  `SimpleDateFormat` on Android, `NSDateFormatter` on iOS.
- Time zones cross that boundary as IANA id strings (`"UTC"`, `"America/New_York"`), or null for the
  device default.
- Timestamps are clamped to years 1–9999, so a corrupt or hostile value cannot overflow the
  elapsed-millis subtraction or truncate the year count when it is narrowed to `Int`.
- Formatters are cached per thread, keyed by pattern, locale, and zone, because constructing one
  costs more than formatting with it and these run per visible row per recomposition. The iOS
  `formatDateTime` formatter is the exception: its styles follow device date/time settings that no
  cache key can observe, so it is rebuilt per call on purpose.

An unknown zone id falls back to the device zone on both platforms. Android needs help there:
`TimeZone.getTimeZone` answers GMT for an id it does not know, so the actual compares the resolved
id against the requested one and substitutes the device zone on a mismatch.

One platform divergence remains: Java renders pre-1582 dates in the Julian calendar, so the lower
clamp bound `0001-01-01` prints as `0001-01-03` on Android.

Tests: `DateUtilsCharacterizationTest` (common) pins the output of every function against the values
the previous kotlinx-datetime implementation produced, so a future swap can be verified against it.
`DateUtilsFormatAndroidTest` and `DateUtilsFormatIosTest` cover the locale- and zone-dependent
formatting per platform.

---

## Adding a dependency that handles dates

1. Check whether it reaches for `java.time` — inspect the artifact:
   `unzip -p <artifact>.jar '*.class' | grep -ao 'java/time/[A-Za-z]*' | sort -u`
2. If it does, either keep it out of the clientApp classpath, or enable core library desugaring in
   [apps/clientApp/build.gradle.kts](../apps/clientApp/build.gradle.kts) — deliberately left off
   today, so treat turning it on as a decision, not a formality.
3. Re-run the dex audit above before shipping.
