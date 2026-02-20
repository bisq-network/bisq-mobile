package network.bisq.mobile.presentation.offer.take_offer.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.isBuy
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.info.InfoBox
import network.bisq.mobile.presentation.common.ui.components.molecules.info.InfoBoxCurrency
import network.bisq.mobile.presentation.common.ui.components.molecules.info.InfoBoxSats
import network.bisq.mobile.presentation.common.ui.components.molecules.info.InfoRowContainer
import network.bisq.mobile.presentation.common.ui.components.organisms.offer.TakeOfferProgressDialog
import network.bisq.mobile.presentation.common.ui.components.organisms.offer.TakeOfferSuccessDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import network.bisq.mobile.presentation.offer.take_offer.TakeOfferPresenter
import org.jetbrains.compose.ui.tooling.preview.Preview
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
    if (takeOffer.hasAmountRange) {
        stepIndex++
    }
    if (takeOffer.hasMultipleQuoteSidePaymentMethods) {
        stepIndex++
    }
    if (takeOffer.hasMultipleBaseSidePaymentMethods) {
        stepIndex++
    }

    TakeOfferReviewContent(
        headLine = presenter.headLine,
        takersDirection = presenter.takersDirection,
        amountToPay = presenter.amountToPay,
        amountToReceive = presenter.amountToReceive,
        price = presenter.price,
        marketCodes = presenter.marketCodes,
        priceDetails = presenter.priceDetails,
        quoteSidePaymentMethodDisplayString = presenter.quoteSidePaymentMethodDisplayString,
        baseSidePaymentMethodDisplayString = presenter.baseSidePaymentMethodDisplayString,
        fee = presenter.fee,
        feeDetails = presenter.feeDetails,
        isSmallScreen = presenter::isSmallScreen,
        stepIndex = stepIndex,
        stepsLength = takeOfferPresenter.totalSteps,
        showProgressDialog = showProgressDialog,
        showSuccessDialog = showSuccessDialog,
        isInteractive = isInteractive,
        snackbarHostState = presenter.getSnackState(),
        onBack = presenter::onBack,
        onTakeOffer = presenter::onTakeOffer,
        onClose = presenter::onClose,
        onGoToOpenTrades = presenter::onGoToOpenTrades,
    )
}

@Composable
fun TakeOfferReviewContent(
    headLine: String,
    takersDirection: DirectionEnum,
    amountToPay: String,
    amountToReceive: String,
    price: String,
    marketCodes: String,
    priceDetails: String,
    quoteSidePaymentMethodDisplayString: String,
    baseSidePaymentMethodDisplayString: String,
    fee: String,
    feeDetails: String,
    isSmallScreen: () -> Boolean,
    stepIndex: Int,
    stepsLength: Int,
    showProgressDialog: Boolean,
    showSuccessDialog: Boolean,
    isInteractive: Boolean,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onTakeOffer: () -> Unit,
    onClose: () -> Unit,
    onGoToOpenTrades: () -> Unit,
) {
    MultiScreenWizardScaffold(
        "bisqEasy.takeOffer.progress.review".i18n(),
        stepIndex = stepIndex,
        stepsLength = stepsLength,
        prevOnClick = onBack,
        nextButtonText = "bisqEasy.takeOffer.review.takeOffer".i18n(),
        nextOnClick = onTakeOffer,
        snackbarHostState = snackbarHostState,
        isInteractive = isInteractive,
        shouldBlurBg = showProgressDialog || showSuccessDialog,
        showUserAvatar = false,
        closeAction = true,
        onConfirmedClose = onClose,
    ) {
        BisqGap.V1()
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X),
        ) {
            InfoBox(
                label = "bisqEasy.tradeState.header.direction".i18n().uppercase(),
                value = headLine,
            )
            if (takersDirection.isBuy) {
                if (isSmallScreen()) {
                    InfoBoxCurrency(
                        label = "bisqEasy.tradeWizard.review.toPay".i18n().uppercase(),
                        value = amountToPay,
                    )
                    InfoBoxSats(
                        label = "bisqEasy.tradeWizard.review.toReceive".i18n().uppercase(),
                        value = amountToReceive,
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
                        )
                    }
                }
            } else {
                if (isSmallScreen()) {
                    InfoBoxSats(
                        label = "bisqEasy.tradeWizard.review.toPay".i18n().uppercase(),
                        value = amountToPay,
                    )
                    InfoBoxCurrency(
                        label = "bisqEasy.tradeWizard.review.toReceive".i18n().uppercase(),
                        value = amountToReceive,
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
                        )
                    }
                }
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
                            BisqText.H6Light(price)
                            BisqGap.HQuarter()
                            BisqText.BaseLightGrey(marketCodes)
                        }
                        BisqText.SmallLightGrey(priceDetails)
                    }
                },
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
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            BisqText.H6Light(fee)
                        }
                        BisqText.SmallLightGrey(feeDetails)
                    }
                },
            )
        }
    }

    if (showProgressDialog) {
        TakeOfferProgressDialog()
    }

    if (showSuccessDialog) {
        TakeOfferSuccessDialog(
            onShowTrades = onGoToOpenTrades,
        )
    }
}

