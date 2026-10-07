package network.bisq.mobile.presentation.offers_below_reputation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.currency.MarketVOExtensions.marketCodes
import network.bisq.mobile.domain.service.offers.OffendingOffer
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.AutoResizeText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.debouncedClickable
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ExclamationRedIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

/**
 * Lists my sell offers that my current reputation score no longer covers. Back and an outside tap
 * dispatch Keep, which the presenter ignores while the offers are being removed.
 */
@Composable
fun OffersBelowReputationDialog(
    uiState: OffersBelowReputationUiState,
    onAction: (OffersBelowReputationUiAction) -> Unit,
) {
    ConfirmationDialog(
        headline = "bisqEasy.offerbook.offerList.popup.offersWithInsufficientReputationWarning.headline".i18n(),
        headlineColor = BisqTheme.colors.warning,
        headlineLeftIcon = { WarningIcon() },
        message = "bisqEasy.offerbook.offerList.popup.offersWithInsufficientReputationWarning.message".i18n(),
        confirmButtonText = "bisqEasy.offerbook.offerList.popup.offersWithInsufficientReputationWarning.removeOffers".i18n(),
        dismissButtonText = "mobile.bisqEasy.offerbook.offersBelowReputation.dialog.keep".i18n(),
        confirmButtonLoading = uiState.isRemoving,
        onConfirm = { onAction(OffersBelowReputationUiAction.RemoveOffers) },
        onDismiss = { onAction(OffersBelowReputationUiAction.Keep) },
        extraContent = {
            Column {
                uiState.offendingOffers.forEach { offer ->
                    OffendingOfferListRow(
                        offer = offer,
                        enabled = !uiState.isRemoving,
                        onGoToMarket = { onAction(OffersBelowReputationUiAction.GoToMarket(offer)) },
                    )
                    BisqGap.VHalf()
                }
                BisqGap.VHalf()
                BisqButton(
                    text = "bisqEasy.offerbook.offerList.popup.offersWithInsufficientReputationWarning.buildReputation".i18n(),
                    type = BisqButtonType.Underline,
                    disabled = uiState.isRemoving,
                    onClick = { onAction(OffersBelowReputationUiAction.BuildReputation) },
                )
            }
        },
    )
}

@Composable
private fun OffendingOfferListRow(
    offer: OffendingOffer,
    enabled: Boolean,
    onGoToMarket: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().debouncedClickable(enabled = enabled, onClick = onGoToMarket),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AutoResizeText(
                text = offer.market.marketCodes,
                color = BisqTheme.colors.white,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            BisqText.SmallLight(
                text = offer.formattedAmount,
                color = BisqTheme.colors.mid_grey20,
            )
        }
        if (offer.hasRemoveError) {
            BisqGap.H1()
            val removeFailedLabel = "mobile.bisqEasy.offerbook.offersBelowReputation.dialog.removeFailed".i18n()
            Box(modifier = Modifier.clearAndSetSemantics { contentDescription = removeFailedLabel }) {
                ExclamationRedIcon()
            }
        }
        BisqGap.H1()
        BisqText.SmallMedium(
            text = "mobile.bisqEasy.offerbook.offersBelowReputation.dialog.goToMarket".i18n(),
            color = BisqTheme.colors.primary,
        )
    }
}

@ExcludeFromCoverage
private fun previewRow(
    offerId: String,
    quoteCurrencyCode: String = "EUR",
    amount: String = "50.00 - 200.00 EUR",
    hasRemoveError: Boolean = false,
) = OffendingOffer(
    offerId = offerId,
    market = MarketVO("BTC", quoteCurrencyCode),
    formattedAmount = amount,
    hasRemoveError = hasRemoveError,
)

// A dialog alone gives the preview a zero-size root, so some host content sits next to it.
@ExcludeFromCoverage
@Composable
private fun DialogPreviewHost(
    uiState: OffersBelowReputationUiState,
    label: String,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(BisqUIConstants.ScreenPadding)) {
        BisqText.SmallLight(label, color = BisqTheme.colors.mid_grey20)
    }
    OffersBelowReputationDialog(uiState = uiState, onAction = {})
}

@ExcludeFromCoverage
@Preview(heightDp = 700)
@Composable
private fun OffersBelowReputationDialog_SingleOfferPreview() {
    BisqTheme.Preview {
        DialogPreviewHost(
            uiState = OffersBelowReputationUiState(offendingOffers = listOf(previewRow("a")), isDialogVisible = true),
            label = "One offer",
        )
    }
}

@ExcludeFromCoverage
@Preview(heightDp = 900)
@Composable
private fun OffersBelowReputationDialog_SeveralOffersPreview() {
    BisqTheme.Preview {
        DialogPreviewHost(
            uiState =
                OffersBelowReputationUiState(
                    offendingOffers =
                        listOf(
                            previewRow("a"),
                            previewRow("b", quoteCurrencyCode = "GBP", amount = "45.00 GBP"),
                            previewRow("c", quoteCurrencyCode = "XAAAAAAAAAAA", amount = "1,250,000.00 XAAAAAAAAAAA"),
                            previewRow("d", quoteCurrencyCode = "NGN", amount = "980,000.00 NGN"),
                        ),
                    isDialogVisible = true,
                ),
            label = "Several offers",
        )
    }
}

@ExcludeFromCoverage
@Preview(heightDp = 700)
@Composable
private fun OffersBelowReputationDialog_RemovingPreview() {
    BisqTheme.Preview {
        DialogPreviewHost(
            uiState =
                OffersBelowReputationUiState(
                    offendingOffers = listOf(previewRow("a"), previewRow("b", quoteCurrencyCode = "GBP", amount = "45.00 GBP")),
                    isDialogVisible = true,
                    isRemoving = true,
                ),
            label = "Removing",
        )
    }
}

@ExcludeFromCoverage
@Preview(heightDp = 700)
@Composable
private fun OffersBelowReputationDialog_RemoveFailedPreview() {
    BisqTheme.Preview {
        DialogPreviewHost(
            uiState =
                OffersBelowReputationUiState(
                    offendingOffers = listOf(previewRow("b", quoteCurrencyCode = "GBP", amount = "45.00 GBP", hasRemoveError = true)),
                    isDialogVisible = true,
                ),
            label = "One removal failed",
        )
    }
}
