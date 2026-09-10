package network.bisq.mobile.presentation.peer_profile

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.data.replicated.common.currency.MarketVOExtensions.marketCodes
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.service.chat.private_chat.PrivateChatNotPermittedException
import network.bisq.mobile.data.service.chat.private_chat.PrivateChatServiceFacade
import network.bisq.mobile.data.service.config.ConfigServiceFacade
import network.bisq.mobile.data.service.contacts.ContactsServiceFacade
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.offers.AuthorOffersSnapshot
import network.bisq.mobile.data.service.offers.OffersServiceFacade
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.trades.hasTradedWith
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.service.community.CommunityHubService
import network.bisq.mobile.domain.service.community.CommunitySegment
import network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.reputation.observeReputation
import network.bisq.mobile.presentation.common.reputation.resolveReputation
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.utils.BisqLinks
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offer.take_offer.TakeOfferCoordinator
import network.bisq.mobile.presentation.offer.take_offer.TakeOfferEligibility

class PeerProfilePresenter(
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val reputationServiceFacade: ReputationServiceFacade,
    private val privateChatServiceFacade: PrivateChatServiceFacade,
    private val contactsServiceFacade: ContactsServiceFacade,
    private val communityHubService: CommunityHubService,
    private val offersServiceFacade: OffersServiceFacade,
    private val tradesServiceFacade: TradesServiceFacade,
    private val takeOfferCoordinator: TakeOfferCoordinator,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val configServiceFacade: ConfigServiceFacade,
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(PeerProfileUiState())
    val uiState: StateFlow<PeerProfileUiState> = _uiState.asStateFlow()

    private val _isIgnoreActionEnabled = MutableStateFlow(true)
    val isIgnoreActionEnabled: StateFlow<Boolean> = _isIgnoreActionEnabled.asStateFlow()

    private val _isOpenPrivateChatEnabled = MutableStateFlow(true)

    private val _isContactActionEnabled = MutableStateFlow(true)
    val isContactActionEnabled: StateFlow<Boolean> = _isContactActionEnabled.asStateFlow()

    val userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage
        get() = userProfileServiceFacade::getUserProfileIcon

    private val _isTakeOfferEnabled = MutableStateFlow(true)
    val isTakeOfferEnabled: StateFlow<Boolean> = _isTakeOfferEnabled.asStateFlow()

    /** The peer this presenter is bound to; null until [initialize]. */
    private var profileId: String? = null
    private var ignoredStateJob: Job? = null
    private var privateChatSupportJob: Job? = null
    private var peerOffersJob: Job? = null
    private var hasTradedWithJob: Job? = null

    /** The loaded offer models by id, so a row tap resolves to the exact model the wizard needs. */
    private var peerOfferModels: Map<String, OfferItemPresentationModel> = emptyMap()

    /** Latest value of [PrivateChatServiceFacade.isSupported]; see [observePrivateChatSupport]. */
    private var isPrivateChatSupported: Boolean = false
    private var loadProfileJob: Job? = null
    private var reputationJob: Job? = null

    /**
     * Binds this presenter to one peer, once. The screen's `LaunchedEffect` re-fires whenever this
     * destination is revealed from the back stack, and reloading then would flash the loading state
     * over already-correct data — hence the early return.
     *
     * A second call for a *different* peer is ignored rather than handled: navigation gives every
     * destination its own back stack entry, so it also gives every peer its own presenter instance
     * (see `PresenterHolder` in `BackStackAwarePresenterLifecycleHelper`). The warning is there
     * because a caller that ever breaks that assumption would otherwise render the wrong peer in
     * silence.
     */
    fun initialize(profileId: String) {
        val bound = this.profileId
        if (bound != null) {
            if (bound != profileId) log.w { "Ignoring re-initialize with a different peer" }
            return
        }
        this.profileId = profileId
        loadProfile(profileId)
        observeIgnoredState(profileId)
        observePrivateChatSupport()
        observeContactState(profileId)
        loadPeerOffers(profileId)
        loadHasTradedWith(profileId)
    }

    /**
     * Under [RememberPresenterLifecycleBackStackAware] this presenter survives beneath the
     * take-offer wizard; re-arming the guard on reveal is what makes the offers tappable again
     * after backing out of it — same pattern as `OfferbookPresenter.resetTransientViewState`.
     */
    override fun onViewRevealed() {
        super.onViewRevealed()
        _isTakeOfferEnabled.value = true
    }

    // Renders from the facade's StateFlow so a mutation here is already reflected on the
    // Contacts tab when the user navigates back, and vice versa. isLoaded rides along because
    // before the snapshot arrives (and again after a re-pair resets it) an empty list is not
    // "not a contact" — the button locks instead of offering an "Add" that may be wrong.
    private fun observeContactState(profileId: String) {
        combine(
            contactsServiceFacade.contacts,
            contactsServiceFacade.isLoaded,
            communityHubService.liveSegments,
        ) { contacts, isLoaded, liveSegments ->
            Triple(
                contacts.firstOrNull { it.userProfile.id == profileId },
                isLoaded,
                CommunitySegment.CONTACTS in liveSegments,
            )
        }.onEach { (entry, isLoaded, showAction) ->
            _uiState.update {
                it.copy(
                    isContact = entry != null,
                    isContactStateLoading = !isLoaded,
                    contactDetails =
                        entry?.let { e ->
                            ContactDetailsUiState(tag = e.tag.orEmpty(), notes = e.notes.orEmpty(), trustScore = e.trustScore ?: 0.0)
                        },
                    showContactAction = showAction && !it.isOwnProfile,
                )
            }
        }.launchIn(presenterScope)
    }

    /**
     * Guarded like [onConfirmIgnore]: the guard disables the button and shows the loading overlay
     * while the toggle is in flight, so a tap gives immediate feedback and a second tap cannot
     * silently undo the first (tap–tap used to net out to "nothing happened"). A `false` result
     * means the list already held the desired state (see [ContactsServiceFacade.addContact]) —
     * nothing happened, so nothing is tracked.
     */
    private fun onToggleContact(add: Boolean) {
        val id = profileId ?: return
        guardedSuspendAction(_isContactActionEnabled, "onToggleContact") {
            val result = if (add) contactsServiceFacade.addContact(id) else contactsServiceFacade.removeContact(id)
            result
                .onSuccess { changed ->
                    if (changed) {
                        analyticsService.track(if (add) AnalyticsEvent.Contact.Added else AnalyticsEvent.Contact.Removed)
                    }
                }.onFailure {
                    log.e(it) { "Contact ${if (add) "add" else "remove"} failed" }
                    analyticsService.track(
                        AnalyticsEvent.Contact.ActionFailed(
                            if (add) AnalyticsEvent.Contact.FailedAction.ADD else AnalyticsEvent.Contact.FailedAction.REMOVE,
                        ),
                    )
                    showSnackbar("mobile.peerProfile.contacts.actionFailed".i18n(), type = SnackbarType.ERROR)
                }
        }
    }

    private fun onSaveContactDetails() {
        val id = profileId ?: return
        val draft = _uiState.value.contactDraft ?: return
        val before = _uiState.value.contactDetails ?: ContactDetailsUiState()
        _uiState.update { it.copy(showEditContactDetailsDialog = false, contactDraft = null) }
        presenterScope.launch {
            // Only changed fields hit the facade (null = unchanged); core would no-op on equal
            // values anyway, but skipping keeps the failure snackbar scoped to edits the user
            // actually made. ONE call for the whole Save: per-field requests made the card
            // update field by field, each a full round trip on the Connect app.
            val editedFields = mutableSetOf<AnalyticsEvent.Contact.EditedField>()
            val tag = draft.tag.takeIf { it != before.tag }?.also { editedFields += AnalyticsEvent.Contact.EditedField.TAG }
            val notes = draft.notes.takeIf { it != before.notes }?.also { editedFields += AnalyticsEvent.Contact.EditedField.NOTES }
            val trustScore =
                draft.trustScore
                    .takeIf { it != before.trustScore }
                    ?.also { editedFields += AnalyticsEvent.Contact.EditedField.TRUST_SCORE }
            val results =
                if (editedFields.isEmpty()) {
                    emptyList()
                } else {
                    listOf(contactsServiceFacade.updateContact(id, tag = tag, notes = notes, trustScore = trustScore))
                }
            if (results.any { it.isFailure }) {
                log.e { "Saving contact details failed" }
                analyticsService.track(AnalyticsEvent.Contact.ActionFailed(AnalyticsEvent.Contact.FailedAction.EDIT))
                showSnackbar("mobile.peerProfile.contacts.actionFailed".i18n(), type = SnackbarType.ERROR)
                // Reopen with the user's edits intact — same reasoning as [onReportFailure]: losing
                // a typed draft to a dropped connection would make the user compose it again.
                _uiState.update { it.copy(showEditContactDetailsDialog = true, contactDraft = draft) }
            } else if (editedFields.isNotEmpty()) {
                analyticsService.track(AnalyticsEvent.Contact.DetailsEdited(editedFields))
            }
        }
    }

    fun onAction(action: PeerProfileUiAction) {
        when (action) {
            PeerProfileUiAction.OnRetryLoadClick -> onRetryLoad()

            PeerProfileUiAction.OnSendPrivateMessageClick -> onSendPrivateMessage()

            PeerProfileUiAction.OnIgnoreClick ->
                _uiState.update { it.copy(showIgnoreConfirmDialog = true) }

            PeerProfileUiAction.OnConfirmIgnore -> onConfirmIgnore()

            PeerProfileUiAction.OnDismissIgnoreDialog ->
                _uiState.update { it.copy(showIgnoreConfirmDialog = false) }

            PeerProfileUiAction.OnUndoIgnoreClick -> onUndoIgnore()

            PeerProfileUiAction.OnAddContactClick -> onToggleContact(add = true)

            PeerProfileUiAction.OnRemoveContactClick -> onToggleContact(add = false)

            PeerProfileUiAction.OnEditContactDetailsClick ->
                _uiState.update {
                    it.copy(
                        showEditContactDetailsDialog = true,
                        contactDraft = it.contactDetails ?: ContactDetailsUiState(),
                    )
                }

            PeerProfileUiAction.OnDismissEditContactDetailsDialog ->
                _uiState.update { it.copy(showEditContactDetailsDialog = false, contactDraft = null) }

            is PeerProfileUiAction.OnContactTagChanged ->
                _uiState.update { it.copy(contactDraft = it.contactDraft?.copy(tag = action.tag.take(ContactsServiceFacade.MAX_TAG_LENGTH))) }

            is PeerProfileUiAction.OnContactNotesChanged ->
                _uiState.update {
                    it.copy(contactDraft = it.contactDraft?.copy(notes = action.notes.take(ContactsServiceFacade.MAX_NOTES_LENGTH)))
                }

            is PeerProfileUiAction.OnContactTrustScoreChanged ->
                _uiState.update { it.copy(contactDraft = it.contactDraft?.copy(trustScore = action.trustScore.coerceIn(0.0, 1.0))) }

            PeerProfileUiAction.OnSaveContactDetailsClick -> onSaveContactDetails()

            is PeerProfileUiAction.OnPeerOfferClick -> onPeerOfferClick(action.offerId)

            PeerProfileUiAction.OnDismissNotEnoughReputationDialog ->
                _uiState.update { it.copy(notEnoughReputation = null) }

            PeerProfileUiAction.OnNavigateToReputationClick -> {
                _uiState.update { it.copy(notEnoughReputation = null) }
                navigateTo(NavRoute.Reputation)
            }

            PeerProfileUiAction.OnOpenReputationWikiClick -> {
                _uiState.update { it.copy(notEnoughReputation = null) }
                navigateToUrl(BisqLinks.BUILD_REPUTATION_WIKI_URL)
            }

            PeerProfileUiAction.OnReportClick ->
                _uiState.update { it.copy(showReportDialog = true) }

            PeerProfileUiAction.OnReportSuccess -> {
                _uiState.update { it.copy(showReportDialog = false, reportDraft = null) }
            }

            is PeerProfileUiAction.OnReportFailure -> onReportFailure(action.reportMessage)
        }
    }

    private fun onRetryLoad() {
        val profileId = this.profileId ?: return
        _uiState.update { it.copy(isLoading = true, isLoadFailed = false) }
        loadProfile(profileId)
    }

    /**
     * Only one load may be in flight. Retrying supersedes the previous attempt rather than racing
     * it: `findUserProfile` is a node round-trip on the client flavour, so a slow first attempt can
     * otherwise land *after* a fast retry and replace a rendered profile with its own failure.
     * Cancelling the superseded job is what prevents that — both attempts are for the same peer, so
     * nothing downstream could tell their writes apart.
     */
    private fun loadProfile(profileId: String) {
        loadProfileJob?.cancel()
        loadProfileJob =
            presenterScope.launch {
                try {
                    if (isOwnProfile(profileId)) {
                        _uiState.update { it.copy(isOwnProfile = true, isLoading = false) }
                        return@launch
                    }

                    val userProfile = userProfileServiceFacade.findUserProfile(profileId)
                    if (userProfile == null) {
                        _uiState.update { it.copy(isNotFound = true, isLoading = false) }
                        return@launch
                    }

                    val reputation = reputationServiceFacade.resolveReputation(profileId)

                    _uiState.update {
                        it
                            .copy(
                                userProfile = userProfile,
                                displayName = userProfile.userName,
                                starRating = reputation?.fiveSystemScore ?: 0.0,
                                reputationScore = reputation?.totalScore ?: 0L,
                                isReputationUnknown = reputation == null,
                                isLoading = false,
                            ).let { updated -> updated.copy(canSendPrivateMessage = canSendPrivateMessage(updated)) }
                    }

                    observePeerReputation(profileId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Not `isNotFound`: the lookup crossing the network means this is just as likely a
                    // connection problem, and telling the user their peer does not exist would be wrong.
                    log.e(e) { "Failed to load peer profile" }
                    _uiState.update { it.copy(isLoadFailed = true, isLoading = false) }
                }
            }
    }

    /**
     * "Own" means any of my identities, not just the selected one — multiple profiles are supported
     * and this screen must never render for any of them.
     *
     * Checks the already-loaded owned-profiles flow first (no network round-trip, and its ids are
     * `UserProfileVO.id`, exactly what callers navigate with). Falls back to the identity-ids call
     * only when that flow hasn't been warmed yet, e.g. right after startup.
     */
    private suspend fun isOwnProfile(profileId: String): Boolean {
        val ownProfiles = userProfileServiceFacade.userProfiles.value
        if (ownProfiles.isNotEmpty()) {
            return ownProfiles.any { it.id == profileId }
        }
        return try {
            userProfileServiceFacade.getUserIdentityIds().contains(profileId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.w(e) { "Failed to read identity ids while checking own profile" }
            false
        }
    }

    private fun observePeerReputation(profileId: String) {
        reputationJob?.cancel()
        reputationJob =
            presenterScope.launch {
                reputationServiceFacade.observeReputation(profileId).collect { reputation ->
                    _uiState.update {
                        it.copy(
                            starRating = reputation?.fiveSystemScore ?: 0.0,
                            reputationScore = reputation?.totalScore ?: 0L,
                            isReputationUnknown = reputation == null,
                        )
                    }
                }
            }
    }

    /**
     * On Bisq Connect the capability set starts at the legacy baseline and only becomes accurate once
     * the node's manifest arrives. Reading it once meant a profile opened before then hid the button
     * for the life of the screen, with re-navigating the only way back.
     */
    private fun observePrivateChatSupport() {
        privateChatSupportJob?.cancel()
        privateChatSupportJob =
            presenterScope.launch {
                privateChatServiceFacade.isSupported.collect { isSupported ->
                    isPrivateChatSupported = isSupported
                    _uiState.update { it.copy(canSendPrivateMessage = canSendPrivateMessage(it)) }
                }
            }
    }

    /**
     * Binds to the facade's ignored-ids flow rather than tracking the state locally, so an
     * ignore/unignore performed elsewhere (chat context menu, ignored-users list) is reflected here
     * live. It is a StateFlow, so the current value arrives immediately and no seed call is needed.
     */
    private fun observeIgnoredState(profileId: String) {
        ignoredStateJob?.cancel()
        ignoredStateJob =
            presenterScope.launch {
                userProfileServiceFacade.ignoredProfileIds.collect { ignoredIds ->
                    val isIgnored = profileId in ignoredIds
                    _uiState.update {
                        // Recomputed here too, so ignoring hides the button immediately without
                        // waiting for a reload.
                        val updated = it.copy(isIgnored = isIgnored)
                        updated.copy(canSendPrivateMessage = canSendPrivateMessage(updated))
                    }
                }
            }
    }

    private fun onConfirmIgnore() {
        val profileId = this.profileId ?: return
        guardedSuspendAction(_isIgnoreActionEnabled, "onConfirmIgnore") {
            _uiState.update { it.copy(showIgnoreConfirmDialog = false) }
            try {
                // isIgnored is deliberately not set here — the ignoredProfileIds collector owns it,
                // so the two never disagree if the call fails.
                userProfileServiceFacade.ignoreUserProfile(profileId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Failed to ignore peer" }
                handleError(e)
            }
        }
    }

    /**
     * No confirmation dialog: un-ignoring is fully reversible, so the design surfaces it as a plain
     * visible button. (The ignored-users list does confirm — there a mis-tap is harder to notice.)
     */
    private fun onUndoIgnore() {
        val profileId = this.profileId ?: return
        guardedSuspendAction(_isIgnoreActionEnabled, "onUndoIgnore") {
            try {
                userProfileServiceFacade.undoIgnoreUserProfile(profileId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Failed to undo ignore for peer" }
                handleError(e)
            }
        }
    }

    /**
     * The single rule for offering a DM, so the two places that recompute it cannot drift apart.
     *
     * [PeerProfileUiState.isOwnProfile] is part of it even though `loadProfile` returns early for
     * own profiles: without it, this would depend on that early return to stay correct, and opening
     * a DM with yourself would create a `sorted(me, me)` channel.
     */
    private fun canSendPrivateMessage(state: PeerProfileUiState): Boolean =
        isPrivateChatSupported &&
            !state.isIgnored &&
            !state.isOwnProfile &&
            state.userProfile != null

    /**
     * Opens (or creates) the DM channel with this peer and navigates to it.
     *
     * Creating the channel is local-only in Bisq 2 — nothing reaches the peer until the first
     * message is sent — so this is safe to do on a tap.
     */
    private fun onSendPrivateMessage() {
        val profileId = this.profileId ?: return
        guardedSuspendAction(_isOpenPrivateChatEnabled, "onSendPrivateMessage") {
            _uiState.update { it.copy(isOpeningPrivateChat = true) }
            privateChatServiceFacade
                .findOrCreateChannel(profileId)
                .onSuccess { channelId ->
                    val destination = NavRoute.PrivateChat(channelId)
                    // Arriving from that very chat (chat → avatar → profile), pushing it again
                    // would grow the stack by two per round trip — going back IS the requested
                    // navigation there, and it bounds the PrivateChat ⇄ PeerProfile cycle.
                    if (navigationManager.isPreviousRoute(destination)) {
                        navigateBack()
                    } else {
                        navigateTo(destination)
                    }
                }.onFailure { e ->
                    // ensureActive, not `if (e is CancellationException) throw e`: a cancellation only
                    // reaches this handler when it is NOT ours. WebSocketApiClient rethrows the caller's
                    // own and deliberately keeps a request timeout as a failure — and
                    // TimeoutCancellationException IS a CancellationException, so rethrowing on type
                    // would swallow the snackbar for a send that really did time out, and skip the
                    // isOpeningPrivateChat reset below, leaving the button spinning.
                    currentCoroutineContext().ensureActive()
                    log.e(e) { "Failed to open a private chat" }
                    // A withheld permission is not a connection problem, and telling the user to
                    // retry would send them in circles — only a re-pairing can fix it.
                    val message =
                        if (e is PrivateChatNotPermittedException) {
                            "mobile.privateChats.notPermitted".i18n()
                        } else {
                            "mobile.privateChats.openChat.failed".i18n()
                        }
                    showSnackbar(message, type = SnackbarType.ERROR)
                }
            _uiState.update { it.copy(isOpeningPrivateChat = false) }
        }
    }

    /**
     * Closes the dialog but holds on to [reportMessage]: reporting can fail on a dropped connection,
     * and losing the text the user just wrote would make them compose it a second time. The error
     * snackbar is `ReportUserPresenter`'s — raising a second one here would double it.
     */
    private fun onReportFailure(reportMessage: String) {
        _uiState.update { it.copy(showReportDialog = false, reportDraft = reportMessage) }
    }

    /**
     * Loads the peer's offers for the "Trade again" section. On Bisq Connect the answer comes from
     * the all-markets cache, which can lag over a cold Tor connection — while the snapshot reports
     * itself as possibly incomplete, re-query on an interval (the query is a local cache read, no
     * round trip) so a late OFFERS snapshot still fills the section without user action. Bounded:
     * once the retries are spent, the syncing row gives way to whatever was found.
     */
    private fun loadPeerOffers(profileId: String) {
        peerOffersJob?.cancel()
        peerOffersJob =
            presenterScope.launch {
                try {
                    if (isOwnProfile(profileId)) return@launch
                    var retriesLeft = PEER_OFFERS_SYNC_RETRIES
                    while (true) {
                        val snapshot = offersServiceFacade.offersByAuthor(profileId)
                        val stillSyncing = snapshot.mayBeIncomplete && retriesLeft > 0
                        publishPeerOffers(snapshot, stillSyncing)
                        if (!stillSyncing) break
                        retriesLeft--
                        delay(PEER_OFFERS_SYNC_RETRY_MS)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.e(e) { "Failed to load the peer's offers" }
                    _uiState.update { it.copy(isPeerOffersSyncing = false) }
                }
            }
    }

    private suspend fun publishPeerOffers(
        snapshot: AuthorOffersSnapshot,
        stillSyncing: Boolean,
    ) {
        markReputationGatedOffers(snapshot.offers)
        peerOfferModels = snapshot.offers.associateBy { it.offerId }
        val groups =
            snapshot.offers
                .groupBy { it.bisqEasyOffer.market.marketCodes }
                .map { (codes, offers) ->
                    PeerOffersMarketGroupUiState(codes, offers.sortedByDescending { it.bisqEasyOffer.date })
                }.sortedByDescending { group ->
                    group.offers
                        .first()
                        .bisqEasyOffer.date
                }
        _uiState.update { it.copy(peerOffers = groups, isPeerOffersSyncing = stillSyncing) }
    }

    /**
     * Marks the BUY-direction rows this user could not cover as seller, mirroring the offerbook's
     * `processOffer`: the row renders muted with the reason instead of failing at tap time. My
     * score is fetched once for the whole list — on the client it can be a websocket round trip.
     */
    private suspend fun markReputationGatedOffers(offers: List<OfferItemPresentationModel>) {
        val myProfile = userProfileServiceFacade.selectedUserProfile.value ?: return
        val buyOffers = offers.filter { it.bisqEasyOffer.direction == DirectionEnum.BUY }
        if (buyOffers.isEmpty()) return
        val myReputation =
            runCatching { reputationServiceFacade.getReputation(myProfile.id) }
                .getOrElse { Result.failure(it) }
        if (myReputation.exceptionOrNull() is CancellationException) {
            currentCoroutineContext().ensureActive()
        }
        val limits = configServiceFacade.tradeAmountLimits.value
        buyOffers.forEach { item ->
            item.isInvalidDueToReputation =
                runCatching {
                    BisqEasyTradeAmountLimits.isBuyOfferInvalid(
                        item = item,
                        useCache = true,
                        marketPriceServiceFacade = marketPriceServiceFacade,
                        reputationServiceFacade = reputationServiceFacade,
                        userProfileId = myProfile.id,
                        limits = limits,
                        preFetchedReputation = myReputation,
                    )
                }.getOrDefault(false)
        }
    }

    /**
     * Failure leaves [PeerProfileUiState.hasTradedBefore] false rather than erroring: the section's
     * gate also admits contacts, so an under-report (old node without the closed-trades API, or a
     * dropped connection) hides the section for a non-contact — the safe direction.
     */
    private fun loadHasTradedWith(profileId: String) {
        hasTradedWithJob?.cancel()
        hasTradedWithJob =
            presenterScope.launch {
                try {
                    if (isOwnProfile(profileId)) return@launch
                    val traded = tradesServiceFacade.hasTradedWith(profileId)
                    _uiState.update { it.copy(hasTradedBefore = traded) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.w(e) { "Failed to determine trade history with this peer; the contact gate stands alone" }
                }
            }
    }

    /**
     * Every row funnels through the shared eligibility gate: eligible goes straight into the
     * take-offer wizard (same first-screen routing as the offerbook), ineligible opens the
     * reputation-requirement dialog with the gate's own copy. The guard is re-armed on reveal
     * (see [onViewRevealed]) after navigating away eligible.
     */
    private fun onPeerOfferClick(offerId: String) {
        val offer = peerOfferModels[offerId]
        if (offer == null) {
            log.w { "Tapped peer offer is no longer loaded; ignoring" }
            return
        }
        guardedSuspendAction(_isTakeOfferEnabled, "onPeerOfferClick", reEnableGuardOnComplete = false) {
            try {
                val myProfile = userProfileServiceFacade.selectedUserProfile.value
                checkNotNull(myProfile) { "No selected user profile" }
                when (val eligibility = takeOfferCoordinator.checkTakeOfferEligibility(offer, myProfile)) {
                    is TakeOfferEligibility.Eligible -> {
                        takeOfferCoordinator.selectOfferToTake(offer)
                        navigateTo(takeOfferCoordinator.firstScreen())
                    }
                    is TakeOfferEligibility.NotEnoughReputation -> {
                        _uiState.update {
                            it.copy(
                                notEnoughReputation =
                                    NotEnoughReputationUiState(
                                        headline = eligibility.headline,
                                        message = eligibility.message,
                                        isSellerAsTakerWarning = eligibility.isSellerAsTakerWarning,
                                    ),
                            )
                        }
                        _isTakeOfferEnabled.value = true
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                log.e(e) { "Failed to start taking the peer's offer" }
                showSnackbar("mobile.bisqEasy.offerbook.unableToTakeOffer".i18n(offer.offerId), type = SnackbarType.ERROR)
                _isTakeOfferEnabled.value = true
            }
        }
    }

    private companion object {
        // Together ~30s of local re-reads, matching the client offerbook's own loading window
        // for a cold Tor start.
        private const val PEER_OFFERS_SYNC_RETRIES = 10
        private const val PEER_OFFERS_SYNC_RETRY_MS = 3_000L
    }
}
