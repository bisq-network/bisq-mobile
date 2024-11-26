package network.bisq.mobile.domain.data.model

import kotlinx.serialization.Serializable

@Serializable
open class Settings : BaseModel() {
    open var bisqUrl: String = ""
    open var isConnected: Boolean = false
}