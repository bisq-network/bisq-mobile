package network.bisq.mobile.presentation.community.contacts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import network.bisq.mobile.data.replicated.user.contact_list.ContactListEntryVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.service.contacts.ContactsServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.utils.DateUtils
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter

/**
 * Contacts tab of the Community hub. Renders straight from
 * [ContactsServiceFacade.contacts] — never a navigation-time snapshot — so a mutation made
 * elsewhere (remove on Peer Profile) is already reflected here on back-navigation.
 */
class ContactsPresenter(
    mainPresenter: MainPresenter,
    private val contactsServiceFacade: ContactsServiceFacade,
    private val userProfileServiceFacade: UserProfileServiceFacade,
) : BasePresenter(mainPresenter) {
    override fun analyticsScreenEvent(): AnalyticsEvent.ScreenOpened = AnalyticsEvent.ScreenOpened.CommunityContacts

    // Seeded synchronously from the facade's CURRENT value, so an already-loaded list renders
    // without an empty-state flash. isLoading comes from the facade's OWN loaded signal — a
    // StateFlow's first emission cannot distinguish "not loaded yet" from "genuinely empty"
    // (it replays its empty initial value immediately), which on Connect showed "no contacts
    // yet" for the seconds the subscription snapshot took to arrive over Tor.
    private val _uiState =
        MutableStateFlow(
            ContactsListUiState(
                contacts = contactsServiceFacade.contacts.value.map { it.toListItem() },
                isLoading = !contactsServiceFacade.isLoaded.value,
            ),
        )
    val uiState: StateFlow<ContactsListUiState> = _uiState.asStateFlow()

    val userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage
        get() = userProfileServiceFacade::getUserProfileIcon

    override fun onViewAttached() {
        super.onViewAttached()
        contactsServiceFacade.contacts
            .combine(contactsServiceFacade.isLoaded) { entries, isLoaded ->
                ContactsListUiState(contacts = entries.map { it.toListItem() }, isLoading = !isLoaded)
            }.onEach { _uiState.value = it }
            .launchIn(presenterScope)
    }

    fun onAction(action: ContactsUiAction) {
        when (action) {
            is ContactsUiAction.OnContactClick -> navigateTo(NavRoute.PeerProfile(action.profileId))
        }
    }

    private fun ContactListEntryVO.toListItem(): ContactListItemUiState =
        ContactListItemUiState(
            id = userProfile.id,
            peerProfile = userProfile,
            trustScore = trustScore ?: 0.0,
            contactReason = contactReason,
            dateAddedLabel = DateUtils.toDateTime(date),
            tag = tag,
        )
}

sealed interface ContactsUiAction {
    data class OnContactClick(
        val profileId: String,
    ) : ContactsUiAction
}
