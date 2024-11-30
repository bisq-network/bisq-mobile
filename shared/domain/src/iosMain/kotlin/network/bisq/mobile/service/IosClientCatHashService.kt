package network.bisq.mobile.service

import network.bisq.mobile.client.cathash.ClientCatHashService

// TODO Implement
class IosClientCatHashService() :
    ClientCatHashService<Any?>("Bisq2_mobile") {

    override fun composeImage(paths: Array<String>, size: Int): Any? {
        return null
    }

    override fun writeRawImage(image: Any?, iconFilePath: String) {
    }

    override fun readRawImage(iconFilePath: String): Any? {
        return null
    }
}
