package network.bisq.mobile.client.common.domain.access.session.dto

import kotlinx.serialization.Serializable

@Serializable
data class SessionRequestDto(
    val clientId: String,
    val clientSecret: String,
)
