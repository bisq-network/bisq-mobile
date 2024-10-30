package network.bisq.mobile.presentation.ui.navigation

import org.jetbrains.compose.resources.StringResource

object Graph {
    const val MainScreenGraph = "mainScreenGraph"
}

enum class Routes(val title: String) {
    Splash(title = "splash"),
    Onboarding(title = "onboarding"),
    CreateProfile(title = "create_profile"),
    BisqUrl(title = "bisq_url"),
    TabContainer(title = "tab_container"),
    TabHome(title = "tab_home"),
    TabExchange(title = "tab_exchange"),
    TabMyTrades(title = "tab_my_trades"),
    TabSettings(title = "tab_settings"),
}