package network.bisq.mobile.presentation.tabs.dashboard.welcome_carousel

/**
 * Actions emitted by the welcome carousel Composable.
 *
 * Primary "opt-in" actions (e.g. requesting the OS permission) require a Composable-
 * side launcher and so are NOT dispatched through this sealed class. They are invoked
 * directly from the Composable on the launcher callback the screen supplies. Only the
 * "Don't ask again" action — which is pure state persistence — is presenter-owned and
 * dispatched through here.
 */
sealed class WelcomeCarouselUiAction {
    data class OnDontAskAgain(
        val type: CarouselPageType,
    ) : WelcomeCarouselUiAction()
}
