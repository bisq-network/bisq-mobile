package network.bisq.mobile.presentation.common.ui.navigation.graph

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.navigation.NavUtils.getDeepLinkBasePath
import network.bisq.mobile.presentation.guide.trade_guide.TradeGuideOverview
import network.bisq.mobile.presentation.guide.trade_guide.TradeGuideProcess
import network.bisq.mobile.presentation.guide.trade_guide.TradeGuideSecurity
import network.bisq.mobile.presentation.guide.trade_guide.TradeGuideTradeRules
import network.bisq.mobile.presentation.guide.wallet_guide.WalletGuideDownload
import network.bisq.mobile.presentation.guide.wallet_guide.WalletGuideIntro
import network.bisq.mobile.presentation.guide.wallet_guide.WalletGuideNewWallet
import network.bisq.mobile.presentation.guide.wallet_guide.WalletGuideReceiving
import network.bisq.mobile.presentation.offer.create_offer.amount.CreateOfferAmountScreen
import network.bisq.mobile.presentation.offer.create_offer.direction.CreateOfferDirectionScreen
import network.bisq.mobile.presentation.offer.create_offer.market.CreateOfferMarketScreen
import network.bisq.mobile.presentation.offer.create_offer.payment_method.CreateOfferPaymentMethodScreen
import network.bisq.mobile.presentation.offer.create_offer.price.CreateOfferPriceScreen
import network.bisq.mobile.presentation.offer.create_offer.review.CreateOfferReviewOfferScreen
import network.bisq.mobile.presentation.offer.create_offer.settlement.CreateOfferSettlementMethodScreen
import network.bisq.mobile.presentation.offer.take_offer.amount.TakeOfferTradeAmountScreen
import network.bisq.mobile.presentation.offer.take_offer.payment_method.TakeOfferPaymentMethodScreen
import network.bisq.mobile.presentation.offer.take_offer.review.TakeOfferReviewTradeScreen
import network.bisq.mobile.presentation.offer.take_offer.settlement.TakeOfferSettlementMethodScreen
import network.bisq.mobile.presentation.offerbook.OfferbookScreen
import network.bisq.mobile.presentation.settings.ignored_users.IgnoredUsersScreen
import network.bisq.mobile.presentation.settings.payment_accounts.PaymentAccountsScreen
import network.bisq.mobile.presentation.settings.reputation.ReputationScreen
import network.bisq.mobile.presentation.settings.resources.ResourcesScreen
import network.bisq.mobile.presentation.settings.settings.SettingsScreen
import network.bisq.mobile.presentation.settings.support.SupportScreen
import network.bisq.mobile.presentation.settings.user_profile.UserProfileScreen
import network.bisq.mobile.presentation.startup.create_profile.CreateProfileScreen
import network.bisq.mobile.presentation.startup.onboarding.OnboardingScreen
import network.bisq.mobile.presentation.startup.splash.SplashScreen
import network.bisq.mobile.presentation.startup.user_agreement.UserAgreementDisplayScreen
import network.bisq.mobile.presentation.startup.user_agreement.UserAgreementScreen
import network.bisq.mobile.presentation.tabs.tab.TabContainerScreen
import network.bisq.mobile.presentation.trade.trade_chat.ChatRulesScreen
import network.bisq.mobile.presentation.trade.trade_chat.TradeChatScreen
import network.bisq.mobile.presentation.trade.trade_detail.OpenTradeScreen

const val NAV_ANIM_MS = 300

