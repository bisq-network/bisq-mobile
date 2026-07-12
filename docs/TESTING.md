# Testing Infrastructure

Extend a **leaf base** for your layer. Do not extend `CoroutineTestBase` or `KoinIntegrationTestBase` directly.

Compose UI bases — next commit.

## Bases

```
CoroutineTestBase (:shared:test-utils)
└── KoinIntegrationTestBase
    ├── ClientKoinIntegrationTestBase → clientTestModule
    └── PresentationKoinTestBase → presentationTestModule(...)
        └── PlatformPresentationKoinTestBase (+ getScreenWidthDp mock)
```

| Layer | Base | Example |
| --- | --- | --- |
| Client facade / service / presenter | `ClientKoinIntegrationTestBase` | `ClientSettingsServiceFacadeTest` |
| Presentation presenter | `PresentationKoinTestBase` | `FaqPresenterTest` |
| Needs screen width mock | `PlatformPresentationKoinTestBase` | `OfferbookPresenterFilterTest` |

## Rules

- Use inherited `runTest { }` in test bodies
- `additionalModules()` — extra Koin bindings; `beforeStartKoin()` — mocks needed before modules load
- Do not combine `TestApplication` with a Koin-starting base
- Legacy Compose tests use `common/di/PresentationTestModule.kt` until migrated

## Run

```bash
./gradlew :shared:presentation:testDebugUnitTest --tests "network.bisq.mobile.presentation.settings.faqs.FaqPresenterTest"
./gradlew :apps:clientApp:testDebugUnitTest --tests "network.bisq.mobile.client.common.domain.service.settings.ClientSettingsServiceFacadeTest"
```
