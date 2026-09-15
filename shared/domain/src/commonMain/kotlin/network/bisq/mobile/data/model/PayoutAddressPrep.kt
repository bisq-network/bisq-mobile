package network.bisq.mobile.data.model

import kotlinx.serialization.Serializable

/**
 * Persisted state for preparing the buyer's payout address before the trade needs it.
 *
 * [prefillByTradeId] carries an address collected in the take-offer wizard across to the
 * mid-trade address screen — the trade can reach that screen after an app restart, so the
 * handoff cannot live in memory. Entries are cleared once the address is sent to the peer.
 *
 * [profilesWithCompletedTrade] marks profiles that have completed a trade, so the optional
 * address wizard step is only offered to first-time traders. Written from two sources: live,
 * when a trade reaches its completed state, and seeded from closed-trade history by
 * `TakeOfferCoordinator.warmUpFirstTimeTraderFlag`. Kept persisted rather than derived per
 * check because the gate must answer synchronously on the take-offer tap, and a veteran
 * wrongly treated as first-timer merely sees one extra skippable step.
 */
@Serializable
data class PayoutAddressPrep(
    val prefillByTradeId: Map<String, String> = emptyMap(),
    val profilesWithCompletedTrade: Set<String> = emptySet(),
)
