package network.bisq.mobile.presentation.ui.uicases.offer.create_offer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.NoteText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.molecules.ToggleTab
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.helpers.StringHelper
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@Composable
fun CreateOfferTradePriceSelectorScreen() {
    val bisqEasyTradeWizardStrings = LocalStrings.current.bisqEasyTradeWizard
    val commonStrings = LocalStrings.current.common
    val bisqEasyStrings = LocalStrings.current.bisqEasy

    val presenter: CreateOfferPricePresenter = koinInject()
    presenter.appStrings = LocalStrings.current // TODO find a more elegant solution
    RememberPresenterLifecycle(presenter)

    val formattedPercentagePrice by presenter.formattedPercentagePrice.collectAsState()
    val formattedPrice by presenter.formattedPrice.collectAsState()
    val priceType by presenter.priceType.collectAsState()

    MultiScreenWizardScaffold(
        bisqEasyStrings.bisqEasy_takeOffer_review_price_price,
        stepIndex = 4,
        stepsLength = 6,
        prevOnClick = { presenter.onBack() },
        nextButtonText = commonStrings.buttons_next,
        nextOnClick = { presenter.onNext() }
    ) {
        BisqText.h3Regular(
            text = bisqEasyTradeWizardStrings.bisqEasy_price_headline,
            color = BisqTheme.colors.light1,
            modifier = Modifier.align(Alignment.Start)
        )
        BisqGap.V1()
        BisqText.largeLight(
            text = bisqEasyTradeWizardStrings.bisqEasy_tradeWizard_price_subtitle,
            color = BisqTheme.colors.grey2
        )
        Column(
            modifier = Modifier.padding(vertical = BisqUIConstants.ScreenPadding2X),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)
        ) {
            ToggleTab(
                options = presenter.priceTypes,
                initialOption = priceType,
                onStateChange = { priceType -> presenter.onSelectPriceType(priceType) },
                getDisplayString = { presenter.getPriceTypeDisplayString(it) },
                textWidth = StringHelper.calculateTotalWidthOfStrings(strings = presenter.priceTypes),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)
            ) {
                if (priceType == CreateOfferPresenter.PriceType.PERCENTAGE) {
                    BisqTextField(
                        label = bisqEasyTradeWizardStrings.bisqEasy_price_percentage_inputBoxText,
                        value = formattedPercentagePrice,
                        onValueChanged = { presenter.onPercentagePriceChanged(it) },
                    )
                    BisqTextField(
                        label = presenter.fixPriceDescription,
                        value = formattedPrice,
                        onValueChanged = {}, // Deactivated
                        indicatorColor = BisqTheme.colors.grey1
                    )
                } else {
                    BisqTextField(
                        label = presenter.fixPriceDescription,
                        value = formattedPrice,
                        onValueChanged = { presenter.onFixPriceChanged(it) },
                    )
                    BisqTextField(
                        label = bisqEasyTradeWizardStrings.bisqEasy_price_percentage_inputBoxText,
                        value = formattedPercentagePrice,
                        onValueChanged = {},// Deactivated
                        indicatorColor = BisqTheme.colors.grey1
                    )
                }
            }
            NoteText(
                notes = bisqEasyTradeWizardStrings.bisqEasy_price_feedback_sentence,
                linkText = bisqEasyTradeWizardStrings.bisqEasy_price_feedback_learnWhySection_openButton,
                textAlign = TextAlign.Center
            )
        }
    }
}