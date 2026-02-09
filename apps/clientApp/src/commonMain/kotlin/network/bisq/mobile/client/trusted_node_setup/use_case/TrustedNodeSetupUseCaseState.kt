package network.bisq.mobile.client.trusted_node_setup.use_case

import network.bisq.mobile.client.common.domain.utils.EMPTY_STRING

data class TrustedNodeSetupUseCaseState(
    val connectionStatus: TrustedNodeConnectionStatus = TrustedNodeConnectionStatus.Idle,
    val serverVersion: String = EMPTY_STRING,
)
