package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.img_bitcoin_payment_waiting
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.CircularLoadingImage
import network.bisq.mobile.presentation.ui.components.atoms.SettingsTextField
import network.bisq.mobile.presentation.ui.components.atoms.icons.UserIcon
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject

interface IUserProfileSettingsPresenter: ViewPresenter {

    val reputation: StateFlow<String>
    val lastUserActivity: StateFlow<String>
    val profileAge: StateFlow<String>
    val profileId: StateFlow<String>
    val nickname: StateFlow<String>
    val botId: StateFlow<String>
    val statement: StateFlow<String>
    val tradeTerms: StateFlow<String>

    val uniqueAvatar: StateFlow<PlatformImage?>

    val showLoading: StateFlow<Boolean>

    val connectivityStatus: StateFlow<ConnectivityService.ConnectivityStatus>

    fun onDelete()
    fun onSave()
    fun updateTradeTerms(it: String)
    fun updateStatement(it: String)
}

@Composable
fun UserProfileSettingsScreen(showBackNavigation: Boolean = false) {
    val presenter: IUserProfileSettingsPresenter = koinInject()


    val botId = presenter.botId.collectAsState().value
    val nickname = presenter.nickname.collectAsState().value
    val profileId = presenter.profileId.collectAsState().value
    val profileAge = presenter.profileAge.collectAsState().value
    val lastUserActivity = presenter.lastUserActivity.collectAsState().value
    val reputation = presenter.reputation.collectAsState().value
    val statement = presenter.statement.collectAsState().value
    val tradeTerms = presenter.tradeTerms.collectAsState().value

    val showLoading = presenter.showLoading.collectAsState().value

    RememberPresenterLifecycle(presenter)

    // Bot Icon
    Spacer(modifier = Modifier.height(16.dp))

    Column(modifier = Modifier.fillMaxSize(),
           horizontalAlignment = Alignment.CenterHorizontally) {

        UserProfileScreenHeader(presenter, showBackNavigation)

        Spacer(modifier = Modifier.height(16.dp))
        BisqScrollLayout(onModifier = { modifier -> modifier.weight(1f) }) {
            SettingsTextField(label = "Bot ID", value = botId, editable = false)

            Spacer(modifier = Modifier.height(8.dp))

            SettingsTextField(label = "Nickname", value = nickname, editable = false)

            Spacer(modifier = Modifier.height(8.dp))

            SettingsTextField(label = "Profile ID", value = profileId, editable = false)

            Spacer(modifier = Modifier.height(8.dp))

            SettingsTextField(label = "Profile age", value = profileAge, editable = false)

            Spacer(modifier = Modifier.height(8.dp))

            SettingsTextField(label = "Last user activity", value = lastUserActivity, editable = false)

            Spacer(modifier = Modifier.height(8.dp))

            // Reputation
            SettingsTextField(label = "Reputation", value = reputation, editable = false)

            Spacer(modifier = Modifier.height(16.dp))

            // Statement
            SettingsTextField(
                label = "Statement",
                value = statement,
                editable = true,
                onValueChange = { presenter.updateStatement(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Trade Terms
            SettingsTextField(
                label = "Trade terms",
                value = tradeTerms,
                editable = true,
                onValueChange = { presenter.updateTradeTerms(it) }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        UserProfileScreenFooter(presenter, showLoading)
    }
}

@Composable
private fun UserProfileScreenHeader(presenter: IUserProfileSettingsPresenter, showBackNavigation: Boolean) {
    val connectivityStatus = presenter.connectivityStatus.collectAsState().value
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(BisqTheme.colors.dark1),
        contentAlignment = Alignment.Center
    ) {
        // Back Button if showBackNavigation is true
        if (showBackNavigation) {
            IconButton(
                onClick = { presenter.getRootNavController().popBackStack() },
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterStart) // Align to the top-left
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = BisqTheme.colors.light1
                )
            }
        }

        Box(
            modifier = Modifier
                .size(80.dp)
                .padding(12.dp)
                .fillMaxWidth()
                .background(BisqTheme.colors.dark1),
            contentAlignment = Alignment.Center
        ) {
            UserIcon(
                presenter.uniqueAvatar.value,
                modifier = Modifier.size(72.dp),
                connectivityStatus = connectivityStatus
            )
        }
    }
}

@Composable
private fun UserProfileScreenFooter(presenter: IUserProfileSettingsPresenter, showLoading: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (showLoading) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)) {
                CircularLoadingImage(
                    // TODO specific image?
                    image = Res.drawable.img_bitcoin_payment_waiting,
                    isLoading = !showLoading
                )
            }
        } else {
            //        TODO uncomment when delete profile gets implemented
            //        Button(
            //            onClick = presenter::onDelete,
            //            colors = ButtonDefaults.buttonColors(
            //                containerColor = BisqTheme.colors.danger,
            //                contentColor = BisqTheme.colors.light1
            //            ),
            //            modifier = Modifier.weight(1f).wrapContentWidth().padding(horizontal = 8.dp)
            //        ) {
            //            Text("Delete profile", fontSize = 14.sp)
            //        }
            //
            //        Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = presenter::onSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BisqTheme.colors.primary,
                    contentColor = BisqTheme.colors.light1
                ),
                // TODO fixed height to match both cases?
                modifier = Modifier.weight(1f)
                    .wrapContentWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text("Save", fontSize = 14.sp)
            }
        }
    }
}