package network.bisq.mobile.data.utils

/**
 * Provides environment config.
 */
class EnvironmentController {
    fun getApiHost(): String = provideApiHost()

    fun getApiPort(): Int = provideApiPort()

    fun getWebSocketHost(): String = provideApiHost()

    fun getWebSocketPort(): Int = provideApiPort()

    fun isSimulator(): Boolean = provideIsSimulator()
}

expect fun provideIsSimulator(): Boolean

expect fun provideApiHost(): String

expect fun provideApiPort(): Int
