package network.bisq.mobile.client.trusted_node_setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.client.common.domain.websocket.ConnectionState
import network.bisq.mobile.client.common.domain.websocket.exception.IncompatibleHttpApiVersionException
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.BarcodeScannerView
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqSelect
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.common.ui.components.atoms.button.CloseIconButton
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ArrowDownIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ScanQrIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.common.ui.components.organisms.dialogs.BisqGeneralErrorDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import network.bisq.mobile.presentation.common.ui.utils.spaceBetweenWithMin
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrustedNodeSetupScreen(
    modifier: Modifier = Modifier,
    isWorkflow: Boolean = true,
) {
    val presenter: TrustedNodeSetupPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val connectionState by presenter.wsClientConnectionState.collectAsState()
    val isPairingInProgress by presenter.isPairingInProgress.collectAsState()
    val selectedProxyOption by presenter.selectedProxyOption.collectAsState()
    val apiUrl by presenter.restApiUrl.collectAsState()
    val pairingCode by presenter.pairingQrCodeString.collectAsState()
    val pairingCodeError by presenter.pairingCodeError.collectAsState()
    val clientName by presenter.clientName.collectAsState()
    val status by presenter.status.collectAsState()
    val torState by presenter.torState.collectAsState()
    val torProgress by presenter.torProgress.collectAsState()
    val timeoutCounter by presenter.timeoutCounter.collectAsState()
    val showQrCodeView by presenter.showQrCodeView.collectAsState()
    val showQrCodeError by presenter.showQrCodeError.collectAsState()
    val pairingCompleted by presenter.pairingCompleted.collectAsState()

    var showConfirmDialog by remember { mutableStateOf(false) }

    BisqScrollScaffold(
        modifier = modifier,
        topBar =
            if (!isWorkflow) {
                { TopBar(title = "mobile.trustedNodeSetup.title".i18n()) }
            } else {
                null
            },
        snackbarHostState = presenter.getSnackState(),
        bottomBar = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = BisqUIConstants.ScreenPadding,
                            horizontal = BisqUIConstants.ScreenPadding,
                        ),
            ) {
                // Status and countdown (kept visible outside scroll)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    BisqText.LargeRegular(
                        status,
                        color =
                            if (isPairingInProgress) {
                                BisqTheme.colors.warning
                            } else if (connectionState is ConnectionState.Connected) {
                                BisqTheme.colors.primary
                            } else {
                                BisqTheme.colors.danger
                            },
                    )
                    // Show countdown during pairing or connecting phases
                    if ((isPairingInProgress || connectionState is ConnectionState.Connecting) && timeoutCounter > 0) {
                        BisqText.LargeRegular(
                            timeoutCounter.toString(),
                            color = BisqTheme.colors.warning,
                        )
                    }
                }

                BisqGap.V1()
            }
        },
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize().padding(horizontal = 0.dp),
        ) {
            if (isWorkflow) {
                BisqText.H2Light(
                    "mobile.trustedNodeSetup.title".i18n(),
                    textAlign = TextAlign.Center,
                )
                BisqGap.V3()
            }

            BisqText.LargeRegular(text = "mobile.trustedNodeSetup.info".i18n())
            BisqGap.V2()

            // TODO add mobile.trustedNodeSetup.info.detail as overlay

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BisqGap.V2()

                BisqButton(
                    text = "mobile.trustedNodeSetup.pairingCode.scan".i18n(),
                    type =
                        if (pairingCompleted) {
                            BisqButtonType.Grey
                        } else {
                            BisqButtonType.Default
                        },
                    onClick = presenter::onShowQrCodeView,
                    // modifier = Modifier.size(BisqUIConstants.ScreenPadding4X),
                    leftIcon = { ScanQrIcon() },
                )
                BisqGap.V2()

                BisqTextField(
                    // modifier = Modifier.weight(0.8f).setBlurTrigger(blurTriggerSetup),
                    label = "mobile.trustedNodeSetup.pairingCode.textField".i18n(),
                    placeholder = "mobile.trustedNodeSetup.pairingCode.textField.prompt".i18n(),
                    onValueChange = { value, _ ->
                        presenter.onPairingCodeChanged(value)
                    },
                    value = pairingCode,
                    readOnly = true, // Only allow input via QR scan or paste
                    disabled = isPairingInProgress,
                    showPaste = true,
                    rightSuffix =
                        if (pairingCode.isNotEmpty() && !isPairingInProgress) {
                            { CloseIconButton(onClick = { presenter.onPairingCodeChanged("") }) }
                        } else {
                            null
                        },
                    helperText = pairingCodeError ?: "",
                    indicatorColor = if (pairingCodeError != null) BisqTheme.colors.danger else BisqTheme.colors.primary,
                )

                if (!apiUrl.isEmpty()) {
                    BisqGap.V1()
                    BisqTextField(
                        label = "mobile.trustedNodeSetup.apiUrl".i18n(),
                        value = apiUrl,
                        readOnly = true,
                        showCopy = true,
                    )
                }

                BisqGap.V4()
                if (isPairingInProgress) {
                    BisqButton(
                        text =
                            if (timeoutCounter > 0) {
                                "mobile.trustedNodeSetup.cancelWithTimeout".i18n(timeoutCounter.toString())
                            } else {
                                "mobile.trustedNodeSetup.cancel".i18n()
                            },
                        type = BisqButtonType.Grey,
                        onClick = presenter::onCancelPressed,
                    )
                } else {
                    BisqButton(
                        text = "mobile.trustedNodeSetup.testAndSave".i18n(),
                        backgroundColor = BisqTheme.colors.primaryDim,
                        onClick = { presenter.onTestAndSavePressed(true) },
                        disabled = pairingCode.isEmpty() || pairingCodeError != null,
                    )
                }
                BisqGap.V2()
            }

            BisqGap.V4()

            // Show Tor state when Tor is active
            if (selectedProxyOption == BisqProxyOption.INTERNAL_TOR || torState !is KmpTorService.TorState.Stopped) {
                Row(horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf)) {
                    BisqText.BaseRegular("mobile.trustedNodeSetup.torState".i18n())
                    BisqText.BaseRegular(torState.displayString)
                    if (torState is KmpTorService.TorState.Starting) {
                        BisqText.BaseRegular(" $torProgress%")
                    }
                }
            }

            val error =
                (connectionState as? ConnectionState.Disconnected)?.error
            if (error is IncompatibleHttpApiVersionException) {
                BisqGap.V3()
                BisqText.BaseRegular(
                    "mobile.trustedNodeSetup.version.expectedAPI".i18n(
                        BuildConfig.BISQ_API_VERSION,
                    ),
                )
                BisqText.BaseRegular(
                    "mobile.trustedNodeSetup.version.nodeAPI".i18n(
                        error.serverVersion,
                    ),
                )
            }
        }

        if (!isWorkflow) {
            BisqText.BaseRegular(
                "mobile.trustedNodeSetup.testConnection.message".i18n(),
                color = BisqTheme.colors.warning,
            )
        }

        BisqGap.V2()
    }

    // Add dialog component
    if (showConfirmDialog) {
        BisqDialog(
            onDismissRequest = { showConfirmDialog = false },
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "mobile.trustedNodeSetup.warning".i18n(),
                        tint = BisqTheme.colors.danger,
                    )
                    BisqGap.H1()
                    BisqText.LargeRegular("mobile.trustedNodeSetup.warning".i18n())
                }

                BisqGap.V2()

                BisqText.BaseRegular(
                    "mobile.trustedNodeSetup.changeWarning".i18n(),
                )

                BisqGap.V2()

                Row(
                    horizontalArrangement =
                        Arrangement.spaceBetweenWithMin(
                            BisqUIConstants.ScreenPadding,
                        ),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                ) {
                    BisqButton(
                        modifier = Modifier.fillMaxHeight(),
                        text = "mobile.trustedNodeSetup.cancel".i18n(),
                        type = BisqButtonType.Grey,
                        onClick = { showConfirmDialog = false },
                    )

                    BisqButton(
                        modifier = Modifier.fillMaxHeight(),
                        text = "mobile.trustedNodeSetup.continue".i18n(),
                        onClick = {
                            showConfirmDialog = false
                            presenter.onTestAndSavePressed(isWorkflow)
                        },
                    )
                }
            }
        }
    }

    if (showQrCodeView) {
        BarcodeScannerView(
            onCancel = presenter::onQrCodeViewDismissed,
            onFail = { presenter.onQrCodeFailed() },
        ) {
            presenter.onQrCodeResult(it.data)
        }
    }

    if (showQrCodeError) {
        BisqGeneralErrorDialog(
            errorTitle = "mobile.barcode.error.title".i18n(),
            errorMessage = "mobile.barcode.error.message".i18n(),
            onClose = presenter::onQrCodeErrorClosed,
        )
    }
}

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
