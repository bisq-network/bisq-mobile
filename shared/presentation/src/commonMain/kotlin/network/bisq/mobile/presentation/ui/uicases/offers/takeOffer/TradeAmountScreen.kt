package network.bisq.mobile.presentation.ui.uicases.offers.takeOffer

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.molecules.BisqAmountSelector
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject

@Composable
fun TakeOfferTradeAmountScreen() {
    val strings = LocalStrings.current.common
    val presenter: TradeAmountPresenter = koinInject()

    val offer = presenter.offerListItems.collectAsState().value.first()

    // TODO: Should be from OfferListItem
    val offerMinFiatAmount = 800.0f
    val offerMaxFiatAmount = 1500.0f

    MultiScreenWizardScaffold(
        strings.take_offer,
        stepIndex = 1,
        stepsLength = 3,
        prevOnClick = { presenter.goBack() },
        nextOnClick = { presenter.amountConfirmed() }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BisqText.h3Regular(
                text = "How much do you want to spend?",
                color = BisqTheme.colors.light1
            )
            BisqText.largeLight(
                text = "The offer allows you to choose a trade amount between $offerMinFiatAmount and $offerMaxFiatAmount USD",
                color = BisqTheme.colors.grey2
            )
        }

        BisqAmountSelector(minAmount = offerMinFiatAmount, maxAmount = offerMaxFiatAmount, exchangeRate = 95000.0, currency = "USD")
    }
}