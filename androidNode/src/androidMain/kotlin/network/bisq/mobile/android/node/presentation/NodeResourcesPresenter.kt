package network.bisq.mobile.android.node.presentation

import network.bisq.mobile.android.node.BuildNodeConfig
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.ResourcesPresenter

class NodeResourcesPresenter(
    mainPresenter: MainPresenter
) : ResourcesPresenter(mainPresenter) {

    override fun onViewAttached() {
        super.onViewAttached()

        val demo = if (isDemo()) "-demo-" else ""
        val appName = demo + BuildNodeConfig.APP_NAME

        _versionInfo.value = "mobile.resources.versionDetails.node".i18n(
            appName,
            BuildNodeConfig.APP_VERSION,
            BuildNodeConfig.TOR_VERSION,
            BuildNodeConfig.BISQ_CORE_VERSION
        )
    }
}