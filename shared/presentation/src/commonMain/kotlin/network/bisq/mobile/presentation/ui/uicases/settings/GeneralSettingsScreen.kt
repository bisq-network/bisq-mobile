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
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

interface IGeneralSettingsPresenter : ViewPresenter {
    val i18nCodes: StateFlow<List<String>>

    val languageCode: StateFlow<String>
    fun setLanguageCode(langCode: String)

    fun getDisplayLanguage(languageCode: String): String

    val supportedLanguageCodes: StateFlow<Set<String>>
    fun setSupportedLanguageCodes(langCodes: Set<String>)

//    val tradeNotification: StateFlow<Boolean>
//    fun setTradeNotification(value: Boolean)

    val chatNotification: StateFlow<String>
    fun setChatNotification(value: String)

    val closeOfferWhenTradeTaken: StateFlow<Boolean>
    fun setCloseOfferWhenTradeTaken(value: Boolean)

    val tradePriceTolerance: StateFlow<Double>
    fun setTradePriceTolerance(value: Double)

    val powFactor: StateFlow<String>
    fun setPowFactor(value: String)

    val ignorePow: StateFlow<Boolean>
    fun setIgnorePow(value: Boolean)

}

@Composable
fun GeneralSettingsScreen(showBackNavigation: Boolean = false) {
    val presenter: IGeneralSettingsPresenter = koinInject()

    val i18nCodes = presenter.i18nCodes.collectAsState().value
    val selectedLauguage = presenter.languageCode.collectAsState().value
    val closeOfferWhenTradeTaken = presenter.closeOfferWhenTradeTaken.collectAsState().value
    val tradePriceTolerance = presenter.tradePriceTolerance.collectAsState().value
    val powFactor = presenter.powFactor.collectAsState().value
    val ignorePow = presenter.ignorePow.collectAsState().value

    RememberPresenterLifecycle(presenter)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        BisqScrollLayout(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
            onModifier = { modifier -> modifier.weight(1f) }
        ) {

            BisqText.h4Regular("settings.language".i18n())

            BisqDropDown(
                label = "settings.language.headline".i18n(),
                items = i18nCodes,
                value = selectedLauguage,
                displayText = selectedLauguage, // TODO
                onValueChanged = { presenter.setLanguageCode(it) },
            )

            BisqDropDown(
                label = "settings.language.supported.headline".i18n() + " :TODO",
                items = i18nCodes,
                value = selectedLauguage,
                displayText = selectedLauguage, // TODO
                onValueChanged = { presenter.setLanguageCode(it) },
                searchable = true,
                chipMultiSelect = true
            )

            BisqHDivider()

            BisqText.h4Regular("settings.notification.options".i18n())

//            BisqSwitch(
//                label = "Trade Notification", // TODO:i18n
//                checked = tradeNotification,
//                onSwitch = { presenter.setTradeNotification(it) }
//            )

            BisqSegmentButton(
                label = "Chat Notification", // TODO:i18n
                items = listOf(
                    "chat.notificationsSettingsMenu.all".i18n(),
                    "chat.notificationsSettingsMenu.mention".i18n(),
                    "chat.notificationsSettingsMenu.off".i18n(),
                ),
                onValueChange = { presenter.setChatNotification(it) }
            )

            BisqHDivider()

            BisqText.h4Regular("settings.trade.headline".i18n())

            BisqSwitch(
                label = "settings.trade.closeMyOfferWhenTaken".i18n(),
                checked = closeOfferWhenTradeTaken,
                onSwitch = { presenter.setCloseOfferWhenTradeTaken(it) }
            )

            BisqTextField(
                label = "settings.trade.maxTradePriceDeviation".i18n(),
                value = "$tradePriceTolerance%",
                onValueChange = { presenter.setTradePriceTolerance(5.0) },
            )

            BisqHDivider()

            BisqText.h4Regular("settings.network.headline".i18n())

            BisqTextField(
                label = "settings.network.difficultyAdjustmentFactor.description.self".i18n(),
                value = powFactor,
                onValueChange = { presenter.setPowFactor(it) },
            )
            BisqSwitch(
                label = "settings.network.difficultyAdjustmentFactor.ignoreValueFromSecManager".i18n(),
                checked = ignorePow,
                onSwitch = { presenter.setIgnorePow(it) }
            )

        }

    }
}