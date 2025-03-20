package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqDropDown
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle

@Composable
fun SellerState1(
    presenter: SellerState1Presenter
) {
    val paymentAccountData by presenter.paymentAccountData.collectAsState()
    val paymentAccountName by presenter.paymentAccountName.collectAsState()
    val accounts by presenter.accounts.collectAsState()
    val accountPairs: List<Pair<String, String>> = accounts.map { account ->
        account.accountName to account.accountPayload.accountData
    }

    RememberPresenterLifecycle(presenter)

    Column {
        BisqGap.V1()
        BisqText.h5Light("bisqEasy.tradeState.info.seller.phase1.headline".i18n()) // Send your payment account data to the buyer

        BisqGap.V1()
        BisqText.baseLightGrey(
            "bisqEasy.tradeState.info.seller.phase1.accountData.prompt".i18n(), // Fill in your payment account data. E.g. IBAN, BIC and account owner name
        )

        BisqGap.V1()
        if (accountPairs.size > 0) {
            BisqDropDown(
                label = "user.paymentAccounts".i18n(),
                items = accountPairs,
                value = paymentAccountName,
                showKey = true,
                onValueChanged = {
                    presenter.setPaymentAccountName(it.first)
                    presenter.onPaymentDataInput(it.second, true)
                }
            )
        }
        BisqTextField(
            label = "bisqEasy.tradeState.info.seller.phase1.accountData".i18n(), // My payment account data
            value = paymentAccountData,
            onValueChange = { it, isValid -> presenter.onPaymentDataInput(it, isValid) },
            isTextArea = true,
            showPaste = true,
            validation = {
                // Same validation as PaymentAccountSettingsScreen.accountData field validation

                if (it.length < 3) {
                    return@BisqTextField "Min length: 3 characters"
                }

                if (it.length > 1024) {
                    return@BisqTextField "Max length: 1024 characters"
                }

                return@BisqTextField null
            }
        )

        BisqGap.V1()
        BisqButton(
            text = "bisqEasy.tradeState.info.seller.phase1.buttonText".i18n(), // Send account data
            onClick = { presenter.onSendPaymentData() },
            disabled = !presenter.paymentAccountDataValid.collectAsState().value,
        )
    }
}