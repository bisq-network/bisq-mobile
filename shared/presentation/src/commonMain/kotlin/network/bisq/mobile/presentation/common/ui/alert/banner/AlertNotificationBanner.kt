package network.bisq.mobile.presentation.common.ui.alert.banner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.model.alert.AlertType
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.alert.AlertNotificationBannerPresenter
import network.bisq.mobile.presentation.common.ui.alert.AlertNotificationUiAction
import network.bisq.mobile.presentation.common.ui.alert.AlertNotificationUiState
import network.bisq.mobile.presentation.common.ui.alert.AlertTypeBadge
import network.bisq.mobile.presentation.common.ui.alert.SimulatedTopBar
import network.bisq.mobile.presentation.common.ui.alert.alertAccentColor
import network.bisq.mobile.presentation.common.ui.alert.alertBannerBackground
import network.bisq.mobile.presentation.common.ui.alert.simulatedAlertNotification
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import org.jetbrains.compose.ui.tooling.preview.Preview

private const val ALERT_ANIMATION_DURATION_MS = 350

@Composable
internal fun AlertNotificationBanner(
    presenter: AlertNotificationBannerPresenter,
    modifier: Modifier = Modifier,
) {
    val uiState by presenter.uiState.collectAsState()

    AnimatedAlertNotificationBannerContent(
        uiState = uiState,
        onAction = presenter::onAction,
        modifier = modifier,
    )
}

@Composable
private fun PendingAlertsCounter(
    count: Int,
    modifier: Modifier = Modifier,
) {
    if (count <= 0) return
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.dark_grey50)
                .padding(
                    horizontal = BisqUIConstants.ScreenPaddingHalf,
                    vertical = BisqUIConstants.ScreenPaddingQuarter,
                ),
        contentAlignment = Alignment.Center,
    ) {
        BisqText.XSmallMedium(
            text = "mobile.alert.pending.more".i18n(count),
            color = BisqTheme.colors.mid_grey30,
        )
    }
}

