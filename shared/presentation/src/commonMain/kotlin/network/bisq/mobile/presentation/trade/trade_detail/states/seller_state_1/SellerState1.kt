@file:Suppress("ktlint:compose:vm-forwarding-check")

package network.bisq.mobile.presentation.trade.trade_detail.states.seller_state_1

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import network.bisq.mobile.domain.data.replicated.account.fiat.UserDefinedFiatAccountPayloadVO
import network.bisq.mobile.domain.data.replicated.account.fiat.UserDefinedFiatAccountVO
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqDropdown
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SellerState1(
    presenter: SellerState1Presenter,
) {
    RememberPresenterLifecycle(presenter)

    val paymentAccountDataEntry by presenter.paymentAccountDataEntry.collectAsState()
    val paymentAccountName by presenter.paymentAccountName.collectAsState()
    val accounts by presenter.accounts.collectAsState()

    val selectedIndex = accounts.indexOfFirst { it.accountName == paymentAccountName }

    SellerState1Content(
        paymentAccountDataEntry = paymentAccountDataEntry,
        accounts = accounts,
        selectedIndex = selectedIndex,
        onPaymentDataInput = { value -> presenter.onPaymentDataInput(value) },
        onAccountSelect = { index ->
            if (index in accounts.indices) {
                val account = accounts[index]
                presenter.setPaymentAccountName(account.accountName)
                presenter.onPaymentDataInput(account.accountPayload.accountData)
            }
        },
        onSendPaymentData = { presenter.onSendPaymentData() },
    )
}

@Composable
fun SellerState1Content(
    paymentAccountDataEntry: DataEntry,
    accounts: List<UserDefinedFiatAccountVO>,
    selectedIndex: Int,
    onPaymentDataInput: (String) -> Unit,
    onAccountSelect: (Int) -> Unit,
    onSendPaymentData: () -> Unit,
) {
    Column {
        BisqGap.V1()
        BisqText.H5Light("bisqEasy.tradeState.info.seller.phase1.headline".i18n()) // Send your payment account data to the buyer

        BisqGap.V1()
        BisqText.BaseLightGrey(
            "bisqEasy.tradeState.info.seller.phase1.accountData.prompt".i18n(), // Fill in your payment account data. E.g. IBAN, BIC and account owner name
        )

        BisqGap.V1()
        if (accounts.isNotEmpty()) {
            BisqDropdown(
                options = accounts.map { it.accountName },
                selectedIndex = selectedIndex,
                onOptionSelect = onAccountSelect,
                label = "paymentAccounts.headline".i18n(),
            )
        }
        BisqTextFieldV0(
            label = "bisqEasy.tradeState.info.seller.phase1.accountData".i18n(), // My payment account data
            value = paymentAccountDataEntry.value,
            onValueChange = onPaymentDataInput,
            minLines = 2,
            isError = paymentAccountDataEntry.errorMessage != null,
            bottomMessage = paymentAccountDataEntry.errorMessage,
        )

        BisqGap.V1()
        BisqButton(
            text = "bisqEasy.tradeState.info.seller.phase1.buttonText".i18n(), // Send account data
            onClick = onSendPaymentData,
            disabled = paymentAccountDataEntry.errorMessage != null || paymentAccountDataEntry.value.isEmpty(),
        )
    }
}

private val previewOnPaymentDataInput: (String) -> Unit = {}
private val previewOnAccountSelect: (Int) -> Unit = {}
private val previewOnSendPaymentData: () -> Unit = {}

@Preview
@Composable
private fun SellerState1_WithAccountsAndDataPreview() {
    val sampleAccounts =
        listOf(
            UserDefinedFiatAccountVO(
                accountName = "PayPal Account",
                accountPayload =
                    UserDefinedFiatAccountPayloadVO(
                        accountData = "user@example.com",
                    ),
            ),
            UserDefinedFiatAccountVO(
                accountName = "Bank Transfer",
                accountPayload =
                    UserDefinedFiatAccountPayloadVO(
                        accountData = "IBAN: DE89370400440532013000",
                    ),
            ),
            UserDefinedFiatAccountVO(
                accountName = "Revolut",
                accountPayload =
                    UserDefinedFiatAccountPayloadVO(
                        accountData = "+1234567890",
                    ),
            ),
        )

    BisqTheme.Preview {
        SellerState1Content(
            paymentAccountDataEntry = DataEntry(value = "user@example.com"),
            accounts = sampleAccounts,
            selectedIndex = 0,
            onPaymentDataInput = previewOnPaymentDataInput,
            onAccountSelect = previewOnAccountSelect,
            onSendPaymentData = previewOnSendPaymentData,
        )
    }
}

@Preview
@Composable
private fun SellerState1_WithAccountsEmptyDataPreview() {
    val sampleAccounts =
        listOf(
            UserDefinedFiatAccountVO(
                accountName = "PayPal Account",
                accountPayload =
                    UserDefinedFiatAccountPayloadVO(
                        accountData = "user@example.com",
                    ),
            ),
            UserDefinedFiatAccountVO(
                accountName = "Bank Transfer",
                accountPayload =
                    UserDefinedFiatAccountPayloadVO(
                        accountData = "IBAN: DE89370400440532013000",
                    ),
            ),
        )

    BisqTheme.Preview {
        SellerState1Content(
            paymentAccountDataEntry = DataEntry(),
            accounts = sampleAccounts,
            selectedIndex = 0,
            onPaymentDataInput = previewOnPaymentDataInput,
            onAccountSelect = previewOnAccountSelect,
            onSendPaymentData = previewOnSendPaymentData,
        )
    }
}

@Preview
@Composable
private fun SellerState1_NoAccountsPreview() {
    BisqTheme.Preview {
        SellerState1Content(
            paymentAccountDataEntry = DataEntry(),
            accounts = emptyList(),
            selectedIndex = -1,
            onPaymentDataInput = previewOnPaymentDataInput,
            onAccountSelect = previewOnAccountSelect,
            onSendPaymentData = previewOnSendPaymentData,
        )
    }
}
