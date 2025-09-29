package network.bisq.mobile.domain.data.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val tradeTerms: String? = null,
    val statement: String? = null,
)