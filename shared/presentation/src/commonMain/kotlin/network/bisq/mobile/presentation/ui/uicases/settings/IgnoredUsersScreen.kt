package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticLayout
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.components.molecules.settings.BreadcrumbNavigation
import network.bisq.mobile.presentation.ui.components.molecules.settings.SettingsButton
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject

interface IIgnoredUsersPresenter : ViewPresenter {
    fun getIgnoredUsers(): List<UserProfileVO>
    fun unblockUser(userId: String)
    fun settingsNavigateBack()
}

@Composable
fun IgnoredUsersScreen() {
    val presenter: IIgnoredUsersPresenter = koinInject()
    val ignoredUsers = presenter.getIgnoredUsers()
    
    RememberPresenterLifecycle(presenter)

    BisqScrollScaffold(
        topBar = { TopBar("mobile.settings.title".i18n()) },
        padding = PaddingValues(all = BisqUIConstants.Zero),
        verticalArrangement = Arrangement.SpaceBetween,
        isInteractive = presenter.isInteractive.collectAsState().value,
    ) {
        Column {
            if (ignoredUsers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    BisqText.baseRegular(
                        text = "mobile.settings.ignoredUsers.empty".i18n(),
                        color = BisqTheme.colors.mid_grey20
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ignoredUsers) { user ->
                        IgnoredUserItem(
                            user = user,
                            onUnblock = { presenter.unblockUser(user.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IgnoredUserItem(
    user: UserProfileVO,
    onUnblock: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = BisqTheme.colors.secondary,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            BisqText.baseRegular(
                text = user.userName.take(1).uppercase(),
                color = BisqTheme.colors.white
            )
        }
        
        BisqGap.HHalf()
        
        // User name
        BisqText.baseRegular(
            text = user.userName,
            color = BisqTheme.colors.white,
            modifier = Modifier.weight(1f)
        )
        
    }
} 