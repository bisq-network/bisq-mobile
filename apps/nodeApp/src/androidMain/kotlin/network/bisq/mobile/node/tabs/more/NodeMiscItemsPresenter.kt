package network.bisq.mobile.node.tabs.more

import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.backup
import bisqapps.shared.presentation.generated.resources.nav_network
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.UiString
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.tabs.more.MenuItem
import network.bisq.mobile.presentation.tabs.more.MiscItemsPresenter

// TODO Temporary flag — flip to true once the Network Info screens are complete (issue #1524).
private const val NETWORK_INFO_ENABLED = false

class NodeMiscItemsPresenter(
    userProfileService: UserProfileServiceFacade,
    mainPresenter: MainPresenter,
) : MiscItemsPresenter(userProfileService, mainPresenter) {
    override fun getPaymentAccountNavRoute(): NavRoute = NavRoute.PaymentAccounts

    override fun addCustomSettings(appItems: MutableList<MenuItem>): List<MenuItem> {
        appItems.add(
            appItems.size.coerceAtMost(1),
            MenuItem(
                label = UiString("mobile.more.backupAndRestore"),
                icon = Res.drawable.backup,
                route = NavRoute.BackupAndRestore,
            ),
        )
        if (NETWORK_INFO_ENABLED) {
            appItems.add(
                appItems.size.coerceAtMost(2),
                MenuItem(
                    label = UiString("mobile.more.network"),
                    icon = Res.drawable.nav_network,
                    route = NavRoute.NetworkInfo,
                ),
            )
        }
        return appItems
    }
}
