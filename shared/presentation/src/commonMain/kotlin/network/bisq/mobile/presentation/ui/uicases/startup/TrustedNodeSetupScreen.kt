package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
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
import network.bisq.mobile.client.httpclient.BisqProxyOption
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.client.websocket.ConnectionState
import network.bisq.mobile.client.websocket.exception.IncompatibleHttpApiVersionException
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqSelect
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.icons.ArrowDownIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.helpers.rememberBlurTriggerSetup
import network.bisq.mobile.presentation.ui.helpers.setBlurTrigger
import network.bisq.mobile.presentation.ui.helpers.spaceBetweenWithMin
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrustedNodeSetupScreen(isWorkflow: Boolean = true) {
    val presenter: TrustedNodeSetupPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val connectionState by presenter.wsClientConnectionState.collectAsState()
    val isNodeSetupInProgress by presenter.isNodeSetupInProgress.collectAsState()
    val selectedProxyOption by presenter.selectedProxyOption.collectAsState()
    val apiUrl by presenter.apiUrl.collectAsState()
    val apiUrlPrompt by presenter.apiUrlPrompt.collectAsState()
    val status by presenter.status.collectAsState()
    val isApiUrlValid by presenter.isApiUrlValid.collectAsState()
    val isProxyUrlValid by presenter.isProxyUrlValid.collectAsState()
    val proxyHost by presenter.proxyHost.collectAsState()
    val proxyPort by presenter.proxyPort.collectAsState()
    val password by presenter.password.collectAsState()
    val isNewApiUrl by presenter.isNewApiUrl.collectAsState()
    val torState by presenter.torState.collectAsState()
    val torProgress by presenter.torProgress.collectAsState()
    val timeoutCounter by presenter.timeoutCounter.collectAsState()

    val blurTriggerSetup = rememberBlurTriggerSetup()

    var showConfirmDialog by remember { mutableStateOf(false) }
    var showAdvancedOptions by remember { mutableStateOf(false) }

    BisqScrollScaffold(
        topBar = if (!isWorkflow) {
            { TopBar(title = "mobile.trustedNodeSetup.title".i18n()) }
        } else null,
        snackbarHostState = presenter.getSnackState(),
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = BisqUIConstants.ScreenPadding, horizontal = BisqUIConstants.ScreenPadding)
            ) {
                // Status and countdown (kept visible outside scroll)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BisqText.largeRegular(
                        status,
                        color = if (isNodeSetupInProgress) BisqTheme.colors.warning
                        else if (connectionState is ConnectionState.Connected) BisqTheme.colors.primary
                        else BisqTheme.colors.danger,
                    )
                    if (connectionState is ConnectionState.Connecting) {
                        BisqText.largeRegular(
                            timeoutCounter.toString(),
                            color = BisqTheme.colors.warning
                        )
                    }
                }

                BisqGap.V1()

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BisqButton(
                        text = if (isNodeSetupInProgress) "mobile.trustedNodeSetup.cancel".i18n() else "mobile.trustedNodeSetup.testAndSave".i18n(),
                        color = if (!isNodeSetupInProgress && (!isApiUrlValid || !isProxyUrlValid)) BisqTheme.colors.mid_grey10 else BisqTheme.colors.light_grey10,
                        disabled = if (isNodeSetupInProgress) false else (!isWorkflow || !isApiUrlValid || !isProxyUrlValid),
                        onClick = {
                            if (isNodeSetupInProgress) {
                                presenter.onCancelPressed()
                            } else if (isNewApiUrl) {
                                showConfirmDialog = true
                            } else {
                                presenter.onTestAndSavePressed(isWorkflow)
                            }
                        },
                        padding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                    )
                }
            }
        }
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize().padding(horizontal = 0.dp)
        ) {
            if (isWorkflow) {
                BisqText.h2Light(
                    "mobile.trustedNodeSetup.title".i18n(),
                    textAlign = TextAlign.Center
                )
                BisqGap.V2()
            }

            BisqText.largeRegular(text = "mobile.trustedNodeSetup.info".i18n())
            BisqGap.V2()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spaceBetweenWithMin(BisqUIConstants.ScreenPadding),
            ) {
                BisqTextField(
                    modifier = Modifier.weight(0.8f).setBlurTrigger(blurTriggerSetup),
                    label = "mobile.trustedNodeSetup.apiUrl".i18n(),
                    onValueChange = { apiUrl, _ -> if (isWorkflow) presenter.onApiUrlChanged(apiUrl) },
                    value = apiUrl,
                    placeholder = apiUrlPrompt,
                    disabled = isNodeSetupInProgress,
                    showPaste = true,
                    validation = {
                        presenter.validateApiUrl(
                            it, selectedProxyOption,
                        )
                    }
                )
            }

            AdvancedOptionsDrawer(
                showAdvancedOptions,
                { showAdvancedOptions = !showAdvancedOptions },
            ) {
                Column {
                    BisqSelect(
                        label = "mobile.trustedNodeSetup.proxy".i18n(),
                        options = BisqProxyOption.entries,
                        selectedKey = selectedProxyOption.name,
                        optionLabel = { it.displayString },
                        optionKey = { it.name },
                        onSelected = {
                            presenter.onProxyOptionChanged(it)
                            blurTriggerSetup.triggerBlur()
                        },
                        disabled = isNodeSetupInProgress || !isWorkflow,
                    )

                    BisqTextField(
                        label = "mobile.trustedNodeSetup.password".i18n(),
                        value = password,
                        onValueChange = {value, _ -> presenter.onPasswordChanged(value)},
                        keyboardType = KeyboardType.Password,
                        isPasswordField = true,
                        disabled = isNodeSetupInProgress || !isWorkflow,
                    )
                }
            }
            BisqGap.V1()

            if (selectedProxyOption == BisqProxyOption.INTERNAL_TOR || torState !is KmpTorService.TorState.Stopped) {
                Row(horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf)) {
                    BisqText.baseRegular("mobile.trustedNodeSetup.torState".i18n())
                    BisqText.baseRegular(torState.displayString)
                    if (torState is KmpTorService.TorState.Starting) {
                        BisqText.baseRegular(" $torProgress%")
                    }
                }
            }

            val isExternalProxyOption =
                selectedProxyOption == BisqProxyOption.EXTERNAL_TOR || selectedProxyOption == BisqProxyOption.SOCKS_PROXY
            AnimatedVisibility(isExternalProxyOption) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spaceBetweenWithMin(BisqUIConstants.ScreenPadding),
                ) {
                    BisqTextField(
                        modifier = Modifier.weight(0.8f),
                        label = "mobile.trustedNodeSetup.proxyHost".i18n(),
                        onValueChange = { host, _ -> presenter.onProxyHostChanged(host) },
                        value = proxyHost,
                        placeholder = "127.0.0.1",
                        keyboardType = KeyboardType.Decimal,
                        disabled = isNodeSetupInProgress || !isWorkflow,
                        validation = presenter::validateProxyHost,
                    )
                    BisqTextField(
                        modifier = Modifier.weight(0.2f),
                        label = "mobile.trustedNodeSetup.port".i18n(),
                        onValueChange = { port, _ -> presenter.onProxyPortChanged(port) },
                        value = proxyPort,
                        placeholder = "9050",
                        keyboardType = KeyboardType.Decimal,
                        disabled = isNodeSetupInProgress || !isWorkflow,
                        validation = presenter::validatePort,
                    )
                }
            }

            val error = (connectionState as? ConnectionState.Disconnected)?.error
            if (error is IncompatibleHttpApiVersionException) {
                BisqGap.V3()
                BisqText.baseRegular("mobile.trustedNodeSetup.version.expectedAPI".i18n(BuildConfig.BISQ_API_VERSION))
                BisqText.baseRegular(
                    "mobile.trustedNodeSetup.version.nodeAPI".i18n(
                        error.serverVersion
                    )
                )
            }
        }

        if (!isWorkflow) {
            BisqText.baseRegular(
                "mobile.trustedNodeSetup.testConnection.message".i18n(),
                BisqTheme.colors.warning
            )
        }

        BisqGap.V2()


    }

    // Add dialog component
    if (showConfirmDialog) {
        BisqDialog(
            onDismissRequest = { showConfirmDialog = false }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "mobile.trustedNodeSetup.warning".i18n(),
                        tint = BisqTheme.colors.danger
                    )
                    BisqGap.H1()
                    BisqText.largeRegular("mobile.trustedNodeSetup.warning".i18n())
                }

                BisqGap.V2()

                BisqText.baseRegular(
                    "mobile.trustedNodeSetup.changeWarning".i18n()
                )

                BisqGap.V2()

                Row(
                    horizontalArrangement = Arrangement.spaceBetweenWithMin(BisqUIConstants.ScreenPadding),
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)
                ) {
                    BisqButton(
                        modifier = Modifier.fillMaxHeight(),
                        text = "mobile.trustedNodeSetup.cancel".i18n(),
                        type = BisqButtonType.Grey,
                        onClick = { showConfirmDialog = false }
                    )

                    BisqButton(
                        modifier = Modifier.fillMaxHeight(),
                        text = "mobile.trustedNodeSetup.continue".i18n(),
                        onClick = {
                            showConfirmDialog = false
                            presenter.onTestAndSavePressed(isWorkflow)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AdvancedOptionsDrawer(
    expanded: Boolean = false,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f)

    Column {
        Row(
            modifier = Modifier.clickable(onClick = onToggle).semantics(true) {
                contentDescription =
                    if (expanded) "mobile.action.hide".i18n() else "mobile.action.show".i18n()
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                BisqUIConstants.ScreenPadding
            ),
        ) {
            BisqText.smallRegularGrey("mobile.trustedNodeSetup.advancedOptions".i18n())
            BisqHDivider(modifier = Modifier.weight(1f))
            OutlinedIconButton(
                onClick = onToggle,
                modifier = Modifier.size(24.dp).clearAndSetSemantics { hideFromAccessibility() },
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
fun AdvancedOptionsDrawerCollapsedPreview() {
    BisqTheme.Preview {
        AdvancedOptionsDrawer(false, {}) {}
    }
}

@Preview
@Composable
fun AdvancedOptionsDrawerExpandedPreview() {
    BisqTheme.Preview {
        AdvancedOptionsDrawer(true, {}) {
            BisqText.baseRegular("this is content")
        }
    }
}