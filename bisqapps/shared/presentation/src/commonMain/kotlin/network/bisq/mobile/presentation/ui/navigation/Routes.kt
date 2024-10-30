package network.bisq.mobile.presentation.ui.navigation

sealed class SplashRouteScreen(var route: String) {
    object Splash : SplashRouteScreen("splash")
}

sealed class OnBoardingRouteScreen(var route: String) {
    object OnBoard : OnBoardingRouteScreen("onboard")
}
