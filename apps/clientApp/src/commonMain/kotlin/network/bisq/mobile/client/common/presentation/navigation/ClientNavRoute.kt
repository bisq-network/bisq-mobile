package network.bisq.mobile.client.common.presentation.navigation

import kotlinx.serialization.Serializable
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute

@Serializable
data class TrustedNodeSetup(
    val showConnectionFailed: Boolean = false,
) : NavRoute

@Serializable
data object TrustedNodeSetupSettings : NavRoute
