package network.bisq.mobile.presentation.ui.composeModels

import org.jetbrains.compose.resources.DrawableResource

data class BottomNavigationItem(val title: String, val route: String, val icon: DrawableResource)
data class PagerViewItem(val title: String, val image: DrawableResource, val desc: String)
