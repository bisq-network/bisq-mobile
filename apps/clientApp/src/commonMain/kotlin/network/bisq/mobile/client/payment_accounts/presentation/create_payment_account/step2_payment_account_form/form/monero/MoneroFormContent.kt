package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.monero

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.CryptoPaymentMethod
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.crypto.CommonCryptoFormSection
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.crypto.CryptoAccountFormUiAction
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.crypto.CryptoAccountFormUiState
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqSwitch
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

private val moneroSubAddressesEnabled =
    false // TODO: remove once bisq2 issue https://github.com/bisq-network/bisq2/issues/4682 is resolved

@Composable
fun MoneroFormContent(
    presenter: MoneroFormPresenter,
    paymentMethod: CryptoPaymentMethod,
    onNavigateToNextScreen: (CreatePaymentAccount) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by presenter.uiState.collectAsState()
    val currentOnNavigate by rememberUpdatedState(onNavigateToNextScreen)

    LaunchedEffect(presenter) {
        presenter.effect.collect { effect ->
            when (effect) {
                is MoneroFormEffect.NavigateToNextScreen -> currentOnNavigate(effect.account)
            }
        }
    }

    MoneroFormContent(
        uiState = uiState,
        paymentMethod = paymentMethod,
        onAction = presenter::onAction,
        onCommonAction = presenter::onCryptoCommonAction,
        modifier = modifier,
    )
}

@Composable
private fun MoneroFormContent(
    uiState: MoneroFormUiState,
    paymentMethod: CryptoPaymentMethod,
    onAction: (MoneroFormUiAction) -> Unit,
    onCommonAction: (CryptoAccountFormUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        CommonCryptoFormSection(
            cryptoUiState = uiState.crypto,
            onAction = onCommonAction,
            showAddress = !uiState.useSubAddresses,
            showAutoConf = moneroSubAddressesEnabled && paymentMethod.supportAutoConf,
        )

        // This feature is disabled for now until we get this fixed in Bisq2
        if (moneroSubAddressesEnabled) {
            BisqSwitch(
                checked = uiState.useSubAddresses,
                modifier = Modifier.padding(top = 12.dp),
                label = "paymentAccounts.crypto.address.xmr.useSubAddresses.switch".i18n(),
                onSwitch = { onAction(MoneroFormUiAction.OnUseSubAddressesChange(it)) },
            )

            if (uiState.useSubAddresses) {
                BisqTextFieldV0(
                    modifier = Modifier.padding(top = 12.dp),
                    value = uiState.mainAddressEntry.value,
                    onValueChange = { onAction(MoneroFormUiAction.OnMainAddressChange(it)) },
                    label = "paymentAccounts.crypto.address.xmr.mainAddresses".i18n(),
                    placeholder = "paymentAccounts.crypto.address.xmr.mainAddresses.prompt".i18n(),
                    isError = uiState.mainAddressEntry.errorMessage != null,
                    bottomMessage = uiState.mainAddressEntry.errorMessage,
                    singleLine = true,
                )

                BisqTextFieldV0(
                    modifier = Modifier.padding(top = 12.dp),
                    value = uiState.privateViewKeyEntry.value,
                    onValueChange = { onAction(MoneroFormUiAction.OnPrivateViewKeyChange(it)) },
                    label = "paymentAccounts.crypto.address.xmr.privateViewKey".i18n(),
                    placeholder = "paymentAccounts.crypto.address.xmr.privateViewKey.prompt".i18n(),
                    isError = uiState.privateViewKeyEntry.errorMessage != null,
                    bottomMessage = uiState.privateViewKeyEntry.errorMessage,
                    singleLine = true,
                )

                BisqTextFieldV0(
                    modifier = Modifier.padding(top = 12.dp),
                    value = uiState.accountIndexEntry.value,
                    onValueChange = { onAction(MoneroFormUiAction.OnAccountIndexChange(it)) },
                    label = "paymentAccounts.crypto.address.xmr.accountIndex".i18n(),
                    placeholder = "paymentAccounts.crypto.address.xmr.accountIndex.prompt".i18n(),
                    isError = uiState.accountIndexEntry.errorMessage != null,
                    bottomMessage = uiState.accountIndexEntry.errorMessage,
                    singleLine = true,
                )

                BisqTextFieldV0(
                    modifier = Modifier.padding(top = 12.dp),
                    value = uiState.initialSubAddressIndexEntry.value,
                    onValueChange = { onAction(MoneroFormUiAction.OnInitialSubAddressIndexChange(it)) },
                    label = "paymentAccounts.crypto.address.xmr.initialSubAddressIndex".i18n(),
                    placeholder = "paymentAccounts.crypto.address.xmr.initialSubAddressIndex.prompt".i18n(),
                    isError = uiState.initialSubAddressIndexEntry.errorMessage != null,
                    bottomMessage = uiState.initialSubAddressIndexEntry.errorMessage,
                    singleLine = true,
                )

                BisqTextFieldV0(
                    modifier = Modifier.padding(top = 12.dp),
                    value = uiState.subAddressEntry.value,
                    onValueChange = {},
                    readOnly = true,
                    label = "paymentAccounts.crypto.address.xmr.subAddress".i18n(),
                    singleLine = true,
                )
            }
        }
    }
}

