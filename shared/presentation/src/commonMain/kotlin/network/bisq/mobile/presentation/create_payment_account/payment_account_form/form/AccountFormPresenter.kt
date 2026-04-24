package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.presentation.main.MainPresenter

abstract class AccountFormPresenter(
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    private val _uniqueAccountNameEntry = MutableStateFlow(DataEntry(validator = ::validateUniqueAccountName))
    val uniqueAccountNameEntry: StateFlow<DataEntry> = _uniqueAccountNameEntry.asStateFlow()

    abstract val uiState: StateFlow<AccountFormUiState>

    fun onAction(action: AccountFormUiAction) {
        when (action) {
            is AccountFormUiAction.OnUniqueAccountNameChange -> {
                _uniqueAccountNameEntry.update {
                    it.updateValue(action.value)
                }
            }

            AccountFormUiAction.OnNextClick -> onNextClick()
            else -> onCustomAction(action)
        }
    }

    protected fun validateUniqueAccountNameEntry(): DataEntry {
        val validated = uniqueAccountNameEntry.value.validate()
        _uniqueAccountNameEntry.value = validated
        return validated
    }

    protected open fun onCustomAction(action: AccountFormUiAction) = Unit

    protected abstract fun onNextClick()
}

internal fun validateUniqueAccountName(value: String): String? {
    val trimmed = value.trim()

    try {
        PaymentAccountValidation.validateUniqueAccountName(trimmed)
    } catch (_: IllegalArgumentException) {
        return "validation.tooShortOrTooLong".i18n(
            PaymentAccountValidation.ACCOUNT_NAME_MIN_LENGTH,
            PaymentAccountValidation.ACCOUNT_NAME_MAX_LENGTH,
        )
    }

    return null
}
