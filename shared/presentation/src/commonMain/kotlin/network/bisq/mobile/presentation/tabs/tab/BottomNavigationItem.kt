package network.bisq.mobile.presentation.tabs.tab

import network.bisq.mobile.presentation.common.ui.navigation.TabNavRoute
import org.jetbrains.compose.resources.DrawableResource

data class BottomNavigationItem(
    val title: String,
    val route: TabNavRoute,
    val icon: DrawableResource,
)
