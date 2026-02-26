package network.bisq.mobile.client.trusted_node_setup

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.client.trusted_node_setup.use_case.TrustedNodeConnectionStatus
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.BarcodeScannerView
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.button.CloseIconButton
import network.bisq.mobile.presentation.common.ui.components.atoms.button.CopyIconButton
import network.bisq.mobile.presentation.common.ui.components.atoms.button.PasteIconButton
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ScanQrIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.common.ui.components.organisms.dialogs.BisqGeneralErrorDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
fun TrustedNodeSetupScreen(
    isWorkflow: Boolean = true,
    showConnectionFailed: Boolean = false,
) {
    val presenter: TrustedNodeSetupPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    LaunchedEffect(isWorkflow, showConnectionFailed) {
        presenter.initialize(isWorkflow, showConnectionFailed)
    }

    val uiState by presenter.uiState.collectAsState()

    TrustedNodeSetupContent(
        uiState = uiState,
        onAction = presenter::onAction,
        isWorkflow = isWorkflow,
        topBar =
            if (!isWorkflow) {
                { TopBar(title = "mobile.trustedNodeSetup.title".i18n()) }
            } else {
                {}
            },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrustedNodeSetupContent(
    uiState: TrustedNodeSetupUiState,
    onAction: (TrustedNodeSetupUiAction) -> Unit,
    isWorkflow: Boolean = true,
    topBar: @Composable () -> Unit = {},
) {
    BisqScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = topBar,
        bottomBar = {
            TrustedNodeBottomBar(uiState = uiState)
        },
    ) { paddingValues ->
        TrustedNodeSetupMainContent(
            uiState = uiState,
            onAction = onAction,
            isWorkflow = isWorkflow,
            paddingValues = paddingValues,
        )
    }

    if (uiState.showQrCodeView) {
        BarcodeScannerView(
            onCancel = { onAction(TrustedNodeSetupUiAction.OnQrCodeViewDismiss) },
            onFail = { onAction(TrustedNodeSetupUiAction.OnQrCodeFail) },
        ) {
            onAction(TrustedNodeSetupUiAction.OnQrCodeResult(it.data))
        }
    }

    if (uiState.showQrCodeError) {
        BisqGeneralErrorDialog(
            errorTitle = "mobile.barcode.error.title".i18n(),
            errorMessage = "mobile.barcode.error.message".i18n(),
            onClose = { onAction(TrustedNodeSetupUiAction.OnQrCodeErrorClose) },
        )
    }

    if (uiState.showChangeNodeWarning) {
        ConfirmationDialog(
            headlineColor = BisqTheme.colors.warning,
            headlineLeftIcon = { WarningIcon() },
            headline = "mobile.trustedNodeSetup.warning".i18n(),
            message = "mobile.trustedNodeSetup.changeWarning".i18n(),
            confirmButtonText = "mobile.trustedNodeSetup.continue".i18n(),
            dismissButtonText = "mobile.trustedNodeSetup.cancel".i18n(),
            onConfirm = { onAction(TrustedNodeSetupUiAction.OnChangeNodeWarningConfirm) },
            onDismiss = { onAction(TrustedNodeSetupUiAction.OnChangeNodeWarningCancel) },
        )
    }

    if (uiState.showConnectionFailedWarning) {
        ConfirmationDialog(
            headlineColor = BisqTheme.colors.danger,
            headline = "mobile.trustedNodeSetup.status.failed".i18n(),
            message = "mobile.trustedNodeSetup.connectionFailed.message".i18n(),
            confirmButtonText = "mobile.action.retry".i18n(),
            dismissButtonText = "mobile.trustedNodeSetup.pairWithNewNode".i18n(),
            onConfirm = { onAction(TrustedNodeSetupUiAction.OnConnectionFailedRetryPress) },
            onDismiss = { onAction(TrustedNodeSetupUiAction.OnConnectionFailedPairWithNewNodePress) },
            dismissOnClickOutside = false,
            verticalButtonPlacement = true,
        )
    }
}

@Composable
private fun TrustedNodeSetupMainContent(
    uiState: TrustedNodeSetupUiState,
    onAction: (TrustedNodeSetupUiAction) -> Unit,
    isWorkflow: Boolean,
    paddingValues: PaddingValues,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp),
    ) {
        if (isWorkflow) {
            BisqText.H2Light(
                "mobile.trustedNodeSetup.title".i18n(),
                textAlign = TextAlign.Center,
            )
            BisqGap.V3()
        }

        BisqText.LargeRegular(text = "mobile.trustedNodeSetup.info".i18n())
        BisqGap.V1()

        // TODO add mobile.trustedNodeSetup.info.detail as overlay

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BisqGap.V1()
            if (isWorkflow) {
                BisqButton(
                    text = "mobile.trustedNodeSetup.pairingCode.scan".i18n(),
                    type =
                        if (uiState.canScanQrCode(isWorkflow)) {
                            BisqButtonType.Default
                        } else {
                            BisqButtonType.Grey
                        },
                    onClick = { onAction(TrustedNodeSetupUiAction.OnShowQrCodeView) },
                    leftIcon = { ScanQrIcon() },
                    disabled = !uiState.canScanQrCode(isWorkflow),
                )
                BisqGap.V2()
            }

            if (isWorkflow) {
                BisqTextFieldV0(
                    label = "mobile.trustedNodeSetup.pairingCode.textField".i18n(),
                    placeholder = "mobile.trustedNodeSetup.pairingCode.textField.prompt".i18n(),
                    value = uiState.pairingCodeEntry.value,
                    singleLine = true,
                    enabled = !uiState.isConnectionInProgress(),
                    onValueChange = { value ->
                        onAction(TrustedNodeSetupUiAction.OnPairingCodeChange(value))
                    },
                    trailingIcon = {
                        if (uiState.showPairingCodeActions()) {
                            PasteIconButton(onPaste = { pasted ->
                                onAction(TrustedNodeSetupUiAction.OnPairingCodeChange(pasted))
                            })
                        }
                    },
                    suffix =
                        if (uiState.pairingCodeEntry.value.isNotEmpty() && uiState.showPairingCodeActions()) {
                            {
                                CloseIconButton(onClick = {
                                    onAction(TrustedNodeSetupUiAction.OnPairingCodeChange(""))
                                })
                            }
                        } else {
                            null
                        },
                    bottomMessage = uiState.pairingCodeEntry.errorMessage,
                    isError = !uiState.pairingCodeEntry.isValid,
                )
            }

            if (uiState.apiUrl.isNotEmpty()) {
                BisqGap.V1()
                BisqTextFieldV0(
                    label = "mobile.trustedNodeSetup.apiUrl".i18n(),
                    value = uiState.apiUrl,
                    singleLine = true,
                    readOnly = true,
                    trailingIcon = {
                        CopyIconButton(uiState.apiUrl)
                    },
                    onValueChange = {},
                )
            }

            BisqGap.V2()
            if (uiState.isConnectionInProgress()) {
                BisqButton(
                    text =
                        if (uiState.timeoutCounter > 0) {
                            "mobile.trustedNodeSetup.cancelWithTimeout".i18n(uiState.timeoutCounter.toString())
                        } else {
                            "mobile.trustedNodeSetup.cancel".i18n()
                        },
                    type = BisqButtonType.Grey,
                    onClick = { onAction(TrustedNodeSetupUiAction.OnCancelPress) },
                )
            } else if (isWorkflow) {
                BisqButton(
                    text = "mobile.trustedNodeSetup.testAndSave".i18n(),
                    backgroundColor = BisqTheme.colors.primaryDim,
                    onClick = { onAction(TrustedNodeSetupUiAction.OnTestAndSavePress) },
                    disabled = uiState.isTestButtonDisabled(),
                )
            }

            if (!isWorkflow) {
                BisqGap.V1()
                BisqButton(
                    text = "mobile.trustedNodeSetup.pairWithNewNode".i18n(),
                    onClick = { onAction(TrustedNodeSetupUiAction.OnPairWithNewNodePress) },
                )
            }
        }

        BisqGap.V2()
        if (uiState.shouldShowTorState()) {
            Row(horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf)) {
                BisqText.BaseRegular("mobile.trustedNodeSetup.torState".i18n())
                BisqText.BaseRegular(uiState.torState.displayString)
                if (uiState.isTorStarting()) {
                    BisqText.BaseRegular(" ${uiState.torProgress}%")
                }
            }
        }

        if (uiState.hasIncompatibleApiVersion()) {
            BisqText.BaseRegular(
                "mobile.trustedNodeSetup.version.expectedAPI".i18n(BuildConfig.BISQ_API_VERSION),
            )
            BisqText.BaseRegular(
                "mobile.trustedNodeSetup.version.nodeAPI".i18n(
                    uiState.serverVersion,
                ),
            )
        }

        BisqGap.V2()
    }
}

