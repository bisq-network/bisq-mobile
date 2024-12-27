package network.bisq.mobile.client.websocket.subscription

import kotlinx.serialization.Serializable

@Serializable
enum class ModificationType {
    REPLACE, // For data which replace existing data
    ADDED,  // List of added items
    REMOVED // List of removed items
}