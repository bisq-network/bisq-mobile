package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.SettingsTextField
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject

interface IUserProfileSettingsPresenter: ViewPresenter {

    val reputation: StateFlow<String>
    val lastUserActivity: StateFlow<String>
    val profileAge: StateFlow<String>
    val profileId: StateFlow<String>
    val botId: StateFlow<String>

    fun onDelete()
    fun onSave()
}

@Composable
fun UserProfileSettingsScreen() {
    val statement = remember { mutableStateOf("") }
    val tradeTerms = remember { mutableStateOf("") }
    val presenter: IUserProfileSettingsPresenter = koinInject()


    val botId = presenter.botId.collectAsState().value
    val profileId = presenter.profileId.collectAsState().value
    val profileAge = presenter.profileAge.collectAsState().value
    val lastUserActivity = presenter.lastUserActivity.collectAsState().value
    val reputation = presenter.reputation.collectAsState().value

    RememberPresenterLifecycle(presenter)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Bot Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(BisqTheme.colors.dark1),
            contentAlignment = Alignment.Center
        ) {
            Text(" PLACEHOLDER ")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bot ID
        SettingsTextField(label = "Bot ID", value = botId, editable = false)

        Spacer(modifier = Modifier.height(8.dp))

        // Profile ID
        SettingsTextField(label = "Profile ID", value = profileId, editable = false)

        Spacer(modifier = Modifier.height(8.dp))

        // Profile Age
        SettingsTextField(label = "Profile age", value = profileAge, editable = false)

        Spacer(modifier = Modifier.height(8.dp))

        // Last User Activity
        SettingsTextField(label = "Last user activity", value = lastUserActivity, editable = false)

        Spacer(modifier = Modifier.height(8.dp))

        // Reputation
        SettingsTextField(label = "Reputation", value = reputation, editable = false)

        Spacer(modifier = Modifier.height(16.dp))

        // Statement
        SettingsTextField(
            label = "Statement",
            value = statement.value,
            editable = true,
            onValueChange = { statement.value = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Trade Terms
        SettingsTextField(
            label = "Trade terms",
            value = tradeTerms.value,
            editable = true,
            onValueChange = { tradeTerms.value = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = presenter::onDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BisqTheme.colors.dark1,
                    contentColor = BisqTheme.colors.light1
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("Delete profile", fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = presenter::onSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BisqTheme.colors.primary,
                    contentColor = BisqTheme.colors.light1
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("Save", fontSize = 14.sp)
            }
        }
    }
}