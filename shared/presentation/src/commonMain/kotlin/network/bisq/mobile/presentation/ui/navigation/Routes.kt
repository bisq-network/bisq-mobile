package network.bisq.mobile.presentation.ui.navigation

object Graph {
    const val MAIN_SCREEN_GRAPH_KEY = "mainScreenGraph"
}

//todo is title used for anything?
enum class Routes(val title: String) {
    Splash(title = "splash"),
    Onboarding(title = "onboarding"),
    CreateProfile(title = "create_profile"),
    TrustedNodeSetup(title = "trusted_node_setup"),

    TabContainer(title = "tab_container"),

    TabHome(title = "tab_home"),
    CreateOfferDirection(title = "create_offer_buy_sel"),
    CreateOfferMarket(title = "create_offer_currency"),
    CreateOfferAmount(title = "create_offer_amount"),
    CreateOfferPrice(title = "create_offer_trade_price"),
    CreateOfferPaymentMethod(title = "create_offer_payment_method"),
    CreateOfferReviewOffer(title = "create_offer_review_offer"),

    TabOfferbook(title = "tab_currencies"),
    OffersByMarket(title = "offer_list"),

    TakeOfferTradeAmount(title = "take_offer_trade_amount"),
    TakeOfferPaymentMethod(title = "take_offer_payment_method"),
    TakeOfferReviewTrade(title = "take_offer_review_trade"),

    TabOpenTradeList(title = "tab_my_trades"),
    OpenTrade(title = "trade_flow"),

    ChatScreen(title = "chat_screen"),

    UserProfileSettings(title = "user_profile_settings"),

    TabSettings(title = "tab_settings"),
}