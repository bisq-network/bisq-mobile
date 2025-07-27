package network.bisq.mobile.android.node.presentation

import network.bisq.mobile.android.node.BuildNodeConfig
import network.bisq.mobile.domain.data.replicated.settings.AboutSettingsVO
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.AboutPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.IAboutPresenter

class NodeAboutPresenter(
    mainPresenter: MainPresenter
) : AboutPresenter(mainPresenter), IAboutPresenter {

    override val appName: String = BuildNodeConfig.APP_NAME

    override fun versioning(): AboutSettingsVO {
        return AboutSettingsVO(
            appVersion = BuildNodeConfig.APP_VERSION,
            bisqCoreVersion = BuildNodeConfig.BISQ_CORE_VERSION,
            torVersion = BuildNodeConfig.TOR_VERSION,
            apiVersion = null,
        )
    }
}