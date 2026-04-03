package network.bisq.mobile.presentation.common.ui.navigation.types

import kotlinx.serialization.Serializable

@Serializable
enum class PaymentAccountType {
    FIAT,
    CRYPTO,
}
