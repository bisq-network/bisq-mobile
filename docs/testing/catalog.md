# Test utilities catalog

Ground-truth index of shared test helpers. Do not invent utilities or paths not listed here.

> Shared presentation test bases live in `shared/test-utils/src/androidMain/.../test/presentation/` (packages `network.bisq.mobile.test.presentation.*`). Presentation-only fakes/factories stay in `shared/presentation/src/androidUnitTest/.../common/test_utils/`.

## Leaf bases

Do **not** extend `CoroutineTestBase` or `KoinIntegrationTestBase` directly.

| Base | Path | Use when |
| --- | --- | --- |
| `PresentationKoinTestBase` | `shared/test-utils/src/androidMain/kotlin/.../test/presentation/coroutines/PresentationKoinTestBase.kt` | `:shared:presentation` presenter and service tests (`androidUnitTest`) — same leaf for both |
| `PlatformPresentationKoinTestBase` | `shared/test-utils/src/androidMain/kotlin/.../test/presentation/coroutines/PlatformPresentationKoinTestBase.kt` | + static platform mocks (`getScreenWidthDp`) |
| `ClientKoinIntegrationTestBase` | `apps/clientApp/src/androidUnitTest/kotlin/.../test_utils/ClientKoinIntegrationTestBase.kt` | Client facades/services/presenters |
| `ClientInjectComposeUiTestBase` | `apps/clientApp/src/androidUnitTest/kotlin/.../test_utils/ClientInjectComposeUiTestBase.kt` | Client Compose + inject overrides (not `TestApplication`) |
| `NodeKoinIntegrationTestBase` | `apps/nodeApp/src/androidUnitTest/kotlin/.../test_utils/NodeKoinIntegrationTestBase.kt` | Node presenters/facades |
| `BisqComposeUiTestBase` | `shared/test-utils/src/androidMain/kotlin/.../test/presentation/compose/BisqComposeUiTestBase.kt` | Compose UI, no Koin |
| `PresentationKoinComposeTestBase` | `shared/test-utils/src/androidMain/kotlin/.../test/presentation/compose/PresentationKoinComposeTestBase.kt` | Compose + `presentationTestModule` (shared `StandardTestDispatcher`) |
| `PresentationInjectComposeUiTestBase` | `shared/test-utils/src/androidMain/kotlin/.../test/presentation/compose/PresentationInjectComposeUiTestBase.kt` | + `ViewModelStoreOwner` and pinned Koin graph, for `RememberPresenterLifecycleBackStackAware` screens |
| `PlatformPresentationKoinComposeTestBase` | `shared/test-utils/src/androidMain/kotlin/.../test/presentation/compose/PlatformPresentationKoinComposeTestBase.kt` | Compose + Koin + platform mocks |

## Key helpers

