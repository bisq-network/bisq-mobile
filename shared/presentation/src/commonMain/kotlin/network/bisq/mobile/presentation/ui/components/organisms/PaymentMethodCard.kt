package network.bisq.mobile.presentation.ui.components.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.presentation.ui.helpers.i18NPaymentMethod
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.molecules.PaymentTypeCard

@Composable
fun PaymentMethodCard(
    title: String,
    imagePaths: List<String>,
    availablePaymentMethods: List<String>,
    selectedPaymentMethods: MutableStateFlow<Set<String>>,
    onToggle: (String) -> Unit,
) {

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BisqText.largeLightGrey(title)
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 38.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            repeat(availablePaymentMethods.size) { index ->
                val paymentMethod = availablePaymentMethods[index]
                val isSelected = selectedPaymentMethods.collectAsState().value.contains(paymentMethod)
                PaymentTypeCard(
                    image = imagePaths[index],
                    title = i18NPaymentMethod(paymentMethod),
                    onClick = { onToggle(paymentMethod) },
                    isSelected = isSelected
                )
            }
        }
    }
}