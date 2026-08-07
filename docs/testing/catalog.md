# Test utilities catalog

Ground-truth index of shared test helpers. Do not invent utilities or paths not listed here.

> Shared presentation test bases live in `shared/test-utils/src/androidMain/.../test/presentation/` (packages `network.bisq.mobile.test.presentation.*`). Presentation-only fakes/factories stay in `shared/presentation/src/androidUnitTest/.../common/test_utils/`.

## Leaf bases

Do **not** extend `CoroutineTestBase` or `KoinIntegrationTestBase` directly.

| Base | Path | Use when |
| --- | --- | --- |
| `PresentationKoinTestBase` | `shared/test-utils/src/androidMain/kotlin/.../test/presentation/coroutines/PresentationKoinTestBase.kt` | `:shared:presentation` presenter tests |
| `PlatformPresentationKoinTestBase` | `shared/test-utils/src/androidMain/kotlin/.../test/presentation/coroutines/PlatformPresentationKoinTestBase.kt` | + static platform mocks (`getScreenWidthDp`) |
| `ClientKoinIntegrationTestBase` | `apps/clientApp/src/androidUnitTest/kotlin/.../test_utils/ClientKoinIntegrationTestBase.kt` | Client facades/services |
| `NodeKoinIntegrationTestBase` | `apps/nodeApp/src/androidUnitTest/kotlin/.../test_utils/NodeKoinIntegrationTestBase.kt` | Node presenters/facades |
| `BisqComposeUiTestBase` | `shared/test-utils/src/androidMain/kotlin/.../test/presentation/compose/BisqComposeUiTestBase.kt` | Compose UI, no Koin |
| `PresentationKoinComposeTestBase` | `shared/test-utils/src/androidMain/kotlin/.../test/presentation/compose/PresentationKoinComposeTestBase.kt` | Compose + `presentationTestModule` (shared `StandardTestDispatcher`) |
| `PlatformPresentationKoinComposeTestBase` | `shared/test-utils/src/androidMain/kotlin/.../test/presentation/compose/PlatformPresentationKoinComposeTestBase.kt` | Compose + Koin + platform mocks |

## Key helpers

| Symbol | Path |
| --- | --- |
| `BisqComposeTestSupport` | `shared/test-utils/src/androidMain/kotlin/.../test/presentation/compose/BisqComposeTestSupport.kt` |
| `presentationTestModule(...)` | `shared/test-utils/src/androidMain/kotlin/.../test/presentation/di/PresentationTestModule.kt` |
| `analyticsTestModule` | `shared/test-utils/src/androidMain/kotlin/.../test/koin/AnalyticsTestModule.kt` |
| `clientTestModule` | `apps/clientApp/src/androidUnitTest/kotlin/.../client/common/di/TestModule.kt` |
| `commonTestModule` | `apps/clientApp/src/commonTest/kotlin/.../client/common/di/CommonTestModule.kt` |
| `testModule` | `shared/domain/src/commonTest/kotlin/.../data/di/TestModule.kt` |
| `TestApplication` | `apps/clientApp/src/androidUnitTest/kotlin/.../test_utils/TestApplication.kt` |
| `TestDoubles` | `apps/clientApp/src/androidUnitTest/kotlin/.../client/test_utils/TestDoubles.kt` |
| `MainPresenterTestFactory` | `.../test_utils/MainPresenterTestFactory.kt` |
| `FakeAppUpdateLinker` | `.../test_utils/FakeAppUpdateLinker.kt` |
| `TEST_APP_UPDATE_URL` | `.../test_utils/FakeAppUpdateLinker.kt` |
| `StateFlowProbe` | `.../test_utils/StateFlowProbe.kt` |
| `NoopNavigationManager` | `shared/test-utils/src/androidMain/kotlin/.../test/presentation/di/NoopNavigationManager.kt` |
| `PlatformStaticMocks` | `shared/test-utils/src/androidMain/kotlin/.../test/presentation/coroutines/PlatformStaticMocks.kt` |
| `FakeConfigServiceFacade` | `shared/presentation/src/androidUnitTest/kotlin/.../test_utils/FakeConfigServiceFacade.kt` |
| `FakeMarketPriceServiceFacade` | `shared/presentation/src/androidUnitTest/kotlin/.../test_utils/FakeMarketPriceServiceFacade.kt` |
| `OfferTestFactory` | `shared/presentation/src/androidUnitTest/kotlin/.../test_utils/OfferTestFactory.kt` |
| `SettingsRepositoryMock(initial, fetchException)` | `shared/test-utils/src/commonMain/kotlin/.../mocks/SettingsRepositoryMock.kt` |
| `UserRepositoryMock` | `shared/test-utils/src/commonMain/kotlin/.../mocks/UserRepositoryMock.kt` |
| `WebLinkDialogTestSupport` | `shared/presentation/src/androidUnitTest/kotlin/.../dialog/WebLinkDialogTestSupport.kt` |