| Symbol | Path |
| --- | --- |
| `BisqComposeTestSupport` | `shared/test-utils/src/androidMain/kotlin/.../test/presentation/compose/BisqComposeTestSupport.kt` |
| `presentationTestModule(...)` — FQN `network.bisq.mobile.test.presentation.di.presentationTestModule` | `shared/test-utils/src/androidMain/kotlin/.../test/presentation/di/PresentationTestModule.kt` |
| `analyticsTestModule` | `shared/test-utils/src/androidMain/kotlin/.../test/koin/AnalyticsTestModule.kt` |
| `clientTestModule` | `apps/clientApp/src/androidUnitTest/kotlin/.../client/common/di/TestModule.kt` |
| `testModule` (node) | `apps/nodeApp/src/androidUnitTest/kotlin/.../node/common/di/TestModule.kt` |
| `testModule` (domain) | `shared/domain/src/commonTest/kotlin/.../data/di/TestModule.kt` |
| `TestApplication` (client) | `apps/clientApp/src/androidUnitTest/kotlin/.../test_utils/TestApplication.kt` |
| `TestApplication` (node) | `apps/nodeApp/src/androidUnitTest/kotlin/.../test_utils/TestApplication.kt` |
| `TestDoubles` | `apps/clientApp/src/androidUnitTest/kotlin/.../client/test_utils/TestDoubles.kt` |
| `MainPresenterTestFactory` | `shared/presentation/src/androidUnitTest/kotlin/.../common/test_utils/MainPresenterTestFactory.kt` |
| `FakeAppUpdateLinker` | `shared/presentation/src/androidUnitTest/kotlin/.../common/test_utils/FakeAppUpdateLinker.kt` |
| `TEST_APP_UPDATE_URL` | `shared/presentation/src/androidUnitTest/kotlin/.../common/test_utils/FakeAppUpdateLinker.kt` |
| `StateFlowProbe` | `shared/presentation/src/androidUnitTest/kotlin/.../common/test_utils/StateFlowProbe.kt` |
| `NoopNavigationManager` | `shared/test-utils/src/androidMain/kotlin/.../test/presentation/di/NoopNavigationManager.kt` |
| `PlatformStaticMocks` | `shared/test-utils/src/androidMain/kotlin/.../test/presentation/coroutines/PlatformStaticMocks.kt` |
| `FakeConfigServiceFacade` | `shared/presentation/src/androidUnitTest/kotlin/.../common/test_utils/FakeConfigServiceFacade.kt` |
| `FakeMarketPriceServiceFacade` | `shared/presentation/src/androidUnitTest/kotlin/.../common/test_utils/FakeMarketPriceServiceFacade.kt` |
| `OfferTestFactory` | `shared/presentation/src/androidUnitTest/kotlin/.../common/test_utils/OfferTestFactory.kt` |
| `authorizedAlert(...)` | `shared/presentation/src/androidUnitTest/kotlin/.../common/test_utils/MutableAlertNotificationsServiceFacade.kt` |
| `MutableAlertNotificationsServiceFacade` | `shared/presentation/src/androidUnitTest/kotlin/.../common/test_utils/MutableAlertNotificationsServiceFacade.kt` |
| `FakeTradeReadStateRepository` | `shared/presentation/src/androidUnitTest/kotlin/.../common/test_utils/FakeTradeReadStateRepository.kt` |
| `TestApplicationLifecycleService` | `shared/presentation/src/androidUnitTest/kotlin/.../common/test_utils/TestApplicationLifecycleService.kt` |
| `SettingsRepositoryMock(initial, fetchException)` | `shared/test-utils/src/commonMain/kotlin/.../test/mocks/SettingsRepositoryMock.kt` |
| `SensitiveSettingsRepositoryMock` | `apps/clientApp/src/androidUnitTest/kotlin/.../common/domain/sensitive_settings/SensitiveSettingsRepositoryMock.kt` |
| `UserRepositoryMock` | `shared/test-utils/src/commonMain/kotlin/.../test/mocks/UserRepositoryMock.kt` |
| `TestCoroutineJobsManager` | `shared/test-utils/src/commonMain/kotlin/.../test/coroutines/TestCoroutineJobsManager.kt` |
| `StandardTestDispatcherProvider` | `shared/test-utils/src/commonMain/kotlin/.../test/coroutines/StandardTestDispatcherProvider.kt` |
| `UnconfinedTestDispatcherProvider` | `shared/test-utils/src/commonMain/kotlin/.../test/coroutines/UnconfinedTestDispatcherProvider.kt` |
| `jsonDataStoreSerializerTestSupport(...)` | `shared/test-utils/src/commonMain/kotlin/.../test/datastore/JsonDataStoreSerializerTestSupport.kt` |
| `WebLinkDialogTestSupport` | `shared/presentation/src/androidUnitTest/kotlin/.../dialog/WebLinkDialogTestSupport.kt` |

`SettingsRepositoryMock` is the **only** hand-rolled `SettingsRepository` fake — do not invent another
class. Prefer it when you need mutable settings state. `mockk<SettingsRepository>()` is fine when the
test only needs stubbing/`coVerify` (see `ClientSettingsServiceFacadeTest` failure paths). Its
setters really mutate state, `initial` seeds it, `fetchException` makes `fetch()` throw, and
`mutableData` lets a non-suspend test body flip a value.

`FakeMarketPriceServiceFacade` answers both `findMarketPriceItem` and `findUSDMarketPriceItem` from a
price table. Amount-limit math needs both — a relaxed mockk returns chained mocks that fail inside
`toBaseSideMonetary`.

Two private same-named fakes still exist for the offerbook market list, keyed on a
`marketsWithPrice: Set<String>` instead of a price table: `OfferbookMarketPresenterTest` and
`ComputeOfferbookMarketListUseCaseTest`. The latter is in `commonTest`, which cannot see an
`androidUnitTest` helper at all — so prefer the shared fake, but do not assume it is the only one.

## Layer exemplars {#proof-tests}

