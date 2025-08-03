package network.bisq.mobile.presentation.ui.uicases.settings

import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

class IgnoredUsersPresenter(
    private val userProfileService: UserProfileServiceFacade,
    mainPresenter: MainPresenter
) : BasePresenter(mainPresenter), IIgnoredUsersPresenter {

    override fun getIgnoredUsers(): List<UserProfileVO> {
        return emptyList()
    }

    override fun unblockUser(userId: String) {
    }

    override fun settingsNavigateBack() {
        navigateBack()
    }
} 