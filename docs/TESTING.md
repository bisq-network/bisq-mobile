# Testing Guide

Agents: [AGENTS.md](../AGENTS.md) → [docs/testing/README.md](testing/README.md). Skeletons: [recipes.md](testing/recipes.md). Paths: [catalog.md](testing/catalog.md).

## Rules

- Mirror production package paths in test source sets.
- Extend a leaf base — do not copy inline `startKoin` / `Dispatchers.setMain` from unmigrated siblings (~80% of existing tests are legacy).
- Grep [catalog.md](testing/catalog.md) and `*TestFactory.kt` / `*TestSupport.kt` before creating mocks.
- Assert behavior (state, side effects, visible UI) — not implementation details.
- Read production control flow before writing assertions — do not invent branch behavior from comments or names (`IS_DEBUG`, “dev mode”, etc.).
- `BuildConfig` / other `const` gates are compile-time: one branch is dead on the classpath. Assert the live path (read the const and adapt), or inject a seam if both branches must be covered in one run. Never hard-code “in debug builds” expectations that ignore the compiled value.
- Run module-scoped Gradle commands; never claim green without output.

## Source sets

| World | Source sets | Framework |
| --- | --- | --- |
| KMP / JVM | `commonTest`, `iosTest` | `kotlin.test`; inline `setMain` + `testModule` for domain ServiceFacades |
| Android unit | `androidUnitTest` | JUnit 4, MockK, Robolectric, Compose UI Test, Koin test |

Android-only APIs cannot go in `commonTest`. iOS: `iosSimulatorArm64Test` (macOS + Xcode only).

## Library allowlist {#library-allowlist}

**Use:** `kotlin.test`, `kotlin-test-junit`, `junit`, `mockk`, `kotlinx-coroutines-test`, `koin-test`, `robolectric`, `androidx-test-compose-junit4`, `androidx-test-junit`

**NEVER:** Mockito, Turbine, Kotest, AssertJ, Truth, Hamcrest, XCTest, Espresso-first UI. Use MockK, `runTest` + `advanceUntilIdle` + `StateFlowProbe`, and Compose UI Test instead.

## Decision tree {#decision-tree}

```
Changed file layer?
├── Pure Kotlin → commonTest (*Test.kt); ServiceFacades: inline setMain + testModule → recipes.md#domain
├── Presenter (:shared:presentation) → *PresenterTest.kt, PresentationKoinTestBase / PlatformPresentationKoinTestBase → recipes.md#presenter
├── Composable (:shared:presentation) → *UiTest.kt, BisqComposeUiTestBase or PresentationKoinComposeTestBase → recipes.md#compose
├── Client facade/service → ClientKoinIntegrationTestBase → recipes.md#client
├── Client Compose + TestApplication → BisqComposeUiTestBase + @Config(TestApplication); no startKoin → recipes.md#compose
├── iOS platform bridge → iosTest → recipes.md#ios
└── DI modules / @Preview / presentation/design/* → DO NOT test
```

Do not extend `CoroutineTestBase` or `KoinIntegrationTestBase` directly.

**Proof tests (copy these, not legacy siblings):** `FaqPresenterTest`, `OfferbookPresenterFilterTest`, `ClientSettingsServiceFacadeTest`, `SwitchUiTest`, `LinkButtonUiTest`, `PaymentAccountMethodIconUiTest` — paths in [catalog.md](testing/catalog.md).

## File placement

```
Production: <module>/src/<sourceSet>/kotlin/<package>/<Name>.kt
Test:       <module>/src/<testSourceSet>/kotlin/<same package>/<Name><Suffix>.kt
```

| Production | Source set | Suffix |
| --- | --- | --- |
| `shared/domain/.../commonMain` | `commonTest` | `Test` |
| `shared/presentation/...` presenter | `androidUnitTest` | `PresenterTest` |
| `shared/presentation/...` composable | `androidUnitTest` | `UiTest` |
| `apps/clientApp/...` | `commonTest` or `androidUnitTest` | per layer |
| iOS platform | `iosTest` | `Test` |

## Koin rules

| Scenario | Rule |
| --- | --- |
| `@Config(application = TestApplication::class)` | Pair with `BisqComposeUiTestBase` (or plain UI without a Koin-starting base). Koin from `TestApplication.onCreate()` — **no** `startKoin` in `@Before` |
| Client facade test | `ClientKoinIntegrationTestBase` — **no** `TestApplication` |
| Presentation test | `PresentationKoinTestBase` + `presentationTestModule` — **not** `clientTestModule` |

Never combine `TestApplication` with a Koin-starting base. Load exactly one module owning `CoroutineJobsManager`, `GlobalUiManager`, and `NavigationManager`.

## Commands {#commands}

```bash
./gradlew :shared:presentation:testDebugUnitTest --tests "network.bisq.mobile.presentation.settings.faqs.FaqPresenterTest"
./gradlew :shared:presentation:testDebugUnitTest --tests "network.bisq.mobile.presentation.offerbook.OfferbookPresenterFilterTest"
./gradlew :apps:clientApp:testDebugUnitTest --tests "network.bisq.mobile.client.common.domain.service.settings.ClientSettingsServiceFacadeTest"
./gradlew clean test -x :apps:nodeApp:test --no-configuration-cache
./gradlew testDebugUnitTest koverVerify
./scripts/coverage.sh
./gradlew :shared:domain:iosSimulatorArm64Test   # macOS only
```

## Coverage

| Gate | Threshold |
| --- | --- |
| Overall | **55%** (`kover.coverage.minimum`) |
| PR diff | **80%** (`kover.diff.coverage.minimum`, `scripts/coverage.sh`, CI) |

**Kover exclusions:** `@Preview`, `*ComposableSingletons*`, generated i18n bundles, `presentation/design/*`, DI modules (`client.common.di.*`, `node.common.di.*`, `data.di.*`), `DefaultSentryClient*`. Do not chase coverage on these.

## PR checklist

- [ ] Test mirrors production package path
- [ ] Correct leaf base — not legacy inline Koin/dispatcher setup
- [ ] No double `startKoin` with `TestApplication`
- [ ] Reused catalog helpers; updated [catalog.md](testing/catalog.md) if adding shared utilities
- [ ] Proof-test patterns, not legacy siblings
- [ ] Gradle run attached or explicitly not run
- [ ] Diff coverage ≥ 80% on new production code
- [ ] No tests for Kover-excluded code
