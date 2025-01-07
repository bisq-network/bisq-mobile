package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.alpha
import androidx.navigation.compose.currentBackStackEntryAsState
import network.bisq.mobile.presentation.ui.components.BackHandler
import network.bisq.mobile.presentation.ui.components.atoms.animations.ShineOverlay
import network.bisq.mobile.presentation.ui.components.atoms.icons.BisqLogoSmall
import network.bisq.mobile.presentation.ui.components.atoms.icons.UserIcon
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject

interface ITopBarPresenter : ViewPresenter {
    val uniqueAvatar: StateFlow<PlatformImage?>
    fun onAvatarClicked()
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
    stepText: String = ""
) {
    val presenter: ITopBarPresenter = koinInject()
    val navController: NavHostController = presenter.getRootNavController()
    val tabNavController: NavHostController = presenter.getRootTabNavController()

    val interactionEnabled = presenter.isInteractive.collectAsState().value
    var showBackConfirmationDialog by remember { mutableStateOf(false) }

    val currentTab = tabNavController.currentBackStackEntryAsState().value?.destination?.route

    val showBackButton = customBackButton == null && navController.previousBackStackEntry != null

    val defaultBackButton: @Composable () -> Unit = {
        IconButton(onClick = {
            if (navController.previousBackStackEntry != null) {
                if (backConfirmation) {
                    if (!showBackConfirmationDialog) {
                        showBackConfirmationDialog = true
                    }
                } else {
                    presenter.goBack()
                }
            }
        }) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = BisqTheme.colors.grey1
            )
        }
    }

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
                        BisqText.smallRegular(
                            text = "Step $stepText",
                            color = BisqTheme.colors.grey1,
                            // modifier = Modifier.padding(top= 8.dp)
                            modifier = Modifier.offset(y = (8).dp)
                        )
                        BisqText.h5Medium(
                            text = title,
                            color = BisqTheme.colors.light1,
                        )
                    }
                } else {
                    BisqText.h4Medium(
                        text = title,
                        color = BisqTheme.colors.light1,
                    )
                }
            }
        },
        actions = {
            Row(
                modifier = Modifier.padding(top = if (isFlowScreen) 15.dp else 0.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
//                TODO implement full feature after MVP
//                BellIcon()
                Spacer(modifier = Modifier.width(12.dp))
                ShineOverlay {
                    UserIcon(
                        presenter.uniqueAvatar.value,
                        modifier = Modifier.size(30.dp)
                            .alpha(if (currentTab == Routes.TabSettings.name) 0.5f else 1.0f)
                            .clickable {
                                if (currentTab != Routes.TabSettings.name) {
                                    navController.navigate(Routes.UserProfileSettings.name)
                                }
                            })
                }
            }
        },
    )

    // TODO: What if both backBehavior and backConfirmation are set
    if (backBehavior != null) {
        BackHandler(onBackPressed = {
            backBehavior.invoke()
        })
    }

    if (backConfirmation) {
        BackHandler(onBackPressed = {
            showBackConfirmationDialog = true
        })
    }

    if (showBackConfirmationDialog) {
        ConfirmationDialog(
            message = "Are you sure want to exit the Trade?",
            subMessage = "You can resume later",
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