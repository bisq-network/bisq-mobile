package network.bisq.mobile.client.trusted_node_setup

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.client.trusted_node_setup.use_case.TrustedNodeConnectionStatus
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarContent
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * UI tests for TrustedNodeSetupContent using Robolectric.
 *
 * These tests verify that the TrustedNodeSetupContent composable renders correctly
 * for different UI states and that user interactions trigger the appropriate actions.
 */
@Config(application = TestApplication::class)
@RunWith(AndroidJUnit4::class)
class TrustedNodeSetupContentUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockOnAction: (TrustedNodeSetupUiAction) -> Unit
    private lateinit var snackbarHostState: SnackbarHostState

    // Test data
    private val validPairingCode =
        "AQBbAQAkNDY4NTI0NTAtNzViMy00OTU1LWJiMTAtZGY0MWQ3ZjViZTk5AAABnBYUkQQAAAAKAAAAAAAAAAEAAAACAAAAAwAAAAQAAAAFAAAABgAAAAcAAAAIAAAACQAVaHR0cDovL2xvY2FsaG9zdDo4MDkwAA"
    private val sampleApiUrl = "http://127.0.0.1:8090"

    @Before
    fun setup() {
        mockOnAction = mockk(relaxed = true)
        snackbarHostState = SnackbarHostState()
    }

    /**
     * Helper function to set up test content with LocalIsTest enabled.
     * Wraps content with CompositionLocalProvider and BisqTheme to avoid repetition.
     */
    private fun setTestContent(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    content()
                }
            }
        }
    }

    // ========== Idle State Tests ==========

    @Test
    fun `when idle state renders then shows all workflow elements`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.Idle,
            )

        // When
        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
            )
        }

        // Then - Static content
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.title".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.info".i18n())
            .assertIsDisplayed()

        // Then - Pairing code text field
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.pairingCode.textField".i18n())
            .assertIsDisplayed()

        // Then - Scan QR button is enabled
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.pairingCode.scan".i18n())
            .assertIsDisplayed()
            .assertIsEnabled()

        // Then - Test & Save button is disabled (empty pairing code)
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.testAndSave".i18n())
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    // ========== Bottom Bar Status Tests ==========

    @Test
    fun `when connecting state renders then shows connecting status in bottom bar`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.Connecting,
                pairingCodeEntry = DataEntry(validPairingCode),
            )

        // When
        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText(TrustedNodeConnectionStatus.Connecting.displayString, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `when connected state renders then shows connected status in bottom bar`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.Connected,
                pairingCodeEntry = DataEntry(validPairingCode),
                apiUrl = sampleApiUrl,
            )

        // When
        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText(TrustedNodeConnectionStatus.Connected.displayString, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `when connecting with timeout renders then shows timeout counter in bottom bar`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.Connecting,
                timeoutCounter = 30,
                pairingCodeEntry = DataEntry(validPairingCode),
            )

        // When
        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        // Timeout counter is displayed in bottom bar as separate text
        composeTestRule
            .onNodeWithText("30", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    // ========== Pairing Code Entry Tests ==========

    @Test
    fun `when pairing code entered then triggers OnPairingCodeChange action`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.Idle,
            )

        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
            )
        }

        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.pairingCode.textField.prompt".i18n())
            .performTextInput("test_code")

        // Then
        verify { mockOnAction(TrustedNodeSetupUiAction.OnPairingCodeChange("test_code")) }
    }

    @Test
    fun `when pairing code with error renders then shows error message`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                pairingCodeEntry = DataEntry("invalid_code", "Invalid pairing code format"),
            )

        // When
        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("Invalid pairing code format")
            .assertIsDisplayed()
    }

    @Test
    fun `when pairing code field has text and idle then shows paste and clear buttons`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.Idle,
                pairingCodeEntry = DataEntry("some_text"),
            )

        // When
        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
            )
        }

        // Then - Text field is displayed
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.pairingCode.textField".i18n())
            .assertIsDisplayed()

        // Then - Paste button is displayed (trailingIcon)
        composeTestRule
            .onNodeWithContentDescription("Paste icon")
            .assertIsDisplayed()

        // Then - Close/Clear button is displayed (suffix, shows when field has text)
        composeTestRule
            .onNodeWithContentDescription("close")
            .assertIsDisplayed()

        // Then - Test & Save button is enabled (field has text, status is idle)
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.testAndSave".i18n())
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun `when close button clicked then triggers OnPairingCodeChange with empty string`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.Idle,
                pairingCodeEntry = DataEntry("some_text"),
            )

        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
            )
        }

        composeTestRule.waitForIdle()

        // When - Click the close/clear button
        composeTestRule
            .onNodeWithContentDescription("close")
            .performClick()

        // Then - Verify action to clear the field
        verify { mockOnAction(TrustedNodeSetupUiAction.OnPairingCodeChange("")) }
    }

    @Test
    fun `when pairing code field has value then shows API url field`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                pairingCodeEntry = DataEntry(validPairingCode),
                apiUrl = sampleApiUrl,
            )

        // When
        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.apiUrl".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(sampleApiUrl)
            .assertIsDisplayed()
    }

    // ========== Connection In Progress Tests ==========

    @Test
    fun `when connection in progress renders then shows cancel button with timeout`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.Connecting,
                timeoutCounter = 30,
                pairingCodeEntry = DataEntry(validPairingCode),
            )

        // When
        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.cancelWithTimeout".i18n("30"))
            .assertIsDisplayed()
    }

    @Test
    fun `when connection in progress and cancel clicked then triggers OnCancelPress action`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.Connecting,
                timeoutCounter = 30,
                pairingCodeEntry = DataEntry(validPairingCode),
            )

        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
            )
        }

        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.cancelWithTimeout".i18n("30"))
            .performClick()

        // Then
        verify { mockOnAction(TrustedNodeSetupUiAction.OnCancelPress) }
    }

    @Test
    fun `when setting up connection renders then shows status message`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.SettingUpConnection,
                pairingCodeEntry = DataEntry(validPairingCode),
            )

        // When
        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.status.settingUpConnection".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when requesting pairing renders then shows status message`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.RequestingPairing,
                pairingCodeEntry = DataEntry(validPairingCode),
            )

        // When
        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.status.requestingPairing".i18n())
            .assertIsDisplayed()
    }

    // ========== Connection Status Tests ==========

    @Test
    fun `when connected state renders then shows connected status`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.Connected,
                pairingCodeEntry = DataEntry(validPairingCode),
                apiUrl = sampleApiUrl,
            )

        // When
        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.status.connected".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when failed state renders then shows failed status`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.Failed(),
                pairingCodeEntry = DataEntry(validPairingCode),
                apiUrl = sampleApiUrl,
            )

        // When
        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.status.failed".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when bootstrapping tor renders then shows tor state and progress`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.BootstrappingTor,
                torState = KmpTorService.TorState.Starting,
                torProgress = 45,
                pairingCodeEntry = DataEntry(validPairingCode),
            )

        // When
        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.torState".i18n(), useUnmergedTree = true)
            .assertIsDisplayed()

        // Also verify the progress percentage is displayed (rendered as separate Text in a Row)
        composeTestRule
            .onNodeWithText(" 45%", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `when starting tor renders then shows tor starting status`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.StartingTor,
                torState = KmpTorService.TorState.Starting,
                pairingCodeEntry = DataEntry(validPairingCode),
            )

        // When
        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.status.startingTor".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when api version incompatible renders then shows version messages`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.IncompatibleHttpApiVersion,
                serverVersion = "1.5.0",
                pairingCodeEntry = DataEntry(validPairingCode),
                apiUrl = sampleApiUrl,
            )

        // When
        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        // Scroll to version messages and verify they're displayed
        composeTestRule
            .onNodeWithText(
                "mobile.trustedNodeSetup.version.expectedAPI".i18n(BuildConfig.BISQ_API_VERSION),
                useUnmergedTree = true,
            ).performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.version.nodeAPI".i18n("1.5.0"), useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    // ========== Button State Tests ==========

    @Test
    fun `when test and save button clicked then triggers OnTestAndSavePress action`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.Idle,
                pairingCodeEntry = DataEntry(validPairingCode),
            )

        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
            )
        }

        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.testAndSave".i18n())
            .performClick()

        // Then
        verify { mockOnAction(TrustedNodeSetupUiAction.OnTestAndSavePress) }
    }

    @Test
    fun `when scan button clicked then triggers OnShowQrCodeView action`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.Idle,
            )

        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
            )
        }

        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.pairingCode.scan".i18n())
            .performClick()

        // Then
        verify { mockOnAction(TrustedNodeSetupUiAction.OnShowQrCodeView) }
    }

    @Test
    fun `when idle with valid pairing code renders then test and save button is enabled`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.Idle,
                pairingCodeEntry = DataEntry(validPairingCode),
            )

        // When
        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.testAndSave".i18n())
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun `when connection in progress renders then scan button is disabled`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.Connecting,
                pairingCodeEntry = DataEntry(validPairingCode),
            )

        // When
        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.pairingCode.scan".i18n())
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun `when connection in progress renders then pairing code field is disabled`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.Connecting,
                pairingCodeEntry = DataEntry(validPairingCode),
            )

        // When
        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
            )
        }

        // Then - Verify field is displayed but disabled during connection
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.pairingCode.textField".i18n())
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun `when connected state renders then scan button is disabled`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.Connected,
                pairingCodeEntry = DataEntry(validPairingCode),
                apiUrl = sampleApiUrl,
            )

        // When
        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.pairingCode.scan".i18n())
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun `when connected state renders then test and save button is disabled in workflow`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.Connected,
                pairingCodeEntry = DataEntry(validPairingCode),
                apiUrl = sampleApiUrl,
            )

        // When
        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
                isWorkflow = true,
            )
        }

        // Then - Test & Save button is shown and disabled when connected in workflow mode
        // Logic: !isConnectionInProgress() && isWorkflow = true, so button shows
        // But isTestButtonDisabled() = true when Connected, so it's disabled
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.testAndSave".i18n())
            .assertExists()
            .assertIsNotEnabled()
    }

    // ========== QR Code Tests ==========

    @Test
    fun `when qr code error shown then displays error dialog`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.Idle,
                showQrCodeError = true,
            )

        // When
        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.barcode.error.title".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("mobile.barcode.error.message".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when qr code error dialog close clicked then triggers OnQrCodeErrorClose action`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.Idle,
                showQrCodeError = true,
            )

        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
            )
        }

        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("action.close".i18n())
            .performClick()

        // Then
        verify { mockOnAction(TrustedNodeSetupUiAction.OnQrCodeErrorClose) }
    }

    // ========== Workflow vs Settings Mode Tests ==========

    @Test
    fun `when settings mode renders then shows title in top bar`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.Connected,
                pairingCodeEntry = DataEntry(validPairingCode),
                apiUrl = sampleApiUrl,
            )

        // When
        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
                isWorkflow = false,
                topBar = {
                    TopBarContent(
                        title = "mobile.trustedNodeSetup.title".i18n(),
                        showUserAvatar = false,
                    )
                },
            )
        }

        // Then - Title appears in TopBar
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.title".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when settings mode renders then shows test connection warning`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.Connected,
                pairingCodeEntry = DataEntry(validPairingCode),
                apiUrl = sampleApiUrl,
            )

        // When
        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
                isWorkflow = false,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.testConnection.message".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when settings mode renders then does not show pairing code text field`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.Idle,
            )

        // When
        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
                isWorkflow = false,
            )
        }

        // Then - Pairing code field is not shown in settings mode
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.pairingCode.textField".i18n())
            .assertDoesNotExist()
    }

    @Test
    fun `when settings mode idle renders then scan button is disabled`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.Idle,
            )

        // When
        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
                isWorkflow = false,
            )
        }

        // Then - Scan button is disabled in settings mode (canScanQrCode returns false)
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.pairingCode.scan".i18n())
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun `when connection in progress with no timeout renders then shows cancel without timeout`() {
        // Given
        val uiState =
            TrustedNodeSetupUiState(
                status = TrustedNodeConnectionStatus.Connecting,
                timeoutCounter = 0,
                pairingCodeEntry = DataEntry(validPairingCode),
            )

        // When
        setTestContent {
            TrustedNodeSetupContent(
                uiState = uiState,
                onAction = mockOnAction,
                snackbarHostState = snackbarHostState,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.trustedNodeSetup.cancel".i18n())
            .assertIsDisplayed()
    }
}
