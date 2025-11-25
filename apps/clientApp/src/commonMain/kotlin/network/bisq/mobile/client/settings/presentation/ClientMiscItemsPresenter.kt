package network.bisq.mobile.client.settings.presentation

import bisqapps.apps.clientapp.generated.resources.Res
import bisqapps.apps.clientapp.generated.resources.nav_trusted_node
import network.bisq.mobile.client.common.presentation.navigation.TrustedNodeSetupSettings
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.MiscItemsPresenter

class ClientMiscItemsPresenter(
    userProfileService: UserProfileServiceFacade,
    mainPresenter: MainPresenter
) : MiscItemsPresenter(userProfileService, mainPresenter) {

    override fun addCustomSettings(menuItems: MutableList<MenuItem>): List<MenuItem> {
        menuItems.add(
            menuItems.size - 2, MenuItem.Leaf(
                "mobile.more.trustedNode".i18n(),
                icon = Res.drawable.nav_trusted_node,
                TrustedNodeSetupSettings
            )
        )
        return menuItems.toList()
    }
}