@Composable
private fun AnimatedAlertNotificationBannerContent(
    uiState: AlertNotificationBannerUiState,
    onAction: (AlertNotificationUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val alert = uiState.currentAlert
    AnimatedVisibility(
        visible = uiState.isBannerVisible && alert != null,
        enter =
            fadeIn(animationSpec = tween(durationMillis = ALERT_ANIMATION_DURATION_MS)) +
                expandVertically(animationSpec = tween(durationMillis = ALERT_ANIMATION_DURATION_MS)),
        exit =
            fadeOut(animationSpec = tween(durationMillis = ALERT_ANIMATION_DURATION_MS)) +
                shrinkVertically(animationSpec = tween(durationMillis = ALERT_ANIMATION_DURATION_MS)),
        modifier = modifier,
    ) {
        if (alert != null) {
            AlertNotificationBannerContent(
                alert = alert,
                pendingCount = uiState.pendingAlertCount,
                onDismiss = { onAction(AlertNotificationUiAction.OnDismissAlertNotification(it)) },
                onPress = { onAction(AlertNotificationUiAction.ExpandAlertNotification(it)) },
                modifier = Modifier.padding(horizontal = BisqUIConstants.ScreenPadding),
            )
        }
    }
}

@Composable
private fun AlertNotificationBannerContent(
    alert: AlertNotificationUiState,
    onDismiss: (alertId: String) -> Unit,
    onPress: (alertId: String) -> Unit,
    modifier: Modifier = Modifier,
    pendingCount: Int = 0,
) {
    val accentColor = alertAccentColor(alert.type)
    val bannerBackground = alertBannerBackground(alert.type)
    val isDismissible = alert.type != AlertType.EMERGENCY

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(bannerBackground)
                .border(
                    width = 3.dp,
                    color = accentColor,
                    shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
                ).clickable(
                    onClick = { onPress(alert.id) },
                    onClickLabel = "mobile.alert.actions.expand".i18n(),
                    role = Role.Button,
                ),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(
                        start = BisqUIConstants.ScreenPadding + 4.dp,
                        end = BisqUIConstants.ScreenPaddingHalf,
                        top = BisqUIConstants.ScreenPadding,
                        bottom = BisqUIConstants.ScreenPadding,
                    ),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
            ) {
                AlertTypeBadge(type = alert.type)
                BisqText.StyledText(
                    text = alert.headline,
                    color = accentColor,
                    style = BisqTheme.typography.smallMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (pendingCount > 0) {
                    PendingAlertsCounter(count = pendingCount)
                }
            }

            if (alert.message.isNotBlank()) {
                BisqText.StyledText(
                    text = alert.message,
                    color = BisqTheme.colors.light_grey10,
                    style = BisqTheme.typography.smallLight,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (alert.type == AlertType.EMERGENCY && alert.haltTrading) {
                BisqText.XSmallMedium(
                    text = "mobile.alert.trade.halt".i18n(),
                    color = BisqTheme.colors.danger,
                )
            }
        }

        if (isDismissible) {
            IconButton(
                onClick = { onDismiss(alert.id) },
                modifier =
                    Modifier
                        .align(Alignment.Top)
                        .padding(top = BisqUIConstants.ScreenPaddingQuarter),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "action.close".i18n(),
                    tint = BisqTheme.colors.light_grey20,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            Box(
                modifier =
                    Modifier
                        .minimumInteractiveComponentSize()
                        .size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = BisqTheme.colors.mid_grey20,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun AlertNotificationBannerHost_WarnPreview() {
    BisqTheme.Preview {
        AnimatedAlertNotificationBannerContent(
            uiState =
                AlertNotificationBannerUiState(
                    currentAlert =
                        simulatedAlertNotification(
                            type = AlertType.WARN,
                            headline = "Connection risk detected",
                            message = "A signed security alert is active. Review the message and confirm your next step.",
                        ),
                    pendingAlertCount = 2,
                    isBannerVisible = true,
                ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun AlertNotificationBanner_Preview() {
    BisqTheme.Preview {
        AlertNotificationBannerContent(
            alert =
                simulatedAlertNotification(
                    type = AlertType.EMERGENCY,
                    headline = "Trading halted",
                    message = "Critical network conditions require immediate attention from all users. This is extra message for making it long",
                    haltTrading = true,
                ),
            pendingCount = 1,
            onDismiss = {},
            onPress = {},
            modifier = Modifier.padding(horizontal = BisqUIConstants.ScreenPadding),
        )
    }
}

@Preview
@Composable
private fun AlertLevelBadge_AllLevels_Preview() {
    BisqTheme.Preview {
        Row(
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlertTypeBadge(type = AlertType.INFO)
            BisqText.SmallLight(text = "Info", color = BisqTheme.colors.primary)
            AlertTypeBadge(type = AlertType.WARN)
            BisqText.SmallLight(text = "Warn", color = BisqTheme.colors.warning)
            AlertTypeBadge(type = AlertType.EMERGENCY)
            BisqText.SmallLight(text = "Emergency", color = BisqTheme.colors.danger)
        }
    }
}

@Preview
@Composable
private fun AlertNotificationBanner_Info_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.background(BisqTheme.colors.backgroundColor)) {
            SimulatedTopBar(title = "Offerbook")
            AnimatedVisibility(
                visible = true,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                AlertNotificationBannerContent(
                    alert =
                        simulatedAlertNotification(
                            type = AlertType.INFO,
                            headline = "Network Information",
                            message = "A routine maintenance window is scheduled for this weekend. Connectivity may be briefly interrupted.",
                        ),
                    pendingCount = 0,
                    onDismiss = {},
                    onPress = {},
                )
            }
        }
    }
}

@Preview
@Composable
private fun AlertNotificationBanner_Warn_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.background(BisqTheme.colors.backgroundColor)) {
            SimulatedTopBar(title = "Offerbook")
            AlertNotificationBannerContent(
                alert =
                    simulatedAlertNotification(
                        type = AlertType.WARN,
                        headline = "Network Warning",
                        message = "Unusual network conditions detected. Trade carefully and verify payment confirmations before releasing Bitcoin.",
                    ),
                pendingCount = 2,
                onDismiss = {},
                onPress = {},
            )
        }
    }
}

@Preview
@Composable
private fun AlertNotificationBanner_Emergency_Dismissible_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.background(BisqTheme.colors.backgroundColor)) {
            SimulatedTopBar(title = "Offerbook")
            AlertNotificationBannerContent(
                alert =
                    simulatedAlertNotification(
                        type = AlertType.EMERGENCY,
                        headline = "Emergency Alert",
                        message = "A critical issue has been identified. The security team is investigating. Monitor official channels.",
                        haltTrading = false,
                    ),
                pendingCount = 0,
                onDismiss = {},
                onPress = {},
            )
        }
    }
}

@Preview
@Composable
private fun AlertNotificationBanner_Emergency_HaltTrading_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.background(BisqTheme.colors.backgroundColor)) {
            SimulatedTopBar(title = "Offerbook")
            AlertNotificationBannerContent(
                alert =
                    simulatedAlertNotification(
                        type = AlertType.EMERGENCY,
                        headline = "Emergency Alert",
                        message = "A critical security vulnerability has been discovered. Trading is temporarily suspended.",
                        haltTrading = true,
                    ),
                pendingCount = 0,
                onDismiss = {},
                onPress = {},
            )
        }
    }
}
