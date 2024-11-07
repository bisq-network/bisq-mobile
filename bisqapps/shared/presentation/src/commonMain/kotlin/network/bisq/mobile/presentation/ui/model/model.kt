package network.bisq.mobile.presentation.ui.model

import org.jetbrains.compose.resources.DrawableResource

data class BottomNavigationItem(val title: String, val route: String, val icon: String)
data class OnBoardingPage(val title: String, val image: DrawableResource, val desc: String)
