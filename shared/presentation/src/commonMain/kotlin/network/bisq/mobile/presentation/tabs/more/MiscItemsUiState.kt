package network.bisq.mobile.presentation.tabs.more

import network.bisq.mobile.i18n.UiString
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import org.jetbrains.compose.resources.DrawableResource

data class MiscItemsUiState(
    val sections: List<MenuSection> = emptyList(),
)

data class MenuSection(
    val title: UiString,
    val items: List<MenuItem>,
)

data class MenuItem(
    val label: UiString,
    val icon: DrawableResource,
    val route: NavRoute,
    val isEnabled: Boolean = true,
)
