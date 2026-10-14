package network.bisq.mobile.presentation.design.device_security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.components.molecules.bottom_sheet.BisqBottomSheet
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.tabs.dashboard.welcome_carousel.CarouselPageType
import network.bisq.mobile.presentation.tabs.dashboard.welcome_carousel.WelcomeCarouselContent
import network.bisq.mobile.presentation.tabs.dashboard.welcome_carousel.WelcomeCarouselUiState

// -------------------------------------------------------------------------------------
// Simulation helpers (primitives only)
// -------------------------------------------------------------------------------------

private fun simulatedDeviceSecurityState(
    isNode: Boolean,
    isIos: Boolean,
    isChanged: Boolean = false,
    fromSettings: Boolean = false,
    isSuppressed: Boolean = false,
): DeviceSecurityUiState =
    DeviceSecurityUiState(
        isVisible = true,
        app = if (isNode) DeviceSecurityApp.NODE else DeviceSecurityApp.CONNECT,
        platform = if (isIos) DeviceSecurityPlatform.IOS else DeviceSecurityPlatform.ANDROID,
        wording = if (isChanged) DeviceSecurityWording.CHANGED_SINCE_SETUP else DeviceSecurityWording.FIRST_DETECTION,
        origin = if (fromSettings) DeviceSecurityOrigin.SETTINGS else DeviceSecurityOrigin.DASHBOARD,
        isSuppressed = isSuppressed,
    )

private fun simulatedBaseline(name: String): DeviceSecurityBaseline = DeviceSecurityBaseline.valueOf(name)

/** German-length copy (about 40 percent longer than English) to exercise the scrolling body. */
private fun simulatedLongCopy(): DeviceSecurityCopy =
    DeviceSecurityCopy(
        title = "Dieses Gerät scheint gerootet zu sein, und der Sicherheitsstatus hat sich geändert",
        changedLead = "Der Sicherheitsstatus dieses Geräts hat sich seit der Einrichtung von Bisq verändert.",
        body =
            "Rooten kann es anderen Apps auf diesem Gerät ermöglichen, auf die Daten von Bisq " +
                "zuzugreifen, einschließlich Ihrer Kopplung mit Ihrem Node, Ihrer Chats und " +
                "Ihrer Zahlungsdetails. Bisq funktioniert weiterhin normal, und die Entscheidung " +
                "liegt bei Ihnen. Wenn Sie ein Problem vermuten, können Sie die Kopplung mit " +
                "Ihrem Node aufheben. Installieren Sie auf diesem Gerät nach Möglichkeit keine " +
                "Apps aus nicht vertrauenswürdigen Quellen und halten Sie das System aktuell.",
        gotIt = "Verstanden",
        dontShowAgain = "Nicht mehr anzeigen",
        suppressedNote = null,
    )

// -------------------------------------------------------------------------------------
// Hosts
// -------------------------------------------------------------------------------------

/** Dashboard stand-in. A sheet-only preview needs a host column with text or the IDE errors. */
@Composable
private fun DashboardHost(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        BisqText.BaseRegular(
            text = label,
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding2X),
        )
        content()
    }
}

@Composable
private fun SheetOnDashboard(uiState: DeviceSecurityUiState) {
    DashboardHost(label = "Dashboard") {
        DeviceSecurityWarningSheet(uiState = uiState, onAction = {})
    }
}

@Composable
private fun SettingsExcerpt(
    rowState: DeviceSecurityRowUiState,
    caption: String,
) {
    Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding2X)) {
        BisqText.SmallLightGrey(text = caption)
        BisqGap.V1()
        DeviceSecuritySettingsRow(rowState = rowState, onAction = {})
        if (rowState.isVisible) {
            BisqHDivider()
        }
        BisqText.H4Light("Language")
        BisqGap.V1()
        BisqText.BaseLightGrey("Language selection continues here...")
    }
}

// -------------------------------------------------------------------------------------
// Previews: the warning
// -------------------------------------------------------------------------------------

