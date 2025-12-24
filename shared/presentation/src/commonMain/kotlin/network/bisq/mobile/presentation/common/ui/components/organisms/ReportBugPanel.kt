package network.bisq.mobile.presentation.common.ui.components.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ExclamationRedIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.toClipEntry
import network.bisq.mobile.presentation.main.AppPresenter
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
fun ReportBugPanel(
    errorMessage: String,
    isUncaughtException: Boolean,
    onClose: () -> Unit,
) {
    val presenter: AppPresenter = koinInject()
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    ReportBugPanelContent(
        errorMessage = errorMessage,
        isUncaughtException = isUncaughtException,
        isIOS = presenter.isIOS(),
        onClose = onClose,
        onShutdown = { presenter.onTerminateApp() },
        onReport = {
            scope.launch {
                clipboard.setClipEntry(AnnotatedString(errorMessage).toClipEntry())
            }
            presenter.navigateToReportError()
        },
    )
}

@Composable
private fun ReportBugPanelContent(
    errorMessage: String,
    isUncaughtException: Boolean,
    isIOS: Boolean,
    onClose: () -> Unit,
    onShutdown: () -> Unit,
    onReport: () -> Unit,
) {
    val scrollState = rememberScrollState()

    BisqDialog(
        horizontalAlignment = Alignment.Start,
        onDismissRequest = onClose,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExclamationRedIcon()
            BisqGap.HQuarter()
            BisqText.H4Light("mobile.genericError.headline".i18n())
        }

        BisqGap.V1()

        BisqText.SmallLight(
            text = "popup.reportError".i18n(),
            color = BisqTheme.colors.mid_grey30,
        )

        BisqGap.V1()

        BisqText.BaseRegular("mobile.genericError.errorMessage".i18n())

        BisqGap.VQuarter()

        Box(
            modifier =
                Modifier
                    .heightIn(max = 200.dp)
                    .verticalScroll(scrollState),
        ) {
            BisqText.BaseRegular(text = errorMessage)
        }

        BisqGap.V1()

        Row(
            modifier = Modifier.height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            // IOS does not support shutdown
            val useShutdownButton = isUncaughtException && !isIOS
            BisqButton(
                text = if (useShutdownButton) "action.shutDown".i18n() else "action.close".i18n(),
                onClick = {
                    if (useShutdownButton) {
                        onShutdown()
                    } else {
                        onClose()
                    }
                },
                type = BisqButtonType.Grey,
                modifier = Modifier.weight(1.0f).fillMaxHeight(),
                padding = PaddingValues(BisqUIConstants.ScreenPaddingHalf),
            )
            BisqButton(
                text = "support.reports.title".i18n(),
                onClick = {
                    onReport()
                    if (!isUncaughtException) {
                        onClose()
                    }
                },
                modifier = Modifier.weight(1.0f).fillMaxHeight(),
                padding = PaddingValues(BisqUIConstants.ScreenPaddingHalf),
            )
        }
    }
}

@Preview
@Composable
private fun ReportBugPanelPreview_DefaultPreview() {
    BisqTheme.Preview {
        ReportBugPanelContent(
            errorMessage = "java.lang.NullPointerException: Attempt to invoke virtual method 'int java.lang.String.length()' on a null object reference\n\tat com.example.MyClass.doSomething(MyClass.java:42)\n\tat com.example.MainActivity.onCreate(MainActivity.java:18)",
            isUncaughtException = false,
            isIOS = false,
            onClose = {},
            onShutdown = {},
            onReport = {},
        )
    }
}

@Preview
@Composable
private fun ReportBugPanelPreview_UncaughtException_AndroidPreview() {
    BisqTheme.Preview {
        ReportBugPanelContent(
            errorMessage = "Fatal error: OutOfMemoryError\nUnable to allocate memory for critical system operation.",
            isUncaughtException = true,
            isIOS = false,
            onClose = {},
            onShutdown = {},
            onReport = {},
        )
    }
}

@Preview
@Composable
private fun ReportBugPanelPreview_UncaughtException_iOSPreview() {
    BisqTheme.Preview {
        ReportBugPanelContent(
            errorMessage = "Fatal error: Thread 1: signal SIGABRT\nApplication terminated unexpectedly.",
            isUncaughtException = true,
            isIOS = true,
            onClose = {},
            onShutdown = {},
            onReport = {},
        )
    }
}

@Preview
@Composable
private fun ReportBugPanelPreview_LongErrorPreview() {
    BisqTheme.Preview {
        ReportBugPanelContent(
            errorMessage =
                """
                Error: Network connection failed

                Stack trace:
                at network.bisq.mobile.NetworkManager.connect(NetworkManager.kt:123)
                at network.bisq.mobile.AppPresenter.initialize(AppPresenter.kt:45)
                at network.bisq.mobile.MainActivity.onCreate(MainActivity.kt:67)
                at android.app.Activity.performCreate(Activity.java:8000)
                at android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:3571)

                Caused by: java.net.SocketTimeoutException: timeout
                at java.net.PlainSocketImpl.socketConnect(PlainSocketImpl.java:142)
                at java.net.AbstractPlainSocketImpl.doConnect(AbstractPlainSocketImpl.java:390)
                at java.net.AbstractPlainSocketImpl.connectToAddress(AbstractPlainSocketImpl.java:230)
                at java.net.AbstractPlainSocketImpl.connect(AbstractPlainSocketImpl.java:212)
                """.trimIndent(),
            isUncaughtException = false,
            isIOS = false,
            onClose = {},
            onShutdown = {},
            onReport = {},
        )
    }
}