@Preview
@Composable
private fun TakeOfferReviewScreen_Buyer_Preview() {
    BisqTheme.Preview {
        TakeOfferReviewContent(
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
            isSmallScreen = { false },
            stepIndex = 4,
            stepsLength = 4,
            showProgressDialog = false,
            showSuccessDialog = false,
            isInteractive = true,
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onTakeOffer = {},
            onClose = {},
            onGoToOpenTrades = {},
        )
    }
}

@Preview
@Composable
private fun TakeOfferReviewScreen_Seller_Preview() {
    BisqTheme.Preview {
        TakeOfferReviewContent(
            headLine = "Sell Bitcoin",
            takersDirection = DirectionEnum.SELL,
            amountToPay = "0.0050",
            amountToReceive = "480 USD",
            price = "45,000",
            marketCodes = "USD/BTC",
            priceDetails = "Market price + 2%",
            quoteSidePaymentMethodDisplayString = "SEPA",
            baseSidePaymentMethodDisplayString = "On-chain",
            fee = "0.50 USD",
            feeDetails = "Trade fee (0.1%)",
            isSmallScreen = { false },
            stepIndex = 4,
            stepsLength = 4,
            showProgressDialog = false,
            showSuccessDialog = false,
            isInteractive = true,
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onTakeOffer = {},
            onClose = {},
            onGoToOpenTrades = {},
        )
    }
}

@Preview
@Composable
private fun TakeOfferReviewScreen_SmallScreen_Buyer_Preview() {
    BisqTheme.Preview {
        TakeOfferReviewContent(
            headLine = "Buy Bitcoin",
            takersDirection = DirectionEnum.BUY,
            amountToPay = "500 USD",
            amountToReceive = "0.0050",
            price = "45,000",
            marketCodes = "USD/BTC",
            priceDetails = "Market price + 2%",
            quoteSidePaymentMethodDisplayString = "SEPA",
            baseSidePaymentMethodDisplayString = "On-chain",
            fee = "0.50 USD",
            feeDetails = "Trade fee (0.1%)",
            isSmallScreen = { true },
            stepIndex = 4,
            stepsLength = 4,
            showProgressDialog = false,
            showSuccessDialog = false,
            isInteractive = true,
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onTakeOffer = {},
            onClose = {},
            onGoToOpenTrades = {},
        )
    }
}

@Preview
@Composable
private fun TakeOfferReviewScreen_SmallScreen_Seller_Preview() {
    BisqTheme.Preview {
        TakeOfferReviewContent(
            headLine = "Sell Bitcoin",
            takersDirection = DirectionEnum.SELL,
            amountToPay = "0.0050",
            amountToReceive = "480 USD",
            price = "45,000",
            marketCodes = "USD/BTC",
            priceDetails = "Market price + 2%",
            quoteSidePaymentMethodDisplayString = "SEPA",
            baseSidePaymentMethodDisplayString = "On-chain",
            fee = "0.50 USD",
            feeDetails = "Trade fee (0.1%)",
            isSmallScreen = { true },
            stepIndex = 4,
            stepsLength = 4,
            showProgressDialog = false,
            showSuccessDialog = false,
            isInteractive = true,
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onTakeOffer = {},
            onClose = {},
            onGoToOpenTrades = {},
        )
    }
}

@Preview
@Composable
private fun TakeOfferReviewScreen_WithProgressDialog_Preview() {
    BisqTheme.Preview {
        TakeOfferReviewContent(
            headLine = "Buy Bitcoin",
            takersDirection = DirectionEnum.BUY,
            amountToPay = "500 USD",
            amountToReceive = "0.0050",
            price = "45,000",
            marketCodes = "USD/BTC",
            priceDetails = "Market price + 2%",
            quoteSidePaymentMethodDisplayString = "SEPA",
            baseSidePaymentMethodDisplayString = "On-chain",
            fee = "0.50 USD",
            feeDetails = "Trade fee (0.1%)",
            isSmallScreen = { false },
            stepIndex = 4,
            stepsLength = 4,
            showProgressDialog = true,
            showSuccessDialog = false,
            isInteractive = false,
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onTakeOffer = {},
            onClose = {},
            onGoToOpenTrades = {},
        )
    }
}

@Preview
@Composable
private fun TakeOfferReviewScreen_WithSuccessDialog_Preview() {
    BisqTheme.Preview {
        TakeOfferReviewContent(
            headLine = "Buy Bitcoin",
            takersDirection = DirectionEnum.BUY,
            amountToPay = "500 USD",
            amountToReceive = "0.0050",
            price = "45,000",
            marketCodes = "USD/BTC",
            priceDetails = "Market price + 2%",
            quoteSidePaymentMethodDisplayString = "SEPA",
            baseSidePaymentMethodDisplayString = "On-chain",
            fee = "0.50 USD",
            feeDetails = "Trade fee (0.1%)",
            isSmallScreen = { false },
            stepIndex = 4,
            stepsLength = 4,
            showProgressDialog = false,
            showSuccessDialog = true,
            isInteractive = false,
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onTakeOffer = {},
            onClose = {},
            onGoToOpenTrades = {},
        )
    }
}
