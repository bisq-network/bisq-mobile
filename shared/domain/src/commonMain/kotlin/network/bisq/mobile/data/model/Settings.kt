package network.bisq.mobile.data.model

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.model.market.MarketFilter
import network.bisq.mobile.data.model.market.MarketSortBy

@Serializable
data class Settings(
    val firstLaunch: Boolean = true,
    val showChatRulesWarnBox: Boolean = true,
    val selectedMarketCode: String = "BTC/USD",
    val notificationPermissionState: PermissionState = PermissionState.NOT_GRANTED,
    val batteryOptimizationState: BatteryOptimizationState = BatteryOptimizationState.NOT_IGNORED,
    val pushNotificationsEnabled: Boolean = false,
    val keepConnectedInBackground: Boolean = false,
    val marketSortBy: MarketSortBy = MarketSortBy.MostOffers,
    val marketFilter: MarketFilter = MarketFilter.All,
    val dontShowAgainHyperlinksOpenInBrowser: Boolean = false,
    val cookiePermitOpeningBrowser: Boolean = false,
    // Opt-in analytics (issue #525). The actual emission gate consulted on
    // every track/captureException via `runtimeOptInProvider` in DI. Default
    // OFF — privacy contract is opt-in, never opt-out.
    val analyticsEnabled: Boolean = false,
    // Has the welcome carousel's analytics page been resolved (either via
    // "Enable" or "Don't ask again")? When false, the dashboard promo will
    // include the analytics page; once true, it never auto-prompts again
    // (user can still flip the toggle manually from Settings).
    val analyticsPromptSeen: Boolean = false,
)

@Serializable
enum class PermissionState {
    NOT_GRANTED,
    GRANTED,
    DENIED,
    DONT_ASK_AGAIN,
}

@Serializable
enum class BatteryOptimizationState {
    NOT_IGNORED,
    IGNORED,
    DONT_ASK_AGAIN,
}
