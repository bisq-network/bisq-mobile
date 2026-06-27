package network.bisq.mobile.presentation.settings.faqs

sealed interface FaqUiAction {
    data object OnWantToKnowMoreClick : FaqUiAction
}
