package network.bisq.mobile.domain.data.model

import kotlinx.serialization.Serializable

@Serializable
open class Settings : BaseModel() {
    open var bisqApiUrl: String = ""
    open var isConnected: Boolean = false
}