package network.bisq.mobile.presentation.common.ui.alert

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.model.alert.AlertType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

@Composable
internal fun alertAccentColor(level: AlertType): Color =
    when (level) {
        AlertType.WARN -> BisqTheme.colors.warning
        AlertType.EMERGENCY -> BisqTheme.colors.danger
        else -> BisqTheme.colors.primary
    }

@Composable
internal fun alertBannerBackground(level: AlertType): Color =
    when (level) {
        AlertType.WARN -> BisqTheme.colors.warning.copy(alpha = 0.12f)
        AlertType.EMERGENCY -> BisqTheme.colors.danger.copy(alpha = 0.15f)
        else -> BisqTheme.colors.primary.copy(alpha = 0.12f)
    }

internal fun alertIcon(level: AlertType): ImageVector =
    when (level) {
        AlertType.WARN -> Icons.Default.Warning
        AlertType.EMERGENCY -> Icons.Default.Warning
        else -> Icons.Default.Info
    }

@Composable
internal fun AlertTypeBadge(
    type: AlertType,
    modifier: Modifier = Modifier,
) {
    val accentColor = alertAccentColor(type)
    Box(
        modifier =
            modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.20f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = alertIcon(type),
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
@ExcludeFromCoverage
internal fun SimulatedTopBar(title: String) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(BisqTheme.colors.dark_grey30)
                .padding(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BisqText.BaseRegular(text = title, color = BisqTheme.colors.white)
    }
}

@ExcludeFromCoverage
internal fun simulatedAlertNotification(
    type: AlertType,
    headline: String,
    message: String,
    haltTrading: Boolean = false,
    requiresUpdate: Boolean = false,
    minVersion: String = "",
): AlertNotificationUiState =
    AlertNotificationUiState(
        id = "preview-alert",
        type = type,
        headline = headline,
        message = message,
        haltTrading = haltTrading,
        requiresUpdate = requiresUpdate,
        minVersion = minVersion,
    )
