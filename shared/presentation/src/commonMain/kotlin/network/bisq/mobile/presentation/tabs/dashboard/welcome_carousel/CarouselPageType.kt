package network.bisq.mobile.presentation.tabs.dashboard.welcome_carousel

/**
 * Identifies a card in the dashboard welcome carousel.
 *
 * Extend this enum when adding a new opt-in card (e.g. ANALYTICS in a follow-up PR).
 * The presenter decides — based on persisted state, platform, and runtime conditions —
 * which subset of values appears in the pending list for the current session.
 */
enum class CarouselPageType {
    NOTIFICATIONS,
    BATTERY,
}
