package network.bisq.mobile.presentation.ui.navigation.graph

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import network.bisq.mobile.presentation.ui.uicases.offers.takeOffer.TakeOfferPaymentMethodScreen
import network.bisq.mobile.presentation.ui.navigation.*
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.uicases.*
import network.bisq.mobile.presentation.ui.uicases.offers.OffersListScreen
import network.bisq.mobile.presentation.ui.uicases.offers.takeOffer.TakeOfferReviewTradeScreen
import network.bisq.mobile.presentation.ui.uicases.offers.takeOffer.TakeOfferTradeAmountScreen
import network.bisq.mobile.presentation.ui.uicases.startup.CreateProfileScreen
import network.bisq.mobile.presentation.ui.uicases.startup.OnBoardingScreen
import network.bisq.mobile.presentation.ui.uicases.startup.SplashScreen
import network.bisq.mobile.presentation.ui.uicases.startup.TrustedNodeSetupScreen
import network.bisq.mobile.presentation.ui.uicases.trades.TradeFlowScreen
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@Composable
fun RootNavGraph() {
    val navController: NavHostController = koinInject(named("RootNavController"))

    NavHost(
        modifier = Modifier.background(color = BisqTheme.colors.backgroundColor),
        navController = navController,
        startDestination = Routes.Splash.name,
    ) {
        composable(route = Routes.Splash.name) {
            SplashScreen()
        }

        addScreen(Routes.Onboarding.name) {
            OnBoardingScreen()
        }

        addScreen(Routes.CreateProfile.name) {
            CreateProfileScreen()
        }

        addScreen(Routes.TrustedNodeSetup.name) {
            TrustedNodeSetupScreen()
        }

        addScreen(route = Routes.TabContainer.name) {
            TabContainerScreen()
        }

        addScreen(Routes.OfferList.name) {
            OffersListScreen()
        }

        addScreen(Routes.TakeOfferTradeAmount.name) {
            TakeOfferTradeAmountScreen()
        }

        addScreen(Routes.TakeOfferPaymentMethod.name) {
            TakeOfferPaymentMethodScreen()
        }

        addScreen(Routes.TakeOfferReviewTrade.name) {
            TakeOfferReviewTradeScreen()
        }

        addScreen(Routes.TradeFlow.name) {
            TradeFlowScreen()
        }

    }
}

fun NavGraphBuilder.addScreen(
    route: String,
    content: @Composable () -> Unit
) {
    composable(
        route = route,
        enterTransition = {
            // When a screen is pushed in, slide in from right edge of the screen to left
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
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
            // When current screen is poped out, slide if from screen to screen's right edge
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        }

    ) {
        content()
    }
}