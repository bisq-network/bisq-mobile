package network.bisq.mobile.presentation.create_payment_account.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.AutoResizeText
import network.bisq.mobile.presentation.common.ui.components.atoms.button.BisqIconButton
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.CloseIcon
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePaymentAccountTopBar(
    title: String,
    showCloseIcon: Boolean = false,
    onBackClick: () -> Unit = {},
    onCloseClick: () -> Unit = {},
) {
    TopAppBar(
        title = {
            AutoResizeText(
                text = title,
                textStyle = BisqTheme.typography.h4Regular,
                color = BisqTheme.colors.white,
                maxLines = 2,
            )
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = BisqTheme.colors.backgroundColor,
            ),
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "action.back".i18n(),
                    tint = BisqTheme.colors.mid_grey30,
                )
            }
        },
        actions = {
            if (showCloseIcon) {
                BisqIconButton(onClick = onCloseClick) {
                    CloseIcon()
                }
            }
        },
    )
}

@Preview
@Composable
private fun CreatePaymentAccountTopBarPreview() {
    BisqTheme {
        CreatePaymentAccountTopBar(title = "paymentAccounts.createAccount.paymentMethod.headline".i18n())
    }
}
