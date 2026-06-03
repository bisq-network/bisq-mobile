package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.other_crypto

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.CryptoPaymentMethod
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.crypto.CommonCryptoFormSection
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.crypto.CryptoAccountFormUiAction
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.crypto.CryptoAccountFormUiState
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING

@Composable
fun OtherCryptoFormContent(
    presenter: OtherCryptoFormPresenter,
    paymentMethod: CryptoPaymentMethod,
    onNavigateToNextScreen: (CreatePaymentAccount) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by presenter.uiState.collectAsState()
    val currentOnNavigate by rememberUpdatedState(onNavigateToNextScreen)

    LaunchedEffect(presenter, paymentMethod) {
        presenter.initialize(paymentMethod)
    }

    LaunchedEffect(presenter) {
        presenter.effect.collect { effect ->
            when (effect) {
                is OtherCryptoFormEffect.NavigateToNextScreen -> currentOnNavigate(effect.account)
            }
        }
    }

    OtherCryptoFormContent(
        uiState = uiState,
        paymentMethod = paymentMethod,
        onAction = presenter::onCryptoCommonAction,
        modifier = modifier,
    )
}

@Composable
private fun OtherCryptoFormContent(
    uiState: OtherCryptoFormUiState,
    paymentMethod: CryptoPaymentMethod,
    onAction: (CryptoAccountFormUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        CommonCryptoFormSection(
            cryptoUiState = uiState.crypto,
            onAction = onAction,
            showAddress = true,
            showAutoConf = paymentMethod.supportAutoConf,
        )
    }
}

private fun previewPaymentMethod(
    supportAutoConf: Boolean,
): CryptoPaymentMethod =
    CryptoPaymentMethod(
        code = "ETH",
        name = "Ethereum",
        supportAutoConf = supportAutoConf,
        tradeLimitInfo = EMPTY_STRING,
        tradeDuration = EMPTY_STRING,
    )

@Preview
@Composable
private fun OtherCryptoFormContent_DefaultPreview() {
    BisqTheme.Preview {
        OtherCryptoFormContent(
            uiState =
                OtherCryptoFormUiState(
                    crypto =
                        CryptoAccountFormUiState(
                            addressEntry = DataEntry(value = "0x1234567890abcdef1234567890abcdef12345678"),
                            isInstant = true,
                            isAutoConf = false,
                        ),
                ),
            paymentMethod = previewPaymentMethod(supportAutoConf = false),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun OtherCryptoFormContent_AutoConfEnabledPreview() {
    BisqTheme.Preview {
        OtherCryptoFormContent(
            uiState =
                OtherCryptoFormUiState(
                    crypto =
                        CryptoAccountFormUiState(
                            addressEntry = DataEntry(value = "0x1234567890abcdef1234567890abcdef12345678"),
                            isInstant = false,
                            isAutoConf = true,
                            autoConfNumConfirmationsEntry = DataEntry(value = "2"),
                            autoConfMaxTradeAmountEntry = DataEntry(value = "1"),
                            autoConfExplorerUrlsEntry = DataEntry(value = "https://explorer.example.com"),
                        ),
                ),
            paymentMethod = previewPaymentMethod(supportAutoConf = true),
            onAction = {},
        )
    }
}
