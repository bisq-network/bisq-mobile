package network.bisq.mobile.domain.data.replicated.contract

import kotlinx.serialization.Serializable

@Serializable
enum class RoleEnum {
    MAKER,
    TAKER,
    ESCROW_AGENT;
}