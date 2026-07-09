package network.bisq.mobile.presentation.common.ui.navigation.graph

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.navigation.NavUtils.getDeepLinkBasePath
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
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
import network.bisq.mobile.presentation.settings.faqs.FaqScreen
import network.bisq.mobile.presentation.settings.ignored_users.IgnoredUsersScreen
import network.bisq.mobile.presentation.settings.payment_accounts.PaymentAccountsScreen
import network.bisq.mobile.presentation.settings.reputation.ReputationScreen
import network.bisq.mobile.presentation.settings.resources.ResourcesScreen
import network.bisq.mobile.presentation.settings.settings.SettingsScreen
import network.bisq.mobile.presentation.settings.support.SupportScreen
import network.bisq.mobile.presentation.settings.user_profile.UserProfileScreen
import network.bisq.mobile.presentation.startup.create_profile.CreateProfileScreen
import network.bisq.mobile.presentation.startup.onboarding.OnboardingScreen
import network.bisq.mobile.presentation.startup.user_agreement.UserAgreementDisplayScreen
import network.bisq.mobile.presentation.startup.user_agreement.UserAgreementScreen
import network.bisq.mobile.presentation.tabs.tab.TabContainerScreen
import network.bisq.mobile.presentation.trade.trade_chat.ChatRulesScreen
import network.bisq.mobile.presentation.trade.trade_chat.TradeChatScreen
import network.bisq.mobile.presentation.trade.trade_detail.OpenTradeScreen
import kotlin.reflect.KType

const val NAV_ANIM_MS = 300

