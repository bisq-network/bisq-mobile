package network.bisq.mobile.presentation.settings.resources

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.utils.DeviceInfoProvider
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

open class ResourcesPresenter(
    mainPresenter: MainPresenter,
    versionProvider: VersionProvider,
    deviceInfoProvider: DeviceInfoProvider,
) : BasePresenter(mainPresenter) {
    private val _uiState: MutableStateFlow<ResourcesUiState> =
        MutableStateFlow(
            ResourcesUiState(
                versionInfo = versionProvider.getVersionInfo(isDemo, isIOS()),
                deviceInfo = deviceInfoProvider.getDeviceInfo(),
            ),
        )
    val uiState = _uiState.asStateFlow()

    fun onAction(action: ResourcesUiAction) {
        when (action) {
            is ResourcesUiAction.OnNavigateToScreen -> navigateTo(action.route)
            is ResourcesUiAction.OnNavigateToUrl -> navigateToUrl(action.link)
        }
    }
}
