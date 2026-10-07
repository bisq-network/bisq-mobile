package network.bisq.mobile.data.service.reputation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.utils.Logging

/**
 * The one reading of a peer's reputation that `PeerProfilePresenter` and `PrivateChatPresenter`
 * share, so the two screens — one tap apart — cannot disagree about the same peer.
 */
private object PeerReputation : Logging

/** A known score of nothing, as opposed to a score not known yet — see [resolveReputation]. */
private val ZERO_REPUTATION = ReputationScoreVO(totalScore = 0L, fiveSystemScore = 0.0, ranking = 0)

/**
 * Returns null when the score is not known yet, as opposed to known-and-zero.
 *
 * The two failure shapes are not interchangeable. The client facade returns `Result.failure` for
 * an unknown peer in release builds, while in debug it calls the API and can throw instead.
 *
 * A *completed* failure is ambiguous but recoverable: `ClientReputationServiceFacade.getReputation`
 * reads a local snapshot filled asynchronously by `subscribeUserReputation()`, so "absent" means
 * either the peer has no reputation or nothing has loaded yet.
 * [ReputationServiceFacade.scoreByUserProfileId] separates the two — it starts empty and is only
 * ever replaced wholesale by a payload, so a non-empty map proves a snapshot arrived and this
 * peer is genuinely unscored.
 *
 * A *thrown* lookup carries no such information: the call never reached a verdict, so the
 * snapshot says nothing about this peer and the result stays unknown. Feeding it through the
 * fallback would render a transport error as a confident zero, which is exactly what this
 * function exists to prevent — the offerbook card the user tapped through may be showing 4.5
 * stars for the same peer.
 *
 * Both verdicts are re-taken whenever the snapshot changes — see [observeReputation].
 *
 * On the node flavour "unknown" does not occur: `NodeReputationServiceFacade.getReputation` goes
 * through bisq2's `ReputationService.getReputationScore`, which answers a zero score for a peer it
 * has none for, so an unscored peer renders as zero stars there and as no stars on Bisq Connect.
 */
suspend fun ReputationServiceFacade.resolveReputation(profileId: String): ReputationScoreVO? {
    val result =
        try {
            getReputation(profileId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            PeerReputation.log.w(e) { "Failed to load reputation for peer" }
            return null
        }
    result.getOrNull()?.let { return it }
    return if (scoreByUserProfileId.value.isNotEmpty()) ZERO_REPUTATION else null
}

/**
 * Keeps the score current after the first paint. On the client flavour `getReputation` reads a
 * local cache filled asynchronously by the `REPUTATION` subscription, so opening a screen before
 * the first payload lands resolves to "unknown" — and without this, it would stay that way for as
 * long as the screen is open. The node fills its own map from the network just the same.
 *
 * The snapshot is the trigger, not the source: it carries scores, while the screens need the whole
 * score object, so each pass re-asks [resolveReputation] for this one peer. Narrowed to the two
 * facts that can change this peer's verdict — its own score, and whether anything has arrived at
 * all, which is what flips "unknown" to a real zero — because on the node that re-ask is not a
 * lookup: Bisq2 ranks a peer by sorting every score it holds, and every other peer's update would
 * pay for it.
 *
 * The first emission is deliberately not dropped: a `StateFlow` replays the current value, which
 * re-resolves to what the caller just wrote, and a `_uiState` conflates the identical copy. That
 * closes the gap between that read and this subscription, where a dropped emission would
 * otherwise be lost for good. The price is one extra resolve per open, right after the caller's
 * own — on the node, one extra sort over every known score — accepted for closing that gap.
 */
fun ReputationServiceFacade.observeReputation(profileId: String): Flow<ReputationScoreVO?> =
    scoreByUserProfileId
        .map { scores -> scores[profileId] to scores.isNotEmpty() }
        .distinctUntilChanged()
        .map { resolveReputation(profileId) }
