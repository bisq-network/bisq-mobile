package network.bisq.mobile.domain.data.replicated.contract

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.network.identity.NetworkIdVO

@Serializable
data class PartyVO(val role: RoleEnum, val networkId: NetworkIdVO)