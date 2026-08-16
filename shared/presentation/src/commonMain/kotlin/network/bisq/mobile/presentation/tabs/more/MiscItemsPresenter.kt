package network.bisq.mobile.presentation.tabs.more

import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.icon_question_mark
import bisqapps.shared.presentation.generated.resources.nav_accounts
import bisqapps.shared.presentation.generated.resources.nav_ignored_users
import bisqapps.shared.presentation.generated.resources.nav_network
import bisqapps.shared.presentation.generated.resources.nav_reputation
import bisqapps.shared.presentation.generated.resources.nav_resources
import bisqapps.shared.presentation.generated.resources.nav_settings
import bisqapps.shared.presentation.generated.resources.nav_support
import bisqapps.shared.presentation.generated.resources.nav_user
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import network.bisq.mobile.domain.service.capabilities.BackendCapabilitiesService
import network.bisq.mobile.domain.service.capabilities.Feature
import network.bisq.mobile.i18n.UiString
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter

abstract class MiscItemsPresenter(
    private val backendCapabilitiesService: BackendCapabilitiesService,
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(MiscItemsUiState())
    val uiState: StateFlow<MiscItemsUiState> = _uiState.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()
        backendCapabilitiesService.capabilities
            .map { buildSections(showNetwork = it.isSupported(Feature.NETWORK_INFO)) }
            .onEach { sections -> _uiState.update { it.copy(sections = sections) } }
            .launchIn(presenterScope)
    }

    abstract fun getPaymentAccountNavRoute(): NavRoute

    private fun buildSections(showNetwork: Boolean): List<MenuSection> {
        val identityItems =
            listOf(
                MenuItem(
                    label = UiString("mobile.more.userProfile"),
                    icon = Res.drawable.nav_user,
                    route = NavRoute.UserProfile,
                ),
                MenuItem(
                    label = UiString("mobile.settings.ignoredUsers"),
                    icon = Res.drawable.nav_ignored_users,
                    route = NavRoute.IgnoredUsers,
                ),
                MenuItem(
                    label = UiString("mobile.more.reputation"),
                    icon = Res.drawable.nav_reputation,
                    route = NavRoute.Reputation,
                ),
            )
        val tradingSetupItems =
            listOf(
                MenuItem(
                    label = UiString("mobile.more.paymentAccounts"),
                    icon = Res.drawable.nav_accounts,
                    route = getPaymentAccountNavRoute(),
                ),
            )
        val helpItems =
            listOf(
                MenuItem(
                    label = UiString("mobile.more.support"),
                    icon = Res.drawable.nav_support,
                    route = NavRoute.Support,
                ),
                MenuItem(
                    label = UiString("mobile.more.faqs"),
                    icon = Res.drawable.icon_question_mark,
                    route = NavRoute.Faqs,
                ),
            )
        val appItems: MutableList<MenuItem> =
            mutableListOf(
                MenuItem(
                    label = UiString("mobile.more.settings"),
                    icon = Res.drawable.nav_settings,
                    route = NavRoute.Settings,
                ),
                MenuItem(
                    label = UiString("mobile.more.resources"),
                    icon = Res.drawable.nav_resources,
                    route = NavRoute.Resources,
                ),
            )
        val appMenuItems = addCustomSettings(appItems).toMutableList()
        if (showNetwork) {
            appMenuItems.add(
                appMenuItems.size.coerceAtMost(2),
                MenuItem(
                    label = UiString("mobile.more.network"),
                    icon = Res.drawable.nav_network,
                    route = NavRoute.NetworkOverview,
                ),
            )
        }
        return listOf(
            MenuSection(title = UiString("mobile.more.section.identity"), items = identityItems),
            MenuSection(title = UiString("mobile.more.section.tradingSetup"), items = tradingSetupItems),
            MenuSection(title = UiString("mobile.more.section.help"), items = helpItems),
            MenuSection(title = UiString("mobile.more.section.app"), items = appMenuItems),
        )
    }

    fun onAction(action: MiscItemsUiAction) {
        when (action) {
            is MiscItemsUiAction.OnMenuItemClick -> navigateTo(action.route)
        }
    }

    abstract fun addCustomSettings(appItems: MutableList<MenuItem>): List<MenuItem>
}
