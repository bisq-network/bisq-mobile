package network.bisq.mobile.domain

interface PlatformInfo {
    val name: String
    val type: PlatformType
}

enum class PlatformType {
    ANDROID,
    IOS,
}