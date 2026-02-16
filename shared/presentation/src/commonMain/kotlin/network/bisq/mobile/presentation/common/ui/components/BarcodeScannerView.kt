package network.bisq.mobile.presentation.common.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.rememberCameraPermissionLauncher
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.ScannerColors
import org.ncgroup.kscan.ScannerUiOptions
import org.ncgroup.kscan.ScannerView

@Composable
fun BarcodeScannerView(
    codeTypes: List<BarcodeFormat> =
        listOf(
            BarcodeFormat.FORMAT_QR_CODE,
        ),
    onCancel: () -> Unit = {},
    onFail: (e: Throwable) -> Unit = {},
    onResult: (barcode: Barcode) -> Unit,
) {
    var showScanner by remember { mutableStateOf(false) }

    val permissionRequestLauncher =
        rememberCameraPermissionLauncher { isCameraPermissionGranted ->
            if (isCameraPermissionGranted) {
                showScanner = true
            } else {
                onCancel()
            }
        }

    LaunchedEffect(Unit) {
        permissionRequestLauncher.launch()
    }

    if (showScanner) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                modifier =
                    Modifier.fillMaxSize().clickable(
                        onClick = onCancel,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ),
            ) {}
            ScannerView(
                codeTypes = codeTypes,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(28.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp)),
                colors =
                    ScannerColors(
                        headerContainerColor = BisqTheme.colors.dark_grey10,
                        barcodeFrameColor = BisqTheme.colors.primary,
                        zoomControllerContainerColor = BisqTheme.colors.dark_grey10,
                    ),
                scannerUiOptions =
                    ScannerUiOptions(
                        headerTitle = "mobile.barcode.header".i18n(),
                        showZoom = false,
                    ),
            ) { result ->
                when (result) {
                    is BarcodeResult.OnSuccess -> {
                        onResult(result.barcode)
                    }

                    is BarcodeResult.OnFailed -> {
                        onFail(result.exception)
                    }

                    BarcodeResult.OnCanceled -> {
                        onCancel()
                    }
                }
            }
        }
    }
}

@Composable
@Preview
private fun BarcodeScannerViewPreview() {
    BisqTheme.Preview {
        BarcodeScannerView {}
    }
}
