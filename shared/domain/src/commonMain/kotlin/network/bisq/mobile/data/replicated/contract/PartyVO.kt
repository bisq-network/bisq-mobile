package network.bisq.mobile.data.replicated.contract

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.replicated.network.identity.NetworkIdVO

@Serializable
data class PartyVO(
    val role: RoleEnum,
    val networkId: NetworkIdVO,
)
