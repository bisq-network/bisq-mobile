package network.bisq.mobile.presentation.ui.uicases.take_offer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.isBuy
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoBox
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoBoxCurrency
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoBoxSats
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoRowContainer
import network.bisq.mobile.presentation.ui.components.organisms.offer.TakeOfferProgressDialog
import network.bisq.mobile.presentation.ui.components.organisms.offer.TakeOfferSuccessDialog
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.helpers.PreviewEnvironment
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.material3.SnackbarHostState
import org.koin.compose.koinInject


@Composable
fun TakeOfferReviewTradeScreen() {
    val presenter: TakeOfferReviewPresenter = koinInject()
    val takeOfferPresenter: TakeOfferPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val showProgressDialog by presenter.showTakeOfferProgressDialog.collectAsState()
    val showSuccessDialog by presenter.showTakeOfferSuccessDialog.collectAsState()
    val isInteractive by presenter.isInteractive.collectAsState()

    val takeOffer = takeOfferPresenter.takeOfferModel
    var stepIndex = 1
    if (takeOffer.hasAmountRange)
        stepIndex++
    if (takeOffer.hasMultipleQuoteSidePaymentMethods)
        stepIndex++
    if (takeOffer.hasMultipleBaseSidePaymentMethods)
        stepIndex++

    MultiScreenWizardScaffold(
        "bisqEasy.takeOffer.progress.review".i18n(),
        stepIndex = stepIndex,
        stepsLength = takeOfferPresenter.totalSteps,
        prevOnClick = { presenter.onBack() },
        nextButtonText = "bisqEasy.takeOffer.review.takeOffer".i18n(),
        nextOnClick = { presenter.onTakeOffer() },
        snackbarHostState = presenter.getSnackState(),
        isInteractive = isInteractive,
        shouldBlurBg = showProgressDialog || showSuccessDialog,
        showUserAvatar = false,
        closeAction = true,
        onConfirmedClose = presenter::onClose
    ) {
        BisqGap.V1()
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X)
        ) {
            InfoBox(
                label = "bisqEasy.tradeState.header.direction".i18n().uppercase(),
                value = presenter.headLine,
            )
            if (presenter.takersDirection.isBuy) {
                if (presenter.isSmallScreen()) {
                    InfoBoxCurrency(
                        label = "bisqEasy.tradeWizard.review.toPay".i18n().uppercase(),
                        value = presenter.amountToPay,
                    )
                    InfoBoxSats(
                        label = "bisqEasy.tradeWizard.review.toReceive".i18n().uppercase(),
                        value = presenter.amountToReceive,
                        rightAlign = true
                    )
                } else {
                    InfoRowContainer {
                        InfoBoxCurrency(
                            label = "bisqEasy.tradeWizard.review.toPay".i18n().uppercase(),
                            value = presenter.amountToPay,
                        )
                        InfoBoxSats(
                            label = "bisqEasy.tradeWizard.review.toReceive".i18n().uppercase(),
                            value = presenter.amountToReceive,
                            rightAlign = true
                        )
                    }
                }
            } else {
                if (presenter.isSmallScreen()) {
                    InfoBoxSats(
                        label = "bisqEasy.tradeWizard.review.toPay".i18n().uppercase(),
                        value = presenter.amountToPay,
                    )
                    InfoBoxCurrency(
                        label = "bisqEasy.tradeWizard.review.toReceive".i18n().uppercase(),
                        value = presenter.amountToReceive,
                        rightAlign = true
                    )
                } else {
                    InfoRowContainer {
                        InfoBoxSats(
                            label = "bisqEasy.tradeWizard.review.toPay".i18n().uppercase(),
                            value = presenter.amountToPay,
                        )
                        InfoBoxCurrency(
                            label = "bisqEasy.tradeWizard.review.toReceive".i18n().uppercase(),
                            value = presenter.amountToReceive,
                            rightAlign = true
                        )
                    }
                }
            }
        }

        BisqHDivider()
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X)
        ) {
            InfoBox(
                label = "bisqEasy.tradeWizard.review.priceDescription.taker".i18n(),
                valueComposable = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            BisqText.h6Light(presenter.price)
                            BisqGap.HQuarter()
                            BisqText.baseLightGrey(presenter.marketCodes)
                        }
                        BisqText.smallLightGrey(presenter.priceDetails)
                    }
                }
            )

            InfoBox(
                label = "bisqEasy.takeOffer.review.method.fiat".i18n(),
                value = presenter.quoteSidePaymentMethodDisplayString,
            )
            InfoBox(
                label = "bisqEasy.takeOffer.review.method.bitcoin".i18n(),
                value = presenter.baseSidePaymentMethodDisplayString,
            )

            InfoBox(
                label = "bisqEasy.tradeWizard.review.feeDescription".i18n(),
                valueComposable = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            BisqText.h6Light(presenter.fee)
                        }
                        BisqText.smallLightGrey(presenter.feeDetails)
                    }
                }
            )
        }
    }

    if (showProgressDialog) {
        TakeOfferProgressDialog()
    }

    if (showSuccessDialog) {
        TakeOfferSuccessDialog(
            onShowTrades = { presenter.onGoToOpenTrades() }
        )
    }
}

