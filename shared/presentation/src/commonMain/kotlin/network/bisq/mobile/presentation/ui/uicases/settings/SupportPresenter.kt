package network.bisq.mobile.presentation.ui.uicases.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.utils.DeviceInfoProvider
import network.bisq.mobile.domain.utils.StringUtils.urlEncode
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.BisqLinks

class SupportPresenter(
    mainPresenter: MainPresenter,
    private var versionProvider: VersionProvider,
    private val deviceInfoProvider: DeviceInfoProvider
) : BasePresenter(mainPresenter) {

    protected val _reportUrl: MutableStateFlow<String> = MutableStateFlow("")
    val reportUrl: StateFlow<String> get() = _reportUrl.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()

        val versionInfo = versionProvider.getVersionInfo(isDemo, isIOS())
        val deviceInfo = deviceInfoProvider.getDeviceInfo()

        val body = "mobile.support.troubleShooting.github.body".i18n(versionInfo, deviceInfo)
        _reportUrl.value = BisqLinks.BISQ_MOBILE_GH_ISSUES + "/new?body=" + body.urlEncode()
    }

    fun onOpenWebUrl(url: String) {
        navigateToUrl(url)
    }

    fun onRestartApp() {
        restartApp()
    }

    fun onTerminateApp() {
        terminateApp()
    }
}