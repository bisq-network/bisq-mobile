package network.bisq.mobile.presentation.ui.navigation.graph

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.navigation.Routes.Companion.getDeeplinkUriPattern
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.ui.uicases.TabContainerScreen
import network.bisq.mobile.presentation.ui.uicases.banners.Banner
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferAmountSelectorScreen
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferCurrencySelectorScreen
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferDirectionScreen
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPaymentMethodScreen
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferReviewOfferScreen
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferSettlementMethodScreen
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferTradePriceSelectorScreen
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
import network.bisq.mobile.presentation.ui.uicases.startup.OnBoardingScreen
import network.bisq.mobile.presentation.ui.uicases.startup.SplashScreen
import network.bisq.mobile.presentation.ui.uicases.startup.TrustedNodeSetupScreen
import network.bisq.mobile.presentation.ui.uicases.startup.UserAgreementDisplayScreen
import network.bisq.mobile.presentation.ui.uicases.startup.UserAgreementScreen
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferPaymentMethodScreen
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferReviewTradeScreen
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferSettlementMethodScreen
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferTradeAmountScreen

private const val NAV_ANIM_MS = 300

@Composable
fun RootNavGraph(rootNavController: NavHostController) {
    NavHost(
        modifier = Modifier.background(color = BisqTheme.colors.backgroundColor),
        navController = rootNavController,
        startDestination = Routes.Splash.name,
    ) {
        composable(route = Routes.Splash.name) {
            SplashScreen()
        }

        addScreen(Routes.TrustedNodeSettings.name) { TrustedNodeSetupScreen(false) }
        addScreen(Routes.UserAgreement.name) { UserAgreementScreen() }
        addScreen(Routes.Onboarding.name) { OnBoardingScreen() }
        addScreen(Routes.CreateProfile.name) { CreateProfileScreen() }
        addScreen(Routes.TrustedNodeSetup.name) { TrustedNodeSetupScreen() }

        addScreen(
            Routes.TabContainer.name,
            deepLinks = listOf(
                 navDeepLink { uriPattern = getDeeplinkUriPattern(Routes.TabContainer) } // TODO fix with refactor
            ),
        ) {
            TabContainerScreen()
        }

        addScreen(
            Routes.OpenTrade.name,
            deepLinks = listOf(
                // navDeepLink { uriPattern = getDeeplinkUriPattern(Routes.OpenTrade) } // TODO implement properly with refactor (use "tradeId" in pattern)
            ),
        ) {
            OpenTradeScreen()
        }
        addScreen(
            Routes.TradeChat.name,
            navAnimation = NavAnimation.SLIDE_IN_FROM_BOTTOM,
            deepLinks = listOf(
                // navDeepLink { uriPattern = getDeeplinkUriPattern(Routes.TradeChat) } // TODO implement properly with refactor (use "tradeId" in pattern)
            ),
        ) {
            TradeChatScreen()
        }

        val otherScreens: List<Pair<Routes, @Composable () -> Unit>> = listOf(
            Routes.OffersByMarket to { OfferbookScreen() },
            Routes.ChatRules to { ChatRulesScreen() },
            Routes.Settings to { SettingsScreen() },
            Routes.Support to { SupportScreen() },
            Routes.Reputation to { ReputationScreen() },
            Routes.UserProfile to { UserProfileScreen() },
            Routes.PaymentAccounts to { PaymentAccountsScreen() },
            Routes.IgnoredUsers to { IgnoredUsersScreen() },
            Routes.Resources to { ResourcesScreen() },
            Routes.UserAgreementDisplay to { UserAgreementDisplayScreen() },
        )
        otherScreens.forEach { (route, screen): Pair<Routes, @Composable () -> Unit> ->
            addScreen(route.name, content = screen)
        }

        val takeOfferScreens: List<Pair<Routes, @Composable () -> Unit>> = listOf(
            Routes.TakeOfferTradeAmount to { TakeOfferTradeAmountScreen() },
            Routes.TakeOfferQuoteSidePaymentMethod to { TakeOfferPaymentMethodScreen() },
            Routes.TakeOfferBaseSidePaymentMethod to { TakeOfferSettlementMethodScreen() },
            Routes.TakeOfferReviewTrade to { TakeOfferReviewTradeScreen() },
        )
        takeOfferScreens.forEachIndexed { i: Int, (route, screen): Pair<Routes, @Composable () -> Unit> ->
            addScreen(route.name, content = screen, wizardTransition = i != 0)
        }

        val createOfferScreens: List<Pair<Routes, @Composable () -> Unit>> = listOf(
            Routes.CreateOfferDirection to { CreateOfferDirectionScreen() },
            Routes.CreateOfferMarket to { CreateOfferCurrencySelectorScreen() },
            Routes.CreateOfferAmount to { CreateOfferAmountSelectorScreen() },
            Routes.CreateOfferPrice to { CreateOfferTradePriceSelectorScreen() },
            Routes.CreateOfferQuoteSidePaymentMethod to { CreateOfferPaymentMethodScreen() },
            Routes.CreateOfferBaseSidePaymentMethod to { CreateOfferSettlementMethodScreen() },
            Routes.CreateOfferReviewOffer to { CreateOfferReviewOfferScreen() },
        )
        createOfferScreens.forEachIndexed { i: Int, (route, screen): Pair<Routes, @Composable () -> Unit> ->
            addScreen(route.name, content = screen, wizardTransition = i != 0)
        }

        val tradeGuideScreens: List<Pair<Routes, @Composable () -> Unit>> = listOf(
            Routes.TradeGuideOverview to { TradeGuideOverview() },
            Routes.TradeGuideSecurity to { TradeGuideSecurity() },
            Routes.TradeGuideProcess to { TradeGuideProcess() },
            Routes.TradeGuideTradeRules to { TradeGuideTradeRules() },
        )
        tradeGuideScreens.forEachIndexed { i: Int, (route, screen): Pair<Routes, @Composable () -> Unit> ->
            addScreen(route.name, content = screen, wizardTransition = i != 0)
        }

        val walletGuideScreens: List<Pair<Routes, @Composable () -> Unit>> = listOf(
            Routes.WalletGuideIntro to { WalletGuideIntro() },
            Routes.WalletGuideDownload to { WalletGuideDownload() },
            Routes.WalletGuideNewWallet to { WalletGuideNewWallet() },
            Routes.WalletGuideReceiving to { WalletGuideReceiving() },
        )
        walletGuideScreens.forEachIndexed { i: Int, (route, screen): Pair<Routes, @Composable () -> Unit> ->
            addScreen(route.name, content = screen, wizardTransition = i != 0)
        }
    }
}

enum class NavAnimation {
    FADE_IN,
    SLIDE_IN_FROM_RIGHT,
    SLIDE_IN_FROM_BOTTOM,
}

fun NavGraphBuilder.addScreen(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    wizardTransition: Boolean = false,
    navAnimation: NavAnimation = if (wizardTransition) NavAnimation.FADE_IN else NavAnimation.SLIDE_IN_FROM_RIGHT,
    content: @Composable () -> Unit
) {
    composable(
        route = route,
        arguments = arguments,
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

    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter)
        ) {
            Banner()
            content()
        }
    }
}
