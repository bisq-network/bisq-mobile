package network.bisq.mobile.presentation.ui.uicases.settings

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.utils.DeviceInfoProvider
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.NavRoute

open class ResourcesPresenter(
    mainPresenter: MainPresenter,
    private var versionProvider: VersionProvider,
    private var deviceInfoProvider: DeviceInfoProvider
) : BasePresenter(mainPresenter) {

    private val _versionInfo: MutableStateFlow<String> = MutableStateFlow("")
    val versionInfo: StateFlow<String> get() = _versionInfo.asStateFlow()

    private val _deviceInfo: MutableStateFlow<String> = MutableStateFlow("")
    val deviceInfo: StateFlow<String> get() = _deviceInfo.asStateFlow()

    protected val _showBackupAndRestore: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showBackupAndRestore: StateFlow<Boolean> get() = _showBackupAndRestore.asStateFlow()

    protected val _showBackupOverlay: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showBackupOverlay: StateFlow<Boolean> get() = _showBackupOverlay.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()

        _versionInfo.value = versionProvider.getVersionInfo(isDemo, isIOS())

        _deviceInfo.value = deviceInfoProvider.getDeviceInfo()
    }

    fun onOpenTradeGuide() {
        navigateTo(NavRoute.TradeGuideOverview)
    }

    fun onOpenChatRules() {
        navigateTo(NavRoute.ChatRules)
    }

    fun onOpenWalletGuide() {
        navigateTo(NavRoute.WalletGuideIntro)
    }

    fun onOpenTac() {
        navigateTo(NavRoute.UserAgreementDisplay)
    }

    fun onOpenWebUrl(url: String) {
        navigateToUrl(url)
    }

    open fun onBackupDataDir() {
        _showBackupOverlay.value = true
    }

    open fun onDismissBackupOverlay() {
        _showBackupOverlay.value = false
    }

    open fun onBackupDataDir(password: String?) {
        // Node will provide implementation as only used in node mode
    }

    open fun onRestoreDataDir(fileName: String, password: String?, data: ByteArray): CompletableDeferred<String?> {
        // Node will provide implementation as only used in node mode
        val result: CompletableDeferred<String?> = CompletableDeferred()
        result.complete(null)
        return result
    }
}