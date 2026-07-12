# Testing Infrastructure

Extend a leaf base for your layer. Do not extend `CoroutineTestBase` or `KoinIntegrationTestBase` directly.

## Bases

```
CoroutineTestBase (:shared:test-utils)
└── KoinIntegrationTestBase
    ├── ClientKoinIntegrationTestBase → clientTestModule
    └── PresentationKoinTestBase → presentationTestModule(...)
        ├── PlatformPresentationKoinTestBase (+ getScreenWidthDp mock)
        └── PresentationKoinComposeTestBase (UnconfinedTestDispatcher)
            └── PlatformPresentationKoinComposeTestBase (+ getScreenWidthDp mock)

BisqComposeUiTestBase — Compose UI, no Koin
```

| Layer | Base | Example |
| --- | --- | --- |
| Client facade / service / presenter | `ClientKoinIntegrationTestBase` | `ClientSettingsServiceFacadeTest` |
| Presentation presenter | `PresentationKoinTestBase` | `FaqPresenterTest` |
| Needs screen width mock | `PlatformPresentationKoinTestBase` | `OfferbookPresenterFilterTest` |
| Compose UI, no Koin | `BisqComposeUiTestBase` | `SwitchUiTest` |
| Compose UI + Koin | `PresentationKoinComposeTestBase` | `LinkButtonUiTest` |
| Compose + Koin + platform mocks | `PlatformPresentationKoinComposeTestBase` | — (deferred) |
| Client Compose + `TestApplication` | Robolectric `@Config(application = TestApplication::class)` | `PaymentAccountMethodIconUiTest` |

## Rules

- Use inherited `runTest { }` in presenter/client test bodies
- Compose Koin bases: pump UI with `composeTestRule.waitForIdle()`, not `advanceUntilIdle`
- `setTestContent { }` wraps `BisqTheme` + `LocalIsTest`; `restartKoinWith(...)` for mid-test module swaps
- `additionalModules()` — extra Koin bindings; `beforeStartKoin()` — mocks needed before modules load
- Do not combine `TestApplication` with a Koin-starting base
- Legacy Compose tests still use `common/di/PresentationTestModule.kt` until migrated

## Run

```bash
./gradlew :shared:presentation:testDebugUnitTest --tests "network.bisq.mobile.presentation.settings.faqs.FaqPresenterTest"
./gradlew :shared:presentation:testDebugUnitTest --tests "network.bisq.mobile.presentation.common.ui.components.atoms.button.LinkButtonUiTest"
./gradlew :apps:clientApp:testDebugUnitTest --tests "network.bisq.mobile.client.common.domain.service.settings.ClientSettingsServiceFacadeTest"
```
