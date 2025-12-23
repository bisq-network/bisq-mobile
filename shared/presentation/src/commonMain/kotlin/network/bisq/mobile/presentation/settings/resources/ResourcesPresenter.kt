package network.bisq.mobile.presentation.settings.resources

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.utils.DeviceInfoProvider
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

open class ResourcesPresenter(
    mainPresenter: MainPresenter,
    private var versionProvider: VersionProvider,
    private var deviceInfoProvider: DeviceInfoProvider
) : BasePresenter(mainPresenter) {

    private val _versionInfo: MutableStateFlow<String> = MutableStateFlow("")
    val versionInfo: StateFlow<String> get() = _versionInfo.asStateFlow()

    private val _deviceInfo: MutableStateFlow<String> = MutableStateFlow("")
    val deviceInfo: StateFlow<String> get() = _deviceInfo.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()

        _versionInfo.value = versionProvider.getVersionInfo(isDemo, isIOS())

        _deviceInfo.value = deviceInfoProvider.getDeviceInfo()
    }

    fun onAction(action: ResourcesUiAction) {
        when(action) {
            is ResourcesUiAction.OnNavigateToScreen -> navigateTo(action.route)
            is ResourcesUiAction.OnNavigateToUrl -> navigateToUrl(action.link)
        }
    }
}