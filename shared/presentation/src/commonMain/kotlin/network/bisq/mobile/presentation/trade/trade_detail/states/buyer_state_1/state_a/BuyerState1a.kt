@file:Suppress("ktlint:compose:vm-forwarding-check")

package network.bisq.mobile.presentation.trade.trade_detail.states.buyer_state_1.state_a

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqCheckbox
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.InfoGreenIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BitcoinLnAddressField
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BitcoinLnAddressFieldType
import network.bisq.mobile.presentation.common.ui.security.SecureScreenEffect
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import network.bisq.mobile.presentation.common.ui.utils.spaceBetweenWithMin

@Composable
fun BuyerState1a(
    presenter: BuyerState1aPresenter,
) {
    RememberPresenterLifecycle(presenter)
    SecureScreenEffect()

    val uiState by presenter.uiState.collectAsState()
    val isSendEnabled by presenter.isSendBitcoinPaymentDataEnabled.collectAsState()
    BuyerState1aContent(uiState = uiState, isSendEnabled = isSendEnabled, onAction = presenter::onAction)
}

@Composable
private fun BuyerState1aContent(
    uiState: BuyerState1aUiState,
    isSendEnabled: Boolean,
    onAction: (BuyerState1aUiAction) -> Unit,
) {
    Column {
        BisqGap.V1()
        // Fill in your Bitcoin address / Fill in your Lightning invoice
        BisqText.H5Light(uiState.headline)

        if (uiState.wasPrefilled) {
            BisqGap.V1()
            Row(
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InfoGreenIcon()
                BisqText.SmallRegular("mobile.tradeState.buyer.phase1a.prefilled.badge".i18n())
            }
        }

        BisqGap.V1()
        BitcoinLnAddressField(
            label = uiState.description, // Bitcoin address / Lightning invoice
            value = uiState.paymentData,
            onValueChange = { value, isValid -> onAction(BuyerState1aUiAction.OnPaymentDataInput(value, isValid)) },
            type = uiState.addressFieldType,
            onBarcodeClick = { onAction(BuyerState1aUiAction.OnBarcodeClick) },
            triggerValidation = uiState.validationTrigger,
        )

        if (uiState.wasPrefilled) {
            // The last thing between the user and Send — an active check that the address they
            // entered back at take-offer time is still the one they want funds to reach.
            BisqGap.V1()
            BisqCheckbox(
                checked = uiState.hasConfirmedPrefill,
                label = "mobile.tradeState.buyer.phase1a.prefilled.confirmCheckbox".i18n(),
                onCheckedChange = { onAction(BuyerState1aUiAction.OnConfirmPrefillChange(it)) },
            )
        }

        BisqGap.V1()

        Row(
            horizontalArrangement = Arrangement.spaceBetweenWithMin(BisqUIConstants.ScreenPadding),
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
        ) {
            BisqButton(
                text = "bisqEasy.tradeState.info.buyer.phase1a.send".i18n(), // Send to seller
                onClick = { onAction(BuyerState1aUiAction.OnSend) },
                disabled =
                    uiState.paymentData.isEmpty() ||
                        !isSendEnabled ||
                        (uiState.wasPrefilled && !uiState.hasConfirmedPrefill),
                modifier = Modifier.fillMaxHeight(),
            )
            BisqButton(
                text = "bisqEasy.tradeState.info.buyer.phase1a.walletHelpButton".i18n(), // Open wallet guide
                onClick = { onAction(BuyerState1aUiAction.OnOpenWalletGuide) },
                type = BisqButtonType.Outline,
                padding =
                    PaddingValues(
                        horizontal = BisqUIConstants.ScreenPadding,
                        vertical = BisqUIConstants.ScreenPaddingHalf,
                    ),
                modifier = Modifier.fillMaxHeight(),
            )
        }
    }
}

private val previewUiState =
    BuyerState1aUiState(
        headline = "Fill in your Bitcoin address",
        description = "Bitcoin address",
    )

@ExcludeFromCoverage
@Preview
@Composable
private fun BuyerState1aContent_EmptyPreview() {
    BisqTheme.Preview {
        BuyerState1aContent(uiState = previewUiState, isSendEnabled = true, onAction = {})
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun BuyerState1aContent_PrefilledPreview() {
    BisqTheme.Preview {
        BuyerState1aContent(
            uiState =
                previewUiState.copy(
                    paymentData = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
                    isPaymentDataValid = true,
                    wasPrefilled = true,
                ),
            isSendEnabled = true,
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun BuyerState1aContent_LightningPreview() {
    BisqTheme.Preview {
        BuyerState1aContent(
            uiState =
                previewUiState.copy(
                    headline = "Fill in your Lightning invoice",
                    description = "Lightning invoice",
                    addressFieldType = BitcoinLnAddressFieldType.Lightning,
                    paymentData = "lnbc2500u1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypqdq5xysxxatsyp3k7enxv4js",
                    isPaymentDataValid = true,
                ),
            isSendEnabled = true,
            onAction = {},
        )
    }
}
