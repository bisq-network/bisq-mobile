package network.bisq.mobile.presentation.ui.components.organisms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.SvgImage
import network.bisq.mobile.presentation.ui.components.atoms.SvgImageNames
import network.bisq.mobile.presentation.ui.components.atoms.icons.*
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.ui.uicases.startup.ITrustedNodeSetupPresenter
import org.koin.compose.koinInject
import qrscanner.CameraLens
import qrscanner.OverlayShape
import qrscanner.QrScanner

// FinalTODO: De-couple from TrustedNodeSetup Presenter
@Composable
fun QRCodeScannerScreen() {
    val presenter: ITrustedNodeSetupPresenter = koinInject()
    var qrCodeURL by remember { mutableStateOf("") }
    var flashlightOn by remember { mutableStateOf(false) }
    var openImagePicker by remember { mutableStateOf(value = false) }
    val coroutineScope = rememberCoroutineScope()

    RememberPresenterLifecycle(presenter)

    LaunchedEffect(Unit) {
        delay(5000L)
        flashlightOn = !flashlightOn
    }

    LaunchedEffect(qrCodeURL) {
        if (qrCodeURL.isNotEmpty()) {
            presenter.updateBisqApiUrl(qrCodeURL)
            presenter.goBackToSetupScreen()
        }
    }

    Box(
        modifier = Modifier
            .background(BisqTheme.colors.dark5)
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            QrScanner(
                modifier = Modifier,
                flashlightOn = flashlightOn,
                cameraLens = CameraLens.Back,
                openImagePicker = openImagePicker,
                onCompletion = {
                    if (!openImagePicker){
                        qrCodeURL = it
                        flashlightOn = false
                    }
                },
                imagePickerHandler = {
                    openImagePicker = it
                },
                onFailure = {
                    coroutineScope.launch {
                        if (it.isEmpty()) {
                            print("it is empty")
                        } else {
                            print("It throw $it")
                        }
                    }
                },
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = BisqUIConstants.ScreenPadding2X)
        ) {
            BisqButton(
                text = "Upload from gallery",
                onClick = { openImagePicker = true },
                leftIcon = { GalleryIcon() }
            )
        }

        Column {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { presenter.goBack() }) {
                    CloseIcon()
                }
                IconButton(
                    modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(
                        if (flashlightOn) {
                            BisqTheme.colors.light1
                        } else {
                            Color.Transparent
                        }
                    ),
                    onClick = { flashlightOn = !flashlightOn }
                ) {
                    FlashLightIcon()
                }
            }
        }
    }
}