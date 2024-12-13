package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.SettingsTextField
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject

interface IUserProfileSettingsPresenter: ViewPresenter {

    val reputation: String
    val lastUserActivity: String
    val profileAge: String
    val profileId: String
    val botId: String

    fun onDelete()
    fun onSave()
}

@Composable
fun UserProfileSettingsScreen() {
val statement = remember { mutableStateOf("") }
    val tradeTerms = remember { mutableStateOf("") }
    val presenter: IUserProfileSettingsPresenter = koinInject()

    RememberPresenterLifecycle(presenter)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BisqTheme.colors.dark1)
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
        SettingsTextField(label = "Bot ID", value = presenter.botId, editable = false)

        Spacer(modifier = Modifier.height(8.dp))

        // Profile ID
        SettingsTextField(label = "Profile ID", value = presenter.profileId, editable = false)

        Spacer(modifier = Modifier.height(8.dp))

        // Profile Age
        SettingsTextField(label = "Profile age", value = presenter.profileAge, editable = false)

        Spacer(modifier = Modifier.height(8.dp))

        // Last User Activity
        SettingsTextField(label = "Last user activity", value = presenter.lastUserActivity, editable = false)

        Spacer(modifier = Modifier.height(8.dp))

        // Reputation
        SettingsTextField(label = "Reputation", value = presenter.reputation, editable = false)

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