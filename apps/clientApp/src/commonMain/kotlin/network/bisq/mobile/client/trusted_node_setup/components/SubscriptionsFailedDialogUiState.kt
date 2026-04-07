package network.bisq.mobile.client.trusted_node_setup.components

import androidx.compose.runtime.Immutable
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic

@Immutable
data class SubscriptionsFailedDialogUiState(
    val failedTopics: List<Topic>,
    /**
     * The API version reported by the connected node, or null if unknown.
     * Shown as a subtle hint when it differs from the client's expected version.
     */
    val connectedApiVersion: String? = null,
)
