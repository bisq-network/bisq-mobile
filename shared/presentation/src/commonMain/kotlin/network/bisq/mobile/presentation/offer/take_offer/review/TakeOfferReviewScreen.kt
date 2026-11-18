package network.bisq.mobile.presentation.offer.take_offer.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.DirectionEnumExtensions.isBuy
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.InitialScreenInteractionLock
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.info.InfoBox
import network.bisq.mobile.presentation.common.ui.components.molecules.info.InfoBoxCurrency
import network.bisq.mobile.presentation.common.ui.components.molecules.info.InfoBoxSats
import network.bisq.mobile.presentation.common.ui.components.molecules.info.InfoRowContainer
import network.bisq.mobile.presentation.common.ui.components.molecules.trade_terms.TradeTermsDisplayMode
import network.bisq.mobile.presentation.common.ui.components.molecules.trade_terms.TradeTermsUiState
import network.bisq.mobile.presentation.common.ui.components.organisms.dialogs.BisqGeneralErrorDialog
import network.bisq.mobile.presentation.common.ui.components.organisms.dialogs.TradeFailureDialog
import network.bisq.mobile.presentation.common.ui.components.organisms.offer.TakeOfferProgressDialog
import network.bisq.mobile.presentation.common.ui.components.organisms.offer.TakeOfferSuccessDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import network.bisq.mobile.presentation.offer.take_offer.TakeOfferCoordinator
import org.koin.compose.koinInject

@ExcludeFromCoverage
@Composable
fun TakeOfferReviewTradeScreen() {
    val presenter: TakeOfferReviewPresenter = koinInject()
    val takeOfferCoordinator: TakeOfferCoordinator = koinInject()
    RememberPresenterLifecycle(presenter)

    val uiState by presenter.uiState.collectAsState()

    val takeOffer = takeOfferCoordinator.takeOfferModel
    var stepIndex = 1
    if (takeOffer.hasAmountRange) {
        stepIndex++
    }
    if (takeOffer.hasMultipleQuoteSidePaymentMethods) {
        stepIndex++
    }
    if (takeOffer.hasMultipleBaseSidePaymentMethods) {
        stepIndex++
    }
    if (takeOfferCoordinator.showBtcAddressScreen()) {
        stepIndex++
    }

    // This final review step is locked briefly to prevent fast repeated taps from creating the offer
    // before the user has reviewed it. A deeper fix is harder because the controls are wrapped inside
    // MultiScreenWizardScaffold.
    InitialScreenInteractionLock {
        TakeOfferReviewContent(
            uiState = uiState,
            onAction = presenter::onAction,
            isSmallScreen = presenter::isSmallScreen,
            stepIndex = stepIndex,
            stepsLength = takeOfferCoordinator.totalSteps,
        )
    }
}

