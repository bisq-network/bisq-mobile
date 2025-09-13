package network.bisq.mobile.presentation.ui.uicases.create_offer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BtcSatsText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.molecules.dialog.InformationConfirmationDialog
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoBox
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoBoxCurrency
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoBoxSats
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoRowContainer
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@Composable
fun CreateOfferReviewOfferScreen() {
    val presenter: CreateOfferReviewPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val isInteractive by presenter.isInteractive.collectAsState()
    val showMediatorWaitingDialog by presenter.showMediatorWaitingDialog.collectAsState()

    MultiScreenWizardScaffold(
        "bisqEasy.tradeWizard.review.headline.maker".i18n(),
        stepIndex = 7,
        stepsLength = 7,
        prevOnClick = { presenter.onBack() },
        nextButtonText = "bisqEasy.tradeWizard.review.nextButton.createOffer".i18n(),
        nextOnClick = { presenter.onCreateOffer() },
        isInteractive = isInteractive,
        showUserAvatar = false,
        closeAction = true,
        onConfirmedClose = presenter::onClose,
    ) {
        BisqGap.V1()
        Column(verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X)) {
            InfoBox(
                label = "bisqEasy.tradeState.header.direction".i18n().uppercase(),
                value = presenter.headLine,
            )
            if (presenter.isRangeOffer) {
                if (presenter.direction == DirectionEnum.BUY) {
                    InfoBoxCurrency(
                        label = "bisqEasy.tradeWizard.review.toPay".i18n().uppercase(),
                        value = presenter.amountToPay
                    )
                    InfoBox(
                        label = "bisqEasy.tradeWizard.review.toReceive".i18n().uppercase(),
                        valueComposable = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BtcSatsText(
                                    formattedBtcAmountValue = presenter.formattedBaseRangeMinAmount,
                                    noCode = true,
                                    textStyle = BisqTheme.typography.h6Regular,
                                    modifier = Modifier.alignByBaseline(),
                                )
                                BisqGap.H1()
                                BisqText.h6Light("–", modifier = Modifier.alignByBaseline())
                                BisqGap.H1()
                                BtcSatsText(
                                    formattedBtcAmountValue = presenter.formattedBaseRangeMaxAmount,
                                    noCode = true,
                                    textStyle = BisqTheme.typography.h6Regular,
                                    modifier = Modifier.alignByBaseline(),
                                )
                                BisqGap.HHalf()
                                BisqText.baseRegularGrey("BTC", modifier = Modifier.alignByBaseline())
                            }
                        }
                    )
                } else {
                    InfoBoxCurrency(
                        label = "bisqEasy.tradeWizard.review.toReceive".i18n().uppercase(),
                        value = presenter.amountToReceive
                    )
                    InfoBox(
                        label = "bisqEasy.tradeWizard.review.toSend".i18n().uppercase(),
                        valueComposable = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BtcSatsText(
                                    presenter.formattedBaseRangeMinAmount,
                                    noCode = true,
                                    textStyle = BisqTheme.typography.h6Regular,
                                    modifier = Modifier.alignByBaseline(),
                                )
                                BisqGap.H1()
                                BisqText.h6Light("–", modifier = Modifier.alignByBaseline())
                                BisqGap.H1()
                                BtcSatsText(
                                    presenter.formattedBaseRangeMaxAmount,
                                    noCode = true,
                                    textStyle = BisqTheme.typography.h6Regular,
                                    modifier = Modifier.alignByBaseline(),
                                )
                                BisqGap.HHalf()
                                BisqText.baseRegularGrey("BTC", modifier = Modifier.alignByBaseline())
                            }
                        }
                    )
                }
            } else {
                if (presenter.direction == DirectionEnum.BUY) {
                    InfoRowContainer {
                        InfoBoxCurrency(
                            label = "bisqEasy.tradeWizard.review.toPay".i18n().uppercase(),
                            value = presenter.amountToPay,
                        )
                        InfoBoxSats(
                            label = "bisqEasy.tradeWizard.review.toReceive".i18n().uppercase(),
                            value = presenter.amountToReceive,
                        )
                    }
                } else {
                    InfoRowContainer {
                        InfoBoxSats(
                            label = "bisqEasy.tradeWizard.review.toPay".i18n().uppercase(),
                            value = presenter.amountToPay
                        )
                        InfoBoxCurrency(
                            label = "bisqEasy.tradeWizard.review.toReceive".i18n().uppercase(),
                            value = presenter.amountToReceive,
                        )
                    }
                }
            }

            BisqHDivider()

            InfoBox(
                label = "bisqEasy.tradeWizard.review.priceDescription.maker".i18n(),
                valueComposable = {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        BisqText.h6Light(presenter.formattedPrice)
                        BisqGap.HQuarter()
                        BisqText.baseLightGrey(presenter.marketCodes)
                    }
                },
                subvalue = presenter.priceDetails
            )

            InfoBox(
                label = "bisqEasy.tradeWizard.review.paymentMethodDescription.fiat".i18n(), // Fiat payment method
                value = presenter.quoteSidePaymentMethodDisplayString,
            )
            InfoBox(
                label = "bisqEasy.tradeWizard.review.paymentMethodDescription.btc".i18n(), // Bitcoin settlement method
                value = presenter.baseSidePaymentMethodDisplayString
            )

            InfoBox(
                label = "bisqEasy.tradeWizard.review.feeDescription".i18n(),
                value = presenter.fee,
                subvalue = presenter.feeDetails,
            )
        }
    }

    // Mediator waiting dialog
    if (showMediatorWaitingDialog) {
        InformationConfirmationDialog(
            message = "mobile.bisqEasy.createOffer.mediatorWaiting.message".i18n(),
            onConfirm = { presenter.onDismissMediatorWaitingDialog() },
            onDismiss = { presenter.onDismissMediatorWaitingDialog() }
        )
    }
}
