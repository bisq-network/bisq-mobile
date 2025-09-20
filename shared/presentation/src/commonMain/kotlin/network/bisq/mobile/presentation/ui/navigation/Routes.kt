package network.bisq.mobile.presentation.ui.navigation

object Graph {
    const val MAIN_SCREEN_GRAPH_KEY = "mainScreenGraph"
}

//todo is title used for anything?
enum class Routes(val title: String) {
    Splash(title = "splash"),
    UserAgreement(title = "user_agreement"),
    UserAgreementDisplay(title = "user_agreement_display"),
    Onboarding(title = "onboarding"),
    CreateProfile(title = "create_profile"),
    TrustedNodeSetup(title = "trusted_node_setup"),
    TrustedNodeSettings(title = "trusted_node_settings"),

    TabContainer(title = "tab_container"),

    TabHome(title = "tab_home"),
    CreateOfferDirection(title = "create_offer_buy_sel"),
    CreateOfferMarket(title = "create_offer_currency"),
    CreateOfferAmount(title = "create_offer_amount"),
    CreateOfferPrice(title = "create_offer_trade_price"),
    CreateOfferQuoteSidePaymentMethod(title = "create_offer_quote_side_payment_method"),
    CreateOfferBaseSidePaymentMethod(title = "create_offer_base_side_payment_method"),
    CreateOfferReviewOffer(title = "create_offer_review_offer"),

    TabOfferbook(title = "tab_currencies"),
    OffersByMarket(title = "offer_list"),

    TakeOfferTradeAmount(title = "take_offer_trade_amount"),
    TakeOfferQuoteSidePaymentMethod(title = "take_offer_quote_side_payment_method"),
    TakeOfferBaseSidePaymentMethod(title = "take_offer_base_side_payment_method"),
    TakeOfferReviewTrade(title = "take_offer_review_trade"),

    TabOpenTradeList(title = "tab_my_trades"),
    OpenTrade(title = "trade_flow"),

    TradeChat(title = "trade_chat"),

    ChatRules(title = "chat_rules"),

    Settings(title = "settings"),
    Support(title = "support"),
    Reputation(title = "reputation"),
    UserProfile(title = "user_profile"),
    PaymentAccounts(title = "payment_accounts"),
    IgnoredUsers(title = "ignored_users_settings"),
    Resources(title = "resources"),

    TabMiscItems(title = "tab_misc_items"),

    TradeGuideOverview(title = "trade_guide_overview"),
    TradeGuideSecurity(title = "trade_guide_security"),
    TradeGuideProcess(title = "trade_guide_process"),
    TradeGuideTradeRules(title = "trade_guide_trade_rules"),

    WalletGuideIntro(title = "wallet_guide_intro"),
    WalletGuideDownload(title = "wallet_guide_download"),
    WalletGuideNewWallet(title = "wallet_guide_newwallet"),
    WalletGuideReceiving(title = "wallet_guide_receiving");

    companion object {
        fun fromString(route: String): Routes? {
            return entries.find { it.title.equals(route, ignoreCase = true) ||
                    it.name.equals(route, ignoreCase = true) }
        }
    }
}