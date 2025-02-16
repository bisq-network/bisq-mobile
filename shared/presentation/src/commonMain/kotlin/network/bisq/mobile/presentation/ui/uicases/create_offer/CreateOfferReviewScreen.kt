package network.bisq.mobile.presentation.ui.uicases.create_offer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoBox
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoRow
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@Composable
fun CreateOfferReviewOfferScreen() {
    val strings = LocalStrings.current.bisqEasyTradeWizard
    val stringsBisqEasy = LocalStrings.current.bisqEasy
    val tradeStateStrings = LocalStrings.current.bisqEasyTradeState
    val presenter: CreateOfferReviewPresenter = koinInject()
    presenter.appStrings = LocalStrings.current // TODO find a more elegant solution
    RememberPresenterLifecycle(presenter)

    MultiScreenWizardScaffold(
        strings.bisqEasy_tradeWizard_review_headline_maker,
        stepIndex = 6,
        stepsLength = 6,
        prevOnClick = { presenter.onBack() },
        nextButtonText = strings.bisqEasy_tradeWizard_review_nextButton_createOffer,
        nextOnClick = { presenter.onCreateOffer() },
        horizontalAlignment = Alignment.Start
    ) {
        BisqText.h3Regular(
            text = strings.bisqEasy_tradeWizard_review_headline_maker,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        BisqGap.V2()
        Column(verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X)) {
            InfoBox(
                label = tradeStateStrings.bisqEasy_tradeState_header_direction.uppercase(),
                value = presenter.headLine,
            )
            InfoBox(
                label = stringsBisqEasy.bisqEasy_takeOffer_review_method_fiat,
                value = presenter.quoteSidePaymentMethodDisplayString,
            )
            InfoRow(
                label1 = strings.bisqEasy_tradeWizard_review_toPay.uppercase(),
                value1 = presenter.amountToPay,
                label2 = strings.bisqEasy_tradeWizard_review_toReceive.uppercase(),
                value2 = presenter.amountToReceive
            )

            BisqHDivider()
            InfoBox(
                label = strings.bisqEasy_tradeWizard_review_priceDescription_taker,
                valueComposable = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            BisqText.h6Regular(text = presenter.formattedPrice)
                            BisqText.baseRegularGrey(text = presenter.marketCodes)
                        }
                        BisqText.smallRegular(
                            text = presenter.priceDetails,
                            color = BisqTheme.colors.grey3
                        )
                    }
                }
            )

            InfoBox(
                label = stringsBisqEasy.bisqEasy_takeOffer_review_method_bitcoin,
                value = presenter.quoteSidePaymentMethodDisplayString
            )
            InfoBox(
                label = stringsBisqEasy.bisqEasy_takeOffer_review_method_fiat,
                value = presenter.baseSidePaymentMethodDisplayString
            )

            InfoBox(
                label = strings.bisqEasy_tradeWizard_review_feeDescription,
                valueComposable = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        BisqText.h6Regular(text = presenter.fee)
                        BisqText.smallRegular(
                            text = presenter.feeDetails,
                            color = BisqTheme.colors.grey3,
                        )
                    }
                }
            )
        }
    }
}