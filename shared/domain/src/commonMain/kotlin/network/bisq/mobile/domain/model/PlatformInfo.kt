package network.bisq.mobile.domain.model

interface PlatformInfo {
    val name: String
    val type: PlatformType
}

enum class PlatformType {
    ANDROID,
    IOS,
}
