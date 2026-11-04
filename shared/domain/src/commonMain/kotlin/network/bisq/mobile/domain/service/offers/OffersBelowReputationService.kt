package network.bisq.mobile.domain.service.offers

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.service.network.NetworkServiceFacade
import network.bisq.mobile.data.service.offers.OffersServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.coroutines.DispatcherProvider
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.domain.utils.redactedSummary
import network.bisq.mobile.domain.utils.resultCatching
import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.milliseconds

/** One of my sell offers whose amount the node says my reputation no longer covers. */
data class OffendingOffer(
    val offerId: String,
    val market: MarketVO,
    val formattedAmount: String,
    val hasRemoveError: Boolean = false,
)

data class OffersBelowReputationState(
    /** Always the currently offending offers: feeds both the warning and the card badges. */
    val offendingOffers: List<OffendingOffer> = emptyList(),
    /** False once the user dismissed exactly these offers (or a subset of them). */
    val isWarningVisible: Boolean = false,
    val isRemoving: Boolean = false,
)

/**
 * The node judges my offers and sends the seller reputation verdict with each item (bisq2#5095);
 * this service reads it from the offers cache once a burst of cache changes settles, on start, on
 * a profile switch and on [refresh]. Only while the node reports its data as complete: while it resyncs
 * nothing is judged and what was flagged is cleared. What the user dismissed lives here, in a
 * singleton, so it lasts until the next cold start rather than the next screen.
 *
 * Domain rather than presentation because every collaborator is domain and it touches no UI.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class OffersBelowReputationService(
    private val offersServiceFacade: OffersServiceFacade,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val networkServiceFacade: NetworkServiceFacade,
    dispatcherProvider: DispatcherProvider,
) : Logging {
    private companion object {
        val CHECK_DEBOUNCE = 500.milliseconds
    }

    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.default)

    private val _state = MutableStateFlow(OffersBelowReputationState())
    val state: StateFlow<OffersBelowReputationState> = _state.asStateFlow()

    // Null until a profile is selected while the node's data is complete.
    @Volatile
    private var myProfileId: String? = null

    // Per profile, the offers the user already saw and dismissed; the warning only returns for new ones.
    private val dismissedOfferIdsByProfile = atomic<Map<String, Set<String>>>(emptyMap())

    // Offers I deleted: Bisq Connect keeps them in its cache until the REMOVED event arrives.
    private val removedOfferIds = atomic<Set<String>>(emptySet())

    // Serializes a check's read-and-publish with reset(), so a check past its guard cannot publish after the clear.
    private val mutex = Mutex()

    @Volatile
    private var observeJob: Job? = null

    // Swapped atomically: refresh() is called from the collector, the presenter and a remove at once.
    private val checkJob = atomic<Job?>(null)

    /**
     * Idempotent: the lifecycle restart paths deactivate and activate the same singleton. Follows the
     * node's sync flag: until the node has all the data its verdicts can rest on scores it has not
     * processed yet, so meanwhile nothing is judged and what was flagged is cleared.
     */
    fun start() {
        if (observeJob?.isActive == true) return
        observeJob =
            scope.launch {
                networkServiceFacade.allDataReceived
                    .flatMapLatest { synced ->
                        reset()
                        if (synced) checkTriggers() else emptyFlow()
                    }.collect { refresh() }
            }
    }

    // Every change of the offers cache for the selected profile, starting with its current state.
    private fun checkTriggers(): Flow<Long> =
        userProfileServiceFacade.selectedUserProfile
            .filterNotNull()
            .distinctUntilChangedBy { it.id }
            .flatMapLatest { profile ->
                reset()
                myProfileId = profile.id
                // The offers cache bumps per message of every market, and a check reads all of it: one per burst.
                offersServiceFacade.offersRevision.debounce(CHECK_DEBOUNCE)
            }

    suspend fun stop() {
        observeJob?.cancelAndJoin()
        observeJob = null
        reset()
    }

    fun refresh() {
        val check =
            scope.launch(start = CoroutineStart.LAZY) {
                // Read here rather than by the caller, so the check that survives a swap uses the latest profile.
                val profileId = myProfileId ?: return@launch
                try {
                    mutex.withLock {
                        val offers = offersServiceFacade.offersByAuthor(profileId).offers
                        // After the read, as the profile can change during it: a check reset() did not stop
                        // (or no longer reaches, when two refreshes race) must not publish another profile's offers.
                        if (myProfileId != profileId) return@withLock
                        publish(profileId, findOffendingOffers(offers))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.e(e) { "Failed to read the reputation verdict of my offers" }
                }
            }
        checkJob.getAndSet(check)?.cancel()
        check.start()
    }

    fun dismiss() {
        val profileId = myProfileId ?: return
        // Added to what was dismissed before: an offer that stopped being flagged in between may be flagged again.
        val offendingIds = _state.value.offendingOffers.map { it.offerId }
        // Written before the state, so a check publishing meanwhile reads it; an offer that check added still shows.
        dismissedOfferIdsByProfile.update { it + (profileId to it[profileId].orEmpty() + offendingIds) }
        val dismissedOfferIds = dismissedOfferIdsByProfile.value[profileId].orEmpty()
        _state.update { state -> state.copy(isWarningVisible = state.offendingOffers.any { it.offerId !in dismissedOfferIds }) }
    }

    enum class RemoveOutcome { REMOVED_ALL, SOME_FAILED, ALREADY_RUNNING }

    /**
     * Deletes every flagged offer, one at a time. Runs in this service's scope, so leaving the
     * screen that asked does not stop it halfway. A second call while one is running deletes
     * nothing and says so, as it is not a failure. Not under the mutex: every delete is a
     * round-trip, and a reset must not wait for them. The state updates are atomic, so a check
     * publishing meanwhile cannot undo a delete or drop a failure mark.
     */
    suspend fun removeOffers(): RemoveOutcome {
        var started = false
        _state.update { state ->
            started = !state.isRemoving
            if (started) state.copy(isRemoving = true) else state
        }
        if (!started) return RemoveOutcome.ALREADY_RUNNING
        return scope
            .async {
                try {
                    var removedAll = true
                    for (offer in _state.value.offendingOffers) {
                        if (deleteOffer(offer.offerId)) {
                            forget(offer.offerId)
                        } else {
                            removedAll = false
                            markRemoveFailed(offer.offerId)
                        }
                    }
                    // An offer can fail to delete because it is already gone: check what is left.
                    if (!removedAll) refresh()
                    if (removedAll) RemoveOutcome.REMOVED_ALL else RemoveOutcome.SOME_FAILED
                } finally {
                    _state.update { it.copy(isRemoving = false) }
                }
            }.await()
    }

    /** For an offer deleted elsewhere, such as from its card. */
    fun markRemoved(offerId: String) {
        forget(offerId)
    }

    // A false result and a thrown error both count as a failed removal (Bisq Connect throws, a dropped
    // connection as a CancellationException); only this job's own cancellation propagates.
    private suspend fun deleteOffer(offerId: String): Boolean =
        resultCatching { offersServiceFacade.deleteOffer(offerId).getOrThrow() }
            .onFailure { log.e { "Failed to delete offer: ${it.redactedSummary()}" } }
            .getOrDefault(false)

    private fun markRemoveFailed(offerId: String) {
        _state.update { state ->
            state.copy(offendingOffers = state.offendingOffers.map { if (it.offerId == offerId) it.copy(hasRemoveError = true) else it })
        }
    }

    private fun forget(offerId: String) {
        removedOfferIds.update { it + offerId }
        val dismissedOfferIds = dismissedOfferIdsByProfile.value[myProfileId].orEmpty()
        _state.update { state ->
            val offers = state.offendingOffers.filterNot { it.offerId == offerId }
            state.copy(offendingOffers = offers, isWarningVisible = state.isWarningVisible && offers.any { it.offerId !in dismissedOfferIds })
        }
    }

    // A check of the previous profile must not publish its offers once another profile is selected.
    // Cleared under the mutex: a check past its profile guard still publishes, and this must land after it.
    private suspend fun reset() {
        checkJob.getAndSet(null)?.cancel()
        mutex.withLock {
            myProfileId = null
            _state.update { OffersBelowReputationState(isRemoving = it.isRemoving) }
        }
    }

    // Only an explicit false: a node before bisq2#5095 sends no verdict, and that is nothing to warn about.
    private fun findOffendingOffers(offers: List<OfferItemPresentationModel>): List<OffendingOffer> =
        offers
            .filter { it.hasSellerSufficientReputation == false }
            .map { item ->
                OffendingOffer(
                    offerId = item.offerId,
                    market = item.bisqEasyOffer.market,
                    formattedAmount = item.formattedQuoteAmount,
                )
            }

    private fun publish(
        profileId: String,
        offers: List<OffendingOffer>,
    ) {
        _state.update { state ->
            // Read here, not when the offers were judged: one can be deleted elsewhere in between.
            val remaining = offers.filterNot { it.offerId in removedOfferIds.value }
            val dismissedOfferIds = dismissedOfferIdsByProfile.value[profileId].orEmpty()
            val failedIds =
                state.offendingOffers
                    .filter { it.hasRemoveError }
                    .map { it.offerId }
                    .toSet()
            state.copy(
                offendingOffers = remaining.map { it.copy(hasRemoveError = it.offerId in failedIds) },
                isWarningVisible = remaining.any { it.offerId !in dismissedOfferIds },
            )
        }
    }
}
