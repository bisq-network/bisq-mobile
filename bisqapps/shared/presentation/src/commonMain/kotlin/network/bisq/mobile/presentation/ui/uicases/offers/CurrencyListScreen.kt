package network.bisq.mobile.presentation.ui.uicases.offers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.presentation.ui.components.CurrencyProfileCard
import network.bisq.mobile.components.MaterialTextField
import network.bisq.mobile.domain.data.model.FiatCurrency
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.icons.SortIcon
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticLayout
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

interface ICurrencyList : ViewPresenter {
    val currencies: StateFlow<List<FiatCurrency>>
    fun onSelectedCurrency(currency: FiatCurrency)
}

@Composable
fun CurrencyListScreen() {
    val strings = LocalStrings.current
    val presenter: ICurrencyList = koinInject()
    val currencies: List<FiatCurrency> = presenter.currencies.collectAsState().value

    RememberPresenterLifecycle(presenter)

    BisqStaticLayout(verticalArrangement = Arrangement.Top) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MaterialTextField(text = strings.common_search, onValueChanged = {})
            SortIcon(modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn() {
            items(currencies) { currency ->
                CurrencyProfileCard(
                    currency,
                    onClick = { presenter.onSelectedCurrency(it) })
            }
        }

    }
}