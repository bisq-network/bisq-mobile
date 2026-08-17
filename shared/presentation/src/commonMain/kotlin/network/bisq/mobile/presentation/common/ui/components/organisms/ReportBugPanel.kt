package network.bisq.mobile.presentation.common.ui.components.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.share.AppLogFile
import network.bisq.mobile.presentation.common.share.AppLogFileProvider
import network.bisq.mobile.presentation.common.share.ShareFileService
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.button.CopyIconButton
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ExclamationRedIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ShareIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.toClipEntry
import network.bisq.mobile.presentation.main.AppPresenter
import org.koin.compose.koinInject

@Composable
fun ReportBugPanel(
    errorMessage: String,
    isUncaughtException: Boolean,
    onClose: () -> Unit,
) {
    val presenter: AppPresenter = koinInject()
    val shareFileService: ShareFileService = koinInject()
    val logFileProvider: AppLogFileProvider = koinInject()
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    // Snackbars are hosted by the app scaffold behind this dialog, so failures are shown inline.
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var logFile by remember { mutableStateOf<AppLogFile?>(null) }

    LaunchedEffect(Unit) {
        logFile = runCatching { logFileProvider.logFile() }.getOrNull()
    }

    ReportBugPanelContent(
        errorMessage = errorMessage,
        isUncaughtException = isUncaughtException,
        isIOS = presenter.isIOS(),
        statusMessage = statusMessage,
        onClose = onClose,
        onShutdown = { presenter.onTerminateApp() },
        logFileName = logFile?.name,
        onSaveToFile = {
            scope.launch {
                // The error dialog must never take the app down, so anything the share path can
                // throw (including Errors such as NoClassDefFoundError) is reported inline instead.
                val result =
                    runCatching {
                        shareFileService.shareUtf8TextFile(errorMessage, ERROR_LOG_FILE_NAME, shareText = errorMessage)
                    }
                statusMessage =
                    if (result.getOrNull()?.isSuccess == true) null else "mobile.genericError.saveToFile.failed".i18n()
            }
        },
        onShareLogFile = {
            scope.launch {
                val file = logFile ?: return@launch
                val result = runCatching { shareFileService.shareFile(file.path) }
                statusMessage =
                    if (result.getOrNull()?.isSuccess == true) null else "mobile.genericError.saveToFile.failed".i18n()
            }
        },
        onReport = {
            scope.launch {
                runCatching { clipboard.setClipEntry(AnnotatedString(errorMessage).toClipEntry()) }
            }
            presenter.navigateToReportError()
        },
    )
}

@Composable
internal fun ReportBugPanelContent(
    errorMessage: String,
    isUncaughtException: Boolean,
    isIOS: Boolean,
    statusMessage: String?,
    logFileName: String?,
    onClose: () -> Unit,
    onShutdown: () -> Unit,
    onSaveToFile: () -> Unit,
    onShareLogFile: () -> Unit,
    onReport: () -> Unit,
) {
    BisqDialog(
        horizontalAlignment = Alignment.Start,
        onDismissRequest = onClose,
        stickyBottomContent = {
            BisqGap.V1()
            ReportBugPanelButtons(
                isUncaughtException = isUncaughtException,
                isIOS = isIOS,
                onClose = onClose,
                onShutdown = onShutdown,
                onReport = onReport,
            )
        },
    ) {
        ReportBugPanelBody(
            errorMessage = errorMessage,
            statusMessage = statusMessage,
            logFileName = logFileName,
            onSaveToFile = onSaveToFile,
            onShareLogFile = onShareLogFile,
        )
    }
}

@Composable
private fun ColumnScope.ReportBugPanelBody(
    errorMessage: String,
    statusMessage: String?,
    logFileName: String?,
    onSaveToFile: () -> Unit,
    onShareLogFile: () -> Unit,
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
        text = "mobile.genericError.reportInfo".i18n(),
        color = BisqTheme.colors.mid_grey30,
    )

    // Only the node app writes a log file; telling client users to attach one is confusing.
    if (logFileName != null) {
        BisqGap.VHalf()

        BisqText.SmallLight(
            text = "mobile.genericError.reportInfo.logFile".i18n(),
            color = BisqTheme.colors.mid_grey30,
        )
    }

    BisqGap.V1()

    if (logFileName != null) {
        LogFileRow(logFileName = logFileName, onShareLogFile = onShareLogFile)

        BisqGap.VHalf()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BisqText.BaseRegular(
            text = "mobile.genericError.errorMessage".i18n(),
            color = BisqTheme.colors.mid_grey30,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf)) {
            CopyIconButton(value = errorMessage, showToast = false, iconSize = IconSize)
            ErrorLogIconButton(onClick = onSaveToFile) { ShareIcon(modifier = Modifier.size(IconSize)) }
        }
    }

    BisqGap.VQuarter()

    ErrorLogTextArea(errorMessage)

    if (statusMessage != null) {
        BisqGap.VQuarter()
        BisqText.SmallRegularGrey(statusMessage)
    }
}

@Composable
private fun ReportBugPanelButtons(
    isUncaughtException: Boolean,
    isIOS: Boolean,
    onClose: () -> Unit,
    onShutdown: () -> Unit,
    onReport: () -> Unit,
) {
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

@Composable
private fun LogFileRow(
    logFileName: String,
    onShareLogFile: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BisqText.BaseRegular(
            text = "mobile.genericError.logFile".i18n(logFileName),
            color = BisqTheme.colors.mid_grey30,
        )
        ErrorLogIconButton(onClick = onShareLogFile) { ShareIcon(modifier = Modifier.size(IconSize)) }
    }
}