Prefer a **same-layer sibling** that already extends the [cataloged leaf](#leaf-bases) for that path. Use an exemplar below when the nearby test is the wrong leaf (Content vs inject, presenter vs Compose) or you are unsure.

| Exemplar | Base | Path |
| --- | --- | --- |
| `FaqPresenterTest` | `PresentationKoinTestBase` | `shared/presentation/src/androidUnitTest/kotlin/.../settings/faqs/FaqPresenterTest.kt` |
| `OfferbookPresenterFilterTest` | `PlatformPresentationKoinTestBase` | `shared/presentation/src/androidUnitTest/kotlin/.../offerbook/OfferbookPresenterFilterTest.kt` |
| `ClientSettingsServiceFacadeTest` | `ClientKoinIntegrationTestBase` | `apps/clientApp/src/androidUnitTest/kotlin/.../domain/service/settings/ClientSettingsServiceFacadeTest.kt` |
| `SwitchUiTest` | `BisqComposeUiTestBase` | `shared/presentation/src/androidUnitTest/kotlin/.../atoms/SwitchUiTest.kt` |
| `LinkButtonUiTest` | `PresentationKoinComposeTestBase` | `shared/presentation/src/androidUnitTest/kotlin/.../button/LinkButtonUiTest.kt` |
| `PaymentAccountMethodIconUiTest` | `BisqComposeUiTestBase` + `@Config(TestApplication)` | `apps/clientApp/src/androidUnitTest/kotlin/.../payment_accounts_list/ui/PaymentAccountMethodIconUiTest.kt` |
| `ClientSplashScreenUiTest` | `ClientInjectComposeUiTestBase` | `apps/clientApp/src/androidUnitTest/kotlin/.../splash/ClientSplashScreenUiTest.kt` |
| `PeerProfileScreenUiTest` | `PresentationInjectComposeUiTestBase` | `shared/presentation/src/androidUnitTest/kotlin/.../peer_profile/PeerProfileScreenUiTest.kt` |

### Compose exceptions / patterns

- **`SecureScreenEffectUiTest`** (`apps/clientApp/.../security/SecureScreenEffectUiTest.kt`) must keep `createAndroidComposeRule<ComponentActivity>()` because it reads `composeTestRule.activity.window` / `FLAG_SECURE`. Do **not** force `BisqComposeUiTestBase` (bases expose only `createComposeRule()`). It also uses `@Config(application = TestApplication::class)` — Koin comes from the Application, not a leaf base. Theme via `setBisqTestContent` only when needed.
- **`ClientUserProfileServiceFacadeTest`** (`apps/clientApp/.../user_profile/ClientUserProfileServiceFacadeTest.kt`) must keep Robolectric + `@Config(TestApplication)` because `getUserProfileIcon` uses real Android bitmap decode. Do **not** extend `ClientKoinIntegrationTestBase` (would double-`startKoin` with `TestApplication`).
- **Inject-heavy client Compose screens** (`RememberPresenterLifecycleBackStackAware` + `BasePresenter` `KoinComponent.inject()`): extend [`ClientInjectComposeUiTestBase`](#leaf-bases) — plain `Application`, owned `startKoin` (`clientTestModule` + `additionalModules()`), `setInjectTestContent`. Do **not** combine `@Config(TestApplication)` with a second `startKoin`. Prefer Content/`UiState` tests on `BisqComposeUiTestBase` + `TestApplication` when inject overrides are unnecessary.
- **Back-stack-aware `:shared:presentation` screens**: extend [`PresentationInjectComposeUiTestBase`](#leaf-bases) and render through `setInjectTestContent`. It adds the two things such a screen needs and `setTestContent` does not give it — a `LocalViewModelStoreOwner` for the `viewModel { }` the lifecycle helper stores the presenter in, and a `KoinIsolatedContext`, because `org.koin.compose.getKoin()` caches its context wrapper process-wide and only re-resolves when reading it *throws*, so the first test in a class would pin the Koin instance and every later one would resolve against the graph stopped in teardown (`Scope '_root_' is closed`). `koinInject` is immune; it goes through `currentKoinScope()`. Koin still comes from the base — no `startKoin` in the test. Exemplar: `PeerProfileScreenUiTest`.
- **`ScreenAnalyticsCoverageTest`** (`shared/presentation/.../analytics/ScreenAnalyticsCoverageTest.kt`): override `AnalyticsService` via `additionalModules()` (property-initializer mock) so it wins over `analyticsTestModule` / `NoOpAnalyticsService`. Extends `PlatformPresentationKoinTestBase`. Do **not** `advanceUntilIdle()` — screen emit on `onViewAttached()` is synchronous.
- **`MainActivityDeepLinkTest`** (`shared/presentation/.../main/MainActivityDeepLinkTest.kt`): Robolectric Activity + `PresentationKoinTestBase` (bind `MainPresenter` in `additionalModules()`). Do **not** use `@Config(TestApplication)` — that would double-`startKoin` with the leaf base.
- **`AndroidShareFileServiceTest` / `NodeLogFileShareTest`**: plain `Application` + local `setMain`; no Koin leaf / `TestApplication`.
