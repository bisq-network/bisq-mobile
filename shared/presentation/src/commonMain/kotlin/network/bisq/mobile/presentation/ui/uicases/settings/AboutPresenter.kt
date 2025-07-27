package network.bisq.mobile.presentation.ui.uicases.settings

import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.domain.data.replicated.settings.AboutSettingsVO
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.BisqLinks

open class AboutPresenter(
    mainPresenter: MainPresenter
) : BasePresenter(mainPresenter), IAboutPresenter {

    override val appName: String = BuildConfig.APP_NAME

    override fun versioning(): AboutSettingsVO {
        val demo = if (isDemo()) "-demo-" else ""
        val version =
            demo + if (isIOS()) BuildConfig.IOS_APP_VERSION else BuildConfig.ANDROID_APP_VERSION
        return AboutSettingsVO(
            appVersion = version,
            bisqCoreVersion = null,
            torVersion = null,
            apiVersion = BuildConfig.BISQ_API_VERSION,
        )
    }

    override fun navigateToMobileGitHub() {
        navigateToUrl(BisqLinks.BISQ_MOBILE_GH)
    }
}