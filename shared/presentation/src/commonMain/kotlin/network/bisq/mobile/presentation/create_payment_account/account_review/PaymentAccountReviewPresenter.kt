package network.bisq.mobile.presentation.create_payment_account.account_review

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.data.service.accounts.PaymentAccountsServiceFacade
import network.bisq.mobile.domain.model.account.PaymentMethod
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.create_payment_account.core.mapping.toReviewPaymentAccount
import network.bisq.mobile.presentation.main.MainPresenter

class PaymentAccountReviewPresenter(
    private val paymentAccountsServiceFacade: PaymentAccountsServiceFacade,
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(PaymentAccountReviewUiState())
    val uiState: StateFlow<PaymentAccountReviewUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<PaymentAccountReviewEffect>()
    val effect = _effect.asSharedFlow()

    private var createAccountJob: Job? = null

    fun initialize(
        createPaymentAccount: CreatePaymentAccount,
        paymentMethod: PaymentMethod,
    ) {
        _uiState.update {
            it.copy(
                isLoading = false,
                paymentAccount = createPaymentAccount.toReviewPaymentAccount(paymentMethod),
            )
        }
    }

    fun onAction(action: PaymentAccountReviewUiAction) {
        when (action) {
            is PaymentAccountReviewUiAction.OnCreateAccountClick -> {
                onCreateAccount(action.account)
            }
        }
    }

    private fun onCreateAccount(account: CreatePaymentAccount) {
        if (createAccountJob?.isActive == true) return
        createAccountJob =
            presenterScope.launch {
                showLoading()
                paymentAccountsServiceFacade
                    .addAccount(account)
                    .onSuccess {
                        _effect.emit(PaymentAccountReviewEffect.CloseCreateAccountFlow)
                    }.onFailure {
                        handleError(it)
                    }
                hideLoading()
            }
    }
}
