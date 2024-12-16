package network.bisq.mobile.domain.data.model

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.PlatformImage

@Serializable
open class User: BaseModel() {
    var tradeTerms: String? = null
    var statement: String? = null
    var lastActivity: Long? = null
    var uniqueAvatar: PlatformImage? = null
}