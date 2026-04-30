package network.bisq.mobile.presentation.create_payment_account.account_review

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.data.service.accounts.PaymentAccountsServiceFacade
import network.bisq.mobile.domain.model.account.PaymentAccount
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

class PaymentAccountReviewPresenter(
    private val paymentAccountsServiceFacade: PaymentAccountsServiceFacade,
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    private val _effect = MutableSharedFlow<PaymentAccountReviewEffect>()
    val effect = _effect.asSharedFlow()

    private var createAccountJob: Job? = null

    fun onAction(action: PaymentAccountReviewUiAction) {
        when (action) {
            is PaymentAccountReviewUiAction.OnCreateAccountClick -> {
                onCreateAccount(action.account)
            }
        }
    }

    private fun onCreateAccount(account: PaymentAccount) {
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
