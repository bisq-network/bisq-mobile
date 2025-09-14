package network.bisq.mobile.presentation.ui.components.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.helpers.i18NPaymentMethod
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextFieldType
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.ui.components.atoms.icons.AddIcon
import network.bisq.mobile.presentation.ui.components.molecules.PaymentTypeCard
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun PaymentMethodCard(
    title: String,
    imagePaths: List<String>,
    availablePaymentMethods: Set<String>,
    selectedPaymentMethods: MutableStateFlow<Set<String>>,
    customMethodCount: Int = 0,
    onToggle: (String) -> Unit,
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

    var customPaymentMethod by remember { mutableStateOf("") }

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
                    onRemove = { onRemoveCustomPayment?.invoke(entry.displayName) },
                    isSelected = selected.contains(entry.key),
                    index = index + 1,
                    isCustomPaymentMethod = entry.isCustom
                )
            }

            if (customMethodCount < 3) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(shape = RoundedCornerShape(6.dp))
                        .background(BisqTheme.colors.dark_grey50).padding(start = 18.dp).padding(vertical = 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.Zero),
                    verticalAlignment = Alignment.CenterVertically
                    // horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DynamicImage(
                        path = "drawable/payment/fiat/add_custom_grey.png",
                        fallbackPath = "drawable/payment/fiat/custom_payment_0.png",
                        contentDescription = "mobile.components.paymentTypeCard.customPaymentMethod".i18n(title),
                        modifier = Modifier.size(20.dp)
                    )
                    BisqTextField(
                        value = customPaymentMethod,
                        onValueChange = { newValue, _ -> customPaymentMethod = newValue },
                        placeholder = "bisqEasy.tradeWizard.paymentMethods.customMethod.prompt".i18n(),
                        modifier = Modifier.weight(1f),
                        backgroundColor = BisqTheme.colors.dark_grey50,
                        type = BisqTextFieldType.Transparent,
                    )
                    IconButton(
                        onClick = {
                            if (customPaymentMethod.isNotBlank()) {
                                onAddCustomPayment?.invoke(customPaymentMethod.trim())
                                customPaymentMethod = ""
                            }
                        }, colors = IconButtonColors(
                            containerColor = BisqTheme.colors.primary,
                            contentColor = BisqTheme.colors.white,
                            disabledContainerColor = BisqTheme.colors.primaryDisabled,
                            disabledContentColor = BisqTheme.colors.mid_grey20
                        )
                    ) {
                        AddIcon()
                    }
                }
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
