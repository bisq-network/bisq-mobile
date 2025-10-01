package network.bisq.mobile.presentation.ui.navigation.graph

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import network.bisq.mobile.presentation.ui.navigation.NavRoute
import network.bisq.mobile.presentation.ui.navigation.NavUtils.getDeepLinkBasePath
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.ui.uicases.TabContainerScreen
import network.bisq.mobile.presentation.ui.uicases.banners.Banner
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferAmountScreen
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferDirectionScreen
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferMarketScreen
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPaymentMethodScreen
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPriceScreen
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferReviewOfferScreen
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferSettlementMethodScreen
import network.bisq.mobile.presentation.ui.uicases.guide.TradeGuideOverview
import network.bisq.mobile.presentation.ui.uicases.guide.TradeGuideProcess
import network.bisq.mobile.presentation.ui.uicases.guide.TradeGuideSecurity
import network.bisq.mobile.presentation.ui.uicases.guide.TradeGuideTradeRules
import network.bisq.mobile.presentation.ui.uicases.guide.WalletGuideDownload
import network.bisq.mobile.presentation.ui.uicases.guide.WalletGuideIntro
import network.bisq.mobile.presentation.ui.uicases.guide.WalletGuideNewWallet
import network.bisq.mobile.presentation.ui.uicases.guide.WalletGuideReceiving
import network.bisq.mobile.presentation.ui.uicases.offerbook.OfferbookScreen
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.OpenTradeScreen
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.trade_chat.ChatRulesScreen
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.trade_chat.TradeChatScreen
import network.bisq.mobile.presentation.ui.uicases.settings.IgnoredUsersScreen
import network.bisq.mobile.presentation.ui.uicases.settings.PaymentAccountsScreen
import network.bisq.mobile.presentation.ui.uicases.settings.ReputationScreen
import network.bisq.mobile.presentation.ui.uicases.settings.ResourcesScreen
import network.bisq.mobile.presentation.ui.uicases.settings.SettingsScreen
import network.bisq.mobile.presentation.ui.uicases.settings.SupportScreen
import network.bisq.mobile.presentation.ui.uicases.settings.UserProfileScreen
import network.bisq.mobile.presentation.ui.uicases.startup.CreateProfileScreen
import network.bisq.mobile.presentation.ui.uicases.startup.OnboardingScreen
import network.bisq.mobile.presentation.ui.uicases.startup.SplashScreen
import network.bisq.mobile.presentation.ui.uicases.startup.TrustedNodeSetupScreen
import network.bisq.mobile.presentation.ui.uicases.startup.UserAgreementDisplayScreen
import network.bisq.mobile.presentation.ui.uicases.startup.UserAgreementScreen
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferPaymentMethodScreen
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferReviewTradeScreen
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferSettlementMethodScreen
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferTradeAmountScreen

const val NAV_ANIM_MS = 300

@Composable
fun RootNavGraph(rootNavController: NavHostController) {
    NavHost(
        modifier = Modifier.background(color = BisqTheme.colors.backgroundColor),
        navController = rootNavController,
        startDestination = NavRoute.Splash,
    ) {
        composable<NavRoute.Splash> { SplashScreen() }

        addScreen<NavRoute.TrustedNodeSetupSettings> { TrustedNodeSetupScreen(false) }
        addScreen<NavRoute.UserAgreement> { UserAgreementScreen() }
        addScreen<NavRoute.Onboarding> { OnboardingScreen() }
        addScreen<NavRoute.CreateProfile> { CreateProfileScreen() }
        addScreen<NavRoute.TrustedNodeSetup> { TrustedNodeSetupScreen() }

        addScreen<NavRoute.TabContainer>(
            deepLinks = listOf(
                navDeepLink<NavRoute.TabContainer>(
                    basePath = getDeepLinkBasePath<NavRoute.TabContainer>()
                ),
            )
        ) { TabContainerScreen() }

        addScreen<NavRoute.OpenTrade>(
            deepLinks = listOf(
                navDeepLink<NavRoute.OpenTrade>(
                    basePath = getDeepLinkBasePath<NavRoute.OpenTrade>()
                )
            ),
        ) { backStackEntry ->
            val openTrade: NavRoute.OpenTrade = backStackEntry.toRoute()
            OpenTradeScreen(openTrade.tradeId)
        }

        addScreen<NavRoute.TradeChat>(
            navAnimation = NavAnimation.SLIDE_IN_FROM_BOTTOM,
            deepLinks = listOf(
                navDeepLink<NavRoute.TradeChat>(
                    basePath = getDeepLinkBasePath<NavRoute.TradeChat>()
                )
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
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    composable<T>(
        deepLinks = deepLinks,
        // 'enter' animation for the 'destination' screen
        enterTransition = {
            when (navAnimation) {
                NavAnimation.SLIDE_IN_FROM_RIGHT -> slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(NAV_ANIM_MS)
                )

                NavAnimation.SLIDE_IN_FROM_BOTTOM -> slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(NAV_ANIM_MS)
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
                NavAnimation.SLIDE_IN_FROM_RIGHT -> slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(NAV_ANIM_MS)
                )

                NavAnimation.SLIDE_IN_FROM_BOTTOM -> slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(NAV_ANIM_MS)
                )

                NavAnimation.FADE_IN -> fadeOut(animationSpec = tween(NAV_ANIM_MS))
            }
        }
    ) { backStackEntry ->
        Column(
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter)
        ) {
            Banner()
            content(backStackEntry)
        }
    }
}
