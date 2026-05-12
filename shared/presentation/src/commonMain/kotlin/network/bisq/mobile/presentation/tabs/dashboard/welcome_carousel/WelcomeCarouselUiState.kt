package network.bisq.mobile.presentation.tabs.dashboard.welcome_carousel

/**
 * State exposed by [WelcomeCarouselPresenter] to the Composable layer.
 *
 * [pages] is the live list of still-pending opt-in cards, ordered for display.
 * The carousel shows the first entry and reactively transitions when the list
 * changes (e.g. NOTIFICATIONS resolves → BATTERY appears as soon as it becomes
 * pending). Empty means the carousel must not render.
 */
data class WelcomeCarouselUiState(
    val pages: List<CarouselPageType> = emptyList(),
)
