package network.bisq.mobile.client.user_profile

interface ClientCatHashService<T> {
    fun getImage(
        pubKeyHash: ByteArray,
        powSolution: ByteArray,
        avatarVersion: Int,
        size: Int
    ): T?
}
