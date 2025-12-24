package network.bisq.mobile.domain.utils

object NetworkUtils {
    private val ipv4Regex: Regex =
        Regex(
            pattern =
                "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}" +
                    "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$",
        )
    private val ipv6Regex: Regex =
        Regex(
            pattern =
                "^(" +
                    "([0-9a-fA-F]{1,4}:){7}([0-9a-fA-F]{1,4}|:)|" +
                    "([0-9a-fA-F]{1,4}:){1,7}:|" +
                    "([0-9a-fA-F]{1,4}:){1,6}(:[0-9a-fA-F]{1,4}|:){1,2}|" +
                    "([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}|:){1,3}|" +
                    "([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}|:){1,4}|" +
                    "([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}|:){1,5}|" +
                    "([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}|:){1,6}|" +
                    "([0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}|:){1,7}))|" +
                    "(:((:[0-9a-fA-F]{1,4}|:){1,7}|:))" +
                    ")$",
        )

    private val onionV3Regex = Regex("^[a-z2-7]{56}\\.onion$")

    fun String.isValidIp(): Boolean = ipv4Regex.matches(this) || ipv6Regex.matches(this)

    fun String.isValidIpv4(): Boolean = ipv4Regex.matches(this)

    fun String.isValidTorV3Address(): Boolean = onionV3Regex.matches(this)

    fun String.isValidPort(): Boolean {
        val num = toIntOrNull() ?: return false
        return num in 1..65535
    }

    fun String.isPrivateIPv4(): Boolean {
        val parts = this.split(".")
        if (parts.size != 4) return false
        val nums = parts.mapNotNull { it.toIntOrNull() }
        if (nums.size != 4 || nums.any { it !in 0..255 }) return false

        val (a, b, c, d) = nums

        // 10.0.0.0 – 10.255.255.255
        if (a == 10) return true

        // 172.16.0.0 – 172.31.255.255
        if (a == 172 && b in 16..31) return true

        // 192.168.0.0 – 192.168.255.255
        if (a == 192 && b == 168) return true

        // 127.0.0.0 – 127.255.255.255 (loopback, often considered private)
        if (a == 127) return true

        // 169.254.0.0 – 169.254.255.255 (link-local)
        if (a == 169 && b == 254) return true

        return false
    }
}
