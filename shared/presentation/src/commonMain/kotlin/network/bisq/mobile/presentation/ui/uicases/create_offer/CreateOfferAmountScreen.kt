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
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.NoteText
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.molecules.BisqAmountSelector
import network.bisq.mobile.presentation.ui.components.molecules.RangeAmountSelector
import network.bisq.mobile.presentation.ui.components.molecules.ToggleTab
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.helpers.StringHelper
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPresenter.AmountType
import org.koin.compose.koinInject

@Composable
fun CreateOfferAmountSelectorScreen() {
    val strings = LocalStrings.current.bisqEasyTradeWizard
    val stringsEasy = LocalStrings.current.bisqEasy
    val stringsCommon = LocalStrings.current.common
    val presenter: CreateOfferAmountPresenter = koinInject()
    presenter.appStrings = LocalStrings.current // TODO find a more elegant solution
    RememberPresenterLifecycle(presenter)

    MultiScreenWizardScaffold(
        stringsEasy.bisqEasy_openTrades_table_quoteAmount,
        stepIndex = 3,
        stepsLength = 6,
        prevOnClick = { presenter.onBack() },
        nextButtonText = stringsCommon.buttons_next,
        nextOnClick = { presenter.onNext() }
    ) {

        BisqText.h3Regular(
            text = presenter.headline,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPadding))

        BisqText.largeLightGrey(
            text = strings.bisqEasy_tradeWizard_amount_description_fixAmount,
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
                        strings.bisqEasy_tradeWizard_fixed_amount
                    else
                        "Range amount" // strings.bisqEasy_tradeWizard_trade_amount
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
            val countString = if (matchingSellerCount == 0) {
                strings.bisqEasy_tradeWizard_amount_numOffers_0
            } else if (matchingSellerCount == 1) {
                strings.bisqEasy_tradeWizard_amount_numOffers_1
            } else {
                strings.bisqEasy_tradeWizard_amount_numOffers_many(matchingSellerCount.toString())
            }

            NoteText(
                notes = strings.bisqEasy_tradeWizard_amount_buyer_limitInfo(countString, presenter.formattedQuoteSideFixedAmount.value),
                linkText = stringsEasy.bisqEasy_takeOffer_amount_buyer_limitInfo_learnMore
            )

        }
    }
}