@ExcludeFromCoverage
@Composable
fun TakeOfferReviewContent(
    uiState: TakeOfferReviewUiState,
    onAction: (TakeOfferReviewUiAction) -> Unit,
    isSmallScreen: () -> Boolean,
    stepIndex: Int,
    stepsLength: Int,
) {
    MultiScreenWizardScaffold(
        "bisqEasy.takeOffer.progress.review".i18n(),
        stepIndex = stepIndex,
        stepsLength = stepsLength,
        prevOnClick = { onAction(TakeOfferReviewUiAction.OnBack) },
        nextButtonText = "bisqEasy.takeOffer.review.takeOffer".i18n(),
        nextOnClick = { onAction(TakeOfferReviewUiAction.OnTakeOffer) },
        shouldBlurBg =
            uiState.showTakeOfferProgressDialog ||
                uiState.showTakeOfferSuccessDialog ||
                uiState.takeOfferErrorDialog != null,
        showUserAvatar = false,
        closeAction = true,
        onConfirmedClose = { onAction(TakeOfferReviewUiAction.OnClose) },
    ) {
        BisqGap.V1()
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X),
        ) {
            InfoBox(
                label = "bisqEasy.tradeState.header.direction".i18n().uppercase(),
                value = uiState.headLine,
            )
            if (uiState.takersDirection.isBuy) {
                if (isSmallScreen()) {
                    InfoBoxCurrency(
                        label = "bisqEasy.tradeWizard.review.toPay".i18n().uppercase(),
                        value = uiState.amountToPay,
                    )
                    InfoBoxSats(
                        label = "bisqEasy.tradeWizard.review.toReceive".i18n().uppercase(),
                        value = uiState.amountToReceive,
                    )
                } else {
                    InfoRowContainer {
                        InfoBoxCurrency(
                            label = "bisqEasy.tradeWizard.review.toPay".i18n().uppercase(),
                            value = uiState.amountToPay,
                        )
                        InfoBoxSats(
                            label = "bisqEasy.tradeWizard.review.toReceive".i18n().uppercase(),
                            value = uiState.amountToReceive,
                        )
                    }
                }
            } else {
                if (isSmallScreen()) {
                    InfoBoxSats(
                        label = "bisqEasy.tradeWizard.review.toPay".i18n().uppercase(),
                        value = uiState.amountToPay,
                    )
                    InfoBoxCurrency(
                        label = "bisqEasy.tradeWizard.review.toReceive".i18n().uppercase(),
                        value = uiState.amountToReceive,
                    )
                } else {
                    InfoRowContainer {
                        InfoBoxSats(
                            label = "bisqEasy.tradeWizard.review.toPay".i18n().uppercase(),
                            value = uiState.amountToPay,
                        )
                        InfoBoxCurrency(
                            label = "bisqEasy.tradeWizard.review.toReceive".i18n().uppercase(),
                            value = uiState.amountToReceive,
                        )
                    }
                }
            }

            // Payout-address notice: after the amounts (the first thing a buyer verifies),
            // before the divider that closes off "the deal".
            uiState.addressNotice?.let { notice ->
                TakeOfferReviewAddressNotice(
                    notice = notice,
                    onEditAddress = { onAction(TakeOfferReviewUiAction.OnEditAddress) },
                    onOpenWalletGuide = { onAction(TakeOfferReviewUiAction.OnOpenWalletGuide) },
                )
            }
        }

        BisqHDivider()
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X),
        ) {
            InfoBox(
                label = "bisqEasy.tradeWizard.review.priceDescription.taker".i18n(),
                valueComposable = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            BisqText.H6Light(uiState.price)
                            BisqGap.HQuarter()
                            BisqText.BaseLightGrey(uiState.marketCodes)
                        }
                        BisqText.SmallLightGrey(uiState.priceDetails)
                    }
                },
            )

            InfoBox(
                label = "bisqEasy.takeOffer.review.method.fiat".i18n(),
                value = uiState.quoteSidePaymentMethodDisplayString,
            )
            InfoBox(
                label = "bisqEasy.takeOffer.review.method.bitcoin".i18n(),
                value = uiState.baseSidePaymentMethodDisplayString,
            )

            InfoBox(
                label = "bisqEasy.tradeWizard.review.feeDescription".i18n(),
                valueComposable = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            BisqText.H6Light(uiState.fee)
                        }
                        BisqText.SmallLightGrey(uiState.feeDetails)
                    }
                },
            )
        }

        TakeOfferReviewTradeTerms(tradeTerms = uiState.tradeTerms, onAction = onAction)
    }

    if (uiState.showTakeOfferProgressDialog) {
        TakeOfferProgressDialog()
    }

    if (uiState.showTakeOfferSuccessDialog) {
        TakeOfferSuccessDialog(
            onShowTrades = { onAction(TakeOfferReviewUiAction.OnGoToOpenTrades) },
        )
    }

    when (val error = uiState.takeOfferErrorDialog) {
        is TakeOfferErrorDialog.ProtocolFailure ->
            TradeFailureDialog(
                errorMessage = error.message,
                onClose = { onAction(TakeOfferReviewUiAction.OnDismissTakeOfferError) },
                atPeer = error.atPeer,
                showSupportChannel = uiState.isSupportChannelAvailable,
                onOpenSupportChannel = { onAction(TakeOfferReviewUiAction.OnOpenSupportChannel) },
            )
        is TakeOfferErrorDialog.Unexpected ->
            BisqGeneralErrorDialog(
                errorMessage = error.message,
                onClose = { onAction(TakeOfferReviewUiAction.OnDismissTakeOfferError) },
            )
        null -> Unit
    }
}