@Composable
private fun TrustedNodeBottomBar(
    uiState: TrustedNodeSetupUiState,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .background(BisqTheme.colors.backgroundColor)
                .fillMaxWidth()
                .padding(16.dp),
    ) {
        val color =
            when {
                uiState.isWarningStatus -> BisqTheme.colors.warning
                uiState.isSuccessStatus -> BisqTheme.colors.primary
                uiState.isErrorStatus -> BisqTheme.colors.danger
                else -> BisqTheme.colors.light_grey10
            }
        BisqText.LargeRegular(
            uiState.status.displayString,
            color = color,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (uiState.isConnectionInProgress() && uiState.timeoutCounter > 0) {
            Spacer(modifier = Modifier.width(4.dp))
            BisqText.LargeRegular(
                uiState.timeoutCounter.toString(),
                color = color,
            )
        }
    }
}

private val PREVIEW_PAIRING_CODE by lazy {
    DataEntry("AQBbAQAkNDY4NTI0NTAtNzViMy00OTU1LWJiMTAtZGY0MWQ3ZjViZTk5AAABnBYUkQQAAAAKAAAAAAAAAAEAAAACAAAAAwAAAAQAAAAFAAAABgAAAAcAAAAIAAAACQAVaHR0cDovL2xvY2FsaG9zdDo4MDkwAA")
}