@ExcludeFromCoverage // declarative nav wiring (like the DI modules); not unit-testable in isolation
fun NavGraphBuilder.addCommonAppRoutes(animationsEnabled: () -> Boolean) {
    addScreen<NavRoute.UserAgreement>(animationsEnabled = animationsEnabled) { UserAgreementScreen() }
    addScreen<NavRoute.Onboarding>(animationsEnabled = animationsEnabled) { OnboardingScreen() }
    addScreen<NavRoute.CreateProfile>(animationsEnabled = animationsEnabled) { backStackEntry ->
        val route: NavRoute.CreateProfile = backStackEntry.toRoute()
        CreateProfileScreen(route.isOnboarding)
    }

    addScreen<NavRoute.TabContainer>(
        animationsEnabled = animationsEnabled,
        deepLinks =
            listOf(
                navDeepLink<NavRoute.TabContainer>(
                    basePath = getDeepLinkBasePath<NavRoute.TabContainer>(),
                ),
            ),
    ) { TabContainerScreen() }

    addScreen<NavRoute.OpenTrade>(
        animationsEnabled = animationsEnabled,
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
        animationsEnabled = animationsEnabled,
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
    addScreen<NavRoute.Offerbook>(animationsEnabled = animationsEnabled) { OfferbookScreen() }
    addScreen<NavRoute.ChatRules>(animationsEnabled = animationsEnabled) { ChatRulesScreen() }
    addScreen<NavRoute.Settings>(animationsEnabled = animationsEnabled) { SettingsScreen() }
    addScreen<NavRoute.Support>(animationsEnabled = animationsEnabled) { SupportScreen() }
    addScreen<NavRoute.Faqs>(animationsEnabled = animationsEnabled) { FaqScreen() }
    addScreen<NavRoute.Reputation>(animationsEnabled = animationsEnabled) { ReputationScreen() }
    addScreen<NavRoute.UserProfile>(animationsEnabled = animationsEnabled) { UserProfileScreen() }
    addScreen<NavRoute.PaymentAccounts>(animationsEnabled = animationsEnabled) { PaymentAccountsScreen() }
    addScreen<NavRoute.IgnoredUsers>(animationsEnabled = animationsEnabled) { IgnoredUsersScreen() }
    addScreen<NavRoute.Resources>(animationsEnabled = animationsEnabled) { ResourcesScreen() }
    addScreen<NavRoute.UserAgreementDisplay>(animationsEnabled = animationsEnabled) { UserAgreementDisplayScreen() }

    // --- Take Offer Screens ---
    addScreen<NavRoute.TakeOfferTradeAmount>(wizardTransition = false, animationsEnabled = animationsEnabled) { TakeOfferTradeAmountScreen() }
    addScreen<NavRoute.TakeOfferPaymentMethod>(wizardTransition = true, animationsEnabled = animationsEnabled) { TakeOfferPaymentMethodScreen() }
    addScreen<NavRoute.TakeOfferSettlementMethod>(wizardTransition = true, animationsEnabled = animationsEnabled) { TakeOfferSettlementMethodScreen() }
    addScreen<NavRoute.TakeOfferReviewTrade>(wizardTransition = true, animationsEnabled = animationsEnabled) { TakeOfferReviewTradeScreen() }

    // --- Create Offer Screens ---
    addScreen<NavRoute.CreateOfferDirection>(wizardTransition = false, animationsEnabled = animationsEnabled) { CreateOfferDirectionScreen() }
    addScreen<NavRoute.CreateOfferMarket>(wizardTransition = true, animationsEnabled = animationsEnabled) { CreateOfferMarketScreen() }
    addScreen<NavRoute.CreateOfferAmount>(wizardTransition = true, animationsEnabled = animationsEnabled) { CreateOfferAmountScreen() }
    addScreen<NavRoute.CreateOfferPrice>(wizardTransition = true, animationsEnabled = animationsEnabled) { CreateOfferPriceScreen() }
    addScreen<NavRoute.CreateOfferPaymentMethod>(wizardTransition = true, animationsEnabled = animationsEnabled) { CreateOfferPaymentMethodScreen() }
    addScreen<NavRoute.CreateOfferSettlementMethod>(wizardTransition = true, animationsEnabled = animationsEnabled) { CreateOfferSettlementMethodScreen() }
    addScreen<NavRoute.CreateOfferReviewOffer>(wizardTransition = true, animationsEnabled = animationsEnabled) { CreateOfferReviewOfferScreen() }

    // --- Trade Guide Screens ---
    addScreen<NavRoute.TradeGuideOverview>(wizardTransition = false, animationsEnabled = animationsEnabled) { TradeGuideOverview() }
    addScreen<NavRoute.TradeGuideSecurity>(wizardTransition = true, animationsEnabled = animationsEnabled) { TradeGuideSecurity() }
    addScreen<NavRoute.TradeGuideProcess>(wizardTransition = true, animationsEnabled = animationsEnabled) { TradeGuideProcess() }
    addScreen<NavRoute.TradeGuideTradeRules>(wizardTransition = true, animationsEnabled = animationsEnabled) { TradeGuideTradeRules() }

    // --- Wallet Guide Screens ---
    addScreen<NavRoute.WalletGuideIntro>(wizardTransition = false, animationsEnabled = animationsEnabled) { WalletGuideIntro() }
    addScreen<NavRoute.WalletGuideDownload>(wizardTransition = true, animationsEnabled = animationsEnabled) { WalletGuideDownload() }
    addScreen<NavRoute.WalletGuideNewWallet>(wizardTransition = true, animationsEnabled = animationsEnabled) { WalletGuideNewWallet() }
    addScreen<NavRoute.WalletGuideReceiving>(wizardTransition = true, animationsEnabled = animationsEnabled) { WalletGuideReceiving() }
}

enum class NavAnimation {
    FADE_IN,
    SLIDE_IN_FROM_RIGHT,
    SLIDE_IN_FROM_BOTTOM,
}

@ExcludeFromCoverage // Compose nav transition wiring; behaviour is exercised via the running app, not unit tests
inline fun <reified T : NavRoute> NavGraphBuilder.addScreen(
    typeMap: Map<KType, NavType<*>> = emptyMap(),
    deepLinks: List<NavDeepLink> = emptyList(),
    wizardTransition: Boolean = false,
    navAnimation: NavAnimation = if (wizardTransition) NavAnimation.FADE_IN else NavAnimation.SLIDE_IN_FROM_RIGHT,
    // Gate for the "use animations" setting. Evaluated per-navigation, so it reflects
    // the live effective value. When off, transitions are None: the destination swaps in instantly
    // and only one screen is composed at a time — avoiding the double-composition heap spike that
    // ANRs low-RAM devices on the offerbook. Defaults to enabled so any un-threaded caller is safe.
    noinline animationsEnabled: () -> Boolean = { true },
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable<T>(
        typeMap = typeMap,
        deepLinks = deepLinks,
        // 'enter' animation for the 'destination' screen
        enterTransition = {
            if (!animationsEnabled()) {
                EnterTransition.None
            } else {
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
            if (!animationsEnabled()) {
                ExitTransition.None
            } else {
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
            }
        },
    ) { backStackEntry ->
        content(backStackEntry)
    }
}
