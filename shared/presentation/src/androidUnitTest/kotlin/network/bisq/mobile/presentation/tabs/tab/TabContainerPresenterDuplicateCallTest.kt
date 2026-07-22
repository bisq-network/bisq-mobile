package network.bisq.mobile.presentation.tabs.tab

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.service.alert.TradeRestrictingAlertServiceFacade
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.common.test_utils.FakeAppUpdateLinker
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.test_utils.di.NoopNavigationManager
import network.bisq.mobile.presentation.common.ui.animation.AnimationSettings
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import network.bisq.mobile.presentation.offer.create_offer.CreateOfferCoordinator
import network.bisq.mobile.test.coroutines.TestCoroutineJobsManager
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TabContainerPresenterDuplicateCallTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var createOfferCoordinator: CreateOfferCoordinator

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480
        startKoin {
            modules(
                module {
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    single<NavigationManager> { NoopNavigationManager() }
                    single { GlobalUiManager(testDispatcher) }
                },
            )
        }
        createOfferCoordinator = mockk(relaxed = true)
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        Dispatchers.resetMain()
        stopKoin()
    }

    private fun buildPresenter(): TabContainerPresenter {
        val settingsServiceFacade = mockk<SettingsServiceFacade>(relaxed = true)
        every { settingsServiceFacade.useAnimations } returns MutableStateFlow(true)
        val tradeRestrictingAlertServiceFacade = mockk<TradeRestrictingAlertServiceFacade>(relaxed = true)
        every { tradeRestrictingAlertServiceFacade.alert } returns MutableStateFlow(null)
        val mainPresenter =
            MainPresenterTestFactory.create(
                applicationLifecycleService = TestApplicationLifecycleService(),
            )
        return TabContainerPresenter(
            mainPresenter,
            createOfferCoordinator,
            settingsServiceFacade,
            tradeRestrictingAlertServiceFacade,
            FakeAppUpdateLinker(),
            AnimationSettings(settingsServiceFacade, mockk(relaxed = true), applyDeviceLock = false),
        )
    }

    @Test
    fun `showAnimation reflects the effective animations flag`() {
        // buildPresenter wires useAnimations = true with no device lock, so the avatar animation is on.
        assertTrue(buildPresenter().showAnimation.value)
    }

    @Test
    fun `rapid double-tap on create offer starts coordinator only once`() =
        runTest(testDispatcher) {
            val presenter = buildPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.createOffer()
            presenter.createOffer()
            advanceUntilIdle()

            verify(exactly = 1) { createOfferCoordinator.onStartCreateOffer() }
            assertFalse(presenter.isCreateOfferEnabled.value)
        }

    @Test
    fun `create offer failure re-enables FAB action`() =
        runTest(testDispatcher) {
            every { createOfferCoordinator.onStartCreateOffer() } throws RuntimeException("fail")
            val presenter = buildPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.createOffer()
            advanceUntilIdle()

            assertTrue(presenter.isCreateOfferEnabled.value)
        }
}
