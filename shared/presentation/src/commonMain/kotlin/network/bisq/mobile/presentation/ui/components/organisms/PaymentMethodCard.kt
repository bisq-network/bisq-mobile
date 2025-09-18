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
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.presentation.ui.helpers.i18NPaymentMethod
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.molecules.PaymentTypeCard
import network.bisq.mobile.presentation.ui.components.molecules.inputfield.CustomPaymentField
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun PaymentMethodCard(
    title: String,
    imagePaths: List<String>,
    availablePaymentMethods: Set<String>,
    selectedPaymentMethods: MutableStateFlow<Set<String>>,
    onToggle: (String) -> Unit,
    showCustomPayment: Boolean = false,
    onAddCustomPayment: ((String) -> Unit)? = null,
    onRemoveCustomPayment: ((String) -> Unit)? = null,
) {

    val selected by selectedPaymentMethods.collectAsState()

    data class Entry(
        val key: String,
        val imagePath: String,
        val displayName: String,
        val isCustom: Boolean
    )

    val entries = availablePaymentMethods.toList()
        .mapIndexed { idx, key ->
            val (name, missing) = i18NPaymentMethod(key)
            Entry(key, imagePaths.getOrElse(idx) { "" }, name, missing)
        }
        .sortedBy { it.displayName }

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

            entries.forEachIndexed { index, entry ->
                PaymentTypeCard(
                    image = entry.imagePath,
                    title = entry.displayName,
                    onClick = { onToggle(entry.key) },
                    onRemove = { onRemoveCustomPayment?.invoke(entry.key) },
                    isSelected = selected.contains(entry.key),
                    isCustomPaymentMethod = entry.isCustom,
                    showRemoveCustom = showCustomPayment
                )
            }

            if (showCustomPayment) {
                CustomPaymentField(onAddCustomPayment = onAddCustomPayment)
            }

        }
    }
}

@Composable
private fun PaymentMethodCardContent(
    language: String = "en",
    imagePaths: List<String>,
    availablePaymentMethods: Set<String>,
    selectedPaymentMethods: MutableStateFlow<Set<String>>,
) {
    BisqTheme.Preview(language = language) {
        PaymentMethodCard(
            "Choose a payment method to transfer USD",
            imagePaths,
            availablePaymentMethods,
            selectedPaymentMethods,
            onToggle = {},
            onAddCustomPayment = {},
        )
    }
}

@Preview
@Composable
private fun PaymentMethodCard_En() = PaymentMethodCardContent(
    imagePaths = emptyList(),
    availablePaymentMethods = emptySet(),
    selectedPaymentMethods =  MutableStateFlow(emptySet())
)
@Preview
@Composable
private fun PaymentMethodCard_Ru() = PaymentMethodCardContent(
    language = "ru",
    imagePaths = emptyList(),
    availablePaymentMethods = emptySet(),
    selectedPaymentMethods =  MutableStateFlow(emptySet())
)
