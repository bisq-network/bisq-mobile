package network.bisq.mobile.presentation.tabs.more

import network.bisq.mobile.presentation.common.ui.navigation.NavRoute

sealed interface MiscItemsUiAction {
    data class OnMenuItemClick(
        val route: NavRoute,
    ) : MiscItemsUiAction
}
