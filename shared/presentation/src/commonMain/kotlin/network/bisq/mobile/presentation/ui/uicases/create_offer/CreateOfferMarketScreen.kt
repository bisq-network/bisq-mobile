package network.bisq.mobile.presentation.ui.uicases.create_offer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.CurrencyCard
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject

@Composable
fun CreateOfferCurrencySelectorScreen() {
    val strings = LocalStrings.current.bisqEasyTradeWizard
    val commonStrings = LocalStrings.current.common
    val presenter: CreateOfferMarketPresenter = koinInject()
    presenter.appStrings = LocalStrings.current // TODO find a more elegant solution
    RememberPresenterLifecycle(presenter)

    val searchText = presenter.searchText.collectAsState().value
    val filteredMarketList = presenter.marketListItemWithNumOffers.collectAsState().value

    MultiScreenWizardScaffold(
        commonStrings.currency,
        stepIndex = 2,
        stepsLength = 6,
        prevOnClick = { presenter.onBack() },
        nextOnClick = { presenter.onNext() },
        useStaticScaffold = true,
        horizontalAlignment = Alignment.Start
    ) {

        BisqText.h3Regular(text = presenter.headline)
        BisqGap.V1()

        BisqText.largeLightGrey(text = strings.bisqEasy_tradeWizard_market_subTitle)

        BisqGap.V2()

        BisqTextField(
            placeholder = commonStrings.common_search,
            onValueChange = { it, isValid -> presenter.setSearchText(it) },
            value = searchText,
            label = "",
        )

        BisqGap.V1()

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredMarketList) { item ->
                CurrencyCard(
                    item,
                    isSelected = presenter.market == item.market,
                    onClick = { presenter.onSelectMarket(item) }
                )
            }
        }

        BisqGap.V1()
    }
}