package network.bisq.mobile.client.common.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ArrowDownIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AdvancedOptionsDrawer(
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f)

    Column(modifier = modifier) {
        Row(
            modifier =
                Modifier.clickable(onClick = onToggle).semantics(true) {
                    contentDescription =
                        if (expanded) "mobile.action.hide".i18n() else "mobile.action.show".i18n()
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(
                    BisqUIConstants.ScreenPadding,
                ),
        ) {
            BisqText.SmallRegularGrey("mobile.trustedNodeSetup.advancedOptions".i18n())
            BisqHDivider(modifier = Modifier.weight(1f))
            OutlinedIconButton(
                onClick = onToggle,
                modifier =
                    Modifier
                        .size(24.dp)
                        .clearAndSetSemantics { hideFromAccessibility() },
                border = BorderStroke(1.dp, BisqTheme.colors.mid_grey10),
            ) {
                ArrowDownIcon(modifier = Modifier.size(12.dp).rotate(rotation))
            }
        }
        AnimatedVisibility(expanded) {
            content()
        }
    }
}

@Preview
@Composable
private fun AdvancedOptionsDrawerCollapsedPreview() {
    BisqTheme.Preview {
        AdvancedOptionsDrawer(onToggle = {}, expanded = false) {}
    }
}

@Preview
@Composable
private fun AdvancedOptionsDrawerExpandedPreview() {
    BisqTheme.Preview {
        AdvancedOptionsDrawer(onToggle = {}, expanded = true) {
            BisqText.BaseRegular("this is content")
        }
    }
}