@ExcludeFromCoverage
@Preview
@Composable
private fun DeviceSecurity_NodeAndroid_Preview() {
    BisqTheme.Preview {
        SheetOnDashboard(simulatedDeviceSecurityState(isNode = true, isIos = false))
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun DeviceSecurity_ConnectAndroid_Preview() {
    BisqTheme.Preview {
        SheetOnDashboard(simulatedDeviceSecurityState(isNode = false, isIos = false))
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun DeviceSecurity_ConnectIosJailbroken_Preview() {
    BisqTheme.Preview {
        SheetOnDashboard(simulatedDeviceSecurityState(isNode = false, isIos = true))
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun DeviceSecurity_ChangedSinceSetup_Preview() {
    BisqTheme.Preview {
        SheetOnDashboard(simulatedDeviceSecurityState(isNode = true, isIos = false, isChanged = true))
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun DeviceSecurity_ChangedSinceSetupConnectIos_Preview() {
    BisqTheme.Preview {
        SheetOnDashboard(simulatedDeviceSecurityState(isNode = false, isIos = true, isChanged = true))
    }
}

/** The explicit decision, in the three contexts where it differs. */
@ExcludeFromCoverage
@Preview
@Composable
private fun DeviceSecurity_DontShowAgainAffordance_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            DashboardHost(label = "Opened from the dashboard (pending): primary plus Clear button") {
                DeviceSecurityWarningSheet(simulatedDeviceSecurityState(isNode = false, isIos = false), {})
            }
            DashboardHost(label = "Opened from Settings, decision not yet made: same two buttons") {
                DeviceSecurityWarningSheet(
                    simulatedDeviceSecurityState(isNode = false, isIos = false, fromSettings = true),
                    {},
                )
            }
            DashboardHost(label = "Opened from Settings after the decision: caption replaces the Clear button") {
                DeviceSecurityWarningSheet(
                    simulatedDeviceSecurityState(
                        isNode = false,
                        isIos = false,
                        fromSettings = true,
                        isSuppressed = true,
                    ),
                    {},
                )
            }
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun DeviceSecurity_LongTranslation_Preview() {
    BisqTheme.Preview {
        DashboardHost(label = "Dashboard (long translation, body scrolls, buttons stay pinned)") {
            BisqBottomSheet(onDismissRequest = {}) {
                DeviceSecurityWarningContent(copy = simulatedLongCopy(), onGotIt = {}, onDontShowAgain = {})
            }
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun DeviceSecurity_LargeFont200_Preview() {
    BisqTheme.Preview {
        CompositionLocalProvider(
            LocalDensity provides Density(density = LocalDensity.current.density, fontScale = 2f),
        ) {
            SheetOnDashboard(simulatedDeviceSecurityState(isNode = false, isIos = false, isChanged = true))
        }
    }
}

// -------------------------------------------------------------------------------------
// Previews: Settings row
// -------------------------------------------------------------------------------------

@ExcludeFromCoverage
@Preview
@Composable
private fun DeviceSecurity_SettingsRowRooted_Preview() {
    BisqTheme.Preview {
        SettingsExcerpt(
            rowState = DeviceSecurityRowUiState(isVisible = true, platform = DeviceSecurityPlatform.ANDROID),
            caption = "Settings, rooted device: row present at the top",
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun DeviceSecurity_SettingsRowJailbroken_Preview() {
    BisqTheme.Preview {
        SettingsExcerpt(
            rowState = DeviceSecurityRowUiState(isVisible = true, platform = DeviceSecurityPlatform.IOS),
            caption = "Settings, jailbroken device: row present at the top",
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun DeviceSecurity_SettingsRowClean_Preview() {
    BisqTheme.Preview {
        SettingsExcerpt(
            rowState = DeviceSecurityRowUiState(isVisible = false),
            caption = "Settings, clean device: no row, no positive status",
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun DeviceSecurity_SettingsReopen_Preview() {
    BisqTheme.Preview {
        DashboardHost(label = "Settings (row tapped): same sheet over Settings") {
            DeviceSecurityWarningSheet(
                simulatedDeviceSecurityState(isNode = true, isIos = false, fromSettings = true),
                {},
            )
        }
    }
}

// -------------------------------------------------------------------------------------
// Previews: ordering with the carousel
// -------------------------------------------------------------------------------------

/** The carousel as it renders once the warning has been closed in the same arrival. */
@ExcludeFromCoverage
@Preview
@Composable
private fun DeviceSecurity_CarouselFollowsWarning_Preview() {
    BisqTheme.Preview {
        WelcomeCarouselContent(
            uiState =
                WelcomeCarouselUiState(
                    pages = listOf(CarouselPageType.NOTIFICATIONS, CarouselPageType.ANALYTICS),
                ),
            onPrimaryAction = {},
            onAction = {},
        )
    }
}

@Composable
private fun OrderingRow(
    scenario: String,
    isPending: Boolean,
    isClosed: Boolean,
    hasPages: Boolean,
) {
    val overlay = resolveDashboardOverlay(isPending, isClosed, hasPages)
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = BisqUIConstants.ScreenPaddingHalf)) {
        BisqText.SmallLight(text = scenario, modifier = Modifier.weight(2f))
        BisqText.SmallRegular(text = overlay.name, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun DeviceSecurity_OrderingTable_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding2X)) {
            BisqText.H5Light("Dashboard overlay per arrival")
            BisqGap.V1()
            OrderingRow("Warning pending, carousel pending", isPending = true, isClosed = false, hasPages = true)
            OrderingRow("Warning closed without deciding, carousel pending", isPending = true, isClosed = true, hasPages = true)
            OrderingRow("Don't show again tapped, carousel pending", isPending = false, isClosed = false, hasPages = true)
            OrderingRow("Warning pending, no carousel pages", isPending = true, isClosed = false, hasPages = false)
            OrderingRow("Warning closed, no carousel pages", isPending = true, isClosed = true, hasPages = false)
            OrderingRow("Clean device, carousel pending", isPending = false, isClosed = false, hasPages = true)
            OrderingRow("Clean device, nothing pending", isPending = false, isClosed = false, hasPages = false)
        }
    }
}

// -------------------------------------------------------------------------------------
// Previews: persisted-state table
// -------------------------------------------------------------------------------------

@Composable
private fun ResolutionRow(
    isRooted: Boolean,
    baseline: String,
    suppressed: Boolean,
) {
    val r = resolveDeviceSecurity(isRooted, simulatedBaseline(baseline), suppressed)
    val shown =
        when {
            r.isPending && r.wording == DeviceSecurityWording.CHANGED_SINCE_SETUP -> "warning (changed)"
            r.isPending -> "warning"
            else -> "nothing"
        }
    val detection = if (isRooted) "rooted" else "clean"
    BisqText.SmallLight(
        text =
            "$detection | $baseline | suppressed=$suppressed -> " +
                "baseline=${r.baseline}, suppressed=${r.suppressed}, shows $shown, row=${r.isRowVisible}",
        modifier = Modifier.padding(vertical = BisqUIConstants.ScreenPaddingQuarter),
    )
}

@ExcludeFromCoverage
@Preview
@Composable
private fun DeviceSecurity_ResolutionTable_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding2X)) {
            BisqText.H5Light("Persisted-state resolution per arrival")
            BisqGap.V1()
            ResolutionRow(isRooted = false, baseline = "UNSET", suppressed = false)
            ResolutionRow(isRooted = true, baseline = "UNSET", suppressed = false)
            ResolutionRow(isRooted = true, baseline = "ROOTED", suppressed = false)
            ResolutionRow(isRooted = true, baseline = "ROOTED", suppressed = true)
            ResolutionRow(isRooted = false, baseline = "ROOTED", suppressed = true)
            ResolutionRow(isRooted = true, baseline = "ROOTED", suppressed = false)
            ResolutionRow(isRooted = true, baseline = "CLEAN", suppressed = false)
            ResolutionRow(isRooted = true, baseline = "CLEAN", suppressed = true)
            ResolutionRow(isRooted = false, baseline = "CLEAN", suppressed = true)
        }
    }
}
