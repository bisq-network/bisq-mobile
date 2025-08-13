package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.img_bot_image
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.ui.components.atoms.icons.rememberPlatformImagePainter
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

interface IIgnoredUsersPresenter : ViewPresenter {
    val ignoredUsers: StateFlow<List<UserProfileVO>>
    val avatarMap: StateFlow<Map<String, PlatformImage?>>
    val ignoreUserId: StateFlow<String>
    fun unblockUser(userId: String)
    fun unblockUserConfirm(userId: String)
    fun dismissConfirm()
}

@Composable
fun IgnoredUsersScreen() {
    val presenter: IIgnoredUsersPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val isInteractive by presenter.isInteractive.collectAsState()
    val ignoredUsers by presenter.ignoredUsers.collectAsState()
    val userAvatarMap by presenter.avatarMap.collectAsState()
    val ignoreUserId by presenter.ignoreUserId.collectAsState()
    val showIgnoreUserWarnBox = ignoreUserId.isNotEmpty()

    BisqScrollScaffold(
        topBar = { TopBar("mobile.settings.ignoredUsers".i18n()) },
        padding = PaddingValues(all = BisqUIConstants.Zero),
        verticalArrangement = Arrangement.SpaceBetween,
        isInteractive = isInteractive,
    ) {

        if (ignoredUsers.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                BisqText.baseRegular(
                    text = "mobile.settings.ignoredUsers.empty".i18n(), color = BisqTheme.colors.mid_grey20
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf)) {
                ignoredUsers.forEach { user ->
                    IgnoredUserItem(
                        user = user,
                        userAvatar = userAvatarMap[user.nym],
                        onUnblock = { presenter.unblockUser(user.id) }
                    )
                }
            }
        }

        if (showIgnoreUserWarnBox) {
            ConfirmationDialog(
                headline = "error.warning".i18n(),
                headlineColor = BisqTheme.colors.warning,
                headlineLeftIcon = { WarningIcon() },
                message = "mobile.chat.undoIgnoreUserWarn".i18n(),
                confirmButtonText = "user.profileCard.userActions.undoIgnore".i18n(),
                dismissButtonText = "action.cancel".i18n(),
                verticalButtonPlacement = true,
                onConfirm = {
                    presenter.unblockUserConfirm(ignoreUserId)
                },
                onDismiss = { presenter.dismissConfirm() }
            )
        }
    }
}

@Composable
private fun IgnoredUserItem(
    user: UserProfileVO, userAvatar: PlatformImage? = null, onUnblock: () -> Unit
) {

    val painter: Painter = if (userAvatar == null) {
        painterResource(Res.drawable.img_bot_image)
    } else {
        rememberPlatformImagePainter(userAvatar)
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(
            horizontal = BisqUIConstants.ScreenPaddingHalf,
            vertical = BisqUIConstants.ScreenPaddingHalf
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painter, contentDescription = null, modifier = Modifier.size(BisqUIConstants.ScreenPadding3X))

        BisqGap.HHalf()

        BisqText.baseRegular(
            text = user.userName, modifier = Modifier.weight(1f)
        )

        BisqGap.H1()

        BisqButton(
            "mobile.settings.ignoredUsers.unblock".i18n(),
            type = BisqButtonType.GreyOutline,
            onClick = onUnblock
        )

    }
} 