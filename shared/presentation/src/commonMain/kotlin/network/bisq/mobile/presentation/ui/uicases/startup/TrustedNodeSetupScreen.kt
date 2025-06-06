package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
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
import network.bisq.mobile.presentation.ui.components.molecules.settings.BreadcrumbNavigation
import network.bisq.mobile.presentation.ui.components.molecules.settings.MenuItem
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.uicases.settings.ISettingsPresenter
import org.koin.compose.koinInject

interface ITrustedNodeSetupPresenter : ViewPresenter {
    val isBisqApiUrlValid: StateFlow<Boolean>
    val isBisqApiVersionValid: StateFlow<Boolean>
    val bisqApiUrl: StateFlow<String>
    val trustedNodeVersion: StateFlow<String>
    val isConnected: StateFlow<Boolean>
    val isLoading: StateFlow<Boolean>

    fun isNewTrustedNodeUrl(): Boolean

    fun updateBisqApiUrl(newUrl: String, isValid: Boolean)

    /**
     * @return the error message if url is invalid, null otherwise
     */
    fun validateWsUrl(url: String): String?

    fun testConnection(isWorkflow: Boolean)

    fun navigateToNextScreen(isWorkflow: Boolean)

    fun goBackToSetupScreen()

    suspend fun validateVersion(): Boolean
}

@Composable
fun TrustedNodeSetupScreen(isWorkflow: Boolean = true) {
    val presenter: ITrustedNodeSetupPresenter = koinInject()
    val settingsPresenter: ISettingsPresenter = koinInject()

    val bisqApiUrl = presenter.bisqApiUrl.collectAsState().value
    val isConnected = presenter.isConnected.collectAsState().value
    val isVersionValid = presenter.isBisqApiVersionValid.collectAsState().value
    val isLoading = presenter.isLoading.collectAsState().value
    val trustedNodeVersion = presenter.trustedNodeVersion.collectAsState().value
    val clipboardManager = LocalClipboardManager.current
    
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
                        contentDescription = "Warning",
                        tint = BisqTheme.colors.danger
                    )
                    BisqGap.H1()
                    BisqText.largeRegular("Warning") // TODO:i18n
                }
                
                BisqGap.V2()
                
                BisqText.baseRegular(
                    "Changing your trusted node will require setting up your profile again. All your current profile data will be lost.\n\nDo you want to continue?" // TODO:i18n
                )
                
                BisqGap.V2()
                
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BisqButton(
                        text = "Cancel", // TODO:i18n
                        type = BisqButtonType.Grey,
                        onClick = { showConfirmDialog.value = false }
                    )
                    
                    BisqButton(
                        text = "Continue", // TODO:i18n
                        onClick = { 
                            showConfirmDialog.value = false
                            presenter.testConnection(isWorkflow)
                        }
                    )
                }
            }
        }
    }

    val menuTree: MenuItem = settingsPresenter.menuTree()
    val menuPath = remember { mutableStateListOf(menuTree) }

    RememberPresenterLifecycle(presenter, {
        if (!isWorkflow) {
            menuPath.add((menuTree as MenuItem.Parent).children[3])
        }
    })

    BisqScrollScaffold(
        topBar = { TopBar("Trusted node") }, // TODO:i18n
        snackbarHostState = presenter.getSnackState()
    ) {
        if (isWorkflow) {
            BisqLogo()
            BisqGap.V2()
        } else {
            BreadcrumbNavigation(path = menuPath) { index ->
                if (index == 0) settingsPresenter.settingsNavigateBack()
            }
        }
        BisqText.largeRegular(
            text = "To use Bisq through your trusted node, please enter the URL to connect to. E.g. ws://10.0.2.2:8090", // TODO:i18n
        )
        BisqGap.V2()
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize().padding(horizontal = 0.dp)
        ) {
            BisqTextField(
                label = "Trusted Bisq Node URL", // TODO:i18n
                onValueChange = { url, isValid -> presenter.updateBisqApiUrl(url, isValid) },
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
                    text = "Paste", // TODO:i18n
                    type = BisqButtonType.Grey,
                    onClick = {
                        val annotatedString = clipboardManager.getText()
                        if (annotatedString != null) {
                            presenter.updateBisqApiUrl(annotatedString.text, false) // TODO: validation gets triggered?
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
            BisqText.baseRegularGrey("STATUS") // TODO:i18n
            BisqGap.V1()
            Row(verticalAlignment = Alignment.CenterVertically) {
                val statusText = if (isConnected)
                        if (isVersionValid) "Connected" else "Connected - Invalid version" // TODO:i18n
                    else
                        "Not Connected" // TODO:i18n
                BisqText.largeRegular(statusText)
                BisqGap.H1()
                BisqText.baseRegular(
                    text = "",
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(color = if (isConnected && isVersionValid) BisqTheme.colors.primary else BisqTheme.colors.danger)
                        .size(10.dp),
                )
            }
            BisqGap.V3()
            if (isConnected && !isVersionValid) {
                BisqText.baseRegular("Expected API version: ${BuildConfig.BISQ_API_VERSION}") // TODO:i18n
                BisqText.baseRegular("Node API version: $trustedNodeVersion") // TODO:i18n
            }
        }

        BisqGap.V4()

        // TODO lots of code repetition for small changes here needs a refactor
        if (!isConnected || (isConnected && !isVersionValid)) {
            BisqButton(
                text = "Test Connection", // TODO:i18n
                onClick = {
                    if (presenter.isNewTrustedNodeUrl()) {
                        showConfirmDialog.value = true
                    } else {
                        presenter.testConnection(isWorkflow)
                    }
                },
                padding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                disabled = !presenter.isBisqApiUrlValid.collectAsState().value,
                isLoading = isLoading,
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp)
            ) {
                AnimatedVisibility(
                    visible = isConnected,
                    enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(700)),
                ) {
                    BisqButton(
                        text = "Test Connection", // TODO:i18n
                        color = if (bisqApiUrl.isEmpty()) BisqTheme.colors.mid_grey10 else BisqTheme.colors.light_grey10,
                        onClick = {
                            if (presenter.isNewTrustedNodeUrl()) {
                                showConfirmDialog.value = true
                            } else {
                                presenter.testConnection(isWorkflow)
                            }
                        },
                        padding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                    )
                }
                AnimatedVisibility(
                    visible = isConnected,
                    enter = fadeIn(animationSpec = tween(300)),
                ) {
                    BisqButton(
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
