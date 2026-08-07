package network.bisq.mobile.presentation.tabs.tab

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.service.alert.TradeRestrictingAlertServiceFacade
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.data.utils.AppUpdateLinker
import network.bisq.mobile.data.utils.UrlLauncher
import network.bisq.mobile.domain.model.alert.AlertType
import network.bisq.mobile.domain.model.alert.AuthorizedAlertData
import network.bisq.mobile.presentation.common.test_utils.FakeAppUpdateLinker
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.TEST_APP_UPDATE_URL
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.ui.alert.AlertNotificationUiAction
import network.bisq.mobile.presentation.common.ui.animation.AnimationSettings
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.offer.create_offer.CreateOfferCoordinator
import network.bisq.mobile.test.presentation.coroutines.PlatformPresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for the trade-restriction dialog flow in [TabContainerPresenter].
 *
 * Mirrors [OfferbookPresenterTradeRestrictionTest] for the identical branching in
 * [TabContainerPresenter.createOffer] and [TabContainerPresenter.onTradeRestrictingAlertAction].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TabContainerPresenterTradeRestrictionTest : PlatformPresentationKoinTestBase() {
    override fun beforeStartKoin() {
        super.beforeStartKoin()
        globalUiManager = GlobalUiManager(testDispatcher)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildPresenter(
        activeAlert: AuthorizedAlertData? = null,
        urlLauncher: UrlLauncher? = null,
        appUpdateLinker: AppUpdateLinker = FakeAppUpdateLinker(),
    ): TabContainerPresenter {
        val resolvedUrlLauncher =
            urlLauncher ?: mockk<UrlLauncher>().also { launcher ->
                coEvery { launcher.openUrl(any()) } returns true
            }
        val mainPresenter =
            MainPresenterTestFactory.create(
                applicationLifecycleService = TestApplicationLifecycleService(),
                urlLauncher = resolvedUrlLauncher,
            )
        val createOfferCoordinator = mockk<CreateOfferCoordinator>(relaxed = true)

        val settingsServiceFacade = mockk<SettingsServiceFacade>(relaxed = true)
        every { settingsServiceFacade.useAnimations } returns MutableStateFlow(false)

        val alertFlow = MutableStateFlow<AuthorizedAlertData?>(activeAlert)
        val tradeRestrictingAlertServiceFacade = mockk<TradeRestrictingAlertServiceFacade>()
        every { tradeRestrictingAlertServiceFacade.alert } returns alertFlow

        return TabContainerPresenter(
            mainPresenter = mainPresenter,
            createOfferCoordinator = createOfferCoordinator,
            settingsServiceFacade = settingsServiceFacade,
            tradeRestrictingAlertServiceFacade = tradeRestrictingAlertServiceFacade,
            appUpdateLinker = appUpdateLinker,
            animationSettings = AnimationSettings(settingsServiceFacade, mockk(relaxed = true), applyDeviceLock = false),
        )
    }

    private fun makeAlert(headline: String = "Trading restricted") =
        AuthorizedAlertData(
            id = "tab-alert",
            type = AlertType.EMERGENCY,
            headline = headline,
            message = "Please update the app.",
            haltTrading = true,
            date = 1L,
        )

    // -------------------------------------------------------------------------
    // Active-alert gating on createOffer
    // -------------------------------------------------------------------------

    @Test
    fun `showTradeRestrictedDialog is null on construction`() =
        runTest {
            val presenter = buildPresenter()
            assertNull(presenter.showTradeRestrictedDialog.value)
        }

    @Test
    fun `createOffer with active alert sets showTradeRestrictedDialog`() =
        runTest {
            val presenter = buildPresenter(activeAlert = makeAlert())
            assertNull(presenter.showTradeRestrictedDialog.value)

            presenter.createOffer()
            advanceUntilIdle()

            val state = presenter.showTradeRestrictedDialog.value
            assertNotNull(state)
        }

    @Test
    fun `createOffer without active alert leaves showTradeRestrictedDialog null`() =
        runTest {
            val presenter = buildPresenter(activeAlert = null)
            // createOfferCoordinator is relaxed so navigation doesn't crash; dialog must stay null.
            presenter.createOffer()
            advanceUntilIdle()

            assertNull(presenter.showTradeRestrictedDialog.value)
        }

    // -------------------------------------------------------------------------
    // OnUpdateNow
    // -------------------------------------------------------------------------

    @Test
    fun `OnUpdateNow clears showTradeRestrictedDialog and opens update URL`() =
        runTest {
            val appUpdateLinker = mockk<AppUpdateLinker>()
            every { appUpdateLinker.getUpdateUrl() } returns TEST_APP_UPDATE_URL
            val urlLauncher = mockk<UrlLauncher>()
            coEvery { urlLauncher.openUrl(any()) } returns true
            val presenter =
                buildPresenter(
                    activeAlert = makeAlert(),
                    urlLauncher = urlLauncher,
                    appUpdateLinker = appUpdateLinker,
                )
            presenter.createOffer()
            advanceUntilIdle()
            assertNotNull(presenter.showTradeRestrictedDialog.value)

            presenter.onTradeRestrictingAlertAction(AlertNotificationUiAction.OnUpdateNow)
            advanceUntilIdle()

            assertNull(presenter.showTradeRestrictedDialog.value)
            verify(exactly = 1) { appUpdateLinker.getUpdateUrl() }
            coVerify(exactly = 1) { urlLauncher.openUrl(TEST_APP_UPDATE_URL) }
        }

    // -------------------------------------------------------------------------
    // OnCloseDialog
    // -------------------------------------------------------------------------

    @Test
    fun `OnCloseDialog dismisses dialog without triggering update`() =
        runTest {
            val presenter = buildPresenter(activeAlert = makeAlert())
            presenter.createOffer()
            advanceUntilIdle()
            assertNotNull(presenter.showTradeRestrictedDialog.value)

            presenter.onTradeRestrictingAlertAction(AlertNotificationUiAction.OnCloseDialog)
            advanceUntilIdle()

            assertNull(presenter.showTradeRestrictedDialog.value)
        }

    // -------------------------------------------------------------------------
    // Round-trip
    // -------------------------------------------------------------------------

    @Test
    fun `dialog can be reopened after being closed`() =
        runTest {
            val presenter = buildPresenter(activeAlert = makeAlert())

            presenter.createOffer()
            advanceUntilIdle()
            assertNotNull(presenter.showTradeRestrictedDialog.value)

            presenter.onTradeRestrictingAlertAction(AlertNotificationUiAction.OnCloseDialog)
            advanceUntilIdle()
            assertNull(presenter.showTradeRestrictedDialog.value)

            presenter.createOffer()
            advanceUntilIdle()
            assertNotNull(presenter.showTradeRestrictedDialog.value)
        }
}
