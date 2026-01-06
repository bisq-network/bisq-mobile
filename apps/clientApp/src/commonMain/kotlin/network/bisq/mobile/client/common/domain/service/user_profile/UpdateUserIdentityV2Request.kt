package network.bisq.mobile.client.common.domain.service.user_profile

import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserIdentityV2Request(
    val profileId: String,
    val terms: String = "",
    val statement: String = "",
)