fun NavGraphBuilder.addCommonAppRoutes() {
    composable<NavRoute.Splash> { SplashScreen() }

    addScreen<NavRoute.UserAgreement> { UserAgreementScreen() }
    addScreen<NavRoute.Onboarding> { OnboardingScreen() }
    addScreen<NavRoute.CreateProfile> { backStackEntry ->
        val route: NavRoute.CreateProfile = backStackEntry.toRoute()
        CreateProfileScreen(route.isOnboarding)
    }

    addScreen<NavRoute.TabContainer>(
        deepLinks =
            listOf(
                navDeepLink<NavRoute.TabContainer>(
                    basePath = getDeepLinkBasePath<NavRoute.TabContainer>(),
                ),
            ),
    ) { TabContainerScreen() }

    addScreen<NavRoute.OpenTrade>(
        deepLinks =
            listOf(
                navDeepLink<NavRoute.OpenTrade>(
                    basePath = getDeepLinkBasePath<NavRoute.OpenTrade>(),
                ),
            ),
    ) { backStackEntry ->
        val openTrade: NavRoute.OpenTrade = backStackEntry.toRoute()
        OpenTradeScreen(openTrade.tradeId)
    }

    addScreen<NavRoute.TradeChat>(
        navAnimation = NavAnimation.FADE_IN,
        deepLinks =
            listOf(
                navDeepLink<NavRoute.TradeChat>(
                    basePath = getDeepLinkBasePath<NavRoute.TradeChat>(),
                ),
            ),
    ) { backStackEntry ->
        val tradeChat: NavRoute.TradeChat = backStackEntry.toRoute()
        TradeChatScreen(tradeChat.tradeId)
    }

    // --- Other Screens ---
    addScreen<NavRoute.Offerbook> { OfferbookScreen() }
    addScreen<NavRoute.ChatRules> { ChatRulesScreen() }
    addScreen<NavRoute.Settings> { SettingsScreen() }
    addScreen<NavRoute.Support> { SupportScreen() }
    addScreen<NavRoute.Reputation> { ReputationScreen() }
    addScreen<NavRoute.UserProfile> { UserProfileScreen() }
    addScreen<NavRoute.PaymentAccounts> { PaymentAccountsScreen() }
    addScreen<NavRoute.IgnoredUsers> { IgnoredUsersScreen() }
    addScreen<NavRoute.Resources> { ResourcesScreen() }
    addScreen<NavRoute.UserAgreementDisplay> { UserAgreementDisplayScreen() }

    // --- Take Offer Screens ---
    addScreen<NavRoute.TakeOfferTradeAmount>(wizardTransition = false) { TakeOfferTradeAmountScreen() }
    addScreen<NavRoute.TakeOfferPaymentMethod>(wizardTransition = true) { TakeOfferPaymentMethodScreen() }
    addScreen<NavRoute.TakeOfferSettlementMethod>(wizardTransition = true) { TakeOfferSettlementMethodScreen() }
    addScreen<NavRoute.TakeOfferReviewTrade>(wizardTransition = true) { TakeOfferReviewTradeScreen() }

    // --- Create Offer Screens ---
    addScreen<NavRoute.CreateOfferDirection>(wizardTransition = false) { CreateOfferDirectionScreen() }
    addScreen<NavRoute.CreateOfferMarket>(wizardTransition = true) { CreateOfferMarketScreen() }
    addScreen<NavRoute.CreateOfferAmount>(wizardTransition = true) { CreateOfferAmountScreen() }
    addScreen<NavRoute.CreateOfferPrice>(wizardTransition = true) { CreateOfferPriceScreen() }
    addScreen<NavRoute.CreateOfferPaymentMethod>(wizardTransition = true) { CreateOfferPaymentMethodScreen() }
    addScreen<NavRoute.CreateOfferSettlementMethod>(wizardTransition = true) { CreateOfferSettlementMethodScreen() }
    addScreen<NavRoute.CreateOfferReviewOffer>(wizardTransition = true) { CreateOfferReviewOfferScreen() }

    // --- Trade Guide Screens ---
    addScreen<NavRoute.TradeGuideOverview>(wizardTransition = false) { TradeGuideOverview() }
    addScreen<NavRoute.TradeGuideSecurity>(wizardTransition = true) { TradeGuideSecurity() }
    addScreen<NavRoute.TradeGuideProcess>(wizardTransition = true) { TradeGuideProcess() }
    addScreen<NavRoute.TradeGuideTradeRules>(wizardTransition = true) { TradeGuideTradeRules() }

    // --- Wallet Guide Screens ---
    addScreen<NavRoute.WalletGuideIntro>(wizardTransition = false) { WalletGuideIntro() }
    addScreen<NavRoute.WalletGuideDownload>(wizardTransition = true) { WalletGuideDownload() }
    addScreen<NavRoute.WalletGuideNewWallet>(wizardTransition = true) { WalletGuideNewWallet() }
    addScreen<NavRoute.WalletGuideReceiving>(wizardTransition = true) { WalletGuideReceiving() }
}

enum class NavAnimation {
    FADE_IN,
    SLIDE_IN_FROM_RIGHT,
    SLIDE_IN_FROM_BOTTOM,
}

inline fun <reified T : NavRoute> NavGraphBuilder.addScreen(
    deepLinks: List<NavDeepLink> = emptyList(),
    wizardTransition: Boolean = false,
    navAnimation: NavAnimation = if (wizardTransition) NavAnimation.FADE_IN else NavAnimation.SLIDE_IN_FROM_RIGHT,
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable<T>(
        deepLinks = deepLinks,
        // 'enter' animation for the 'destination' screen
        enterTransition = {
            when (navAnimation) {
                NavAnimation.SLIDE_IN_FROM_RIGHT ->
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(NAV_ANIM_MS),
                    )

                NavAnimation.SLIDE_IN_FROM_BOTTOM ->
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(NAV_ANIM_MS),
                    )

                NavAnimation.FADE_IN -> fadeIn(animationSpec = tween(NAV_ANIM_MS))
            }
        },
        exitTransition = {
            // When a 'new' screen is pushed over 'current' screen, don't do exit animation for 'current' screen
            null
        },
        popEnterTransition = {
            // When the 'newly' pushed screen is popped out, don't do pop enter animation
            null
        },
        popExitTransition = {
            when (navAnimation) {
                NavAnimation.SLIDE_IN_FROM_RIGHT ->
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(NAV_ANIM_MS),
                    )

                NavAnimation.SLIDE_IN_FROM_BOTTOM ->
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(NAV_ANIM_MS),
                    )

                NavAnimation.FADE_IN -> fadeOut(animationSpec = tween(NAV_ANIM_MS))
            }
        },
    ) { backStackEntry ->
        content(backStackEntry)
    }
}
