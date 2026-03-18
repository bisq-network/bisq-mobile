package network.bisq.mobile.data.replicated.contract

import kotlinx.serialization.Serializable

@Serializable
enum class RoleEnum {
    MAKER,
    TAKER,
    ESCROW_AGENT,
}
