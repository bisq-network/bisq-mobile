package network.bisq.mobile.domain.data.model

import network.bisq.mobile.domain.PlatformImage

/**
 *
 */
open class User: BaseModel() {
    var uniqueAvatar: PlatformImage? = null
}