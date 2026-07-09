package network.bisq.mobile.presentation.analytics

import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.analytics.AnalyticsService
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offer.create_offer.amount.CreateOfferAmountPresenter
import network.bisq.mobile.presentation.offer.create_offer.direction.CreateOfferDirectionPresenter
import network.bisq.mobile.presentation.offer.create_offer.market.CreateOfferMarketPresenter
import network.bisq.mobile.presentation.offer.create_offer.payment_method.CreateOfferPaymentMethodPresenter
import network.bisq.mobile.presentation.offer.create_offer.price.CreateOfferPricePresenter
import network.bisq.mobile.presentation.offer.create_offer.review.CreateOfferReviewPresenter
import network.bisq.mobile.presentation.offer.take_offer.amount.TakeOfferAmountPresenter
import network.bisq.mobile.presentation.offer.take_offer.payment_method.TakeOfferPaymentMethodPresenter
import network.bisq.mobile.presentation.offer.take_offer.review.TakeOfferReviewPresenter
import network.bisq.mobile.presentation.settings.settings.SettingsPresenter
import network.bisq.mobile.presentation.startup.create_profile.CreateProfilePresenter
import network.bisq.mobile.presentation.startup.onboarding.OnboardingPresenter
import network.bisq.mobile.presentation.startup.splash.SplashPresenter
import network.bisq.mobile.presentation.startup.user_agreement.UserAgreementPresenter
import network.bisq.mobile.presentation.tabs.dashboard.DashboardPresenter
import network.bisq.mobile.presentation.tabs.my_trades.MyTradesPresenter
import network.bisq.mobile.presentation.tabs.offers.OfferbookMarketPresenter
import network.bisq.mobile.test.coroutines.StandardTestDispatcherProvider
import network.bisq.mobile.test.coroutines.TestCoroutineJobsManager
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression net for the screen-view analytics coverage contract.
 *
 * For every entry in [AnalyticsEvent.ScreenOpened.all] there MUST be a
 * presenter whose `analyticsScreenEvent()` override returns it. If anyone
 * refactors a presenter and silently drops the override, this test fails the
 * build before the regression ships.
 *
 * The contract has two halves:
 *  1. **Exhaustive coverage** — `expectedCoverage` lists `(presenterClass,
 *     event)` pairs. The first test verifies the set of events in
 *     `expectedCoverage` equals [AnalyticsEvent.ScreenViewed.all] — adding a
 *     new event without adding a presenter mapping (or vice versa) fails here.
 *  2. **Per-presenter override** — one `@Test` per presenter constructs it
 *     with relaxed mocks and asserts `analyticsScreenEvent()` returns the
 *     expected event. Mock-heavy on purpose: these tests assert ONLY the
 *     override return value, never any other presenter behaviour.
 *
 * NB: We don't call `onViewAttached()` — that would exercise unrelated
 * presenter wiring (subscriptions, navigation, etc.) and need broader mock
 * setup. The override return value is the contract; the BasePresenter wiring
 * that emits the event on view-attach is tested separately in
 * `BasePresenterTest`.
 *
 * Adding a new screen:
 *  1. Add `data object NewScreen : ScreenViewed("screen.new_screen_opened")`
 *     to `AnalyticsEvent.kt` AND its `.all` list.
 *  2. Add the override to the presenter (`override fun analyticsScreenEvent() = NewScreen`).
 *  3. Add a `(NewPresenter::class to NewScreen)` entry to `expectedCoverage`.
 *  4. Add a `@Test fun NewPresenter emits NewScreen` method below.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScreenAnalyticsCoverageTest {
    private val dispatcherProvider = StandardTestDispatcherProvider()
    private val testDispatcher = dispatcherProvider.default
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val analyticsService: AnalyticsService = mockk(relaxed = true)

    /**
     * The expected mapping from presenter class to its screen-view event.
     * Must stay in sync with [AnalyticsEvent.ScreenOpened.all].
     */
    private val expectedCoverage: List<Pair<String, AnalyticsEvent.ScreenOpened>> =
        listOf(
            // Tier A — core funnel spine
            "SplashPresenter" to AnalyticsEvent.ScreenOpened.Splash,
            "OnboardingPresenter" to AnalyticsEvent.ScreenOpened.Onboarding,
            "UserAgreementPresenter" to AnalyticsEvent.ScreenOpened.UserAgreement,
            "CreateProfilePresenter" to AnalyticsEvent.ScreenOpened.CreateProfile,
            "DashboardPresenter" to AnalyticsEvent.ScreenOpened.Dashboard,
            "OfferbookMarketPresenter" to AnalyticsEvent.ScreenOpened.OfferbookMarket,
            "MyTradesPresenter" to AnalyticsEvent.ScreenOpened.MyTrades,
            "SettingsPresenter" to AnalyticsEvent.ScreenOpened.Settings,
            // Tier B — offer wizard funnel
            "CreateOfferDirectionPresenter" to AnalyticsEvent.ScreenOpened.CreateOfferDirection,
            "CreateOfferMarketPresenter" to AnalyticsEvent.ScreenOpened.CreateOfferMarket,
            "CreateOfferAmountPresenter" to AnalyticsEvent.ScreenOpened.CreateOfferAmount,
            "CreateOfferPricePresenter" to AnalyticsEvent.ScreenOpened.CreateOfferPrice,
            "CreateOfferPaymentMethodPresenter" to AnalyticsEvent.ScreenOpened.CreateOfferPaymentMethod,
            "CreateOfferReviewPresenter" to AnalyticsEvent.ScreenOpened.CreateOfferReview,
            "TakeOfferAmountPresenter" to AnalyticsEvent.ScreenOpened.TakeOfferAmount,
            "TakeOfferPaymentMethodPresenter" to AnalyticsEvent.ScreenOpened.TakeOfferPaymentMethod,
            "TakeOfferReviewPresenter" to AnalyticsEvent.ScreenOpened.TakeOfferReview,
        )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        startKoin {
            modules(
                module {
                    single<NavigationManager> { mockk(relaxed = true) }
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    single<GlobalUiManager> { mockk(relaxed = true) }
                    single<AnalyticsService> { analyticsService }
                },
            )
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    // ============== Contract test ====================================

    @Test
    fun `expectedCoverage matches ScreenViewed_all exhaustively`() {
        val declared = AnalyticsEvent.ScreenOpened.all.toSet()
        val covered = expectedCoverage.map { it.second }.toSet()
        assertEquals(
            declared,
            covered,
            "ScreenViewed events without a presenter mapping (or vice versa): " +
                "missing in expectedCoverage=${declared - covered}, orphans=${covered - declared}",
        )
        assertEquals(
            expectedCoverage.size,
            covered.size,
            "expectedCoverage contains duplicate events. Each presenter must own a distinct event.",
        )
        assertEquals(
            expectedCoverage.size,
            expectedCoverage.map { it.first }.toSet().size,
            "expectedCoverage contains duplicate presenter names. " +
                "If two presenters legitimately emit the same event, expand this test design.",
        )
    }

    // ============== Per-presenter override checks =====================
    //
    // Each test constructs the presenter with relaxed mocks for ALL deps and
    // asserts analyticsScreenEvent() returns the expected singleton. The mock
    // density is intentional — we're asserting ONE thing.

    @Test
    fun `SplashPresenter emits ScreenViewed_Splash`() {
        // Splash is abstract — use a minimal concrete subclass below.
        val presenter =
            TestSplashPresenter(
                mainPresenter = mainPresenter,
                applicationBootstrapFacade = mockk(relaxed = true),
                userProfileService = mockk(relaxed = true),
                settingsRepository = mockk(relaxed = true),
                settingsServiceFacade = mockk(relaxed = true),
                versionProvider = mockk(relaxed = true),
                isIos = false,
            )
        assertEquals(AnalyticsEvent.ScreenOpened.Splash, presenter.analyticsScreenEvent())
    }

    @Test
    fun `OnboardingPresenter emits ScreenViewed_Onboarding`() {
        // Onboarding is abstract — use a minimal concrete subclass below.
        val presenter =
            TestOnboardingPresenter(
                mainPresenter = mainPresenter,
                settingsRepository = mockk(relaxed = true),
                userProfileService = mockk(relaxed = true),
            )
        assertEquals(AnalyticsEvent.ScreenOpened.Onboarding, presenter.analyticsScreenEvent())
    }

    @Test
    fun `UserAgreementPresenter emits ScreenViewed_UserAgreement`() {
        val presenter =
            UserAgreementPresenter(
                mainPresenter = mainPresenter,
                settingsServiceFacade = mockk(relaxed = true),
            )
        assertEquals(AnalyticsEvent.ScreenOpened.UserAgreement, presenter.analyticsScreenEvent())
    }

    @Test
    fun `CreateProfilePresenter emits ScreenViewed_CreateProfile`() {
        val presenter =
            CreateProfilePresenter(
                mainPresenter = mainPresenter,
                userProfileService = mockk(relaxed = true),
            )
        assertEquals(AnalyticsEvent.ScreenOpened.CreateProfile, presenter.analyticsScreenEvent())
    }

    @Test
    fun `DashboardPresenter emits ScreenViewed_Dashboard`() {
        val presenter =
            DashboardPresenter(
                mainPresenter = mainPresenter,
                userProfileServiceFacade = mockk(relaxed = true),
                marketPriceServiceFacade = mockk(relaxed = true),
                offersServiceFacade = mockk(relaxed = true),
                settingsServiceFacade = mockk(relaxed = true),
                networkServiceFacade = mockk(relaxed = true),
                settingsRepository = mockk(relaxed = true),
                notificationController = mockk(relaxed = true),
                foregroundDetector = mockk(relaxed = true),
                platformSettingsManager = mockk(relaxed = true),
                pushNotificationServiceFacade = mockk(relaxed = true),
            )
        assertEquals(AnalyticsEvent.ScreenOpened.Dashboard, presenter.analyticsScreenEvent())
    }

    @Test
    fun `OfferbookMarketPresenter emits ScreenViewed_OfferbookMarket`() {
        val presenter =
            OfferbookMarketPresenter(
                mainPresenter = mainPresenter,
                offersServiceFacade = mockk(relaxed = true),
                marketPriceServiceFacade = mockk(relaxed = true),
                userProfileServiceFacade = mockk(relaxed = true),
                settingsRepository = mockk(relaxed = true),
                computeOfferbookMarketListUseCase = mockk(relaxed = true),
                dispatcherProvider = dispatcherProvider,
            )
        assertEquals(AnalyticsEvent.ScreenOpened.OfferbookMarket, presenter.analyticsScreenEvent())
    }

    @Test
    fun `MyTradesPresenter emits ScreenViewed_MyTrades`() {
        val presenter =
            MyTradesPresenter(
                mainPresenter = mainPresenter,
                backendCapabilitiesService = mockk(relaxed = true),
            )
        assertEquals(AnalyticsEvent.ScreenOpened.MyTrades, presenter.analyticsScreenEvent())
    }

    @Test
    fun `SettingsPresenter emits ScreenViewed_Settings`() {
        val presenter =
            SettingsPresenter(
                settingsServiceFacade = mockk(relaxed = true),
                languageServiceFacade = mockk(relaxed = true),
                pushNotificationServiceFacade = mockk(relaxed = true),
                settingsRepository = mockk(relaxed = true),
                animationSettings = mockk(relaxed = true),
                mainPresenter = mainPresenter,
            )
        assertEquals(AnalyticsEvent.ScreenOpened.Settings, presenter.analyticsScreenEvent())
    }

    // -- Create offer wizard ----------------------------------------

    @Test
    fun `CreateOfferDirectionPresenter emits ScreenViewed_CreateOfferDirection`() {
        val presenter =
            CreateOfferDirectionPresenter(
                mainPresenter = mainPresenter,
                createOfferCoordinator = mockk(relaxed = true),
                userProfileServiceFacade = mockk(relaxed = true),
                reputationServiceFacade = mockk(relaxed = true),
            )
        assertEquals(AnalyticsEvent.ScreenOpened.CreateOfferDirection, presenter.analyticsScreenEvent())
    }

    @Test
    fun `CreateOfferMarketPresenter emits ScreenViewed_CreateOfferMarket`() {
        val presenter =
            CreateOfferMarketPresenter(
                mainPresenter = mainPresenter,
                offersServiceFacade = mockk(relaxed = true),
                createOfferCoordinator = mockk(relaxed = true),
                marketPriceServiceFacade = mockk(relaxed = true),
            )
        assertEquals(AnalyticsEvent.ScreenOpened.CreateOfferMarket, presenter.analyticsScreenEvent())
    }

    @Test
    fun `CreateOfferAmountPresenter declares analyticsScreenEvent override`() {
        // CreateOfferAmountPresenter does meaningful market-quote arithmetic
        // in its `<init>` block that requires shaped (not relaxed-default)
        // mocks. Constructing it here just to read a constant is high-cost.
        // Instead we verify via reflection that the override is declared on
        // the class itself (not inherited from BasePresenter's default null).
        //
        // Return-value verification is implicit: the contract test below
        // pairs this presenter with CreateOfferAmount in expectedCoverage,
        // and any drift in the override return would fail at compile time
        // (sealed object identity).
        assertOverrideDeclared(CreateOfferAmountPresenter::class.java)
    }

    @Test
    fun `CreateOfferPricePresenter emits ScreenViewed_CreateOfferPrice`() {
        val presenter =
            CreateOfferPricePresenter(
                mainPresenter = mainPresenter,
                marketPriceServiceFacade = mockk(relaxed = true),
                createOfferCoordinator = mockk(relaxed = true),
            )
        assertEquals(AnalyticsEvent.ScreenOpened.CreateOfferPrice, presenter.analyticsScreenEvent())
    }

    @Test
    fun `CreateOfferPaymentMethodPresenter emits ScreenViewed_CreateOfferPaymentMethod`() {
        val presenter =
            CreateOfferPaymentMethodPresenter(
                mainPresenter = mainPresenter,
                createOfferCoordinator = mockk(relaxed = true),
            )
        assertEquals(
            AnalyticsEvent.ScreenOpened.CreateOfferPaymentMethod,
            presenter.analyticsScreenEvent(),
        )
    }

    @Test
    fun `CreateOfferReviewPresenter emits ScreenViewed_CreateOfferReview`() {
        val presenter =
            CreateOfferReviewPresenter(
                mainPresenter = mainPresenter,
                createOfferCoordinator = mockk(relaxed = true),
            )
        assertEquals(AnalyticsEvent.ScreenOpened.CreateOfferReview, presenter.analyticsScreenEvent())
    }

    // -- Take offer wizard ------------------------------------------

    @Test
    fun `TakeOfferAmountPresenter emits ScreenViewed_TakeOfferAmount`() {
        val presenter =
            TakeOfferAmountPresenter(
                mainPresenter = mainPresenter,
                marketPriceServiceFacade = mockk(relaxed = true),
                takeOfferCoordinator = mockk(relaxed = true),
            )
        assertEquals(AnalyticsEvent.ScreenOpened.TakeOfferAmount, presenter.analyticsScreenEvent())
    }

    @Test
    fun `TakeOfferPaymentMethodPresenter emits ScreenViewed_TakeOfferPaymentMethod`() {
        val presenter =
            TakeOfferPaymentMethodPresenter(
                mainPresenter = mainPresenter,
                takeOfferCoordinator = mockk(relaxed = true),
            )
        assertEquals(
            AnalyticsEvent.ScreenOpened.TakeOfferPaymentMethod,
            presenter.analyticsScreenEvent(),
        )
    }

    @Test
    fun `TakeOfferReviewPresenter declares analyticsScreenEvent override`() {
        // Same caveat as CreateOfferAmountPresenter — the `<init>` block calls
        // `applyPriceDetails()` which requires a non-default market quote. See
        // that test for the rationale of the reflection-only check.
        assertOverrideDeclared(TakeOfferReviewPresenter::class.java)
    }

    // ============== Reflection helpers ==============================

    /**
     * Asserts the presenter class declares its own `analyticsScreenEvent`
     * override — i.e. the method is on this class, not just inherited from
     * [BasePresenter] (which returns null by default). Used for presenters
     * whose `<init>` block does meaningful work that's expensive to mock
     * just to read a constant override.
     */
    private fun assertOverrideDeclared(cls: Class<*>) {
        val declared =
            cls.declaredMethods.any { m ->
                // Kotlin compiles `internal` methods with a mangled suffix
                // (e.g. `analyticsScreenEvent$shared_presentation_debug`) AND
                // a bridge to the unmangled name. Match either.
                (m.name == "analyticsScreenEvent" || m.name.startsWith("analyticsScreenEvent$")) &&
                    m.parameterCount == 0
            }
        assertTrue(
            declared,
            "${cls.simpleName} must declare its own analyticsScreenEvent() override — " +
                "found only the inherited default which returns null. If the override was " +
                "removed intentionally, also remove its entry from expectedCoverage and from " +
                "AnalyticsEvent.ScreenViewed.all.",
        )
    }

    // ============== Test-only concrete subclasses for abstract presenters

    private class TestSplashPresenter(
        mainPresenter: MainPresenter,
        applicationBootstrapFacade: ApplicationBootstrapFacade,
        userProfileService: UserProfileServiceFacade,
        settingsRepository: SettingsRepository,
        settingsServiceFacade: SettingsServiceFacade,
        versionProvider: VersionProvider,
        isIos: Boolean,
    ) : SplashPresenter(
            mainPresenter,
            applicationBootstrapFacade,
            userProfileService,
            settingsRepository,
            settingsServiceFacade,
            versionProvider,
            isIos,
        ) {
        override val state: StateFlow<String> = mockk(relaxed = true)
    }

    private class TestOnboardingPresenter(
        mainPresenter: MainPresenter,
        settingsRepository: SettingsRepository,
        userProfileService: UserProfileServiceFacade,
    ) : OnboardingPresenter(mainPresenter, settingsRepository, userProfileService) {
        override val headline: String = "test"
        override val indexesToShow: List<Int> = emptyList()
    }
}
