package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.BackHandler
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.animations.ShineOverlay
import network.bisq.mobile.presentation.ui.components.atoms.icons.BisqLogoSmall
import network.bisq.mobile.presentation.ui.components.atoms.icons.UserIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject

interface ITopBarPresenter : ViewPresenter {
    val uniqueAvatar: StateFlow<PlatformImage?>
    val showAnimation: StateFlow<Boolean>
    val connectivityStatus: StateFlow<ConnectivityService.ConnectivityStatus>

    fun avatarEnabled(currentTab: String?): Boolean
    fun navigateToUserProfile()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String = "",
    isHome: Boolean = false,
    customBackButton: @Composable (() -> Unit)? = null,
    backConfirmation: Boolean = false,
    backBehavior: (() -> Unit)? = null,
    isFlowScreen: Boolean = false,
    stepText: String = "",
) {
    val presenter: ITopBarPresenter = koinInject()
    val navController: NavHostController = presenter.getRootNavController()
    val tabNavController: NavHostController = presenter.getRootTabNavController()

    val showAnimation = presenter.showAnimation.collectAsState().value
    var showBackConfirmationDialog by remember { mutableStateOf(false) }

    val currentTab = tabNavController.currentBackStackEntryAsState().value?.destination?.route

    val showBackButton = (customBackButton == null &&
                          navController.previousBackStackEntry != null &&
                          !presenter.isAtHome())

    val connectivityStatus = presenter.connectivityStatus.collectAsState().value

    val defaultBackButton: @Composable () -> Unit = {
        IconButton(onClick = {
            if (navController.previousBackStackEntry != null) {
                if (backConfirmation) {
                    if (!showBackConfirmationDialog) {
                        showBackConfirmationDialog = true
                    }
                } else {
                    presenter.onMainBackNavigation()
                }
            }
        }) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = BisqTheme.colors.mid_grey30
            )
        }
    }

    RememberPresenterLifecycle(presenter)

    TopAppBar(
        navigationIcon = {
            if (showBackButton) {
                defaultBackButton()
            } else {
                customBackButton?.invoke()
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BisqTheme.colors.backgroundColor, //Color.DarkGray,
        ),
        title = {
            if (isHome) {
                BisqLogoSmall(modifier = Modifier.height(34.dp).width(100.dp))
            } else {
                if (isFlowScreen) {
                    Column {
                        BisqText.smallRegularGrey(
                            text = "Step $stepText",
                            modifier = Modifier.offset(y = 2.dp)
                        )
                        BisqGap.VHalf()
                        BisqText.h5Medium(title)
                    }
                } else {
                    BisqText.h4Medium(title)
                }
            }
        },
        actions = {
            Row(
                modifier = Modifier.padding(top = if (isFlowScreen) 15.dp else 0.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                val userIconModifier = Modifier
                    .size(30.dp)
                    .alpha(if (presenter.avatarEnabled(currentTab)) 1.0f else 0.5f)
                    .clickable {
                        if (presenter.avatarEnabled(currentTab)) {
                            presenter.navigateToUserProfile()
                        }
                    }

//                TODO implement full feature after MVP
//                BellIcon()
                Spacer(modifier = Modifier.width(12.dp))
                if (showAnimation && connectivityStatus != ConnectivityService.ConnectivityStatus.DISCONNECTED) {
                    ShineOverlay {
                        UserIcon(
                            presenter.uniqueAvatar.value,
                            modifier = userIconModifier,
                            connectivityStatus = connectivityStatus
                        )
                    }
                } else {
                    UserIcon(
                        presenter.uniqueAvatar.value,
                        modifier = userIconModifier,
                        connectivityStatus = connectivityStatus
                    )
                }
            }
        },
    )

    if (backBehavior != null) {
        BackHandler(onBackPressed = {
            backBehavior.invoke()
        })
    } else if (backConfirmation) {
        BackHandler(onBackPressed = {
            showBackConfirmationDialog = true
        })
    }

    if (showBackConfirmationDialog) {
        ConfirmationDialog(
            headline = "Are you sure want to exit the Trade?",
            message = "You can resume later",
            onConfirm = {
                showBackConfirmationDialog = false
                presenter.goBack()
            },
            onDismiss = {
                showBackConfirmationDialog = false
            }
        )
    }
}