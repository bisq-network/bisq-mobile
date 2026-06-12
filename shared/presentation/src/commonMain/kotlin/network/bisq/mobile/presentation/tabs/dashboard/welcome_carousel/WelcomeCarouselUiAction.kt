package network.bisq.mobile.presentation.tabs.dashboard.welcome_carousel

/**
 * Actions emitted by the welcome carousel Composable.
 *
 * OS-coupled "opt-in" actions (requesting notification permission, opening battery
 * settings) need a Composable-side launcher and are NOT dispatched through this
 * sealed class — they're invoked directly from the screen on the launcher callback.
 *
 * Pure state actions (Don't ask again, Enable analytics, Learn more) are presenter-
 * owned and dispatched through here.
 */
sealed class WelcomeCarouselUiAction {
    data class OnDontAskAgain(
        val type: CarouselPageType,
    ) : WelcomeCarouselUiAction()

    /**
     * User accepted the analytics opt-in from the carousel. Persists both the
     * enabled flag and the prompt-seen flag so the carousel won't re-prompt and
     * the runtime opt-in provider picks up the new state on the next track() call.
     */
    data object OnEnableAnalytics : WelcomeCarouselUiAction()

    /**
     * User tapped "Learn more" on the analytics card — opens the external wiki
     * page describing what's collected and how it's handled. Same URL as the
     * Settings → Analytics "Learn more" link.
     */
    data object OnAnalyticsLearnMore : WelcomeCarouselUiAction()
}
