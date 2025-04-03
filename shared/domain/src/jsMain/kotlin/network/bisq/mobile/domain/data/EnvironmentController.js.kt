package network.bisq.mobile.domain.data

import network.bisq.mobile.client.shared.BuildConfig

actual fun provideIsSimulator(): Boolean {
    // **IMPORTANT** In JS, we can't really determine if it's a simulator
    // Replaced with "is localhost" implementation
    return js("window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'") as Boolean
}

actual fun provideApiHost(): String {
    return BuildConfig.WS_IOS_HOST.takeIf { it.isNotEmpty() } ?: 
           js("window.location.hostname") as String
}

actual fun provideApiPort(): Int {
    return (BuildConfig.WS_PORT.takeIf { it.isNotEmpty() } ?: "8090").toInt()
}