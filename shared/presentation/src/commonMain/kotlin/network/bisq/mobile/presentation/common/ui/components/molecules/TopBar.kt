package network.bisq.mobile.presentation.common.ui.components.molecules

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.dummy_user_profile_icon
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.presentation.common.ui.base.ViewPresenter
import network.bisq.mobile.presentation.common.ui.components.BackHandler
import network.bisq.mobile.presentation.common.ui.components.atoms.AutoResizeText
import network.bisq.mobile.presentation.common.ui.components.atoms.debouncedClickable
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.BisqLogoSmall
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.MyUserProfileIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.navigation.TabNavRoute
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

interface ITopBarPresenter : ViewPresenter {
    val userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage
    val userProfile: StateFlow<UserProfileVO?>
    val showAnimation: StateFlow<Boolean>
    val connectivityStatus: StateFlow<ConnectivityService.ConnectivityStatus>

    fun avatarEnabled(currentTab: TabNavRoute?): Boolean

    fun navigateToUserProfile()
}

/**
 * Stateful TopBar with dependencies - for production use
 * @param extraActions will be rendered before user avatar
 */
@Composable
fun TopBar(
    title: String = "",
    isHome: Boolean = false,
    backBehavior: (() -> Unit)? = null,
    showUserAvatar: Boolean = true,
    extraActions: @Composable (RowScope.() -> Unit)? = null,
) {
    val navigationManager: NavigationManager = koinInject()
    val presenter: ITopBarPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val currentTabDestination by navigationManager.currentTab.collectAsState()
    val showAnimation by presenter.showAnimation.collectAsState()
    val userProfile by presenter.userProfile.collectAsState()

    TopBarContent(
        title = title,
        isHome = isHome,
        showBackButton = navigationManager.showBackButton(),
        onBackClick = { presenter.onMainBackNavigation() },
        showUserAvatar = showUserAvatar,
        userProfile = userProfile,
        userProfileIconProvider = presenter.userProfileIconProvider,
        connectivityStatusFlow = presenter.connectivityStatus,
        showAnimation = showAnimation,
        avatarEnabled = presenter.avatarEnabled(currentTabDestination),
        onAvatarClick = { presenter.navigateToUserProfile() },
        extraActions = extraActions,
    )

    if (backBehavior != null) {
        BackHandler(onBackPress = { backBehavior.invoke() })
    }
}

/**
 * Stateless TopBar content - for previews and testing
 * @param extraActions will be rendered before user avatar
 * @param connectivityStatusFlow StateFlow for connectivity status (for production use)
 * @param connectivityStatus Static connectivity status (for previews only, ignored if connectivityStatusFlow is provided)
 */
@OptIn(ExperimentalMaterial3Api::class)
@ExcludeFromCoverage
@Composable
fun TopBarContent(
    title: String = "",
    isHome: Boolean = false,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    showUserAvatar: Boolean = true,
    userProfile: UserProfileVO? = null,
    userProfileIconProvider: (suspend (UserProfileVO) -> PlatformImage)? = null,
    connectivityStatusFlow: StateFlow<ConnectivityService.ConnectivityStatus>? = null,
    connectivityStatus: ConnectivityService.ConnectivityStatus = ConnectivityService.ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED,
    showAnimation: Boolean = false,
    avatarEnabled: Boolean = true,
    onAvatarClick: () -> Unit = {},
    extraActions: @Composable (RowScope.() -> Unit)? = null,
) {
    // Collect the flow if provided, otherwise use the static value (for previews)
    val currentConnectivityStatus = connectivityStatusFlow?.collectAsState()?.value ?: connectivityStatus
    TopAppBar(
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = BisqTheme.colors.mid_grey30,
                    )
                }
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = BisqTheme.colors.backgroundColor,
            ),
        title = {
            if (isHome) {
                BisqLogoSmall(modifier = Modifier.height(34.dp))
            } else {
                // we will allow overflow to 2 lines here, for better accessibility
                AutoResizeText(
                    text = title,
                    textStyle = BisqTheme.typography.h4Regular,
                    color = BisqTheme.colors.white,
                    maxLines = 2,
                )
            }
        },
        actions = {
            Row(
                modifier = Modifier.padding(end = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (extraActions != null) {
                    extraActions()
                }

                if (showUserAvatar) {
                    val userIconModifier =
                        Modifier
                            .size(BisqUIConstants.topBarAvatarSize)
                            .debouncedClickable(enabled = avatarEnabled) {
                                onAvatarClick()
                            }.semantics { contentDescription = "top_bar_avatar" }

                    BisqGap.H1()
                    if (userProfile != null && userProfileIconProvider != null) {
                        MyUserProfileIcon(
                            userProfile,
                            userProfileIconProvider,
                            modifier = userIconModifier,
                            connectivityStatus = currentConnectivityStatus,
                            showAnimations = showAnimation,
                        )
                    } else {
                        Image(
                            painterResource(Res.drawable.dummy_user_profile_icon),
                            "",
                            modifier = userIconModifier,
                        )
                    }
                }
            }
        },
    )
}

@Preview
@Composable
private fun TopBarContent_HomePreview() {
    BisqTheme.Preview {
        TopBarContent(
            title = "",
            isHome = true,
            showBackButton = false,
            showUserAvatar = true,
        )
    }
}

@Preview
@Composable
private fun TopBarContent_WithTitlePreview() {
    BisqTheme.Preview {
        TopBarContent(
            title = "Payment Accounts",
            isHome = false,
            showBackButton = true,
            showUserAvatar = true,
        )
    }
}

@Preview
@Composable
private fun TopBarContent_WithBackButtonPreview() {
    BisqTheme.Preview {
        TopBarContent(
            title = "Settings",
            isHome = false,
            showBackButton = true,
            showUserAvatar = true,
        )
    }
}

@Preview
@Composable
private fun TopBarContent_LongTitlePreview() {
    BisqTheme.Preview {
        TopBarContent(
            title = "This is a Very Long Title That Should Wrap to Multiple Lines",
            isHome = false,
            showBackButton = true,
            showUserAvatar = true,
        )
    }
}

@Preview
@Composable
private fun TopBarContent_NoAvatarPreview() {
    BisqTheme.Preview {
        TopBarContent(
            title = "Private Trades",
            isHome = false,
            showBackButton = true,
            showUserAvatar = false,
        )
    }
}
