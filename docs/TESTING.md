# Testing Guide

Agents: [AGENTS.md](../AGENTS.md) → [docs/testing/README.md](testing/README.md). Skeletons: [recipes.md](testing/recipes.md). Paths: [catalog.md](testing/catalog.md). Rationale: [TESTING_FIXTURES.md](TESTING_FIXTURES.md).

## Rules

- Mirror production package paths in test source sets.
- When the [decision tree](#decision-tree) / [catalog](testing/catalog.md) lists a leaf base for that layer, extend it — do not invent inline `startKoin` / `Dispatchers.setMain`. Layers with no cataloged leaf base (domain `commonTest`, domain `androidUnitTest` Robolectric; domain ServiceFacades use inline `setMain` + `testModule` per [recipes.md](testing/recipes.md#domain)) need no leaf base — do not invent a base.
- Grep [catalog.md](testing/catalog.md) and `*TestFactory.kt` / `*TestSupport.kt` before creating mocks.
- Assert behavior (state, side effects, visible UI) — not implementation details.
- Read production control flow before writing assertions — do not invent branch behavior from comments or names (`IS_DEBUG`, “dev mode”, etc.).
- `BuildConfig` / other `const` gates are compile-time: one branch is dead on the classpath. Assert the live path (read the const and adapt), or inject a seam if both branches must be covered in one run. Never hard-code “in debug builds” expectations that ignore the compiled value.
- Do not put commas in test function names (including backtick names like `` `when X, then Y` ``). They are compile-time errors.
- Run module-scoped Gradle commands; never claim green without output.

## Source sets

| World | Source sets | Framework |
| --- | --- | --- |
| KMP / JVM | `commonTest`, `iosTest` | `kotlin.test`; inline `setMain` + `testModule` for domain ServiceFacades |
| Android unit | `androidUnitTest` | JUnit 4, MockK, Robolectric, Compose UI Test, Koin test |

Android-only APIs cannot go in `commonTest`. iOS: `iosSimulatorArm64Test` (macOS + Xcode only).

## Library allowlist {#library-allowlist}

**Use:** `kotlin.test`, `kotlin-test-junit`, `junit`, `mockk`, `kotlinx-coroutines-test`, `koin-test`, `robolectric`, `androidx-test-compose-junit4`, `androidx-test-junit`

**NEVER:** Mockito, Turbine, Kotest, AssertJ, Truth, Hamcrest, XCTest, Espresso-first UI (`Espresso.pressBack` OK). Use MockK, `runTest` + `advanceUntilIdle` + `StateFlowProbe`, and Compose UI Test instead.

## Decision tree {#decision-tree}

```text
Changed file layer?
├── Pure Kotlin → commonTest (*Test.kt); ServiceFacades: inline setMain + testModule → recipes.md#domain
├── Domain Android-only API (androidMain) → androidUnitTest + @RunWith(RobolectricTestRunner) — no leaf base
├── Presenter (:shared:presentation) → *PresenterTest.kt, PresentationKoinTestBase / PlatformPresentationKoinTestBase → recipes.md#presenter
├── Composable (:shared:presentation) → *UiTest.kt, BisqComposeUiTestBase or PresentationKoinComposeTestBase → recipes.md#compose
├── Screen (:shared:presentation) resolved via RememberPresenterLifecycleBackStackAware → PresentationInjectComposeUiTestBase → recipes.md#compose
├── Client/Node app presenter → ClientKoinIntegrationTestBase / NodeKoinIntegrationTestBase → recipes.md#client
├── Client/Node facade/service → ClientKoinIntegrationTestBase / NodeKoinIntegrationTestBase → recipes.md#client
├── Client/Node Compose + TestApplication → BisqComposeUiTestBase + @Config(TestApplication); no startKoin → recipes.md#compose
├── Client Compose + inject overrides → ClientInjectComposeUiTestBase (not TestApplication) → recipes.md#compose
├── iOS platform bridge → iosTest → recipes.md#ios
└── DI modules / @Preview / presentation/design/* → DO NOT test
```

Do not extend `CoroutineTestBase` or `KoinIntegrationTestBase` directly.

**Same-layer sibling first** (must already extend the cataloged leaf). If unsure or the nearby test is the wrong leaf, use a [layer exemplar](testing/catalog.md#proof-tests): `FaqPresenterTest`, `OfferbookPresenterFilterTest`, `ClientSettingsServiceFacadeTest`, `SwitchUiTest`, `LinkButtonUiTest`, `PaymentAccountMethodIconUiTest`, `ClientSplashScreenUiTest`, `PeerProfileScreenUiTest`.

## File placement

```text
Production: <module>/src/<sourceSet>/kotlin/<package>/<Name>.kt
Test:       <module>/src/<testSourceSet>/kotlin/<same package>/<Name><Suffix>.kt
```

| Production | Source set | Suffix |
| --- | --- | --- |
| `shared/domain/.../commonMain` | `commonTest` | `Test` |
| `shared/presentation/...` presenter | `androidUnitTest` | `PresenterTest` |
| `shared/presentation/...` composable | `androidUnitTest` | `UiTest` |
| `apps/clientApp/...` | `commonTest` or `androidUnitTest` | per layer |
| `apps/nodeApp/...` | `androidUnitTest` | per layer |
| `shared/presentation/...` `commonTest` | `commonTest` | `Test` |
| Android instrumented (Keystore) | `androidInstrumentedTest` | `Test` |
| iOS platform | `iosTest` | `Test` |

## Koin rules

| Scenario | Rule |
| --- | --- |
| `@Config(application = TestApplication::class)` | Pair with `BisqComposeUiTestBase` (or plain UI without a Koin-starting base). Koin from `TestApplication.onCreate()` — **no** `startKoin` in `@Before`. Same for client and node `TestApplication`. |
| Client Compose inject overrides | `ClientInjectComposeUiTestBase` — plain `Application`, owned `startKoin`; **no** `TestApplication` |
| Back-stack-aware presentation screen | `PresentationInjectComposeUiTestBase` + `setInjectTestContent` — Koin from the base; **no** `startKoin` in the test |
| Client facade test | `ClientKoinIntegrationTestBase` — **no** `TestApplication` (except `ClientUserProfileServiceFacadeTest` — catalog) |
| Presentation test | `PresentationKoinTestBase` + `presentationTestModule` — **not** `clientTestModule` |
| Presenter that attaches a view | Needs `AnalyticsService` bound. The cataloged bases and modules already bind `NoOpAnalyticsService`; a hand-rolled `module { }` must add it or `onViewAttached()` throws `NoDefinitionFoundException` |

Never combine `TestApplication` with a Koin-starting base. Load exactly one module owning `CoroutineJobsManager`, `GlobalUiManager`, and `NavigationManager`.

`BasePresenter` resolves `AnalyticsService` as a non-null dependency, so presenters that opt into
screen tracking need the binding present when `onViewAttached()` runs. To assert on tracking, load
your mock *after* the shared module — later definitions win.

## Commands {#commands}

Module-scoped (prefer these):

```bash
./gradlew :shared:presentation:testDebugUnitTest --tests "network.bisq.mobile.presentation.settings.faqs.FaqPresenterTest"
./gradlew :shared:presentation:testDebugUnitTest --tests "network.bisq.mobile.presentation.offerbook.OfferbookPresenterFilterTest"
./gradlew :apps:clientApp:testDebugUnitTest --tests "network.bisq.mobile.client.common.domain.service.settings.ClientSettingsServiceFacadeTest"
./gradlew :shared:domain:iosSimulatorArm64Test   # macOS only
```

Repository-wide (full suite / coverage — not for day-to-day iteration):

```bash
./gradlew clean test -x :apps:nodeApp:test --no-configuration-cache
./gradlew testDebugUnitTest koverVerify
./scripts/coverage.sh
```

## Coverage

| Gate | Threshold |
| --- | --- |
| Overall | **55%** (`kover.coverage.minimum`) |
| PR diff | **80%** (`kover.diff.coverage.minimum`, `scripts/coverage.sh`, CI) |

**Kover exclusions:** `@Preview`, `ExcludeFromCoverage`, `*ComposableSingletons*`, generated i18n bundles, `presentation/design/*`, DI modules (`client.common.di.*`, `node.common.di.*`, `data.di.*`), `DefaultSentryClient*`, `NoOpAnalyticsService`, `KeystoreKeyWrapper`, `LegacyMasterKey`. Do not chase coverage on these.

## PR checklist

- [ ] Test mirrors production package path
- [ ] Correct leaf base (or domain inline recipe when no leaf exists); `beforeStartKoin`/`onTearDown` overrides call `super`
- [ ] No double `startKoin` with `TestApplication`
- [ ] Reused catalog helpers; updated [catalog.md](testing/catalog.md) if adding shared utilities
- [ ] Same-layer sibling on the cataloged leaf (or a [layer exemplar](testing/catalog.md#proof-tests) if unsure); no ad-hoc harness
- [ ] Gradle run attached or explicitly not run
- [ ] Diff coverage ≥ 80% on new production code
- [ ] No tests for Kover-excluded code
