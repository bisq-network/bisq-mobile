package network.bisq.mobile.domain.helper

import okhttp3.Dns
import java.net.InetAddress

class NoDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        return listOf(InetAddress.getByAddress(hostname, ByteArray(4)))
    }
}