package network.bisq.mobile.presentation.device_security

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.model.DeviceSecurityBaseline
import network.bisq.mobile.data.model.Settings
import network.bisq.mobile.domain.model.PlatformType
import network.bisq.mobile.domain.service.device_security.DeviceIntegrity
import network.bisq.mobile.domain.service.device_security.DeviceIntegrityProbe
import network.bisq.mobile.domain.service.device_security.DeviceSecurityService
import network.bisq.mobile.domain.service.device_security.DeviceSecurityWording
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.mocks.SettingsRepositoryMock
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceSecurityPresenterTest : PresentationKoinTestBase() {
    private lateinit var mainPresenter: MainPresenter

    private class FakeProbe(
        var result: DeviceIntegrity,
    ) : DeviceIntegrityProbe {
        override fun detect(): DeviceIntegrity = result
    }

    override fun onKoinReady() {
        I18nSupport.initialize("en")
        mainPresenter = mockk(relaxed = true)
    }

    private fun createPresenter(
        integrity: DeviceIntegrity,
        settings: SettingsRepositoryMock = SettingsRepositoryMock(),
        app: DeviceSecurityAppVariant = DeviceSecurityAppVariant.CONNECT,
        platform: PlatformType = PlatformType.ANDROID,
        isDemo: Boolean = false,
    ): DeviceSecurityPresenter {
        val service = DeviceSecurityService(FakeProbe(integrity), settings, testDispatcher)
        return DeviceSecurityPresenter(
            mainPresenter = mainPresenter,
            deviceSecurityService = service,
            settingsRepository = settings,
            appVariant = app,
            platformType = platform,
            isDemoMode = { isDemo },
        )
    }

    @Test
    fun `before the arrival is resolved nothing is shown and the carousel waits`() =
        runTest {
            val presenter = createPresenter(DeviceIntegrity.ROOTED)

            assertFalse(presenter.uiState.value.isResolved)
            assertFalse(presenter.uiState.value.isSheetVisible)
        }

    @Test
    fun `a clean device resolves with no sheet and no settings row`() =
        runTest {
            val presenter = createPresenter(DeviceIntegrity.CLEAN)
            presenter.onViewAttached()

            presenter.onAction(DeviceSecurityUiAction.OnDashboardArrival)
            advanceUntilIdle()

            val state = presenter.uiState.value
            assertTrue(state.isResolved)
            assertFalse(state.isSheetVisible)
            assertFalse(state.isRowVisible)
        }

    @Test
    fun `a rooted device shows the sheet on the dashboard`() =
        runTest {
            val presenter = createPresenter(DeviceIntegrity.ROOTED)
            presenter.onViewAttached()

            presenter.onAction(DeviceSecurityUiAction.OnDashboardArrival)
            advanceUntilIdle()

            val state = presenter.uiState.value
            assertTrue(state.isResolved)
            assertTrue(state.isSheetVisible)
            assertEquals(DeviceSecurityOrigin.DASHBOARD, state.origin)
            assertEquals(DeviceSecurityWording.FIRST_DETECTION, state.wording)
        }

    @Test
    fun `dismissing closes the sheet without persisting a decision`() =
        runTest {
            val settings = SettingsRepositoryMock()
            val presenter = createPresenter(DeviceIntegrity.ROOTED, settings)
            presenter.onAction(DeviceSecurityUiAction.OnDashboardArrival)
            advanceUntilIdle()

            presenter.onAction(DeviceSecurityUiAction.OnDismiss)
            advanceUntilIdle()

            assertFalse(presenter.uiState.value.isSheetVisible)
            assertFalse(settings.data.value.deviceSecurityWarningSuppressed)
        }

    @Test
    fun `a dismissed warning comes back on the next arrival`() =
        runTest {
            val settings = SettingsRepositoryMock()
            val first = createPresenter(DeviceIntegrity.ROOTED, settings)
            first.onAction(DeviceSecurityUiAction.OnDashboardArrival)
            advanceUntilIdle()
            first.onAction(DeviceSecurityUiAction.OnDismiss)
            advanceUntilIdle()

            val next = createPresenter(DeviceIntegrity.ROOTED, settings)
            next.onAction(DeviceSecurityUiAction.OnDashboardArrival)
            advanceUntilIdle()

            assertTrue(next.uiState.value.isSheetVisible)
        }

    @Test
    fun `dont show again persists the decision and confirms with a snackbar`() =
        runTest {
            val settings = SettingsRepositoryMock()
            val presenter = createPresenter(DeviceIntegrity.ROOTED, settings)
            presenter.onAction(DeviceSecurityUiAction.OnDashboardArrival)
            advanceUntilIdle()

            presenter.onAction(DeviceSecurityUiAction.OnDontShowAgain)
            advanceUntilIdle()

            assertFalse(presenter.uiState.value.isSheetVisible)
            assertTrue(presenter.uiState.value.isSuppressed)
            assertTrue(settings.data.value.deviceSecurityWarningSuppressed)
            verify {
                globalUiManager.showSnackbar("mobile.deviceSecurity.dontShowAgain.confirmation".i18n(), any(), any(), any())
            }
        }

    @Test
    fun `after dont show again the next arrival shows no sheet but keeps the settings row`() =
        runTest {
            val settings =
                SettingsRepositoryMock(
                    Settings(deviceSecurityBaseline = DeviceSecurityBaseline.ROOTED, deviceSecurityWarningSuppressed = true),
                )
            val presenter = createPresenter(DeviceIntegrity.ROOTED, settings)
            presenter.onViewAttached()

            presenter.onAction(DeviceSecurityUiAction.OnDashboardArrival)
            advanceUntilIdle()

            assertTrue(presenter.uiState.value.isResolved)
            assertFalse(presenter.uiState.value.isSheetVisible)
            assertTrue(presenter.uiState.value.isRowVisible)
        }

    @Test
    fun `a device rooted after a clean setup gets the changed wording`() =
        runTest {
            val settings = SettingsRepositoryMock(Settings(deviceSecurityBaseline = DeviceSecurityBaseline.CLEAN))
            val presenter = createPresenter(DeviceIntegrity.ROOTED, settings)

            presenter.onAction(DeviceSecurityUiAction.OnDashboardArrival)
            advanceUntilIdle()

            assertEquals(DeviceSecurityWording.CHANGED_SINCE_SETUP, presenter.uiState.value.wording)
        }

    @Test
    fun `demo mode never shows the warning`() =
        runTest {
            val presenter = createPresenter(DeviceIntegrity.ROOTED, isDemo = true)

            presenter.onAction(DeviceSecurityUiAction.OnDashboardArrival)
            advanceUntilIdle()

            assertTrue(presenter.uiState.value.isResolved)
            assertFalse(presenter.uiState.value.isSheetVisible)
        }

    @Test
    fun `settings shows the row on a rooted device and opens the sheet from it`() =
        runTest {
            val presenter = createPresenter(DeviceIntegrity.ROOTED)
            presenter.onViewAttached()
            advanceUntilIdle()

            assertTrue(presenter.uiState.value.isRowVisible)

            presenter.onAction(DeviceSecurityUiAction.OnOpenFromSettings)
            advanceUntilIdle()

            assertTrue(presenter.uiState.value.isSheetVisible)
            assertEquals(DeviceSecurityOrigin.SETTINGS, presenter.uiState.value.origin)
        }

    @Test
    fun `settings shows no row on a clean device`() =
        runTest {
            val presenter = createPresenter(DeviceIntegrity.CLEAN)
            presenter.onViewAttached()
            advanceUntilIdle()

            assertFalse(presenter.uiState.value.isRowVisible)
        }

    @Test
    fun `settings uses the changed wording when the device was clean at setup`() =
        runTest {
            val settings = SettingsRepositoryMock(Settings(deviceSecurityBaseline = DeviceSecurityBaseline.CLEAN))
            val presenter = createPresenter(DeviceIntegrity.ROOTED, settings)
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(DeviceSecurityUiAction.OnOpenFromSettings)
            advanceUntilIdle()

            assertEquals(DeviceSecurityWording.CHANGED_SINCE_SETUP, presenter.uiState.value.wording)
        }

    @Test
    fun `the platform and app select the copy variant`() =
        runTest {
            val presenter = createPresenter(DeviceIntegrity.ROOTED, app = DeviceSecurityAppVariant.CONNECT, platform = PlatformType.IOS)

            assertEquals(DeviceSecurityAppVariant.CONNECT, presenter.uiState.value.app)
            assertEquals(PlatformType.IOS, presenter.uiState.value.platform)
        }
}
