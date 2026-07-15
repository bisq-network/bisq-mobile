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

Cross-platform bases (`CoroutineTestBase`, `KoinIntegrationTestBase`) stay in `:shared:test-utils` `commonMain`. Android-only presenter/UI/Koin bases belong with presentation tests.

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

## Current approach

Helpers live in presentation's `androidUnitTest`:

```text
shared/presentation/src/androidUnitTest/kotlin/.../common/test_utils/
  compose/     BisqComposeUiTestBase, PresentationKoinComposeTestBase, …
  coroutines/  PresentationKoinTestBase, PlatformPresentationKoinTestBase
  di/          presentationTestModule(...)
```

`:shared:presentation-test-utils` was removed.

`clientApp` reuses helpers via `kotlin.srcDirs` grafts in `apps/clientApp/build.gradle.kts` — not a module dependency, so no cycle. Point only at `compose/`, `coroutines/`, and `di/` (not the `test_utils` root). Do **not** use `kotlin.include` on that source set: it replaces the default `**/*` and drops clientApp's own `androidUnitTest` sources.

```text
presentation main ← clientApp (implementation)
presentation test_utils/{compose,coroutines,di} ← clientApp androidUnitTest (srcDirs graft)
```

Explicit `implementation(libs.kotlin.test)` on `androidUnitTest` avoids IDE gaps when only `kotlin-test-junit` is declared.

## When to revisit fixtures

Try AGP test fixtures again when **KMP** publishes Kotlin classes into the testFixtures AAR (look for a real `compileDebugTestFixturesKotlin` task on `:shared:presentation`, not only the experimental AGP flag). Migration would be:

1. Move `common/test_utils/{compose,coroutines,di}` → `src/testFixtures/kotlin/`
2. `testFixtures { enable = true }` on `:shared:presentation`
3. `android.experimental.enableTestFixturesKotlinSupport=true` in `gradle.properties` (until non-experimental)
4. `clientApp`: `implementation(testFixtures(project(":shared:presentation")))`
5. Remove `kotlin.srcDirs` graft
6. Smoke-check: `assembleDebugTestFixtures` → `classes.jar` contains helper `.class` files; presentation + clientApp unit tests still resolve bases in the IDE

## References

- [Gradle: Using test fixtures](https://docs.gradle.org/current/userguide/java_testing.html#sec:java_test_fixtures) — core model (`testFixtures(project(...))`)
- [AGP 7.2 release notes: Support for test fixtures](https://developer.android.com/build/releases/agp-7-2-0-release-notes) — `android { testFixtures { enable = true } }`
- Kotlin in Android test fixtures remains experimental; see AGP source / `android.experimental.enableTestFixturesKotlinSupport` (works for `kotlin-android`, not verified for KMP `androidTarget`)
- Related: [KT-63142](https://youtrack.jetbrains.com/issue/KT-63142) — Gradle test fixtures beyond plain JVM (broader KMP fixtures gap)
