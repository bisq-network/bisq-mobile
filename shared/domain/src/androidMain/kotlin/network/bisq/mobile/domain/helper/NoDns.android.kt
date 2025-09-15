package network.bisq.mobile.domain.helper

import okhttp3.Dns
import java.net.InetAddress

/**
 * Prevent DNS requests.
 * Important when proxying all requests over Tor to not leak DNS queries.
 *
 * taken from https://github.com/f-droid/fdroidclient/blob/7803475afa8a04d4db42ad682366890f79226e72/libs/download/src/androidMain/kotlin/org/fdroid/download/HttpManager.kt#L67-L75
 */
class NoDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        return listOf(InetAddress.getByAddress(hostname, ByteArray(4)))
    }
}