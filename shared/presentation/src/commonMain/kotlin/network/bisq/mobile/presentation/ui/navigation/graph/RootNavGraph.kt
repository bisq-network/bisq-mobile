package network.bisq.mobile.presentation.ui.navigation.graph

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.uicases.TabContainerScreen
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferAmountSelectorScreen
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferCurrencySelectorScreen
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferDirectionScreen
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferSettlementMethodScreen
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPaymentMethodScreen
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferReviewOfferScreen
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
import network.bisq.mobile.presentation.ui.uicases.settings.AboutScreen
import network.bisq.mobile.presentation.ui.uicases.settings.GeneralSettingsScreen
import network.bisq.mobile.presentation.ui.uicases.settings.PaymentAccountSettingsScreen
import network.bisq.mobile.presentation.ui.uicases.settings.UserProfileSettingsScreen
import network.bisq.mobile.presentation.ui.uicases.startup.AgreementScreen
import network.bisq.mobile.presentation.ui.uicases.startup.CreateProfileScreen
import network.bisq.mobile.presentation.ui.uicases.startup.OnBoardingScreen
import network.bisq.mobile.presentation.ui.uicases.startup.SplashScreen
import network.bisq.mobile.presentation.ui.uicases.startup.TrustedNodeSetupScreen
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferPaymentMethodScreen
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferReviewTradeScreen
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferTradeAmountScreen

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

        addScreen(Routes.TrustedNodeSettings.name, content = { TrustedNodeSetupScreen(false) })

        val startupScreens: List<Pair<Routes, @Composable () -> Unit>> = listOf(
            Routes.Agreement to { AgreementScreen() },
            Routes.Onboarding to { OnBoardingScreen() },
            Routes.CreateProfile to { CreateProfileScreen() },
            Routes.TrustedNodeSetup to { TrustedNodeSetupScreen() },
            Routes.TabContainer to { TabContainerScreen() },
        )
        startupScreens.forEach{ (route, screen): Pair<Routes, @Composable () -> Unit> ->
            addScreen(route.name, content = screen)
        }

        val otherScreens: List<Pair<Routes, @Composable () -> Unit>> = listOf(
            Routes.OffersByMarket to { OfferbookScreen() },
            Routes.OpenTrade to { OpenTradeScreen() },
            Routes.TradeChat to { TradeChatScreen() },
            Routes.ChatRules to { ChatRulesScreen() },
            Routes.GeneralSettings to { GeneralSettingsScreen() },
            Routes.UserProfileSettings to { UserProfileSettingsScreen() },
            Routes.PaymentAccountSettings to { PaymentAccountSettingsScreen() },
            Routes.About to { AboutScreen() },
        )
        otherScreens.forEach{ (route, screen): Pair<Routes, @Composable () -> Unit> ->
            addScreen(route.name, content = screen)
        }

        val takeOfferScreens: List<Pair<Routes, @Composable () -> Unit>> = listOf(
            Routes.TakeOfferTradeAmount to { TakeOfferTradeAmountScreen() },
            Routes.TakeOfferPaymentMethod to { TakeOfferPaymentMethodScreen() },
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

fun NavGraphBuilder.addScreen(
    route: String,
    wizardTransition: Boolean = false,
    content: @Composable () -> Unit
) {
    composable(
        route = route,
        enterTransition = {
            if (wizardTransition) {
                // When user presses 'Next', fadeIn the next step screen
                fadeIn(animationSpec = tween(150))
            } else {
                // When a screen is pushed in, slide in from right edge of the screen to left
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            }
        },
        exitTransition = {
            // When a new screen is pushed over current screen, don't do exit animation
            null
        },
        popEnterTransition = {
            // When the new pushed screen is poppped out, don't do pop Enter animation
            null
        },
        popExitTransition = {
            if (wizardTransition) {
                // When user presses 'Back', fadeOut the current step screen
                fadeOut(animationSpec = tween(150))
            } else {
                // When current screen is poped out, slide if from screen to screen's right edge
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        }

    ) {
        content()
    }
}
