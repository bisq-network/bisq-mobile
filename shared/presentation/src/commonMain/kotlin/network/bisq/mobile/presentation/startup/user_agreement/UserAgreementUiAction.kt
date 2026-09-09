package network.bisq.mobile.presentation.startup.user_agreement

sealed interface UserAgreementUiAction {
    data class OnAcceptedChange(
        val accepted: Boolean,
    ) : UserAgreementUiAction

    data object OnAcceptTerms : UserAgreementUiAction
}
