package network.bisq.mobile.domain.data.model

import kotlinx.serialization.Serializable

@Serializable
open class Settings : BaseModel() {
    open var bisqUrl: String = ""
    open var isConnected: Boolean = false
}

interface SettingsFactory {
    fun createSettings(): Settings
}

class DefaultSettingsFactory : SettingsFactory {
    override fun createSettings() = Settings()
}