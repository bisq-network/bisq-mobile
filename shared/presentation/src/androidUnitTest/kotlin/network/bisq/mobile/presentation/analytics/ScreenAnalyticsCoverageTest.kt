package network.bisq.mobile.presentation.analytics

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.analytics.AnalyticsService
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.presentation.common.test_utils.FakeConfigServiceFacade
import network.bisq.mobile.presentation.common.test_utils.FakeMarketPriceServiceFacade
import network.bisq.mobile.presentation.common.test_utils.OfferTestFactory
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import network.bisq.mobile.presentation.community.CommunityHubPresenter
import network.bisq.mobile.presentation.community.contacts.ContactsPresenter
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offer.create_offer.CreateOfferCoordinator
import network.bisq.mobile.presentation.offer.create_offer.amount.CreateOfferAmountPresenter
import network.bisq.mobile.presentation.offer.create_offer.direction.CreateOfferDirectionPresenter
import network.bisq.mobile.presentation.offer.create_offer.market.CreateOfferMarketPresenter
import network.bisq.mobile.presentation.offer.create_offer.payment_method.CreateOfferPaymentMethodPresenter
import network.bisq.mobile.presentation.offer.create_offer.price.CreateOfferPricePresenter
import network.bisq.mobile.presentation.offer.create_offer.review.CreateOfferReviewPresenter
import network.bisq.mobile.presentation.offer.take_offer.TakeOfferCoordinator
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
import network.bisq.mobile.test.mocks.SettingsRepositoryMock
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression net for the screen-view analytics coverage contract.
 *
 * For every entry in [AnalyticsEvent.ScreenOpened.all] there MUST be a presenter that emits it on
 * view-attach. If anyone refactors a presenter and silently drops the override, this test fails the
 * build before the regression ships.
 *
 * The contract has two halves:
 *  1. **Exhaustive coverage** — [expectedCoverage] lists `(presenterName, event)` pairs. The first
 *     test verifies that set equals [AnalyticsEvent.ScreenOpened.all], so adding an event without a
 *     presenter mapping (or vice versa) fails here.
 *  2. **Emission** — one `@Test` per presenter constructs it, calls `onViewAttached()`, and verifies
 *     the event reached the Koin-bound [AnalyticsService]. This covers the whole path — the override
 *     AND `BasePresenter.onViewAttached()`'s emit — against a DI-bound mock. `analyticsScreenEvent()`
 *     is `protected` and deliberately never called directly from here.
 *
 * The presenters are attached with a [TestCoroutineJobsManager] over a `StandardTestDispatcher`, so
 * work they `launch` in `onViewAttached()` stays queued and out of the way. Do NOT advance the
 * dispatcher: the emit is synchronous and every override calls `super.onViewAttached()` first.
 *
 * Adding a new screen:
 *  1. Add `data object NewScreen : ScreenOpened("screen.new_screen_opened")` to `AnalyticsEvent.kt`
 *     AND its `.all` list.
 *  2. Add the override to the presenter (`override fun analyticsScreenEvent() = NewScreen`).
 *  3. Add a `"NewPresenter" to NewScreen` entry to [expectedCoverage].
 *  4. Add a `@Test` below that attaches the presenter and asserts the emission.
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
            // Tier C — community
            "CommunityHubPresenter" to AnalyticsEvent.ScreenOpened.CommunityHub,
            "ContactsPresenter" to AnalyticsEvent.ScreenOpened.CommunityContacts,
        )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // MainPresenter and the offer wizard presenters read the screen width during init.
        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480
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
        unmockkAll()
        Dispatchers.resetMain()
    }

    // ============== Contract test ====================================

    @Test
    fun `expectedCoverage matches ScreenOpened_all exhaustively`() {
        val declared = AnalyticsEvent.ScreenOpened.all.toSet()
        val covered = expectedCoverage.map { it.second }.toSet()
        assertEquals(
            declared,
            covered,
            "ScreenOpened events without a presenter mapping (or vice versa): " +
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

    // ============== Emission checks ===================================
    //
    // Each test constructs the presenter, attaches it, and verifies the event reached the bound
    // AnalyticsService. Mocks are relaxed wherever the presenter only stores or launches with a
    // dependency; the offer wizard presenters get real coordinators because they read the wizard
    // model synchronously.

    @Test
    fun `SplashPresenter emits ScreenOpened_Splash`() {
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
        assertEmitsOnAttach(presenter, AnalyticsEvent.ScreenOpened.Splash)
    }

    @Test
    fun `OnboardingPresenter emits ScreenOpened_Onboarding`() {
        // Onboarding is abstract — use a minimal concrete subclass below.
        val presenter =
            TestOnboardingPresenter(
                mainPresenter = mainPresenter,
                settingsRepository = mockk(relaxed = true),
                userProfileService = mockk(relaxed = true),
            )
        assertEmitsOnAttach(presenter, AnalyticsEvent.ScreenOpened.Onboarding)
    }

    @Test
    fun `UserAgreementPresenter emits ScreenOpened_UserAgreement`() {
        val presenter =
            UserAgreementPresenter(
                mainPresenter = mainPresenter,
                settingsServiceFacade = mockk(relaxed = true),
            )
        assertEmitsOnAttach(presenter, AnalyticsEvent.ScreenOpened.UserAgreement)
    }

    @Test
    fun `CreateProfilePresenter emits ScreenOpened_CreateProfile`() {
        val presenter =
            CreateProfilePresenter(
                mainPresenter = mainPresenter,
                userProfileService = mockk(relaxed = true),
            )
        assertEmitsOnAttach(presenter, AnalyticsEvent.ScreenOpened.CreateProfile)
    }

    @Test
    fun `DashboardPresenter emits ScreenOpened_Dashboard`() {
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
        assertEmitsOnAttach(presenter, AnalyticsEvent.ScreenOpened.Dashboard)
    }

    @Test
    fun `OfferbookMarketPresenter emits ScreenOpened_OfferbookMarket`() {
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
        assertEmitsOnAttach(presenter, AnalyticsEvent.ScreenOpened.OfferbookMarket)
    }

    @Test
    fun `MyTradesPresenter emits ScreenOpened_MyTrades`() {
        val presenter =
            MyTradesPresenter(
                mainPresenter = mainPresenter,
                backendCapabilitiesService = mockk(relaxed = true),
            )
        assertEmitsOnAttach(presenter, AnalyticsEvent.ScreenOpened.MyTrades)
    }

    @Test
    fun `CommunityHubPresenter emits ScreenOpened_CommunityHub`() {
        val presenter =
            CommunityHubPresenter(
                mainPresenter = mainPresenter,
                communityHubService =
                    mockk {
                        every { liveSegments } returns MutableStateFlow(emptySet())
                    },
            )
        assertEmitsOnAttach(presenter, AnalyticsEvent.ScreenOpened.CommunityHub)
    }

    @Test
    fun `ContactsPresenter emits ScreenOpened_CommunityContacts`() {
        val presenter =
            ContactsPresenter(
                mainPresenter = mainPresenter,
                contactsServiceFacade =
                    mockk {
                        every { contacts } returns MutableStateFlow(emptyList())
                    },
                userProfileServiceFacade = mockk(relaxed = true),
            )
        assertEmitsOnAttach(presenter, AnalyticsEvent.ScreenOpened.CommunityContacts)
    }

    @Test
    fun `SettingsPresenter emits ScreenOpened_Settings`() {
        val presenter =
            SettingsPresenter(
                settingsServiceFacade = mockk(relaxed = true),
                languageServiceFacade = mockk(relaxed = true),
                pushNotificationServiceFacade = mockk(relaxed = true),
                settingsRepository = SettingsRepositoryMock(),
                animationSettings = mockk(relaxed = true),
                mainPresenter = mainPresenter,
            )
        assertEmitsOnAttach(presenter, AnalyticsEvent.ScreenOpened.Settings)
    }

    // -- Create offer wizard ----------------------------------------

    @Test
    fun `CreateOfferDirectionPresenter emits ScreenOpened_CreateOfferDirection`() {
        val presenter =
            CreateOfferDirectionPresenter(
                mainPresenter = mainPresenter,
                createOfferCoordinator = createOfferCoordinator(),
                userProfileServiceFacade = mockk(relaxed = true),
                reputationServiceFacade = mockk(relaxed = true),
            )
        assertEmitsOnAttach(presenter, AnalyticsEvent.ScreenOpened.CreateOfferDirection)
    }

    @Test
    fun `CreateOfferMarketPresenter emits ScreenOpened_CreateOfferMarket`() {
        val presenter =
            CreateOfferMarketPresenter(
                mainPresenter = mainPresenter,
                offersServiceFacade = mockk(relaxed = true),
                createOfferCoordinator = createOfferCoordinator(),
                marketPriceServiceFacade = marketPriceServiceFacade,
            )
        assertEmitsOnAttach(presenter, AnalyticsEvent.ScreenOpened.CreateOfferMarket)
    }

    @Test
    fun `CreateOfferAmountPresenter emits ScreenOpened_CreateOfferAmount`() {
        val presenter =
            CreateOfferAmountPresenter(
                mainPresenter = mainPresenter,
                marketPriceServiceFacade = marketPriceServiceFacade,
                createOfferCoordinator = createOfferCoordinator(),
                userProfileServiceFacade = mockk(relaxed = true),
                reputationServiceFacade = mockk(relaxed = true),
                configServiceFacade = FakeConfigServiceFacade(),
            )
        assertEmitsOnAttach(presenter, AnalyticsEvent.ScreenOpened.CreateOfferAmount)
    }

    @Test
    fun `CreateOfferPricePresenter emits ScreenOpened_CreateOfferPrice`() {
        val presenter =
            CreateOfferPricePresenter(
                mainPresenter = mainPresenter,
                marketPriceServiceFacade = marketPriceServiceFacade,
                createOfferCoordinator = createOfferCoordinator(),
            )
        assertEmitsOnAttach(presenter, AnalyticsEvent.ScreenOpened.CreateOfferPrice)
    }

    @Test
    fun `CreateOfferPaymentMethodPresenter emits ScreenOpened_CreateOfferPaymentMethod`() {
        val presenter =
            CreateOfferPaymentMethodPresenter(
                mainPresenter = mainPresenter,
                createOfferCoordinator = createOfferCoordinator(),
            )
        assertEmitsOnAttach(presenter, AnalyticsEvent.ScreenOpened.CreateOfferPaymentMethod)
    }

    @Test
    fun `CreateOfferReviewPresenter emits ScreenOpened_CreateOfferReview`() {
        val presenter =
            CreateOfferReviewPresenter(
                mainPresenter = mainPresenter,
                createOfferCoordinator = createOfferCoordinator(),
            )
        assertEmitsOnAttach(presenter, AnalyticsEvent.ScreenOpened.CreateOfferReview)
    }

    // -- Take offer wizard ------------------------------------------

    @Test
    fun `TakeOfferAmountPresenter emits ScreenOpened_TakeOfferAmount`() {
        val presenter =
            TakeOfferAmountPresenter(
                mainPresenter = mainPresenter,
                marketPriceServiceFacade = marketPriceServiceFacade,
                takeOfferCoordinator = takeOfferCoordinator(),
                configServiceFacade = FakeConfigServiceFacade(),
            )
        assertEmitsOnAttach(presenter, AnalyticsEvent.ScreenOpened.TakeOfferAmount)
    }

    @Test
    fun `TakeOfferPaymentMethodPresenter emits ScreenOpened_TakeOfferPaymentMethod`() {
        val presenter =
            TakeOfferPaymentMethodPresenter(
                mainPresenter = mainPresenter,
                takeOfferCoordinator = takeOfferCoordinator(),
            )
        assertEmitsOnAttach(presenter, AnalyticsEvent.ScreenOpened.TakeOfferPaymentMethod)
    }

    @Test
    fun `TakeOfferReviewPresenter emits ScreenOpened_TakeOfferReview`() {
        val presenter =
            TakeOfferReviewPresenter(
                mainPresenter = mainPresenter,
                marketPriceServiceFacade = marketPriceServiceFacade,
                takeOfferCoordinator = takeOfferCoordinator(),
            )
        assertEmitsOnAttach(presenter, AnalyticsEvent.ScreenOpened.TakeOfferReview)
    }

    // ============== Negative case ====================================

    @Test
    fun `presenter without an override emits nothing on attach`() {
        NoScreenEventPresenter(mainPresenter).onViewAttached()
        verify(exactly = 0) { analyticsService.track(any()) }
    }

    // ============== Helpers ==========================================

    /**
     * Attaches [presenter] and asserts it emitted exactly [expected], and no other screen event —
     * a presenter that fires a second [AnalyticsEvent.ScreenOpened] on attach fails here.
     *
     * `ofType` rather than `any`: MockK's `any<T>()` is type-erased and matches every argument, so
     * it would also fail on a presenter that legitimately tracks a non-screen event on attach.
     */
    private fun assertEmitsOnAttach(
        presenter: BasePresenter,
        expected: AnalyticsEvent.ScreenOpened,
    ) {
        presenter.onViewAttached()
        verify(exactly = 1) { analyticsService.track(expected) }
        verify(exactly = 1) { analyticsService.track(ofType<AnalyticsEvent.ScreenOpened>()) }
    }

    private val marketPriceServiceFacade = FakeMarketPriceServiceFacade(SettingsRepositoryMock())

    private fun createOfferCoordinator(): CreateOfferCoordinator =
        CreateOfferCoordinator(
            marketPriceServiceFacade,
            mockk(relaxed = true),
            mockk(relaxed = true),
        ).also { it.createOfferModel = OfferTestFactory.makeCreateOfferModel() }

    private fun takeOfferCoordinator(): TakeOfferCoordinator =
        TakeOfferCoordinator(
            marketPriceServiceFacade,
            mockk(relaxed = true),
            FakeConfigServiceFacade(),
        ).also { it.selectOfferToTake(OfferItemPresentationModel(OfferTestFactory.makeOfferDto())) }

    // ============== Test-only presenters =============================

    /** Inherits BasePresenter's `null` default — used by the negative case. */
    private class NoScreenEventPresenter(
        mainPresenter: MainPresenter,
    ) : BasePresenter(mainPresenter) {
        override fun onDestroying() {}
    }

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
