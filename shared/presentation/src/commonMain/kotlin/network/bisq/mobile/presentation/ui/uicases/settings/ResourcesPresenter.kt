package network.bisq.mobile.presentation.ui.uicases.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.utils.DeviceInfoProvider
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

class ResourcesPresenter(
    mainPresenter: MainPresenter,
    private var versionProvider: VersionProvider,
    private var deviceInfoProvider: DeviceInfoProvider
) : BasePresenter(mainPresenter) {

    protected val _versionInfo: MutableStateFlow<String> = MutableStateFlow("")
    val versionInfo: StateFlow<String> get() = _versionInfo.asStateFlow()

    protected val _deviceInfo: MutableStateFlow<String> = MutableStateFlow("")
    val deviceInfo: StateFlow<String> get() = _deviceInfo.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()

        _versionInfo.value = versionProvider.getVersionInfo(isDemo, isIOS())

        _deviceInfo.value = deviceInfoProvider.getDeviceInfo()
    }

    fun onOpenTradeGuide() {
        navigateTo(Routes.TradeGuideOverview)
    }

    fun onOpenChatRules() {
        navigateTo(Routes.ChatRules)
    }

    fun onOpenWalletGuide() {
        navigateTo(Routes.WalletGuideIntro)
    }

    fun onOpenTac() {
        navigateTo(Routes.UserAgreementDisplay)
    }

    fun onOpenWebUrl(url: String) {
        navigateToUrl(url)
    }
}