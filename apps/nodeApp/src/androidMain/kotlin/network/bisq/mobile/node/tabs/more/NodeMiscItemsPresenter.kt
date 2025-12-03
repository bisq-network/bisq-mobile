package network.bisq.mobile.node.tabs.more

import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.tabs.more.MiscItemsPresenter

class NodeMiscItemsPresenter(
    userProfileService: UserProfileServiceFacade,
    mainPresenter: MainPresenter
) : MiscItemsPresenter(userProfileService, mainPresenter) {

    override fun addCustomSettings(menuItems: MutableList<MenuItem>): List<MenuItem> {
        return menuItems.toList()
    }
}