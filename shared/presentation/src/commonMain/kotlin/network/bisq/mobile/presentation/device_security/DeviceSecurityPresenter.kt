package network.bisq.mobile.presentation.device_security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.data.model.DeviceSecurityBaseline
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.utils.getPlatformInfo
import network.bisq.mobile.domain.model.PlatformType
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.service.device_security.DeviceIntegrity
import network.bisq.mobile.domain.service.device_security.DeviceSecurityService
import network.bisq.mobile.domain.service.device_security.DeviceSecurityWording
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

/**
 * Drives the rooted / jailbroken device warning on the dashboard and the "Device security" row in
 * Settings. One instance per screen visit: a dismissal only closes the sheet for that visit, so the
 * warning returns on the next dashboard arrival until the user taps "Don't show again".
 *
 * Warn only: nothing else in the app reads the detection result, and it is never reported.
 */
open class DeviceSecurityPresenter(
    mainPresenter: MainPresenter,
    private val deviceSecurityService: DeviceSecurityService,
    private val settingsRepository: SettingsRepository,
    appVariant: DeviceSecurityAppVariant,
    platformType: PlatformType = getPlatformInfo().type,
    private val isDemoMode: () -> Boolean = { ApplicationBootstrapFacade.isDemo },
) : BasePresenter(mainPresenter) {
    private val _uiState =
        MutableStateFlow(
            DeviceSecurityUiState(
                app = appVariant,
                platform = platformType,
            ),
        )
    val uiState: StateFlow<DeviceSecurityUiState> = _uiState.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()
        presenterScope.launch { deviceSecurityService.ensureChecked() }
        presenterScope.launch {
            combine(deviceSecurityService.integrity, settingsRepository.data) { integrity, settings ->
                Triple(integrity == DeviceIntegrity.ROOTED, settings.deviceSecurityWarningSuppressed, settings.deviceSecurityBaseline)
            }.collect { (isRooted, isSuppressed, baseline) ->
                _uiState.update {
                    it.copy(
                        isRowVisible = isRooted,
                        isSuppressed = isSuppressed,
                        wording = wordingFor(baseline),
                    )
                }
            }
        }
    }

    fun onAction(action: DeviceSecurityUiAction) {
        when (action) {
            DeviceSecurityUiAction.OnDashboardArrival -> onDashboardArrival()
            DeviceSecurityUiAction.OnDismiss -> _uiState.update { it.copy(isSheetVisible = false) }
            DeviceSecurityUiAction.OnDontShowAgain -> onDontShowAgain()
            DeviceSecurityUiAction.OnOpenFromSettings ->
                _uiState.update { it.copy(isSheetVisible = true, origin = DeviceSecurityOrigin.SETTINGS) }
        }
    }

    private fun onDashboardArrival() {
        if (isDemoMode()) {
            _uiState.update { it.copy(isResolved = true) }
            return
        }
        presenterScope.launch {
            val resolution = deviceSecurityService.onDashboardArrival()
            _uiState.update {
                it.copy(
                    isResolved = true,
                    isSheetVisible = resolution.isPending,
                    isRowVisible = resolution.isRooted,
                    isSuppressed = resolution.suppressed,
                    origin = DeviceSecurityOrigin.DASHBOARD,
                    wording = resolution.wording ?: DeviceSecurityWording.FIRST_DETECTION,
                )
            }
        }
    }

    private fun onDontShowAgain() {
        presenterScope.launch {
            deviceSecurityService.suppressWarning()
            _uiState.update { it.copy(isSheetVisible = false, isSuppressed = true) }
            showSnackbar("mobile.deviceSecurity.dontShowAgain.confirmation".i18n())
        }
    }

    private fun wordingFor(baseline: DeviceSecurityBaseline): DeviceSecurityWording =
        if (baseline == DeviceSecurityBaseline.CLEAN) {
            DeviceSecurityWording.CHANGED_SINCE_SETUP
        } else {
            DeviceSecurityWording.FIRST_DETECTION
        }
}
