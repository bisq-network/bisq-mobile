# Android platform constraints

What the Android API floor rules out, and the workarounds already in place. Read this before adding
a dependency or an API call that assumes a modern JDK.

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
cd $(mktemp -d) && unzip -q <path-to>/Bisq_Connect-*-debug.apk 'classes*.dex'
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
