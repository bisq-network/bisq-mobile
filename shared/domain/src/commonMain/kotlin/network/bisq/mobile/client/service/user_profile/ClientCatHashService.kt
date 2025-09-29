package network.bisq.mobile.client.service.user_profile

import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO

interface ClientCatHashService<T> {
    companion object {
        // We use 40.dp at offers, to get a 3x resolution we use 120px
        const val DEFAULT_SIZE = 120
    }

    fun getImage(
        pubKeyHash: ByteArray,
        powSolution: ByteArray,
        avatarVersion: Int,
        size: Int
    ): T

    fun getImage(userProfile: UserProfileVO, size: Int): T
}
