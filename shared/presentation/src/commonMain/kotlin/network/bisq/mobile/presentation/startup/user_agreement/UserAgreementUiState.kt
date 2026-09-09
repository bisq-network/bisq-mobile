package network.bisq.mobile.presentation.startup.user_agreement

/** @param isAccepted the checkbox state; accepting the terms is only enabled while it is set. */
data class UserAgreementUiState(
    val isAccepted: Boolean = false,
)
