package network.bisq.mobile.presentation.ui.uicases.offers.createOffer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import cafe.adriel.lyricist.LocalStrings
import com.ionspin.kotlin.bignum.decimal.toBigDecimalUsingSignificandAndExponent
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
import org.koin.compose.koinInject

@Composable
fun CreateOfferAmountSelectorScreen() {
    val strings = LocalStrings.current.bisqEasyTradeWizard
    val stringsEasy = LocalStrings.current.bisqEasy
    val stringsCommon = LocalStrings.current.common
    val presenter: ICreateOfferPresenter = koinInject()

    val offerMinFiatAmount = 800.0f
    val offerMaxFiatAmount = 1500.0f
    val state by presenter.state.collectAsState()
    val isBuy = presenter.direction.collectAsState().value.isBuy
    val fixedAmount = presenter.fixedAmount.collectAsState().value

    RememberPresenterLifecycle(presenter)

    MultiScreenWizardScaffold(
        stringsEasy.bisqEasy_openTrades_table_quoteAmount,
        stepIndex = 3,
        stepsLength = 6,
        prevOnClick = { presenter.goBack() },
        nextButtonText = stringsCommon.buttons_next,
        nextOnClick = { presenter.navigateToTradePriceSelector() }
    ) {
        val headerText = if (isBuy)
            strings.bisqEasy_tradeWizard_amount_headline_buyer
        else
            strings.bisqEasy_tradeWizard_amount_headline_seller

        BisqText.h3Regular(
            text = headerText,
            color = BisqTheme.colors.light1,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPadding))

        BisqText.largeLight(
            text = strings.bisqEasy_tradeWizard_amount_description_fixAmount,
            color = BisqTheme.colors.grey2,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPadding2X))

        Column(verticalArrangement = Arrangement.spacedBy(52.dp)) {

            ToggleTab(
                options = AmountType.entries,
                initialOption = AmountType.FIXED_AMOUNT,
                onStateChange = { amountType -> presenter.onSelectAmountType(amountType) },
                getDisplayString = { direction ->
                    if (direction == AmountType.FIXED_AMOUNT)
                        strings.bisqEasy_tradeWizard_fixed_amount
                    else
                        strings.bisqEasy_tradeWizard_trade_amount
                },
                textWidth = StringHelper.calculateTotalWidthOfStrings(
                    strings = listOf(
                        strings.bisqEasy_tradeWizard_fixed_amount,
                        strings.bisqEasy_tradeWizard_range_amount
                    )
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            if (state.selectedAmountType == AmountType.FIXED_AMOUNT) {
                BisqAmountSelector(
                    minAmount = offerMinFiatAmount,
                    maxAmount = offerMaxFiatAmount,
                    exchangeRate = 95000.0,
                    currency = "USD",
                    onValueChange = { value -> presenter.onFixedAmountChange(value) }
                )
            } else {
                RangeAmountSelector(
                    minAmount = offerMinFiatAmount,
                    maxAmount = offerMaxFiatAmount
                )
            }

            val matchingSellerCount = 2
            val countString = if (matchingSellerCount == 0) {
                strings.bisqEasy_tradeWizard_amount_numOffers_0
            } else if (matchingSellerCount == 1) {
                strings.bisqEasy_tradeWizard_amount_numOffers_1
            } else  {
                strings.bisqEasy_tradeWizard_amount_numOffers_many(matchingSellerCount.toString())
            }

            val finalText = strings.bisqEasy_tradeWizard_amount_buyer_limitInfo(countString, "$fixedAmount USD")
            NoteText(
                notes = finalText,
                linkText = stringsEasy.bisqEasy_takeOffer_amount_buyer_limitInfo_learnMore
            )

        }
    }
}