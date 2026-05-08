package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.other_crypto

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.domain.model.account.PaymentAccount
import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.crypto.CommonCryptoFormSection
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.crypto.CryptoAccountFormUiState
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.CryptoPaymentMethodVO

@Composable
fun OtherCryptoPaymentAccountFormContent(
    presenter: OtherCryptoFormPresenter,
    paymentMethod: CryptoPaymentMethodVO,
    onNavigateToNextScreen: (PaymentAccount) -> Unit,
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
        onAction = presenter::onAction,
        modifier = modifier,
    )
}

@Composable
fun OtherCryptoFormContent(
    uiState: OtherCryptoFormUiState,
    paymentMethod: CryptoPaymentMethodVO,
    onAction: (AccountFormUiAction) -> Unit,
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
): CryptoPaymentMethodVO =
    CryptoPaymentMethodVO(
        paymentType = PaymentTypeVO.ETH,
        code = "ETH",
        name = "Ethereum",
        supportAutoConf = supportAutoConf,
        tradeLimitInfo = EMPTY_STRING,
        tradeDuration = EMPTY_STRING,
    )

@Preview
@Composable
private fun OtherCryptoFormContentPreview_DefaultPreview() {
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
private fun OtherCryptoFormContentPreview_AutoConfEnabledPreview() {
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