@Composable
fun TakeOfferReviewContent(
    isInteractive: Boolean,
    showProgressDialog: Boolean,
    showSuccessDialog: Boolean,
    isSmallScreen: Boolean,
    headLine: String,
    takersIsBuy: Boolean,
    amountToPay: String,
    amountToReceive: String,
    price: String,
    marketCodes: String,
    priceDetails: String,
    quoteSidePaymentMethodDisplayString: String,
    baseSidePaymentMethodDisplayString: String,
    fee: String,
    feeDetails: String,
    onBack: () -> Unit,
    onTakeOffer: () -> Unit,
    onClose: () -> Unit,
    onGoToOpenTrades: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    MultiScreenWizardScaffold(
        "bisqEasy.takeOffer.progress.review".i18n(),
        stepIndex = 4,
        stepsLength = 4,
        prevOnClick = onBack,
        nextButtonText = "bisqEasy.takeOffer.review.takeOffer".i18n(),
        nextOnClick = onTakeOffer,
        snackbarHostState = snackbarHostState,
        isInteractive = isInteractive,
        shouldBlurBg = showProgressDialog || showSuccessDialog,
        showUserAvatar = false,
        closeAction = true,
        onConfirmedClose = onClose
    ) {
        BisqGap.V1()
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X)
        ) {
            InfoBox(
                label = "bisqEasy.tradeState.header.direction".i18n().uppercase(),
                value = headLine,
            )
            if (takersIsBuy) {
                if (isSmallScreen) {
                    InfoBoxCurrency(
                        label = "bisqEasy.tradeWizard.review.toPay".i18n().uppercase(),
                        value = amountToPay,
                    )
                    InfoBoxSats(
                        label = "bisqEasy.tradeWizard.review.toReceive".i18n().uppercase(),
                        value = amountToReceive,
                        rightAlign = true
                    )
                } else {
                    InfoRowContainer {
                        InfoBoxCurrency(
                            label = "bisqEasy.tradeWizard.review.toPay".i18n().uppercase(),
                            value = amountToPay,
                        )
                        InfoBoxSats(
                            label = "bisqEasy.tradeWizard.review.toReceive".i18n().uppercase(),
                            value = amountToReceive,
                            rightAlign = true
                        )
                    }
                }
            } else {
                if (isSmallScreen) {
                    InfoBoxSats(
                        label = "bisqEasy.tradeWizard.review.toPay".i18n().uppercase(),
                        value = amountToPay,
                    )
                    InfoBoxCurrency(
                        label = "bisqEasy.tradeWizard.review.toReceive".i18n().uppercase(),
                        value = amountToReceive,
                        rightAlign = true
                    )
                } else {
                    InfoRowContainer {
                        InfoBoxSats(
                            label = "bisqEasy.tradeWizard.review.toPay".i18n().uppercase(),
                            value = amountToPay,
                        )
                        InfoBoxCurrency(
                            label = "bisqEasy.tradeWizard.review.toReceive".i18n().uppercase(),
                            value = amountToReceive,
                            rightAlign = true
                        )
                    }
                }
            }
        }

        BisqHDivider()
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X)
        ) {
            InfoBox(
                label = "bisqEasy.tradeWizard.review.priceDescription.taker".i18n(),
                valueComposable = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            BisqText.h6Light(price)
                            BisqGap.HQuarter()
                            BisqText.baseLightGrey(marketCodes)
                        }
                        BisqText.smallLightGrey(priceDetails)
                    }
                }
            )

            InfoBox(
                label = "bisqEasy.takeOffer.review.method.fiat".i18n(),
                value = quoteSidePaymentMethodDisplayString,
            )
            InfoBox(
                label = "bisqEasy.takeOffer.review.method.bitcoin".i18n(),
                value = baseSidePaymentMethodDisplayString,
            )

            InfoBox(
                label = "bisqEasy.tradeWizard.review.feeDescription".i18n(),
                valueComposable = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            BisqText.h6Light(fee)
                        }
                        BisqText.smallLightGrey(feeDetails)
                    }
                }
            )
        }
    }

    if (showProgressDialog) {
        TakeOfferProgressDialog()
    }


    if (showSuccessDialog) {
        TakeOfferSuccessDialog(
            onShowTrades = onGoToOpenTrades
        )
    }
}

/**
 * Preview environment is now provided by PreviewEnvironment helper.
 * See: network.bisq.mobile.presentation.ui.helpers.PreviewEnvironment
 */

@Preview
@Composable
fun TakeOfferReviewPreview() {
    BisqTheme.Preview {
        PreviewEnvironment {
            TakeOfferReviewContent(
                isInteractive = true,
                showProgressDialog = false,
                showSuccessDialog = false,
                isSmallScreen = false,
                headLine = "BUY Bitcoin",
                takersIsBuy = true,
                amountToPay = "1,000.00 USD",
                amountToReceive = "0.01000000 BTC",
                price = "100,000.00",
                marketCodes = "BTC/USD",
                priceDetails = "1.0% above market at 99,000.00 USD",
                quoteSidePaymentMethodDisplayString = "SEPA",
                baseSidePaymentMethodDisplayString = "On-chain",
                fee = "Seller pays miner fee",
                feeDetails = "No trade fees",
                onBack = {},
                onTakeOffer = {},
                onClose = {},
                onGoToOpenTrades = {},
                snackbarHostState = SnackbarHostState(),
            )
        }
    }
}