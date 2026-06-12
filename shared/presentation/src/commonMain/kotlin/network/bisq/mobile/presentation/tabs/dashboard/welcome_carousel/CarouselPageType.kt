package network.bisq.mobile.presentation.tabs.dashboard.welcome_carousel

/**
 * Identifies a card in the dashboard welcome carousel.
 *
 * The presenter decides — based on persisted state, platform, and runtime conditions —
 * which subset of values appears in the pending list for the current session. Order
 * here is also the display order when multiple cards are pending.
 */
enum class CarouselPageType {
    NOTIFICATIONS,
    BATTERY,
    ANALYTICS,
}
