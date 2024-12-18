package network.bisq.mobile.presentation.ui.uicases.offers.takeOffer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import cafe.adriel.lyricist.LocalStrings
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.model.OfferListItem
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.molecules.BisqAmountSelector
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject

interface ITakeOfferTradeAmountPresenter : ViewPresenter {
    // TODO: Update later to refer to a single OfferListItem
    val offerListItems: StateFlow<List<OfferListItem>>
    fun amountConfirmed()

    fun onFixedAmountChange(amount: Float)
}

@Composable
fun TakeOfferTradeAmountScreen() {
    val strings = LocalStrings.current.bisqEasy
    val presenter: ITakeOfferTradeAmountPresenter = koinInject()

    val offer = presenter.offerListItems.collectAsState().value.first()

    // TODO: Should be from OfferListItem
    val offerMinFiatAmount = 800.0
    val offerMaxFiatAmount = 1500.0

    MultiScreenWizardScaffold(
        strings.bisqEasy_takeOffer_progress_amount,
        stepIndex = 1,
        stepsLength = 3,
        prevOnClick = { presenter.goBack() },
        nextOnClick = { presenter.amountConfirmed() }
    ) {
        BisqText.h3Regular(
            text = strings.bisqEasy_takeOffer_amount_headline_buyer,
            color = BisqTheme.colors.light1
        )
        BisqGap.V1()
        BisqText.largeLight(
            text = strings.bisqEasy_takeOffer_amount_description(
                offerMinFiatAmount.toDouble(),
                offerMaxFiatAmount.toDouble()
            ),
            color = BisqTheme.colors.grey2
        )

        BisqGap.V5()

        BisqAmountSelector(
            minAmount = offerMinFiatAmount,
            maxAmount = offerMaxFiatAmount,
            exchangeRate = 100000.0,
            currency = "USD",
            onValueChange = { println(it) }
        )
    }
}