package network.bisq.mobile.domain.data.model

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.PlatformImage

@Serializable
data class User(
    val tradeTerms: String? = null,
    val statement: String? = null,
    val uniqueAvatar: PlatformImage? = null
)