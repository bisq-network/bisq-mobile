package network.bisq.mobile.client.service.user_profile

interface ClientCatHashService<T> {
    fun getImage(
        pubKeyHash: ByteArray,
        powSolution: ByteArray,
        avatarVersion: Int,
        size: Int
    ): T?
}
