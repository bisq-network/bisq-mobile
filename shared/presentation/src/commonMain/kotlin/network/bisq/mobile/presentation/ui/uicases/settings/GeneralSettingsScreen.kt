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
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.*
import network.bisq.mobile.presentation.ui.components.atoms.icons.UserIcon
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject

interface IGeneralSettingsPresenter : ViewPresenter {
    val i18nCodes: StateFlow<List<String>>

    val selectedLanguage: StateFlow<String>
    fun selectLanguage(langCode: String)

}

@Composable
fun GeneralSettingsScreen(showBackNavigation: Boolean = false) {
    val presenter: IGeneralSettingsPresenter = koinInject()

    val i18nCodes = presenter.i18nCodes.collectAsState().value
    val selectedLauguage = presenter.selectedLanguage.collectAsState().value

    RememberPresenterLifecycle(presenter)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        BisqScrollLayout(onModifier = { modifier -> modifier.weight(1f) }) {

            BisqDropDown(
                label = "settings.language.headline".i18n(),
                items = i18nCodes,
                value = selectedLauguage,
                onValueChanged = { newValue -> presenter.selectLanguage(newValue) },
            )

            BisqDropDown(
                label = "settings.language.headline".i18n(),
                items = listOf("English", "Spanish", "French"),
                value = "English",
                onValueChanged = { newValue -> println(newValue) },
            )

            BisqSwitch(
                label = "Trade Notification",
                checked = true,
                onSwitch = { newValue -> println(newValue) }
            )

            BisqText.baseRegular("[TODO] Chat Notifs :: With Segment control")

            BisqSwitch(
                label = "Close my offer when trade taken",
                checked = true,
                onSwitch = { newValue -> println(newValue) }
            )

            SettingsTextField(
                label = "Max trade price tolerance",
                value = "5%",
                onValueChange = { newValue -> println(newValue) },
                editable = true,
            )

            BisqSwitch(
                label = "Display: Use animations",
                checked = true,
                onSwitch = { newValue -> println(newValue) }
            )

            SettingsTextField(
                label = "PoW difficulty adjustment factor",
                value = "1",
                onValueChange = { newValue -> println(newValue) },
                editable = true,
            )

        }

    }
}