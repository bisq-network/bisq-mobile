package network.bisq.mobile.presentation.ui.uicases.create_offer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.NoteText
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.molecules.BisqAmountSelector
import network.bisq.mobile.presentation.ui.components.molecules.RangeAmountSelector
import network.bisq.mobile.presentation.ui.components.molecules.ToggleTab
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPresenter.AmountType
import org.koin.compose.koinInject

@Composable
fun CreateOfferAmountSelectorScreen() {
    val presenter: CreateOfferAmountPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    MultiScreenWizardScaffold(
        "bisqEasy.openTrades.table.quoteAmount".i18n(),
        stepIndex = 3,
        stepsLength = 6,
        prevOnClick = { presenter.onBack() },
        nextButtonText = "action.next".i18n(),
        nextOnClick = { presenter.onNext() }
    ) {

        BisqText.h3Regular(
            text = presenter.headline,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPadding))

        BisqText.largeLightGrey(
            text = "bisqEasy.tradeWizard.amount.description.fixAmount".i18n(),
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPadding2X))

        Column(verticalArrangement = Arrangement.spacedBy(52.dp)) {

            ToggleTab(
                options = AmountType.entries,
                initialOption = presenter.amountType.value,
                onStateChange = { value ->
                    presenter.onSelectAmountType(value)
                },
                getDisplayString = { direction ->
                    if (direction == AmountType.FIXED_AMOUNT)
                        "Fixed amount" // TODO:i18n
                    else
                        "Range amount" // TODO:i18n
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            if (presenter.amountType.collectAsState().value == AmountType.FIXED_AMOUNT) {
                BisqAmountSelector(
                    presenter.quoteCurrencyCode,
                    presenter.formattedMinAmountWithCode,
                    presenter.formattedMaxAmountWithCode,
                    presenter.fixedAmountSliderPosition,
                    presenter.formattedQuoteSideFixedAmount,
                    presenter.formattedBaseSideFixedAmount,
                    { presenter.onFixedAmountSliderChanged(it) },
                    { presenter.onFixedAmountTextValueChanged(it) }
                )
            } else {
                RangeAmountSelector(
                    presenter.formattedMinAmountWithCode,
                    presenter.formattedMaxAmountWithCode,
                    presenter.quoteCurrencyCode,
                    presenter.rangeSliderPosition,
                    presenter.formattedQuoteSideMinRangeAmount,
                    presenter.formattedBaseSideMinRangeAmount,
                    presenter.formattedQuoteSideMaxRangeAmount,
                    presenter.formattedBaseSideMaxRangeAmount,
                    { presenter.onRangeAmountSliderChanged(it) },
                    { presenter.onMinAmountTextValueChanged(it) },
                    { presenter.onMaxAmountTextValueChanged(it) }
                )
            }

            val matchingSellerCount = 2
            val countString = when (matchingSellerCount) {
                0 -> { "bisqEasy.tradeWizard.amount.numOffers.0".i18n() }
                1 -> { "bisqEasy.tradeWizard.amount.numOffers.1".i18n() }
                else -> { "bisqEasy.tradeWizard.amount.numOffers.*".i18n(matchingSellerCount) }
            }

            NoteText(
                notes = "bisqEasy.tradeWizard.amount.buyer.limitInfo".i18n(countString, presenter.formattedQuoteSideFixedAmount.value),
                linkText = "bisqEasy.tradeWizard.amount.buyer.limitInfo.learnMore".i18n()
            )

        }
    }
}