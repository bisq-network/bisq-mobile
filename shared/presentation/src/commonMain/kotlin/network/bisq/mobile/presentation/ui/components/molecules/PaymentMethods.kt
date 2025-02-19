package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage

// TODO: Get params and render apt
@Composable
fun PaymentMethods(
    baseSidePaymentMethods: List<String>,
    quoteSidePaymentMethods: List<String>
) {
    var customMethodCounter = 1
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            quoteSidePaymentMethods.forEach { paymentMethod ->
                DynamicImage(
                    path = "drawable/payment/fiat/${
                        paymentMethod
                            .lowercase()
                            .replace("-", "_")
                    }.png",
                    fallbackPath = "drawable/payment/fiat/custom_payment_${customMethodCounter++}.png",
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        DynamicImage(
            "drawable/payment/interchangeable_grey.png",
            modifier = Modifier.size(16.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            baseSidePaymentMethods.forEach { paymentMethod ->
                DynamicImage(
                    "drawable/payment/bitcoin/${
                        paymentMethod
                            .lowercase()
                            .replace("-", "_")
                    }.png",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}