package network.bisq.mobile.domain.service.community

/**
 * The Community hub's gated segments. Declaration order is the tab order. A segment only
 * renders when it is live — see [CommunityHubService.liveSegments].
 */
enum class CommunitySegment {
    DISCUSSIONS,
    MESSAGES,
    CONTACTS,
}
