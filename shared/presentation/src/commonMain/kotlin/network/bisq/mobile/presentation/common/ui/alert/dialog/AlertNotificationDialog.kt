package network.bisq.mobile.presentation.common.ui.alert.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import network.bisq.mobile.domain.model.alert.AlertType
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.alert.AlertNotificationBannerPresenter
import network.bisq.mobile.presentation.common.ui.alert.AlertNotificationUiAction
import network.bisq.mobile.presentation.common.ui.alert.AlertNotificationUiState
import network.bisq.mobile.presentation.common.ui.alert.alertAccentColor
import network.bisq.mobile.presentation.common.ui.alert.alertIcon
import network.bisq.mobile.presentation.common.ui.alert.simulatedAlertNotification
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun AlertNotificationDialog(
    presenter: AlertNotificationBannerPresenter,
    modifier: Modifier = Modifier,
) {
    val uiState by presenter.uiState.collectAsState()

    uiState.currentAlertDialog?.let { alert ->
        AlertNotificationDialogContent(
            alert,
            presenter::onAction,
            modifier,
        )
    }
}

@Composable
internal fun AlertNotificationDialogContent(
    alert: AlertNotificationUiState,
    onAction: (AlertNotificationUiAction) -> Unit,
    modifier: Modifier = Modifier,
    showDismissButton: Boolean = true,
) {
    val accentColor = alertAccentColor(alert.type)
    Dialog(
        onDismissRequest = {
            onAction(AlertNotificationUiAction.OnCloseDialog)
        },
    ) {
        Box(modifier) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                        .background(BisqTheme.colors.dark_grey40)
                        .border(
                            width = 1.dp,
                            color = accentColor,
                            shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
                        ).padding(BisqUIConstants.ScreenPadding2X),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
                ) {
                    Icon(
                        imageVector = alertIcon(alert.type),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(48.dp),
                    )

                    BisqText.H6Regular(
                        text = alert.headline,
                        color = accentColor,
                        textAlign = TextAlign.Center,
                    )

                    if (alert.message.isNotBlank()) {
                        BisqText.BaseLight(
                            text = alert.message,
                            color = BisqTheme.colors.light_grey10,
                            textAlign = TextAlign.Center,
                        )
                    }

                    BisqGap.V1()

                    if (alert.haltTrading) {
                        Box(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .border(
                                        width = 1.dp,
                                        color = accentColor.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
                                    ).padding(
                                        horizontal = BisqUIConstants.ScreenPadding,
                                        vertical = BisqUIConstants.ScreenPaddingHalf,
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            BisqText.SmallMedium(
                                text = "mobile.alert.trade.halt".i18n(),
                                color = accentColor,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    if (alert.requiresUpdate) {
                        if (alert.minVersion.isNotBlank()) {
                            BisqText.SmallLight(
                                text = "mobile.alert.update.minimum".i18n(alert.minVersion),
                                color = BisqTheme.colors.light_grey10,
                                textAlign = TextAlign.Center,
                            )
                        }
                        BisqButton(
                            text = "mobile.alert.update.button".i18n(),
                            onClick = { onAction(AlertNotificationUiAction.OnUpdateNow) },
                            fullWidth = true,
                        )
                    }

                    if (showDismissButton) {
                        BisqButton(
                            text = "mobile.alert.actions.dismiss.label".i18n(),
                            onClick = { onAction(AlertNotificationUiAction.OnDismissAlertNotification(alert.id)) },
                            type = BisqButtonType.Outline,
                            fullWidth = true,
                        )
                    }
                }
            }

            Box(Modifier.align(Alignment.TopEnd)) {
                IconButton(onClick = {
                    onAction(AlertNotificationUiAction.OnCloseDialog)
                }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "mobile.alert.actions.dismiss.description".i18n(),
                        tint = BisqTheme.colors.light_grey10,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun DialogPreview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            AlertNotificationDialogContent(
                alert =
                    simulatedAlertNotification(
                        type = AlertType.EMERGENCY,
                        headline = "Update Required",
                        message = "",
                        requiresUpdate = true,
                        minVersion = "",
                    ),
                onAction = {},
            )
        }
    }
}

@Preview
@Composable
private fun DialogHaltTradingPreview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            AlertNotificationDialogContent(
                alert =
                    simulatedAlertNotification(
                        type = AlertType.EMERGENCY,
                        headline = "Trading Suspended",
                        message = "A critical security vulnerability requires immediate action. All trading is temporarily halted until the issue is resolved. We apologize for the interruption.",
                        haltTrading = true,
                    ),
                onAction = {},
            )
        }
    }
}

@Preview
@Composable
private fun DialogUpdateRequiredPreview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            AlertNotificationDialogContent(
                alert =
                    simulatedAlertNotification(
                        type = AlertType.WARN,
                        headline = "Update Required",
                        message = "This version contains a security fix. Please update immediately.",
                        requiresUpdate = true,
                        minVersion = "2.1.8",
                    ),
                onAction = {},
            )
        }
    }
}
