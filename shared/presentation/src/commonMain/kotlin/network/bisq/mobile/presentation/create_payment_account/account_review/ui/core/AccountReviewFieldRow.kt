package network.bisq.mobile.presentation.create_payment_account.account_review.ui.core

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme

/**
 * A labeled key-value row in the review summary card.
 * Label in subdued grey (SmallLight), value in white (BaseRegular).
 */
@Composable
fun AccountReviewFieldRow(
    label: String,
    value: String,
) {
    Column {
        BisqText.SmallLight(label, color = BisqTheme.colors.mid_grey20)
        BisqGap.VQuarter()
        BisqText.BaseRegular(value)
    }
}