@Composable
private fun ErrorLogTextArea(errorMessage: String) {
    val scrollState = rememberScrollState()
    val shape = RoundedCornerShape(BisqUIConstants.textFieldBorderRadius)

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(TextAreaHeight)
                .background(BisqTheme.colors.secondaryDisabled, shape)
                .border(1.dp, BisqTheme.colors.mid_grey30, shape)
                .padding(BisqUIConstants.ScreenPaddingHalf)
                .verticalScroll(scrollState),
    ) {
        SelectionContainer {
            BisqText.XSmallRegularGrey(errorMessage)
        }
    }
}

@Composable
private fun ErrorLogIconButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        IconButton(
            modifier = Modifier.size(BisqUIConstants.ScreenPadding2X),
            onClick = onClick,
            content = { icon() },
        )
    }
}

private const val ERROR_LOG_FILE_NAME = "bisq-error-log.txt"

private val IconSize = 18.dp

// Fixed so the dialog keeps the same footprint on short phones; the trace scrolls inside.
private val TextAreaHeight = 100.dp

@Preview
@ExcludeFromCoverage
@Composable
private fun LogFileRowPreview() {
    PreviewCard {
        LogFileRow(logFileName = "bisq.log", onShareLogFile = {})
    }
}

@Preview
@ExcludeFromCoverage
@Composable
private fun ErrorLogTextAreaPreview() {
    PreviewCard {
        ErrorLogTextArea(SHORT_ERROR)
    }
}

@Preview
@ExcludeFromCoverage
@Composable
private fun ErrorLogIconButtonPreview() {
    PreviewCard {
        ErrorLogIconButton(onClick = {}) { ShareIcon(modifier = Modifier.size(IconSize)) }
    }
}

@Preview
@ExcludeFromCoverage
@Composable
private fun ReportBugPanelButtons_ClosePreview() {
    PreviewCard {
        ReportBugPanelButtons(
            isUncaughtException = false,
            isIOS = false,
            onClose = {},
            onShutdown = {},
            onReport = {},
        )
    }
}

@Preview
@ExcludeFromCoverage
@Composable
private fun ReportBugPanelButtons_ShutdownPreview() {
    PreviewCard {
        ReportBugPanelButtons(
            isUncaughtException = true,
            isIOS = false,
            onClose = {},
            onShutdown = {},
            onReport = {},
        )
    }
}

@Preview
@ExcludeFromCoverage
@Composable
private fun ReportBugPanel_DefaultPreview() {
    ReportBugPanelPreview(errorMessage = LONG_ERROR)
}

@Preview
@ExcludeFromCoverage
@Composable
private fun ReportBugPanel_WithLogFilePreview() {
    ReportBugPanelPreview(
        errorMessage = SHORT_ERROR,
        logFileName = "bisq.log",
    )
}

@Preview
@ExcludeFromCoverage
@Composable
private fun ReportBugPanel_UncaughtExceptionPreview() {
    ReportBugPanelPreview(
        errorMessage = SHORT_ERROR,
        statusMessage = "Could not save the error log",
        isUncaughtException = true,
    )
}

@Preview
@ExcludeFromCoverage
@Composable
private fun ReportBugPanel_UncaughtException_iOSPreview() {
    ReportBugPanelPreview(
        errorMessage = SHORT_ERROR,
        isUncaughtException = true,
        isIOS = true,
    )
}

/**
 * Previews render the dialog's content directly: a Compose [androidx.compose.ui.window.Dialog]
 * shows up empty in the preview pane, since it draws into its own window.
 */
@ExcludeFromCoverage
@Composable
private fun ReportBugPanelPreview(
    errorMessage: String,
    statusMessage: String? = null,
    logFileName: String? = null,
    isUncaughtException: Boolean = false,
    isIOS: Boolean = false,
) {
    PreviewCard {
        ReportBugPanelBody(
            errorMessage = errorMessage,
            statusMessage = statusMessage,
            logFileName = logFileName,
            onSaveToFile = {},
            onShareLogFile = {},
        )
        BisqGap.V1()
        ReportBugPanelButtons(
            isUncaughtException = isUncaughtException,
            isIOS = isIOS,
            onClose = {},
            onShutdown = {},
            onReport = {},
        )
    }
}

@ExcludeFromCoverage
@Composable
private fun PreviewCard(content: @Composable ColumnScope.() -> Unit) {
    BisqTheme.Preview {
        Surface(color = BisqTheme.colors.dark_grey30) {
            Column(
                modifier = Modifier.padding(BisqUIConstants.ScreenPadding2X),
                horizontalAlignment = Alignment.Start,
                content = content,
            )
        }
    }
}

private const val SHORT_ERROR = "Coroutine operation failed\nNoClassDefFoundError: Ljava/time/LocalDateTime;"

private val LONG_ERROR =
    """
    Error: Network connection failed

    Stack trace:
    at network.bisq.mobile.NetworkManager.connect(NetworkManager.kt:123)
    at network.bisq.mobile.AppPresenter.initialize(AppPresenter.kt:45)
    at network.bisq.mobile.MainActivity.onCreate(MainActivity.kt:67)

    Caused by: java.net.SocketTimeoutException: timeout
    at java.net.PlainSocketImpl.socketConnect(PlainSocketImpl.java:142)
    at java.net.AbstractPlainSocketImpl.doConnect(AbstractPlainSocketImpl.java:390)
    """.trimIndent()
