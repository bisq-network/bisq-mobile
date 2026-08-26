package network.bisq.mobile.presentation.community.contacts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import network.bisq.mobile.data.replicated.user.contact_list.ContactListEntryVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.service.contacts.ContactsServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.domain.utils.DateUtils
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter

/**
 * Contacts tab of the Community hub (#1238). Renders straight from
 * [ContactsServiceFacade.contacts] — never a navigation-time snapshot — so a mutation made
 * elsewhere (remove on Peer Profile) is already reflected here on back-navigation.
 */
class ContactsPresenter(
    mainPresenter: MainPresenter,
    private val contactsServiceFacade: ContactsServiceFacade,
    private val userProfileServiceFacade: UserProfileServiceFacade,
) : BasePresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(ContactsListUiState())
    val uiState: StateFlow<ContactsListUiState> = _uiState.asStateFlow()

    val userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage
        get() = userProfileServiceFacade::getUserProfileIcon

    override fun onViewAttached() {
        super.onViewAttached()
        contactsServiceFacade.contacts
            .onEach { entries -> _uiState.value = ContactsListUiState(contacts = entries.map { it.toListItem() }) }
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
