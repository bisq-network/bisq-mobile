package network.bisq.mobile.presentation.settings.resources

import network.bisq.mobile.presentation.common.ui.navigation.NavRoute

sealed interface ResourcesUiAction {
    data class OnNavigateToScreen(
        val route: NavRoute,
    ) : ResourcesUiAction

    data class OnNavigateToUrl(
        val link: String,
    ) : ResourcesUiAction
}
