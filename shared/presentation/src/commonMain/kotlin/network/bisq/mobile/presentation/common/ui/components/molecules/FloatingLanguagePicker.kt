package network.bisq.mobile.presentation.common.ui.components.molecules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import network.bisq.mobile.data.service.common.LanguageServiceFacade
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.LanguageIcon
import network.bisq.mobile.presentation.common.ui.components.molecules.bottom_sheet.BisqBottomSheet
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import org.koin.compose.koinInject

/**
 * Debug floating language picker for in-app language switching during testing.
 *
 * Mirrors the Settings screen language list ([LanguageServiceFacade.i18nPairs]) and
 * persists via [SettingsServiceFacade.setLanguageCode]. Remove before merging the
 * language composition-tree work.
 */
@ExcludeFromCoverage
@Composable
fun FloatingLanguagePicker(modifier: Modifier = Modifier) {
    val settingsService: SettingsServiceFacade = koinInject()
    val languageService: LanguageServiceFacade = koinInject()
    val languageCode by I18nSupport.currentLanguage.collectAsState()
    val i18nPairs by languageService.i18nPairs.collectAsState()
    val scope = rememberCoroutineScope()

    var expanded by remember { mutableStateOf(false) }
    var isChanging by remember { mutableStateOf(false) }

    Surface(
        modifier =
            modifier.clickable(enabled = !isChanging) {
                expanded = true
            },
        shape = RoundedCornerShape(BisqUIConstants.ScreenPadding),
        color = BisqTheme.colors.primary2,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPaddingHalf,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        ) {
            if (isChanging) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = BisqTheme.colors.primary,
                    strokeWidth = 2.dp,
                )
            } else {
                LanguageIcon()
            }
            BisqText.SmallRegular(
                text = languageCode,
                color = BisqTheme.colors.primary65,
            )
        }
    }

    if (expanded) {
        BisqBottomSheet(onDismissRequest = { if (!isChanging) expanded = false }) {
            BisqText.H4Light(
                text = "Language (debug)",
                modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
            )

            LazyColumn {
                items(
                    items = i18nPairs.entries.toList(),
                    key = { it.key },
                ) { entry ->
                    val isSelected = entry.key == languageCode
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isChanging && !isSelected) {
                                    isChanging = true
                                    scope.launch {
                                        try {
                                            settingsService
                                                .setLanguageCode(entry.key)
                                                .onSuccess { expanded = false }
                                        } finally {
                                            isChanging = false
                                        }
                                    }
                                }.padding(
                                    horizontal = BisqUIConstants.ScreenPadding,
                                    vertical = BisqUIConstants.ScreenPaddingHalfQuarter,
                                ),
                    ) {
                        BisqText.BaseLight(
                            text = entry.value,
                            color =
                                if (isSelected) {
                                    BisqTheme.colors.primary
                                } else {
                                    BisqTheme.colors.white
                                },
                        )
                    }
                }
            }
        }
    }
}