@Preview
@Composable
private fun TrustedNodeSetupContent_IdlePreview() {
    BisqTheme.Preview {
        TrustedNodeSetupContent(
            uiState =
                TrustedNodeSetupUiState(
                    status = TrustedNodeConnectionStatus.Idle,
                ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun TrustedNodeSetupContent_ReadyToTestPreview() {
    BisqTheme.Preview {
        TrustedNodeSetupContent(
            uiState =
                TrustedNodeSetupUiState(
                    status = TrustedNodeConnectionStatus.Idle,
                    pairingCodeEntry = PREVIEW_PAIRING_CODE,
                    apiUrl = "http://127.0.0.1:8090",
                ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun TrustedNodeSetupContent_PairingInProgressPreview() {
    BisqTheme.Preview {
        TrustedNodeSetupContent(
            uiState =
                TrustedNodeSetupUiState(
                    status = TrustedNodeConnectionStatus.Connecting,
                    timeoutCounter = 30,
                    pairingCodeEntry = PREVIEW_PAIRING_CODE,
                    apiUrl = "http://127.0.0.1:8090",
                ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun TrustedNodeSetupContent_ConnectedPreview() {
    BisqTheme.Preview {
        TrustedNodeSetupContent(
            uiState =
                TrustedNodeSetupUiState(
                    status = TrustedNodeConnectionStatus.Connected,
                    pairingCodeEntry = PREVIEW_PAIRING_CODE,
                    apiUrl = "http://127.0.0.1:8090",
                ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun TrustedNodeSetupContent_ErrorPreview() {
    BisqTheme.Preview {
        TrustedNodeSetupContent(
            uiState =
                TrustedNodeSetupUiState(
                    status = TrustedNodeConnectionStatus.Failed(),
                    pairingCodeEntry = PREVIEW_PAIRING_CODE,
                    apiUrl = "http://127.0.0.1:8090",
                ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun TrustedNodeSetupContent_TorStartingPreview() {
    BisqTheme.Preview {
        TrustedNodeSetupContent(
            uiState =
                TrustedNodeSetupUiState(
                    status = TrustedNodeConnectionStatus.BootstrappingTor,
                    torState = KmpTorService.TorState.Starting,
                    torProgress = 45,
                    pairingCodeEntry = PREVIEW_PAIRING_CODE,
                ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun TrustedNodeSetupContent_PairingCodeErrorPreview() {
    BisqTheme.Preview {
        TrustedNodeSetupContent(
            uiState =
                TrustedNodeSetupUiState(
                    pairingCodeEntry = DataEntry("invalid_code", "Invalid pairing code format"),
                ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun TrustedNodeSetupContent_QrCodeErrorPreview() {
    BisqTheme.Preview {
        TrustedNodeSetupContent(
            uiState =
                TrustedNodeSetupUiState(
                    status = TrustedNodeConnectionStatus.Idle,
                    showQrCodeError = true,
                ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun TrustedNodeSetupContent_ApiVersionErrorPreview() {
    BisqTheme.Preview {
        TrustedNodeSetupContent(
            uiState =
                TrustedNodeSetupUiState(
                    status = TrustedNodeConnectionStatus.IncompatibleHttpApiVersion,
                    serverVersion = "1.5.0",
                    pairingCodeEntry = PREVIEW_PAIRING_CODE,
                    apiUrl = "http://127.0.0.1:8090",
                ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun TrustedNodeSetupContent_Settings_ConnectedPreview() {
    BisqTheme.Preview {
        TrustedNodeSetupContent(
            uiState =
                TrustedNodeSetupUiState(
                    status = TrustedNodeConnectionStatus.Connected,
                    pairingCodeEntry = PREVIEW_PAIRING_CODE,
                    apiUrl = "http://127.0.0.1:8090",
                ),
            onAction = {},
            isWorkflow = false,
        )
    }
}

@Preview
@Composable
private fun TrustedNodeSetupContent_ChangeNodeWarningPreview() {
    BisqTheme.Preview {
        TrustedNodeSetupContent(
            uiState =
                TrustedNodeSetupUiState(
                    status = TrustedNodeConnectionStatus.Connected,
                    pairingCodeEntry = PREVIEW_PAIRING_CODE,
                    apiUrl = "http://127.0.0.1:8090",
                    showChangeNodeWarning = true,
                ),
            onAction = {},
            isWorkflow = false,
        )
    }
}
