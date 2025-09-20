package network.bisq.mobile.presentation.ui.uicases.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

open class ResourcesPresenter(
    mainPresenter: MainPresenter
) : BasePresenter(mainPresenter) {

    protected val _versionInfo: MutableStateFlow<String> = MutableStateFlow("")
    val versionInfo: StateFlow<String> get() = _versionInfo.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()

        val demo = if (isDemo()) "-demo-" else ""
        val appVersion = demo + if (isIOS()) BuildConfig.IOS_APP_VERSION else BuildConfig.ANDROID_APP_VERSION

        _versionInfo.value =
            "mobile.resources.versionDetails.client".i18n(BuildConfig.APP_NAME, appVersion, BuildConfig.BISQ_API_VERSION)
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