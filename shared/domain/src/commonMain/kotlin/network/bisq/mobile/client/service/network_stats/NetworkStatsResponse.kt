package network.bisq.mobile.client.service.network_stats

import kotlinx.serialization.Serializable

@Serializable
data class NetworkStatsResponse(
    val publishedProfiles: Int,
    // Add other network stats as needed
)