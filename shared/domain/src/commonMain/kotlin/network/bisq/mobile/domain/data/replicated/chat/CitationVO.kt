package network.bisq.mobile.domain.data.replicated.chat

import kotlinx.serialization.Serializable

@Serializable
data class CitationVO(val authorUserProfileId: String, val text: String)

