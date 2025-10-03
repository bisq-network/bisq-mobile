package network.bisq.mobile.presentation.ui.uicases.settings

import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.nav_accounts
import bisqapps.shared.presentation.generated.resources.nav_ignored_users
import bisqapps.shared.presentation.generated.resources.nav_reputation
import bisqapps.shared.presentation.generated.resources.nav_resources
import bisqapps.shared.presentation.generated.resources.nav_settings
import bisqapps.shared.presentation.generated.resources.nav_support
import bisqapps.shared.presentation.generated.resources.nav_trusted_node
import bisqapps.shared.presentation.generated.resources.nav_user
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.NavRoute
import org.jetbrains.compose.resources.DrawableResource

/**
 * SettingsPresenter with default implementation
 */
open class MiscItemsPresenter(
    private val userProfileService: UserProfileServiceFacade,
    mainPresenter: MainPresenter
) : BasePresenter(mainPresenter) {

    sealed class MenuItem(val label: String, val icon: DrawableResource? = null) {
        class Leaf(label: String, icon: DrawableResource, val route: NavRoute) :
            MenuItem(label, icon)
        class Parent(label: String, val children: List<MenuItem>) : MenuItem(label)
    }


    private val _menuItems = MutableStateFlow<MenuItem?>(null)
    val menuItems: StateFlow<MenuItem?> get() = _menuItems.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()

        _menuItems.value = buildMenu(showIgnoredUser = false)
        loadIgnoredUsers()
    }

    private fun buildMenu(showIgnoredUser: Boolean): MenuItem.Parent {
        val defaultList: MutableList<MenuItem> = mutableListOf(
            MenuItem.Leaf(
                label = "mobile.more.support".i18n(),
                icon = Res.drawable.nav_support,
                route = NavRoute.Support
            ),
            MenuItem.Leaf(
                label = "mobile.more.paymentAccounts".i18n(),
                icon = Res.drawable.nav_accounts,
                route = NavRoute.PaymentAccounts
            ),
            MenuItem.Leaf(
                label = "mobile.more.reputation".i18n(),
                icon = Res.drawable.nav_reputation,
                route = NavRoute.Reputation
            ),
            MenuItem.Leaf(
                label = "mobile.more.userProfile".i18n(),
                icon = Res.drawable.nav_user,
                route = NavRoute.UserProfile
            ),
            MenuItem.Leaf(
                label = "mobile.more.settings".i18n(),
                icon = Res.drawable.nav_settings,
                route = NavRoute.Settings
            ),
            MenuItem.Leaf(
                label = "mobile.more.resources".i18n(),
                icon = Res.drawable.nav_resources,
                route = NavRoute.Resources
            )
        )
        if (showIgnoredUser) {
            defaultList.add(
                defaultList.size - 1, MenuItem.Leaf(
                    label = "mobile.settings.ignoredUsers".i18n(),
                    icon = Res.drawable.nav_ignored_users,
                    route = NavRoute.IgnoredUsers
                )
            )
        }
        return MenuItem.Parent(
            label = "Bisq", children = addCustomSettings(defaultList)
        )
    }

    protected open fun addCustomSettings(menuItems: MutableList<MenuItem>): List<MenuItem> {
        menuItems.add(
            menuItems.size - 2, MenuItem.Leaf(
                "mobile.more.trustedNode".i18n(),
                icon = Res.drawable.nav_trusted_node,
                NavRoute.TrustedNodeSetupSettings
            )
        )
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

    fun onNavigateTo(route: NavRoute) {
        navigateTo(route)
    }
}