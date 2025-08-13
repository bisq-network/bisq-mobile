package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.icons.BisqLogo
import network.bisq.mobile.presentation.ui.components.atoms.icons.CopyIcon
import network.bisq.mobile.presentation.ui.components.atoms.icons.QuestionIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.helpers.spaceBetweenWithMin
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

interface ITrustedNodeSetupPresenter : ViewPresenter {
    val isBisqApiUrlValid: StateFlow<Boolean>
    val isBisqApiVersionValid: StateFlow<Boolean>
    val bisqApiUrl: StateFlow<String>
    val trustedNodeVersion: StateFlow<String>
    val isConnected: StateFlow<Boolean>
    val isLoading: StateFlow<Boolean>

    fun isNewTrustedNodeUrl(): Boolean

    fun updateBisqApiUrl(newUrl: String)

    /**
     * @return the error message if url is invalid, null otherwise
     */
    fun validateWsUrl(url: String): String?

    fun testConnection(isWorkflow: Boolean)

    fun navigateToNextScreen(isWorkflow: Boolean)

    fun goBackToSetupScreen()

    suspend fun validateVersion(): Boolean
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrustedNodeSetupScreen(isWorkflow: Boolean = true) {
    val presenter: ITrustedNodeSetupPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val bisqApiUrl by presenter.bisqApiUrl.collectAsState()
    val isConnected by presenter.isConnected.collectAsState()
    val isVersionValid by presenter.isBisqApiVersionValid.collectAsState()
    val isLoading by presenter.isLoading.collectAsState()
    val trustedNodeVersion by presenter.trustedNodeVersion.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val isBisqApiUrlValid by presenter.isBisqApiUrlValid.collectAsState()

    // Add state for dialog
    val showConfirmDialog = remember { mutableStateOf(false) }

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

    BisqScrollScaffold(
        topBar = { TopBar("mobile.trustedNodeSetup.title".i18n()) },
        snackbarHostState = presenter.getSnackState()
    ) {
        if (isWorkflow) {
            BisqLogo()
            BisqGap.V2()
        }
        BisqText.largeRegular(
            text = "mobile.trustedNodeSetup.subTitle".i18n(),
        )
        BisqGap.V2()
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize().padding(horizontal = 0.dp)
        ) {
            BisqTextField(
                label = "mobile.trustedNodeSetup.trustedBisqNodeURL.label".i18n(),
                onValueChange = { url, isValid -> presenter.updateBisqApiUrl(url) },
                value = bisqApiUrl,
                placeholder = "ws://10.0.2.2:8090",
                keyboardType = KeyboardType.Uri,
                disabled = isLoading,
                labelRightSuffix = {
                    BisqButton(
                        iconOnly = { QuestionIcon() },
                        backgroundColor = BisqTheme.colors.backgroundColor,
                        onClick = { /* presenter.navigateToNextScreen(isWorkflow) */ },
                        disabled = isLoading,
                    )
                },
                validation = {
                    return@BisqTextField presenter.validateWsUrl(it)
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BisqButton(
                    text = "mobile.trustedNodeSetup.paste".i18n(),
                    type = BisqButtonType.Grey,
                    onClick = {
                        val annotatedString = clipboardManager.getText()
                        if (annotatedString != null) {
                            presenter.updateBisqApiUrl(annotatedString.text)
                        }
                    },
                    disabled = isLoading,
                    color = BisqTheme.colors.light_grey10,
                    leftIcon = { CopyIcon() }
                )
//              TODO uncomment when feature gets implemented
//                BisqButton(
//                    text = "Scan",
//                    onClick = {},
//                    leftIcon= { ScanIcon() }
//                )
            }
            BisqGap.V3()
            BisqText.baseRegularGrey("mobile.trustedNodeSetup.status".i18n())
            BisqGap.V1()
            Row(verticalAlignment = Alignment.CenterVertically) {
                val statusText = if (isConnected)
                    if (isVersionValid) "mobile.trustedNodeSetup.status.connected".i18n() else "mobile.trustedNodeSetup.status.connectedInvalidVersion".i18n()
                else
                    "mobile.trustedNodeSetup.status.notConnected".i18n()
                BisqText.largeRegular(statusText)
                BisqGap.H1()
                BisqText.baseRegular(
                    text = "",
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            color =
                                if (isLoading)
                                    BisqTheme.colors.warning
                                else if (isConnected && isVersionValid)
                                    BisqTheme.colors.primary
                                else
                                    BisqTheme.colors.danger
                        )
                        .size(10.dp),
                )
            }
            BisqGap.V3()
            if (isConnected && !isVersionValid) {
                BisqText.baseRegular("mobile.trustedNodeSetup.version.expectedAPI".i18n(BuildConfig.BISQ_API_VERSION))
                BisqText.baseRegular(
                    "mobile.trustedNodeSetup.version.nodeAPI".i18n(
                        trustedNodeVersion
                    )
                )
            }
        }

        BisqGap.V4()

        LazyRow(
            horizontalArrangement = if (isConnected)
                Arrangement.spaceBetweenWithMin(BisqUIConstants.ScreenPadding)
            else Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            item {
                BisqButton(
                    modifier = Modifier.animateItem(),
                    text = "mobile.trustedNodeSetup.testConnection".i18n(),
                    color = if (bisqApiUrl.isEmpty()) BisqTheme.colors.mid_grey10 else BisqTheme.colors.light_grey10,
                    onClick = {
                        if (presenter.isNewTrustedNodeUrl()) {
                            showConfirmDialog.value = true
                        } else {
                            presenter.testConnection(isWorkflow)
                        }
                    },
                    padding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                    disabled = !isBisqApiUrlValid,
                )
            }

            if (isConnected) {
                item {
                    BisqButton(
                        modifier = Modifier.animateItem(),
                        text = if (isWorkflow) "action.next".i18n() else "action.save".i18n(),
                        color = BisqTheme.colors.light_grey10,
                        onClick = { presenter.navigateToNextScreen(isWorkflow) },
                        padding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}
