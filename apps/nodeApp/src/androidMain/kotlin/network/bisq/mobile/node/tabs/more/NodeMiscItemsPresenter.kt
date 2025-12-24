package network.bisq.mobile.node.tabs.more

import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.backup
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.tabs.more.MiscItemsPresenter

class NodeMiscItemsPresenter(
    userProfileService: UserProfileServiceFacade,
    mainPresenter: MainPresenter,
) : MiscItemsPresenter(userProfileService, mainPresenter) {
    override fun addCustomSettings(menuItems: MutableList<MenuItem>): List<MenuItem> {
        menuItems.add(
            MenuItem.Leaf(
                label = "mobile.more.backupAndRestore".i18n(),
                icon = Res.drawable.backup,
                route = NavRoute.BackupAndRestore,
            ),
        )
        return menuItems.toList()
    }
}
