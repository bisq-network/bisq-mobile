package network.bisq.mobile.domain.data.model

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.PlatformImage

@Serializable
open class User: BaseModel() {
    var uniqueAvatar: PlatformImage? = null
}