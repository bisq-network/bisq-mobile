package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.data.model.offerbook.OfferListItem

// TODO: Get params and render apt
@Composable
fun PaymentMethods(item: OfferListItem) {
    val baseSidePaymentMethods = item.baseSidePaymentMethods
    val quoteSidePaymentMethods = item.quoteSidePaymentMethods
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
                    modifier = Modifier.size(15.dp),
                )
            }
        }
        DynamicImage(
            "drawable/payment/interchangeable_grey.png",
            modifier = Modifier.size(12.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            baseSidePaymentMethods.forEach { paymentMethod ->
                DynamicImage(
                    "drawable/payment/bitcoin/${
                        paymentMethod
                            .lowercase()
                            .replace("-", "_")
                    }.png",
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}