package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.BackHandler
import network.bisq.mobile.presentation.ui.components.atoms.AutoResizeText
import network.bisq.mobile.presentation.ui.components.atoms.icons.BisqLogoSmall
import network.bisq.mobile.presentation.ui.components.atoms.icons.MyUserProfileIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.navigation.TabNavRoute
import network.bisq.mobile.presentation.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.jetbrains.compose.resources.painterResource
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
 * @param extraActions will be rendered before user avatar
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val connectivityStatus by presenter.connectivityStatus.collectAsState()

    val defaultBackButton: @Composable () -> Unit = {
        IconButton(onClick = {
            presenter.onMainBackNavigation()
        }) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = BisqTheme.colors.mid_grey30
            )
        }
    }

    TopAppBar(
        navigationIcon = {
            if (navigationManager.showBackButton()) {
                defaultBackButton()
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BisqTheme.colors.backgroundColor, //Color.DarkGray,
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
                verticalAlignment = Alignment.CenterVertically
            ) {

                if (extraActions != null) {
                    extraActions()
                }

                if (showUserAvatar) {
                    val userIconModifier = Modifier
                        .size(BisqUIConstants.topBarAvatarSize)
                        .clickable {
                            if (presenter.avatarEnabled(currentTabDestination)) {
                                presenter.navigateToUserProfile()
                            }
                        }
                        .semantics { contentDescription = "top_bar_avatar" }

                    BisqGap.H1()
                    if (userProfile != null) {
                        MyUserProfileIcon(
                            userProfile!!,
                            presenter.userProfileIconProvider,
                            modifier = userIconModifier,
                            connectivityStatus = connectivityStatus,
                            showAnimation
                        )
                    } else {
                        Image(
                            painterResource(Res.drawable.dummy_user_profile_icon), "",
                            modifier = userIconModifier
                        )
                    }
                }
            }
        },
    )

    if (backBehavior != null) {
        BackHandler(onBackPressed = { backBehavior.invoke() })
    }
}