package network.bisq.mobile.domain.utils

object NetworkUtils {

    private val ipv4Regex: Regex = Regex(
        pattern = "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}" +
                "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$"
    )
    private val ipv6Regex: Regex = Regex(
        pattern = "^(" +
                "([0-9a-fA-F]{1,4}:){7}([0-9a-fA-F]{1,4}|:)|" +
                "([0-9a-fA-F]{1,4}:){1,7}:|" +
                "([0-9a-fA-F]{1,4}:){1,6}(:[0-9a-fA-F]{1,4}|:){1,2}|" +
                "([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}|:){1,3}|" +
                "([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}|:){1,4}|" +
                "([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}|:){1,5}|" +
                "([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}|:){1,6}|" +
                "([0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}|:){1,7}))|" +
                "(:((:[0-9a-fA-F]{1,4}|:){1,7}|:))" +
                ")$"
    )

    private val onionV3Regex = Regex("^[a-z2-7]{56}\\.onion$")


    fun String.isValidIp(): Boolean {
        return ipv4Regex.matches(this) || ipv6Regex.matches(this)
    }

    fun String.isValidIpv4(): Boolean {
        return ipv4Regex.matches(this)
    }

    fun String.isValidTorV3Address(): Boolean {
        return onionV3Regex.matches(this)
    }

    fun String.isValidPort(): Boolean {
        val num = toIntOrNull() ?: return false
        return num in 1..65535
    }
}