@ExcludeFromCoverage
fun previewPaymentMethod(): CryptoPaymentMethod =
    CryptoPaymentMethod(
        code = "XMR",
        name = "Monero",
        supportAutoConf = true,
        tradeLimitInfo = EMPTY_STRING,
        tradeDuration = EMPTY_STRING,
    )

@Preview
@Composable
private fun MoneroFormContent_DefaultPreview() {
    BisqTheme.Preview {
        MoneroFormContent(
            uiState =
                MoneroFormUiState(
                    crypto =
                        CryptoAccountFormUiState(
                            addressEntry = DataEntry(value = "44AFFq5kSiGBoZ..."),
                            isInstant = false,
                            isAutoConf = false,
                        ),
                    useSubAddresses = false,
                ),
            paymentMethod = previewPaymentMethod(),
            onAction = {},
            onCommonAction = {},
        )
    }
}

@Preview
@Composable
private fun MoneroFormContent_SubAddressEnabledPreview() {
    BisqTheme.Preview {
        MoneroFormContent(
            uiState =
                MoneroFormUiState(
                    useSubAddresses = true,
                    mainAddressEntry = DataEntry(value = "44AFFq5kSiGBoZ..."),
                    privateViewKeyEntry = DataEntry(value = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"),
                    accountIndexEntry = DataEntry(value = "0"),
                    initialSubAddressIndexEntry = DataEntry(value = "0"),
                    subAddressEntry = DataEntry(value = "TODO: SubAddress creation not implemented yet"),
                ),
            paymentMethod = previewPaymentMethod(),
            onAction = {},
            onCommonAction = {},
        )
    }
}

@Preview
@Composable
private fun MoneroFormContent_ErrorPreview() {
    BisqTheme.Preview {
        MoneroFormContent(
            uiState =
                MoneroFormUiState(
                    crypto =
                        CryptoAccountFormUiState(
                            isAutoConf = true,
                            autoConfNumConfirmationsEntry =
                                DataEntry(
                                    value = "0",
                                    errorMessage = "validation.invalidNumber".i18n(),
                                ),
                            autoConfMaxTradeAmountEntry =
                                DataEntry(
                                    value = "2",
                                    errorMessage = "validation.invalidNumber".i18n(),
                                ),
                            autoConfExplorerUrlsEntry =
                                DataEntry(
                                    value = "x",
                                    errorMessage = "validation.tooShortOrTooLong".i18n(10, 200),
                                ),
                        ),
                    useSubAddresses = true,
                    mainAddressEntry = DataEntry(value = "", errorMessage = "validation.empty".i18n()),
                    privateViewKeyEntry =
                        DataEntry(
                            value = "abc",
                            errorMessage = "validation.tooShortOrTooLong".i18n(10, 200),
                        ),
                    accountIndexEntry = DataEntry(value = "100001", errorMessage = "validation.invalidNumber".i18n()),
                    initialSubAddressIndexEntry =
                        DataEntry(
                            value = "x",
                            errorMessage = "validation.invalidNumber".i18n(),
                        ),
                ),
            paymentMethod = previewPaymentMethod(),
            onAction = {},
            onCommonAction = {},
        )
    }
}