`SettingsRepositoryMock` is the **only** `SettingsRepository` double — do not hand-roll another. Its
setters really mutate state, `initial` seeds it, `fetchException` makes `fetch()` throw, and
`mutableData` lets a non-suspend test body flip a value.

`FakeMarketPriceServiceFacade` answers both `findMarketPriceItem` and `findUSDMarketPriceItem` from a
price table. Amount-limit math needs both — a relaxed mockk returns chained mocks that fail inside
`toBaseSideMonetary`.

Two private same-named fakes still exist for the offerbook market list, keyed on a
`marketsWithPrice: Set<String>` instead of a price table: `OfferbookMarketPresenterTest` and
`ComputeOfferbookMarketListUseCaseTest`. The latter is in `commonTest`, which cannot see an
`androidUnitTest` helper at all — so prefer the shared fake, but do not assume it is the only one.

## Proof tests {#proof-tests}

| Test | Base | Path |
| --- | --- | --- |
| `FaqPresenterTest` | `PresentationKoinTestBase` | `shared/presentation/src/androidUnitTest/kotlin/.../settings/faqs/FaqPresenterTest.kt` |
| `OfferbookPresenterFilterTest` | `PlatformPresentationKoinTestBase` | `shared/presentation/src/androidUnitTest/kotlin/.../offerbook/OfferbookPresenterFilterTest.kt` |
| `ClientSettingsServiceFacadeTest` | `ClientKoinIntegrationTestBase` | `apps/clientApp/src/androidUnitTest/kotlin/.../settings/ClientSettingsServiceFacadeTest.kt` |
| `SwitchUiTest` | `BisqComposeUiTestBase` | `shared/presentation/src/androidUnitTest/kotlin/.../atoms/SwitchUiTest.kt` |
| `LinkButtonUiTest` | `PresentationKoinComposeTestBase` | `shared/presentation/src/androidUnitTest/kotlin/.../button/LinkButtonUiTest.kt` |
| `PaymentAccountMethodIconUiTest` | `BisqComposeUiTestBase` + `@Config(TestApplication)` | `apps/clientApp/src/androidUnitTest/kotlin/.../payment_accounts_list/ui/PaymentAccountMethodIconUiTest.kt` |

## Removed — do not cite

| Artifact | Reason |
| --- | --- |
| `:shared:presentation-test-utils` | Removed `2cb248bb`; helpers in `presentation/common/test_utils/` |
| `OfferbookMarketPresenterTestFactory` | Consolidated into `OfferbookMarketPresenterTest` (#1573) |
| `FakeSettingsRepository`, `TestSettingsRepository`, `FakeSettingsRepo` | Per-test copies; consolidated into `SettingsRepositoryMock` (#1488) |
| `FakeMarketPriceServiceFacade` copies in the create-offer / take-offer tests | Consolidated into `test_utils/FakeMarketPriceServiceFacade.kt` (#1488) |
| `TradeStatePresenterTestSupport` | Inline Koin/dispatcher helper; consumers extend `PresentationKoinTestBase` |