private val previewBuyerUiState =
    TakeOfferReviewUiState(
        headLine = "Buy Bitcoin",
        takersDirection = DirectionEnum.BUY,
        amountToPay = "500 USD",
        amountToReceive = "0.0050",
        price = "45,000",
        marketCodes = "USD/BTC",
        priceDetails = "Market price + 2%",
        quoteSidePaymentMethodDisplayString = "SEPA, Zelle",
        baseSidePaymentMethodDisplayString = "Bitcoin Lightning, On-chain",
        fee = "0.50 USD",
        feeDetails = "Trade fee (0.1%)",
    )

private val previewSellerUiState =
    previewBuyerUiState.copy(
        headLine = "Sell Bitcoin",
        takersDirection = DirectionEnum.SELL,
        amountToPay = "0.0050",
        amountToReceive = "480 USD",
        quoteSidePaymentMethodDisplayString = "SEPA",
        baseSidePaymentMethodDisplayString = "On-chain",
    )

@Composable
private fun TakeOfferReviewPreview(
    uiState: TakeOfferReviewUiState,
    isSmallScreen: Boolean = false,
) {
    BisqTheme.Preview {
        TakeOfferReviewContent(
            uiState = uiState,
            onAction = {},
            isSmallScreen = { isSmallScreen },
            stepIndex = 4,
            stepsLength = 4,
        )
    }
}

@Preview
@Composable
private fun TakeOfferReviewScreen_Buyer_Preview() {
    TakeOfferReviewPreview(previewBuyerUiState)
}

@Preview
@Composable
private fun TakeOfferReviewScreen_Seller_Preview() {
    TakeOfferReviewPreview(previewSellerUiState)
}

@Preview
@Composable
private fun TakeOfferReviewScreen_SmallScreen_Buyer_Preview() {
    TakeOfferReviewPreview(previewBuyerUiState, isSmallScreen = true)
}

@Preview
@Composable
private fun TakeOfferReviewScreen_SmallScreen_Seller_Preview() {
    TakeOfferReviewPreview(previewSellerUiState, isSmallScreen = true)
}

@Preview
@Composable
private fun TakeOfferReviewScreen_WithTradeTerms_Preview() {
    TakeOfferReviewPreview(
        previewBuyerUiState.copy(
            tradeTerms =
                TradeTermsUiState(
                    text =
                        "I only accept payment from an account in your own name. No third party payments, " +
                            "no payments from business accounts and no cash deposits. Please send the payment " +
                            "within 30 minutes of the trade starting.",
                    displayMode = TradeTermsDisplayMode.COLLAPSIBLE,
                ),
        ),
    )
}

@Preview
@Composable
private fun TakeOfferReviewScreen_WithProgressDialog_Preview() {
    TakeOfferReviewPreview(previewBuyerUiState.copy(showTakeOfferProgressDialog = true))
}

@Preview
@Composable
private fun TakeOfferReviewScreen_WithSuccessDialog_Preview() {
    TakeOfferReviewPreview(previewBuyerUiState.copy(showTakeOfferSuccessDialog = true))
}

@Preview
@Composable
private fun TakeOfferReviewScreen_WithFailureDialog_Preview() {
    TakeOfferReviewPreview(
        previewBuyerUiState.copy(
            takeOfferErrorDialog =
                TakeOfferErrorDialog.ProtocolFailure(
                    "Takers (buyers) Bitcoin amount is too high. " +
                        "This can be caused by differences in the 2 traders market price.",
                ),
            isSupportChannelAvailable = true,
        ),
    )
}
