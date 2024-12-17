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
import network.bisq.mobile.presentation.ui.uicases.offers.takeOffer.TakeOfferPaymentMethodScreen
import network.bisq.mobile.presentation.ui.navigation.*
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.uicases.*
import network.bisq.mobile.presentation.ui.uicases.offers.OffersListScreen
import network.bisq.mobile.presentation.ui.uicases.offers.createOffer.*
import network.bisq.mobile.presentation.ui.uicases.offers.takeOffer.TakeOfferReviewTradeScreen
import network.bisq.mobile.presentation.ui.uicases.offers.takeOffer.TakeOfferTradeAmountScreen
import network.bisq.mobile.presentation.ui.uicases.settings.UserProfileSettingsScreen
import network.bisq.mobile.presentation.ui.uicases.startup.CreateProfileScreen
import network.bisq.mobile.presentation.ui.uicases.startup.OnBoardingScreen
import network.bisq.mobile.presentation.ui.uicases.startup.SplashScreen
import network.bisq.mobile.presentation.ui.uicases.startup.TrustedNodeSetupScreen
import network.bisq.mobile.presentation.ui.uicases.trades.TradeFlowScreen

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

        addScreen(Routes.TakeOfferPaymentMethod.name, wizardTransition = true) {
            TakeOfferPaymentMethodScreen()
        }

        addScreen(Routes.TakeOfferReviewTrade.name, wizardTransition = true) {
            TakeOfferReviewTradeScreen()
        }

        addScreen(Routes.TradeFlow.name) {
            TradeFlowScreen()
        }

        addScreen(Routes.CreateOfferBuySell.name) {
            CreateOfferBuySellScreen()
        }

        addScreen(Routes.CreateOfferCurrency.name, wizardTransition = true) {
            CreateOfferCurrencySelectorScreen()
        }

        addScreen(Routes.CreateOfferAmount.name, wizardTransition = true) {
            CreateOfferAmountSelectorScreen()
        }

        addScreen(Routes.CreateOfferTradePrice.name, wizardTransition = true) {
            CreateOfferTradePriceSelectorScreen()
        }

        addScreen(Routes.CreateOfferPaymentMethod.name, wizardTransition = true) {
            CreateOfferPaymentMethodSelectorScreen()
        }

        addScreen(Routes.CreateOfferReviewOffer.name, wizardTransition = true) {
            CreateOfferReviewOfferScreen()
        }

        composable(Routes.UserProfileSettings.name) {
            UserProfileSettingsScreen(showBackNavigation = true)
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
            } else  {
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
