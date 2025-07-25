package network.bisq.mobile.presentation.ui.components.organisms.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun AppPaymentAccountCard(
    onConfirm: (String, String) -> Unit,
    onCancel: () -> Unit,
) {
    var accountName by remember { mutableStateOf("") }
    var accountNameValid by remember { mutableStateOf(false) }
    var accountDescription by remember { mutableStateOf("") }
    var accountDescriptionValid by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(
            horizontal = BisqUIConstants.ScreenPadding,
        ), verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)
    ) {
        BisqGap.VQuarter()
        BisqText.h5Regular(
            text = "user.paymentAccounts.createAccount.headline".i18n(),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        BisqText.smallRegularGrey(
            text = "user.paymentAccounts.createAccount.subtitle".i18n(),
            textAlign = TextAlign.Center
        )

        BisqGap.VHalf()

        BisqTextField(
            value = accountName,
            onValueChange = { it, isValid ->
                accountName = it
                accountNameValid = isValid
            },
            placeholder = "user.paymentAccounts.createAccount.accountName.prompt".i18n(),
            label = "user.paymentAccounts.createAccount.accountName".i18n(),
            validation = {
                if (it.isEmpty()) {
                    return@BisqTextField "mobile.user.paymentAccounts.createAccount.validations.name.isMandatory".i18n()
                }
                if (it.length < 3) {
                    return@BisqTextField "mobile.user.paymentAccounts.createAccount.validations.name.minLength=".i18n()
                }
                if (it.length > 256) {
                    return@BisqTextField "mobile.user.paymentAccounts.createAccount.validations.name.maxLength".i18n()
                }
                return@BisqTextField null
            }
        )
        BisqTextField(
            value = accountDescription,
            onValueChange = { it, isValid ->
                accountDescription = it
                accountDescriptionValid = isValid
            },
            placeholder = "user.paymentAccounts.createAccount.accountData.prompt".i18n(),
            label = "user.paymentAccounts.accountData".i18n(),
            isTextArea = true,
            minLines = 2,
            validation = {
                if (it.isEmpty()) {
                    return@BisqTextField "mobile.user.paymentAccounts.createAccount.validations.accountData.isMandatory".i18n()
                }
                if (it.length < 3) {
                    return@BisqTextField "mobile.user.paymentAccounts.createAccount.validations.accountData.minLength".i18n()
                }
                if (it.length > 256) {
                    return@BisqTextField "mobile.user.paymentAccounts.createAccount.validations.accountData.maxLength".i18n()
                }
                return@BisqTextField null
            }
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            BisqButton(
                text = "action.cancel".i18n(),
                type = BisqButtonType.Grey,
                onClick = onCancel,
                padding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            )
            BisqButton(
                text = "action.save".i18n(),
                onClick = { onConfirm(accountName, accountDescription) },
                padding = PaddingValues(horizontal = 64.dp, vertical = 12.dp),
                disabled = !accountNameValid || !accountDescriptionValid
            )
        }
    }
}