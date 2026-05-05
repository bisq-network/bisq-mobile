package network.bisq.mobile.presentation.create_payment_account.account_review.ui.core

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.settings.payment_accounts_musig.ui.AccountFlowDialog

@Composable
fun AccountDetailFieldRow(
    label: String,
    value: String,
) {
    Column {
        BisqText.SmallLight(label, color = BisqTheme.colors.mid_grey20)
        BisqGap.VQuarter()
        BisqText.BaseRegular(value)
    }
}

@Composable
fun ExpandableAccountDetailFieldRow(
    label: String,
    value: String,
    dialogTitle: String = label,
    maxLines: Int = 4,
) {
    val isTruncated = remember(value) { mutableStateOf(false) }
    val showDialog = remember(value) { mutableStateOf(false) }

    Column {
        BisqText.SmallLight(label, color = BisqTheme.colors.mid_grey20)
        BisqGap.VQuarter()
        BisqText.StyledText(
            text = value,
            style =
                if (isTruncated.value) {
                    BisqTheme.typography.baseRegular.copy(textDecoration = TextDecoration.Underline)
                } else {
                    BisqTheme.typography.baseRegular
                },
            color = if (isTruncated.value) BisqTheme.colors.primary else BisqTheme.colors.white,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { textLayoutResult ->
                isTruncated.value = textLayoutResult.hasVisualOverflow
            },
            modifier = Modifier.clickable(enabled = isTruncated.value) { showDialog.value = true },
        )
    }

    if (showDialog.value) {
        AccountFlowDialog(
            title = dialogTitle,
            bodyText = value,
            onDismissRequest = { showDialog.value = false },
        )
    }
}
