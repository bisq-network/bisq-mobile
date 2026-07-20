package network.bisq.mobile.client.common.presentation.navigation

import kotlinx.serialization.Serializable
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.navigation.types.PaymentAccountType

interface ClientNavRoute : NavRoute {
    @Serializable
    data class TrustedNodeSetup(
        val showConnectionFailed: Boolean = false,
        val showKeystoreError: Boolean = false,
        val showSubscriptionsFailed: Boolean = false,
    ) : ClientNavRoute

    @Serializable
    data object TrustedNodeSetupSettings : ClientNavRoute

    @Serializable
    data object NetworkConnections : ClientNavRoute

    @Serializable
    data object NetworkMyConnection : ClientNavRoute

    @Serializable
    data object PaymentAccountsMusig : ClientNavRoute

    @Serializable
    data class PaymentAccountsMusigDetail(
        val accountName: String,
    ) : ClientNavRoute

    @Serializable
    @ConsistentCopyVisibility
    data class CreatePaymentAccount private constructor(
        val accountTypeName: String,
    ) : ClientNavRoute {
        constructor(accountType: PaymentAccountType) : this(accountTypeName = accountType.name)
    }
}
