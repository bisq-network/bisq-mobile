# Test Fixtures

See [TESTING.md](TESTING.md) for which base class to use. This doc explains why shared presentation test helpers are structured the way they are.

## Problem fixtures solve

Android unit tests in one module often need reusable helpers that depend on that module's production code — Koin test modules, Compose wrappers, no-op navigation, presenter bases.

Without a proper sharing mechanism, you either:

- Duplicate helpers in every consumer (`clientApp`, `presentation`, …), or
- Extract a separate test module that depends on production code while production tests depend on it → circular module dependency

Gradle can sometimes compile circular test deps; Android Studio often cannot index them, causing widespread unresolved-reference errors.

Test fixtures (AGP `android { testFixtures { enable = true } }`) are the intended fix: publish test utilities *from* a library *to* downstream test classpaths, one-way:

```text
presentation (main) ← testFixtures
clientApp androidUnitTest → testFixtures(presentation)
```

Gradle's `java-test-fixtures` plugin is not for Kotlin Multiplatform modules. Use AGP test fixtures on the Android target of a KMP library.

## What we tried

### 1. `:shared:presentation-test-utils` module

A dedicated module held `PresentationKoinTestBase`, `BisqComposeUiTestBase`, `presentationTestModule`, etc.

```text
presentation androidUnitTest → presentation-test-utils → presentation main
```

This reintroduced the cycle above and broke IDE resolution.

### 2. AGP test fixtures on `:shared:presentation`

We moved helpers to `src/testFixtures/kotlin/` and enabled `testFixtures { enable = true }`.

Issues hit (KGP + AGP 8.13, Kotlin 2.4):

| Issue | Effect |
| --- | --- |
| KGP does not compile Kotlin into the testFixtures AAR | `classes.jar` was empty (~22 bytes); `testFixtures(project(...))` from `clientApp` resolved to nothing |
| `kotlin.srcDir("src/testFixtures/kotlin")` bridge | Gradle compiled fine; Android Studio did not index `presentation.test.*` or `kotlin.test.*` reliably |
| Fixtures are Android/JVM-only | Correct for presenter/Compose helpers; `commonTest` / `iosTest` cannot consume them |

Cross-platform bases (`CoroutineTestBase`, dispatcher providers, repository mocks) live in `:shared:test-utils` `commonMain`; Android-only bases (`KoinIntegrationTestBase`, presenter/UI/Compose bases) live in its `androidMain` — see [Current approach](#current-approach).

### 3. Re-check with `enableTestFixturesKotlinSupport` (Jul 2026)

Retried on the same stack (AGP 8.13.2, Kotlin 2.4.0, KMP `android.library` on `:shared:presentation`) with:

```properties
android.experimental.enableTestFixturesKotlinSupport=true
```

and `android { testFixtures { enable = true } }`.

Results:

| Probe | Outcome |
| --- | --- |
| Kotlin under `src/testFixtures/kotlin` | **Still ignored.** No `compileDebugTestFixturesKotlin` task is registered. AAR `classes.jar` stays ~22 bytes. |
| Java under `src/testFixtures/java` | **Works.** `compileDebugTestFixturesJavaWithJavac` runs; class lands in `classes.jar`. |

So AGP's experimental Kotlin fixtures support applies to pure `kotlin-android` modules. It does **not** wire a Kotlin compilation for the testFixtures variant of a **KMP** `androidTarget`. Java-only fixtures are not useful here — all shared helpers are Kotlin.

Do not re-enable `testFixtures` on `:shared:presentation` until KGP registers `compile*TestFixturesKotlin` for KMP Android libraries (or AGP built-in Kotlin / a later KGP release documents that path).

### 4. `kotlin.srcDirs` graft from clientApp (removed Jul 2026)

`clientApp` grafted presentation's `test_utils/{compose,coroutines,di}` directories into its own
`androidUnitTest` source set via `kotlin.srcDirs`. Gradle compiled fine (each module compiles its
own copy), but the IDE broke: **Android Studio assigns each directory to exactly one module**, and
the most specific content root wins. The grafted subdirectories were claimed by
`clientApp.unitTest`, so presentation's own tests could no longer resolve
`PresentationKoinTestBase` and friends — "Unresolved reference `test_utils`" on every presenter
test, on every machine, surviving cache invalidation (plus duplicated-PSI `different providers`
exceptions in `idea.log`). Diagnosed 2026-07-29; do not reintroduce source-dir grafts across
modules.

## Current approach

Android presentation test bases live in `:shared:test-utils` `androidMain`:

```text
shared/test-utils/src/androidMain/kotlin/.../test/presentation/
  compose/     BisqComposeUiTestBase, PresentationKoinComposeTestBase, …
  coroutines/  PresentationKoinTestBase, PlatformPresentationKoinTestBase
  di/          presentationTestModule(...), NoopNavigationManager
```

Packages: `network.bisq.mobile.test.presentation.*`.

`:shared:test-utils` `androidMain` has `api(project(":shared:presentation"))` — a one-way
dependency on presentation's production code. Presentation's `androidUnitTest` depends back on
`:shared:test-utils`'s *main* compilation; at source-set granularity this is acyclic
(`test-utils androidMain → presentation main`; `presentation androidUnitTest → test-utils
androidMain`), and both Gradle and the IDE handle it. This differs from the removed
`:shared:presentation-test-utils` era (attempt 1) mainly in that helpers live in the existing
shared test module and consumers resolve them as a normal module dependency instead of shared
source directories.

Presentation-only fakes/factories (`FakeConfigServiceFacade`, `OfferTestFactory`, `StateFlowProbe`,
…) stay in `shared/presentation/src/androidUnitTest/.../common/test_utils/` — they are not needed
outside that module.

Explicit `implementation(libs.kotlin.test)` on `androidUnitTest` avoids IDE gaps when only `kotlin-test-junit` is declared.

## When to revisit fixtures

Try AGP test fixtures again when **KMP** publishes Kotlin classes into the testFixtures AAR (look for a real `compileDebugTestFixturesKotlin` task on `:shared:presentation`, not only the experimental AGP flag). Migration would be:

1. Move `test-utils/src/androidMain/.../test/presentation/{compose,coroutines,di}` → presentation's `src/testFixtures/kotlin/`
2. `testFixtures { enable = true }` on `:shared:presentation`
3. `android.experimental.enableTestFixturesKotlinSupport=true` in `gradle.properties` (until non-experimental)
4. Consumers: `implementation(testFixtures(project(":shared:presentation")))`; drop `api(project(":shared:presentation"))` from `:shared:test-utils` `androidMain`
5. Smoke-check: `assembleDebugTestFixtures` → `classes.jar` contains helper `.class` files; presentation + clientApp unit tests still resolve bases in the IDE

## References

- [Gradle: Using test fixtures](https://docs.gradle.org/current/userguide/java_testing.html#sec:java_test_fixtures) — core model (`testFixtures(project(...))`)
- [AGP 7.2 release notes: Support for test fixtures](https://developer.android.com/build/releases/agp-7-2-0-release-notes) — `android { testFixtures { enable = true } }`
- Kotlin in Android test fixtures remains experimental; see AGP source / `android.experimental.enableTestFixturesKotlinSupport` (works for `kotlin-android`, not verified for KMP `androidTarget`)
- Related: [KT-63142](https://youtrack.jetbrains.com/issue/KT-63142) — Gradle test fixtures beyond plain JVM (broader KMP fixtures gap)
