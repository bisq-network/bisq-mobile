package network.bisq.mobile.presentation.ui.components.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.PaymentTypeCard
import network.bisq.mobile.presentation.ui.composeModels.PaymentTypeData
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.uicases.offers.createOffer.ICreateOfferPresenter
import org.koin.compose.koinInject

@Composable
fun PaymentMethodCard(
    paymentMethodTitle: String,
    paymentTypeList: List<PaymentTypeData>
) {
    val presenter: ICreateOfferPresenter = koinInject()
    val state by presenter.state.collectAsState()
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BisqText.largeLight(
            text = paymentMethodTitle,
            color = BisqTheme.colors.grey2
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 38.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            repeat(paymentTypeList.size) { index ->
                PaymentTypeCard(
                    image = paymentTypeList[index].image,
                    title = paymentTypeList[index].title,
                    onClick = { presenter.onSelectPayment(it) },
                    isSelected = paymentTypeList[index].title == state.paymentMethod
                )
            }
        }
    }
}