package network.bisq.mobile.presentation.ui.navigation

import kotlinx.serialization.Serializable
import network.bisq.mobile.presentation.ui.navigation.NavUtils.getDeepLinkBasePath

const val NAV_BASE_PATH = "bisq://"

sealed interface NavRoute {

    @Serializable
    data object HomeScreenGraphKey : NavRoute

    @Serializable
    data object Splash : NavRoute

    @Serializable
    data object UserAgreement : NavRoute

    @Serializable
    data object UserAgreementDisplay : NavRoute

    @Serializable
    data object Onboarding : NavRoute

    @Serializable
    data object CreateProfile : NavRoute

    @Serializable
    data object TrustedNodeSetup : NavRoute

    @Serializable
    data object TrustedNodeSetupSettings : NavRoute

    @Serializable
    data object TabContainer : NavRoute, DeepLinkableRoute {
        override fun toUriString(): String {
            return getDeepLinkBasePath(this)
        }
    }

    // --- Home Tabs ---
    @Serializable
    data object TabHome : NavRoute, TabNavRoute

    @Serializable
    data object TabOfferbookMarket : NavRoute, TabNavRoute

    @Serializable
    data object TabOpenTradeList : NavRoute, TabNavRoute, DeepLinkableRoute {
        override fun toUriString(): String {
            return getDeepLinkBasePath(this)
        }
    }

    @Serializable
    data object TabMiscItems : NavRoute, TabNavRoute

    // --- Create Offer Flow ---
    @Serializable
    data object CreateOfferDirection : NavRoute

    @Serializable
    data object CreateOfferMarket : NavRoute

    @Serializable
    data object CreateOfferAmount : NavRoute

    @Serializable
    data object CreateOfferPrice : NavRoute

    @Serializable
    data object CreateOfferPaymentMethod : NavRoute

    @Serializable
    data object CreateOfferSettlementMethod : NavRoute

    @Serializable
    data object CreateOfferReviewOffer : NavRoute

    // --- Take Offer Flow ---
    @Serializable
    data object TakeOfferTradeAmount : NavRoute

    @Serializable
    data object TakeOfferPaymentMethod : NavRoute

    @Serializable
    data object TakeOfferSettlementMethod : NavRoute

    @Serializable
    data object TakeOfferReviewTrade : NavRoute

    @Serializable
    data object Offerbook : NavRoute

    @Serializable
    data class OpenTrade(val tradeId: String) : NavRoute, DeepLinkableRoute {
        override fun toUriString(): String {
            return getDeepLinkBasePath(this) + "/$tradeId"
        }
    }

    @Serializable
    data class TradeChat(val tradeId: String) : NavRoute, DeepLinkableRoute {
        override fun toUriString(): String {
            return getDeepLinkBasePath(this) + "/$tradeId"
        }
    }

    // --- Settings Sub-screens ---
    @Serializable
    data object ChatRules : NavRoute

    @Serializable
    data object Settings : NavRoute

    @Serializable
    data object Support : NavRoute

    @Serializable
    data object Reputation : NavRoute

    @Serializable
    data object UserProfile : NavRoute

    @Serializable
    data object PaymentAccounts : NavRoute

    @Serializable
    data object IgnoredUsers : NavRoute

    @Serializable
    data object Resources : NavRoute

    // --- Trade Guide Flow ---
    @Serializable
    data object TradeGuideOverview : NavRoute

    @Serializable
    data object TradeGuideSecurity : NavRoute

    @Serializable
    data object TradeGuideProcess : NavRoute

    @Serializable
    data object TradeGuideTradeRules : NavRoute

    // --- Wallet Guide Flow ---
    @Serializable
    data object WalletGuideIntro : NavRoute

    @Serializable
    data object WalletGuideDownload : NavRoute

    @Serializable
    data object WalletGuideNewWallet : NavRoute

    @Serializable
    data object WalletGuideReceiving : NavRoute

}

interface TabNavRoute

interface DeepLinkableRoute {
    fun toUriString(): String
}

/**
 * Utility functions for navigation deep linking.
 */
object NavUtils {
    /**
     * Gets the deep link base path for a given route instance.
     */
    fun getDeepLinkBasePath(route: DeepLinkableRoute): String {
        return NAV_BASE_PATH + route::class.simpleName
    }

    /**
     * Gets the deep link base path for a route type (no instance needed).
     */
    inline fun <reified T : DeepLinkableRoute> getDeepLinkBasePath(): String {
        return NAV_BASE_PATH + T::class.simpleName
    }
}

