package network.bisq.mobile.client.tabs.more

import bisqapps.apps.clientapp.generated.resources.Res
import bisqapps.apps.clientapp.generated.resources.nav_trusted_node
import network.bisq.mobile.client.common.presentation.navigation.ClientNavRoute
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.domain.service.capabilities.BackendCapabilitiesService
import network.bisq.mobile.i18n.UiString
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.tabs.more.MenuItem
import network.bisq.mobile.presentation.tabs.more.MiscItemsPresenter

class ClientMiscItemsPresenter(
    backendCapabilitiesService: BackendCapabilitiesService,
    mainPresenter: MainPresenter,
) : MiscItemsPresenter(backendCapabilitiesService, mainPresenter) {
    override fun getPaymentAccountNavRoute(): NavRoute = if (BuildConfig.MU_SIG_ENABLED) ClientNavRoute.PaymentAccountsMusig else NavRoute.PaymentAccounts

    override fun addCustomSettings(appItems: MutableList<MenuItem>): List<MenuItem> {
        appItems.add(
            appItems.size.coerceAtMost(1),
            MenuItem(
                label = UiString("mobile.more.trustedNode"),
                icon = Res.drawable.nav_trusted_node,
                route = ClientNavRoute.TrustedNodeSetupSettings,
            ),
        )
        return appItems
    }
}
