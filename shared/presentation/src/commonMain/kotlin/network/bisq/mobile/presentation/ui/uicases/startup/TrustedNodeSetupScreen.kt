package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import network.bisq.mobile.presentation.ui.components.atoms.BisqDropDown
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.helpers.rememberBlurTriggerSetup
import network.bisq.mobile.presentation.ui.helpers.setBlurTrigger
import network.bisq.mobile.presentation.ui.helpers.spaceBetweenWithMin
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrustedNodeSetupScreen(isWorkflow: Boolean = true) {
    val presenter: TrustedNodeSetupPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val connectionState by presenter.wsClientConnectionState.collectAsState()
    val isLoading by presenter.isLoading.collectAsState()
    val selectedProxyOption by presenter.selectedProxyOption.collectAsState()
    val apiUrl by presenter.apiUrl.collectAsState()
    val apiUrlPrompt by presenter.apiUrlPrompt.collectAsState()
    val status by presenter.status.collectAsState()
    val isApiUrlValid by presenter.isApiUrlValid.collectAsState()
    val isProxyUrlValid by presenter.isProxyUrlValid.collectAsState()
    val proxyHost by presenter.proxyHost.collectAsState()
    val proxyPort by presenter.proxyPort.collectAsState()
    val isNewApiUrl by presenter.isNewApiUrl.collectAsState()
    val torState by presenter.torState.collectAsState()
    val timeoutCounter by presenter.timeoutCounter.collectAsState()
    val proxyOptions = presenter.proxyOptions
    val isIOS = presenter.isIOS()

    val blurTriggerSetup = rememberBlurTriggerSetup()

    // Add state for dialog
    val showConfirmDialog = remember { mutableStateOf(false) }

    BisqScrollScaffold(
        topBar = if (!isWorkflow) {
            { TopBar(title = "mobile.trustedNodeSetup.title".i18n()) }
        } else null,
        snackbarHostState = presenter.getSnackState()
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
                    disabled = isLoading,
                    showPaste = true,
                    validation = {
                        presenter.validateApiUrl(
                            it, selectedProxyOption,
                        )
                    }
                )
            }
            BisqGap.V1()
            if (!isIOS) {
                BisqDropDown(
                    label = "mobile.trustedNodeSetup.proxy".i18n(),
                    items = proxyOptions,
                    value = selectedProxyOption.name,
                    onValueChanged = {
                        presenter.onProxyOptionChanged(it)
                        blurTriggerSetup.triggerBlur()
                    },
                    disabled = isLoading || !isWorkflow,
                )
                if (selectedProxyOption == BisqProxyOption.INTERNAL_TOR || torState != KmpTorService.State.IDLE) {
                    Row(horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)) {
                        BisqText.baseRegular("mobile.trustedNodeSetup.torState".i18n())
                        BisqText.baseRegular(torState.displayString)
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
                        disabled = isLoading,
                        validation = presenter::validateProxyHost,
                    )
                    BisqTextField(
                        modifier = Modifier.weight(0.2f),
                        label = "mobile.trustedNodeSetup.port".i18n(),
                        onValueChange = { port, _ -> presenter.onProxyPortChanged(port) },
                        value = proxyPort,
                        placeholder = "9050",
                        keyboardType = KeyboardType.Decimal,
                        disabled = isLoading,
                        validation = presenter::validatePort,
                    )
                }
            }
            BisqGap.V2()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            ) {
                BisqText.largeRegular(
                    status,
                    color =
                        if (isLoading)
                            BisqTheme.colors.warning
                        else if (connectionState is ConnectionState.Connected)
                            BisqTheme.colors.primary
                        else
                            BisqTheme.colors.danger,
                )
                if (connectionState is ConnectionState.Connecting) {
                    BisqText.largeRegular(timeoutCounter.toString(), color = BisqTheme.colors.warning)
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

        Row(
            horizontalArrangement = Arrangement.Center,
        ) {
            BisqButton(
                text = "mobile.trustedNodeSetup.testAndSave".i18n(),
                color = if (!isApiUrlValid || !isProxyUrlValid) BisqTheme.colors.mid_grey10 else BisqTheme.colors.light_grey10,
                disabled = !isWorkflow || isLoading || !isApiUrlValid || !isProxyUrlValid,
                onClick = {
                    if (isNewApiUrl) {
                        showConfirmDialog.value = true
                    } else {
                        presenter.onTestAndSavePressed(isWorkflow)
                    }
                },
                padding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
            )
        }

        BisqGap.V1()
    }

    // Add dialog component
    if (showConfirmDialog.value) {
        BisqDialog(
            onDismissRequest = { showConfirmDialog.value = false }
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
                        onClick = { showConfirmDialog.value = false }
                    )

                    BisqButton(
                        modifier = Modifier.fillMaxHeight(),
                        text = "mobile.trustedNodeSetup.continue".i18n(),
                        onClick = {
                            showConfirmDialog.value = false
                            presenter.onTestAndSavePressed(isWorkflow)
                        }
                    )
                }
            }
        }
    }
}
