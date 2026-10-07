package network.bisq.mobile.domain.service.offers

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.config.ConfigServiceFacade
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.network.NetworkServiceFacade
import network.bisq.mobile.data.service.offers.OffersServiceFacade
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.reputation.observeReputation
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.domain.utils.resultCatching
import kotlin.concurrent.Volatile

/** One of my sell offers whose amount my current reputation score no longer covers. */
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
 * Checks my sell offers against my current score while the app runs: on start, whenever my score
 * or selected profile changes, and on [refresh]. What the user dismissed lives here, in a
 * singleton, so it lasts until the next cold start rather than the next screen.
 *
 * Domain rather than presentation because every collaborator is domain and it touches no UI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OffersBelowReputationService(
    private val offersServiceFacade: OffersServiceFacade,
    private val reputationServiceFacade: ReputationServiceFacade,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val configServiceFacade: ConfigServiceFacade,
    private val networkServiceFacade: NetworkServiceFacade,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : Logging {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _state = MutableStateFlow(OffersBelowReputationState())
    val state: StateFlow<OffersBelowReputationState> = _state.asStateFlow()

    // My profile id and its latest known score; null until the score of the selected profile arrives.
    @Volatile
    private var lastKnownReputation: Pair<String, ReputationScoreVO>? = null

    // Per profile, the offers the user already saw and dismissed; the warning only returns for new ones.
    @Volatile
    private var dismissedOfferIdsByProfile: Map<String, Set<String>> = emptyMap()

    // Offers I deleted: Bisq Connect keeps them in its cache until the REMOVED event arrives.
    // Updated atomically, as a remove and a card deletion can add to it at the same time.
    private val removedOfferIds = MutableStateFlow<Set<String>>(emptySet())

    // Serializes a check's evaluate-and-publish with a whole remove, so neither overwrites the other.
    private val mutex = Mutex()

    @Volatile
    private var observeJob: Job? = null

    @Volatile
    private var checkJob: Job? = null

    /**
     * Idempotent: the lifecycle restart paths deactivate and activate the same singleton. Waits
     * for the initial data, because until then the node answers a score of 0 for data it has not
     * processed yet, which would flag valid offers.
     */
    fun start() {
        if (observeJob?.isActive == true) return
        observeJob =
            scope.launch {
                networkServiceFacade.allDataReceived.first { it }
                userProfileServiceFacade.selectedUserProfile
                    .filterNotNull()
                    .distinctUntilChangedBy { it.id }
                    .flatMapLatest { profile ->
                        reset()
                        reputationServiceFacade.observeReputation(profile.id).filterNotNull().map { profile.id to it }
                    }.collect { reputation ->
                        lastKnownReputation = reputation
                        refresh()
                    }
            }
    }

    suspend fun stop() {
        observeJob?.cancelAndJoin()
        observeJob = null
        reset()
    }

    /**
     * Mirrors the peer profile's offers load: on Bisq Connect my offers come from a cache that can
     * lag on a cold start, so an incomplete snapshot is re-read on an interval instead of publishing
     * a partial answer. The USD price can also land after the initial data on Bisq Node, and
     * without it every offer would pass.
     */
    fun refresh() {
        val (myProfileId, myScore) = lastKnownReputation ?: return
        checkJob?.cancel()
        checkJob =
            scope.launch {
                try {
                    var retriesLeft = SYNC_RETRIES
                    while (true) {
                        val published =
                            mutex.withLock {
                                // Two refreshes racing can leave a job reset() no longer reaches; it must not publish another profile's offers.
                                if (lastKnownReputation?.first != myProfileId) return@withLock true
                                val snapshot = offersServiceFacade.offersByAuthor(myProfileId)
                                if (snapshot.mayBeIncomplete || marketPriceServiceFacade.findUSDMarketPriceItem() == null) {
                                    return@withLock false
                                }
                                publish(myProfileId, findOffendingOffers(snapshot.offers, myScore))
                                true
                            }
                        if (published || retriesLeft == 0) break
                        retriesLeft--
                        delay(SYNC_RETRY_MS)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.e(e) { "Failed to check my offers against my reputation" }
                }
            }
    }

    fun dismiss() {
        val (myProfileId, _) = lastKnownReputation ?: return
        val dismissedOfferIds =
            _state.value.offendingOffers
                .map { it.offerId }
                .toSet()
        // Written before the state, so a check publishing meanwhile reads it on its retry; an offer that check added still shows.
        dismissedOfferIdsByProfile = dismissedOfferIdsByProfile + (myProfileId to dismissedOfferIds)
        _state.update { state -> state.copy(isWarningVisible = state.offendingOffers.any { it.offerId !in dismissedOfferIds }) }
    }

    /**
     * Deletes every flagged offer, one at a time, and returns whether all of them went. Runs in this
     * service's scope, so leaving the screen that asked does not stop it halfway. A second call
     * while one is running returns false without deleting anything.
     */
    suspend fun removeOffers(): Boolean {
        var started = false
        _state.update { state ->
            started = !state.isRemoving
            if (started) state.copy(isRemoving = true) else state
        }
        if (!started) return false
        return scope
            .async {
                try {
                    mutex.withLock {
                        var removedAll = true
                        for (offer in _state.value.offendingOffers) {
                            if (deleteOffer(offer.offerId)) {
                                forget(offer.offerId)
                            } else {
                                removedAll = false
                                markRemoveFailed(offer.offerId)
                            }
                        }
                        removedAll
                    }
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
            .onFailure { log.e(it) { "Failed to delete offer" } }
            .getOrDefault(false)

    private fun markRemoveFailed(offerId: String) {
        _state.update { state ->
            state.copy(offendingOffers = state.offendingOffers.map { if (it.offerId == offerId) it.copy(hasRemoveError = true) else it })
        }
    }

    private fun forget(offerId: String) {
        removedOfferIds.update { it + offerId }
        _state.update { state ->
            val offers = state.offendingOffers.filterNot { it.offerId == offerId }
            state.copy(offendingOffers = offers, isWarningVisible = state.isWarningVisible && offers.isNotEmpty())
        }
    }

    // A check of the previous profile must not publish its offers once another profile is selected.
    private fun reset() {
        checkJob?.cancel()
        lastKnownReputation = null
        _state.update { OffersBelowReputationState(isRemoving = it.isRemoving) }
    }

    private fun findOffendingOffers(
        offers: List<OfferItemPresentationModel>,
        myScore: ReputationScoreVO,
    ): List<OffendingOffer> {
        val limits = configServiceFacade.tradeAmountLimits.value
        return offers
            .filter { item ->
                item.offerId !in removedOfferIds.value &&
                    BisqEasyTradeAmountLimits.isSellOfferBelowReputation(marketPriceServiceFacade, item.bisqEasyOffer, myScore.totalScore, limits)
            }.map { item ->
                OffendingOffer(
                    offerId = item.offerId,
                    market = item.bisqEasyOffer.market,
                    formattedAmount = item.formattedQuoteAmount,
                )
            }
    }

    private fun publish(
        myProfileId: String,
        offers: List<OffendingOffer>,
    ) {
        _state.update { state ->
            val dismissedOfferIds = dismissedOfferIdsByProfile[myProfileId].orEmpty()
            val failedIds =
                state.offendingOffers
                    .filter { it.hasRemoveError }
                    .map { it.offerId }
                    .toSet()
            state.copy(
                offendingOffers = offers.map { it.copy(hasRemoveError = it.offerId in failedIds) },
                isWarningVisible = offers.any { it.offerId !in dismissedOfferIds },
            )
        }
    }

    private companion object {
        // Same ~30s window as the peer profile's offers load.
        const val SYNC_RETRIES = 10
        const val SYNC_RETRY_MS = 3_000L
    }
}
