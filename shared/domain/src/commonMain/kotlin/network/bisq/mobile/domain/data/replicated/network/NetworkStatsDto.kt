package network.bisq.mobile.domain.data.replicated.network

import kotlinx.serialization.Serializable

@Serializable
data class NetworkStatsDto(
    val publishedProfiles: Int
)