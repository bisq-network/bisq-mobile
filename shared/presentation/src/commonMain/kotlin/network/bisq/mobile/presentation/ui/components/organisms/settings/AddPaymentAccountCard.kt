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
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun AppPaymentAccountCard(
    onConfirm: (String, String) -> Unit,
    onCancel: () -> Unit,
) {
    val strings = LocalStrings.current.user
    val stringsCommon = LocalStrings.current.common

    var accountName by remember { mutableStateOf("") }
    var accountDescription by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.padding(
            horizontal = BisqUIConstants.ScreenPadding,
        ), verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)
    ) {
            BisqText.h5Regular(
                text = strings.user_paymentAccounts_createAccount_headline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            BisqText.smallRegular(
                text = strings.user_paymentAccounts_createAccount_subtitle,
                color = BisqTheme.colors.grey1,
                textAlign = TextAlign.Center
            )
        BisqTextField(
            value = accountName,
            onValueChanged = { accountName = it },
            placeholder = strings.user_paymentAccounts_createAccount_accountName_prompt,
            label = strings.user_userProfile_payment_account
        )
        BisqTextField(
            value = accountDescription,
            onValueChanged = { accountDescription = it },
            placeholder = strings.user_paymentAccounts_createAccount_accountData_prompt,
            label = strings.user_paymentAccounts_accountData,
            isTextArea = true
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            BisqButton(
                text = stringsCommon.buttons_cancel,
                backgroundColor = BisqTheme.colors.dark5,
                onClick = onCancel,
                padding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            )
            BisqButton(
                text = stringsCommon.buttons_save,
                onClick = { onConfirm(accountName, accountDescription) },
                padding = PaddingValues(horizontal = 64.dp, vertical = 12.dp)
            )
        }
    }
}