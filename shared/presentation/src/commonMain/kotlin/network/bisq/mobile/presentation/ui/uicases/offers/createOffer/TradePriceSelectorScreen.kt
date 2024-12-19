package network.bisq.mobile.presentation.ui.uicases.offers.createOffer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.ibm_plex_sans_medium
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.NoteText
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.molecules.ToggleTab
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.helpers.StringHelper
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.jetbrains.compose.resources.Font
import org.koin.compose.koinInject

@Composable
fun CreateOfferTradePriceSelectorScreen() {
    val strings = LocalStrings.current.bisqEasyTradeWizard
    val commonStrings = LocalStrings.current.common
    val bisqEasyStrings = LocalStrings.current.bisqEasy
    val presenter: ICreateOfferPresenter = koinInject()

    val state by presenter.state.collectAsState()

    RememberPresenterLifecycle(presenter)

    MultiScreenWizardScaffold(
        bisqEasyStrings.bisqEasy_takeOffer_review_price_price,
        stepIndex = 4,
        stepsLength = 6,
        prevOnClick = { presenter.goBack() },
        nextButtonText = commonStrings.buttons_next,
        nextOnClick = { presenter.navigateToPaymentMethod() }
    ) {
        BisqText.h3Regular(
            text = strings.bisqEasy_price_headline,
            color = BisqTheme.colors.light1,
            modifier = Modifier.align(Alignment.Start)
        )
        BisqGap.V1()
        BisqText.largeLight(
            text = strings.bisqEasy_tradeWizard_price_subtitle,
            color = BisqTheme.colors.grey2
        )
        Column(
            modifier = Modifier.padding(vertical = BisqUIConstants.ScreenPadding2X),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)
        ) {
            ToggleTab(
                options = PriceType.entries,
                initialOption = PriceType.PERCENTAGE,
                onStateChange = { priceType -> presenter.onSelectPriceType(priceType) },
                getDisplayString = { direction ->
                    if (direction == PriceType.PERCENTAGE)
                        strings.bisqEasy_tradeWizard_trade_price_percentage
                    else
                        strings.bisqEasy_tradeWizard_trade_price_fixed
                },
                textWidth = StringHelper.calculateTotalWidthOfStrings(
                    strings = PriceType.entries,
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)
            ) {
                if (state.selectedPriceType == PriceType.PERCENTAGE) {
                    BisqTextField(
                        label = strings.bisqEasy_price_percentage_inputBoxText,
                        value = "10%",
                        onValueChanged = {},
                    )
                    BisqTextField(
                        label = strings.bisqEasy_price_tradePrice_inputBoxText("USD"),
                        value = "106212.99",
                        onValueChanged = {},
                        indicatorColor = BisqTheme.colors.grey1
                    )
                } else {
                    BisqTextField(
                        label = strings.bisqEasy_price_tradePrice_inputBoxText("USD"),
                        value = "106212.99",
                        onValueChanged = {},
                    )
                    BisqTextField(
                        label = strings.bisqEasy_price_percentage_inputBoxText,
                        value = "10%",
                        onValueChanged = {},
                        indicatorColor = BisqTheme.colors.grey1
                    )
                }
            }
            NoteText(
                notes = strings.bisqEasy_price_feedback_sentence,
                linkText = strings.bisqEasy_price_feedback_learnWhySection_openButton,
                textAlign = TextAlign.Center
            )
        }
    }
}