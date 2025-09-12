package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    RememberPresenterLifecycle(presenter)

    val paymentAccountDataValid by presenter.paymentAccountDataValid.collectAsState()
    val paymentAccountData by presenter.paymentAccountData.collectAsState()
    val paymentAccountName by presenter.paymentAccountName.collectAsState()
    val accounts by presenter.accounts.collectAsState()
    val accountPairs = remember(accounts) { accounts.map { it.accountName to it.accountPayload.accountData } }

    Column {
        BisqGap.V1()
        BisqText.h5Light("bisqEasy.tradeState.info.seller.phase1.headline".i18n()) // Send your payment account data to the buyer

        BisqGap.V1()
        BisqText.baseLightGrey(
            "bisqEasy.tradeState.info.seller.phase1.accountData.prompt".i18n(), // Fill in your payment account data. E.g. IBAN, BIC and account owner name
        )

        BisqGap.V1()
        if (accountPairs.isNotEmpty()) {
            BisqDropDown(
                label = "paymentAccounts.headline".i18n(),
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
            minLines = 2,
            showPaste = true,
            validation = {
                // Same validation as PaymentAccountSettingsScreen.accountData field validation

                if (it.length < 3) {
                    return@BisqTextField "mobile.bisqEasy.tradeState.info.seller.phase1.accountData.validations.minLength".i18n()
                }

                if (it.length > 1024) {
                    return@BisqTextField "mobile.bisqEasy.tradeState.info.seller.phase1.accountData.validations.maxLength".i18n()
                }

                return@BisqTextField null
            }
        )

        BisqGap.V1()
        BisqButton(
            text = "bisqEasy.tradeState.info.seller.phase1.buttonText".i18n(), // Send account data
            onClick = { presenter.onSendPaymentData() },
            disabled = !paymentAccountDataValid,
        )
    }
}