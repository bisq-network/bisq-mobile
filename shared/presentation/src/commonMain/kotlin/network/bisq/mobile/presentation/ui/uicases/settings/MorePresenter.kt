package network.bisq.mobile.presentation.ui.uicases.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.components.molecules.settings.MenuItem
import network.bisq.mobile.presentation.ui.navigation.Routes

/**
 * SettingsPresenter with default implementation
 */
open class MorePresenter(
    private val settingsRepository: SettingsRepository,
    private val userProfileService: UserProfileServiceFacade,
    mainPresenter: MainPresenter
) : BasePresenter(mainPresenter) {

    private val _menuItems = MutableStateFlow<MenuItem?>(null)
    val menuItems: StateFlow<MenuItem?> get() = _menuItems.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()

        _menuItems.value = buildMenu(showIgnoredUser = false)
        loadIgnoredUsers()
    }

    private fun buildMenu(showIgnoredUser: Boolean): MenuItem.Parent {
        val defaultList: MutableList<MenuItem> = mutableListOf(
            MenuItem.Leaf(label = "mobile.more.settings".i18n(), route = Routes.Settings),
            MenuItem.Leaf(label = "mobile.more.support".i18n(), route = Routes.Support),
            MenuItem.Leaf(label = "mobile.more.reputation".i18n(), route = Routes.Reputation),
            MenuItem.Leaf(label = "mobile.more.userProfile".i18n(), route = Routes.UserProfile),
            MenuItem.Leaf(label = "mobile.more.paymentAccounts".i18n(), route = Routes.PaymentAccounts),
            MenuItem.Leaf(label = "mobile.more.resources".i18n(), route = Routes.Resources)
        )
        if (showIgnoredUser) {
            defaultList.add(
                defaultList.size - 1, MenuItem.Leaf(label = "mobile.settings.ignoredUsers".i18n(), route = Routes.IgnoredUsers)
            )
        }
        return MenuItem.Parent(
            label = "Bisq", children = addCustomSettings(defaultList)
        )
    }

    protected open fun addCustomSettings(menuItems: MutableList<MenuItem>): List<MenuItem> {
        menuItems.add(1, MenuItem.Leaf("mobile.more.trustedNode".i18n(), Routes.TrustedNodeSettings))
        return menuItems.toList()
    }

    private fun loadIgnoredUsers() {
        launchIO {
            try {
                val ignoredUserIds = userProfileService.getIgnoredUserProfileIds()

                if (ignoredUserIds.isNotEmpty()) {
                    _menuItems.value = buildMenu(showIgnoredUser = true)
                }

            } catch (e: Exception) {
                log.e(e) { "Failed to load ignored users" }
            }
        }
    }

    fun onNavigateTo(route: Routes) {
        navigateTo(route)
    }
}