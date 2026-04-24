package network.bisq.mobile.presentation.create_payment_account

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

open class CreatePaymentAccountPresenter(
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(CreatePaymentAccountUiState())
    val uiState: StateFlow<CreatePaymentAccountUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<CreatePaymentAccountEffect>()
    val effect = _effect.asSharedFlow()

    fun onAction(action: CreatePaymentAccountUiAction) {
        presenterScope.launch {
            when (action) {
                is CreatePaymentAccountUiAction.OnNavigateFromSelectPaymentMethod -> {
                    _uiState.update { it.copy(paymentMethod = action.paymentMethod) }
                    _effect.emit(CreatePaymentAccountEffect.NavigateToPaymentAccountForm)
                }

                is CreatePaymentAccountUiAction.OnNavigateFromPaymentAccountForm -> {
                    _uiState.update { it.copy(paymentAccount = action.paymentAccount) }
                    _effect.emit(CreatePaymentAccountEffect.NavigateToPaymentAccountReview)
                }
            }
        }
    }
}
