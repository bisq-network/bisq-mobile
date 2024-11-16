package network.bisq.mobile.domain.data.model

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