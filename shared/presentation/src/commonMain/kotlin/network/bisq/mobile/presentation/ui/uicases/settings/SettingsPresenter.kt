package network.bisq.mobile.presentation.ui.uicases.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
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
open class SettingsPresenter(
    private val settingsRepository: SettingsRepository,
    private val userProfileService: UserProfileServiceFacade,
    mainPresenter: MainPresenter
) : BasePresenter(mainPresenter), ISettingsPresenter {

    private val _menuItems = MutableStateFlow<MenuItem?>(null)
    override val menuItems: StateFlow<MenuItem?> get() = _menuItems.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()

        _menuItems.value = buildMenu(hasIgnored = false)
        loadIgnoredUsers()
    }

    private fun buildMenu(hasIgnored: Boolean): MenuItem.Parent {
        val defaultList: MutableList<MenuItem> = mutableListOf(
            MenuItem.Leaf(label = "mobile.settings.general".i18n(), route = Routes.GeneralSettings),
            MenuItem.Leaf(label = "user.userProfile".i18n(), route = Routes.UserProfileSettings),
            MenuItem.Leaf(label = "paymentAccounts.headline".i18n(), route = Routes.PaymentAccountSettings),
        )
        if (hasIgnored) {
            defaultList.add(
                2, MenuItem.Leaf(label = "mobile.settings.ignoredUsers".i18n(), route = Routes.IgnoredUsers)
            )
        }
        return MenuItem.Parent(
            label = "Bisq", children = addCustomSettings(defaultList)
        )
    }

    protected open fun addCustomSettings(menuItems: MutableList<MenuItem>): List<MenuItem> {
        menuItems.add(MenuItem.Leaf("mobile.settings.trustedNode".i18n(), Routes.TrustedNodeSettings))
        menuItems.add(MenuItem.Leaf(label = "mobile.settings.about".i18n(), route = Routes.About))
        return menuItems.toList()
    }

    private fun loadIgnoredUsers() {
        launchIO {
            try {
                val ignoredUserIds = userProfileService.getIgnoredUserProfileIds()

                if (ignoredUserIds.isNotEmpty()) {
                    _menuItems.value = buildMenu(hasIgnored = true)
                }

            } catch (e: Exception) {
                log.e(e) { "Failed to load ignored users" }
            }
        }
    }

    override fun navigate(route: Routes) {
        navigateTo(route)
    }
}