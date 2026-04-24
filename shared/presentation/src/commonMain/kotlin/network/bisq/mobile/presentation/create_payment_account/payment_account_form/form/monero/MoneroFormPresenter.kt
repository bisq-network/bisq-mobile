package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.monero

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.model.account.crypto.MoneroAccount
import network.bisq.mobile.domain.model.account.crypto.MoneroAccountPayload
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.MoneroFormUiAction
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.crypto.CryptoAccountFormPresenter
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.crypto.CryptoAccountFormUiState
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.crypto.validateOptionalIntInRange
import network.bisq.mobile.presentation.main.MainPresenter

const val SUB_ADDRESS_PLACEHOLDER = "TODO: SubAddress creation not implemented yet"

private const val MONERO_TEXT_MIN_LENGTH = 10
private const val MONERO_TEXT_MAX_LENGTH = 200
private const val MONERO_INDEX_MIN = 0
private const val MONERO_INDEX_MAX = 100000

open class MoneroFormPresenter(
    mainPresenter: MainPresenter,
) : CryptoAccountFormPresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(MoneroFormUiState())
    override val uiState: StateFlow<MoneroFormUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<MoneroFormEffect>()
    val effect = _effect.asSharedFlow()

    override fun updateCryptoUiState(transform: (CryptoAccountFormUiState) -> CryptoAccountFormUiState) {
        _uiState.update { it.copy(crypto = transform(it.crypto)) }
    }

    override fun onCryptoCustomAction(action: AccountFormUiAction) {
        when (action) {
            is MoneroFormUiAction.OnUseSubAddressesChange -> {
                _uiState.update {
                    it.copy(
                        useSubAddresses = action.value,
                        subAddressEntry = it.subAddressEntry.updateValue(SUB_ADDRESS_PLACEHOLDER),
                    )
                }
            }

            is MoneroFormUiAction.OnMainAddressChange -> {
                _uiState.update {
                    it.copy(
                        mainAddressEntry = it.mainAddressEntry.updateValue(action.value),
                        subAddressEntry =
                            if (it.useSubAddresses) {
                                it.subAddressEntry.updateValue(SUB_ADDRESS_PLACEHOLDER)
                            } else {
                                it.subAddressEntry
                            },
                    )
                }
            }

            is MoneroFormUiAction.OnPrivateViewKeyChange -> {
                _uiState.update {
                    it.copy(
                        privateViewKeyEntry = it.privateViewKeyEntry.updateValue(action.value),
                        subAddressEntry =
                            if (it.useSubAddresses) {
                                it.subAddressEntry.updateValue(SUB_ADDRESS_PLACEHOLDER)
                            } else {
                                it.subAddressEntry
                            },
                    )
                }
            }

            is MoneroFormUiAction.OnAccountIndexChange -> {
                _uiState.update {
                    it.copy(
                        accountIndexEntry = it.accountIndexEntry.updateValue(action.value),
                        subAddressEntry =
                            if (it.useSubAddresses) {
                                it.subAddressEntry.updateValue(SUB_ADDRESS_PLACEHOLDER)
                            } else {
                                it.subAddressEntry
                            },
                    )
                }
            }

            is MoneroFormUiAction.OnInitialSubAddressIndexChange -> {
                _uiState.update {
                    it.copy(
                        initialSubAddressIndexEntry =
                            it.initialSubAddressIndexEntry.updateValue(action.value),
                        subAddressEntry =
                            if (it.useSubAddresses) {
                                it.subAddressEntry.updateValue(SUB_ADDRESS_PLACEHOLDER)
                            } else {
                                it.subAddressEntry
                            },
                    )
                }
            }

            else -> Unit
        }
    }

    override fun onNextClick() {
        val validatedState =
            _uiState.updateAndGet { current ->
                val validatedCrypto =
                    validateCryptoAutoConfState(
                        current = current.crypto,
                    ).copy(
                        addressEntry =
                            if (current.useSubAddresses) {
                                current.crypto.addressEntry.clearError()
                            } else {
                                current.crypto.addressEntry.validate()
                            },
                    )

                current.copy(
                    crypto = validatedCrypto,
                    mainAddressEntry =
                        if (current.useSubAddresses) {
                            current.mainAddressEntry.validate()
                        } else {
                            current.mainAddressEntry.clearError()
                        },
                    privateViewKeyEntry =
                        if (current.useSubAddresses) {
                            current.privateViewKeyEntry.validate()
                        } else {
                            current.privateViewKeyEntry.clearError()
                        },
                    accountIndexEntry =
                        if (current.useSubAddresses) {
                            current.accountIndexEntry.validate()
                        } else {
                            current.accountIndexEntry.clearError()
                        },
                    initialSubAddressIndexEntry =
                        if (current.useSubAddresses) {
                            current.initialSubAddressIndexEntry.validate()
                        } else {
                            current.initialSubAddressIndexEntry.clearError()
                        },
                    subAddressEntry =
                        if (current.useSubAddresses) {
                            current.subAddressEntry.updateValue(SUB_ADDRESS_PLACEHOLDER).clearError()
                        } else {
                            current.subAddressEntry.clearError()
                        },
                )
            }

        val validatedAccountName = validateUniqueAccountNameEntry()
        val isValid =
            validatedAccountName.isValid &&
                validatedState.crypto.addressEntry.isValid &&
                isCryptoAutoConfStateValid(validatedState.crypto) &&
                validatedState.mainAddressEntry.isValid &&
                validatedState.privateViewKeyEntry.isValid &&
                validatedState.accountIndexEntry.isValid &&
                validatedState.initialSubAddressIndexEntry.isValid

        if (!isValid) {
            return
        }

        val payload =
            MoneroAccountPayload(
                address =
                    if (validatedState.useSubAddresses) {
                        validatedState.mainAddressEntry.value.trim()
                    } else {
                        validatedState.crypto.addressEntry.value
                            .trim()
                    },
                isInstant = validatedState.crypto.isInstant,
                isAutoConf = validatedState.crypto.isAutoConf,
                autoConfNumConfirmations =
                    if (validatedState.crypto.isAutoConf) {
                        validatedState.crypto.autoConfNumConfirmationsEntry.value
                            .trim()
                            .toIntOrNull()
                    } else {
                        null
                    },
                autoConfMaxTradeAmount =
                    if (validatedState.crypto.isAutoConf) {
                        validatedState.crypto.autoConfMaxTradeAmountEntry.value
                            .trim()
                            .toLongOrNull()
                    } else {
                        null
                    },
                autoConfExplorerUrls =
                    if (validatedState.crypto.isAutoConf) {
                        validatedState.crypto.autoConfExplorerUrlsEntry.value
                            .trim()
                            .ifBlank { null }
                    } else {
                        null
                    },
                useSubAddresses = validatedState.useSubAddresses,
                mainAddress =
                    if (validatedState.useSubAddresses) {
                        validatedState.mainAddressEntry.value
                            .trim()
                            .ifBlank { null }
                    } else {
                        null
                    },
                privateViewKey =
                    if (validatedState.useSubAddresses) {
                        validatedState.privateViewKeyEntry.value
                            .trim()
                            .ifBlank { null }
                    } else {
                        null
                    },
                subAddress =
                    if (validatedState.useSubAddresses) {
                        null
                    } else {
                        validatedState.subAddressEntry.value
                            .trim()
                            .takeUnless { it == SUB_ADDRESS_PLACEHOLDER }
                            ?.ifBlank { null }
                    },
                accountIndex =
                    if (validatedState.useSubAddresses) {
                        validatedState.accountIndexEntry.value
                            .trim()
                            .toIntOrNull()
                    } else {
                        null
                    },
                initialSubAddressIndex =
                    if (validatedState.useSubAddresses) {
                        validatedState.initialSubAddressIndexEntry.value
                            .trim()
                            .toIntOrNull()
                    } else {
                        null
                    },
            )

        presenterScope.launch {
            _effect.emit(
                MoneroFormEffect.NavigateToNextScreen(
                    account =
                        MoneroAccount(
                            accountName = uniqueAccountNameEntry.value.value.trim(),
                            accountPayload = payload,
                            creationDate = null,
                            tradeLimitInfo = null,
                            tradeDuration = null,
                        ),
                ),
            )
        }
    }
}

internal fun validateMainAddress(value: String): String? = validateTextMinMaxLength(value)

internal fun validatePrivateViewKey(value: String): String? = validateTextMinMaxLength(value)

internal fun validateAccountIndex(value: String): String? = validateOptionalIntInRange(value, MONERO_INDEX_MIN, MONERO_INDEX_MAX)

internal fun validateInitialSubAddressIndex(value: String): String? = validateOptionalIntInRange(value, MONERO_INDEX_MIN, MONERO_INDEX_MAX)

internal fun validateTextMinMaxLength(value: String): String? {
    val trimmed = value.trim()

    return if (trimmed.length !in MONERO_TEXT_MIN_LENGTH..MONERO_TEXT_MAX_LENGTH) {
        "validation.tooShortOrTooLong".i18n(MONERO_TEXT_MIN_LENGTH, MONERO_TEXT_MAX_LENGTH)
    } else {
        null
    }
}
