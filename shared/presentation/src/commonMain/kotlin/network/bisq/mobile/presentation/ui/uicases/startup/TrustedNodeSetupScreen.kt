package network.bisq.mobile.presentation.ui.uicases.startup

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.client.websocket.ConnectionState
import network.bisq.mobile.client.websocket.exception.IncompatibleHttpApiVersionException
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.ui.components.molecules.ToggleTab
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.helpers.spaceBetweenWithMin
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.ui.uicases.startup.TrustedNodeSetupPresenter.NetworkType
import org.koin.compose.koinInject

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrustedNodeSetupScreen(isWorkflow: Boolean = true) {
    val presenter: TrustedNodeSetupPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val host by presenter.host.collectAsState()
    val port by presenter.port.collectAsState()
    val connectionState by presenter.wsClientConnectionState.collectAsState()
    val isLoading by presenter.isLoading.collectAsState()
    val selectedNetworkType by presenter.selectedNetworkType.collectAsState()
    val hostPrompt by presenter.hostPrompt.collectAsState()
    val status by presenter.status.collectAsState()
    val isApiUrlValid by presenter.isApiUrlValid.collectAsState()

    // Add state for dialog
    val showConfirmDialog = remember { mutableStateOf(false) }

    val networkType: List<NetworkType> = if (BuildConfig.IS_DEBUG) {
        listOf(NetworkType.LAN, NetworkType.TOR)
    } else {
        listOf(NetworkType.LAN)
    }

    val isNewApiUrl by produceState(initialValue = false, host, port, selectedNetworkType) {
        value = presenter.isNewApiUrl()
    }

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
                BisqText.h2Light("mobile.trustedNodeSetup.title".i18n(), textAlign = TextAlign.Center)
                BisqGap.V2()
            }

            BisqText.largeRegular(text = "mobile.trustedNodeSetup.info".i18n())
            BisqGap.V2()

            ToggleTab(
                options = networkType,
                selectedOption = selectedNetworkType,
                onOptionSelected = { presenter.onNetworkType(it) },
                singleLine = true,
                getDisplayString = { it.displayString }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spaceBetweenWithMin(BisqUIConstants.ScreenPadding),
            ) {
                BisqTextField(
                    modifier = Modifier.weight(0.8f),
                    label = "mobile.trustedNodeSetup.host".i18n(),
                    onValueChange = { host, _ -> presenter.onHostChanged(host) },
                    value = host,
                    placeholder = hostPrompt,
                    keyboardType = if (selectedNetworkType == NetworkType.LAN) {
                        KeyboardType.Decimal
                    } else {
                        KeyboardType.Text
                    },
                    disabled = isLoading,
                    validation = { return@BisqTextField presenter.validateHost(it) }
                )
                BisqTextField(
                    modifier = Modifier.weight(0.2f),
                    label = "mobile.trustedNodeSetup.port".i18n(),
                    onValueChange = { port, _ -> presenter.onPortChanged(port) },
                    value = port,
                    placeholder = "8090",
                    keyboardType = KeyboardType.Decimal,
                    disabled = isLoading,
                    validation = { return@BisqTextField presenter.validatePort(it) }
                )
            }
            BisqGap.V3()
            Row(verticalAlignment = Alignment.CenterVertically) {
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
            }
            BisqGap.V3()

            val error = (connectionState as? ConnectionState.Disconnected)?.error
            if (error is IncompatibleHttpApiVersionException) {
                BisqText.baseRegular("mobile.trustedNodeSetup.version.expectedAPI".i18n(BuildConfig.BISQ_API_VERSION))
                BisqText.baseRegular(
                    "mobile.trustedNodeSetup.version.nodeAPI".i18n(
                        error.serverVersion
                    )
                )
            }
        }

        BisqGap.V4()

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(
                BisqUIConstants.ScreenPadding,
                Alignment.CenterHorizontally
            ),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            item {
                if (connectionState !is ConnectionState.Connected) {
                    BisqButton(
                        modifier = Modifier.animateItem(),
                        text = "mobile.trustedNodeSetup.testConnection".i18n(),
                        color = if (host.isEmpty()) BisqTheme.colors.mid_grey10 else BisqTheme.colors.light_grey10,
                        disabled = isLoading || !isApiUrlValid,
                        onClick = {
                            if (isNewApiUrl) {
                                showConfirmDialog.value = true
                            } else {
                                presenter.testConnection(isWorkflow)
                            }
                        },
                        padding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                    )
                } else {
                    BisqButton(
                        modifier = Modifier.animateItem(),
                        text = if (isWorkflow) "mobile.trustedNodeSetup.createProfile".i18n() else "action.save".i18n(),
                        color = BisqTheme.colors.light_grey10,
                        onClick = {
                            if (isWorkflow) presenter.navigateToCreateProfile()
                            else presenter.onSave()
                        },
                        padding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                    )
                }
            }
        }
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
                            presenter.testConnection(isWorkflow)
                        }
                    )
                }
            }
        }
    }
}
