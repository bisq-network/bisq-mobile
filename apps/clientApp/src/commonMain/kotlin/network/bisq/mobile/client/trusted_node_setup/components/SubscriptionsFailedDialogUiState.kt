package network.bisq.mobile.client.trusted_node_setup.components

import androidx.compose.runtime.Immutable
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic

@Immutable
data class SubscriptionsFailedDialogUiState(
    val failedTopics: List<Topic>,
)
