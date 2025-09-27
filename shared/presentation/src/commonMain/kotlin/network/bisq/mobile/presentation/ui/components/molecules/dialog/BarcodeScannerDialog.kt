package network.bisq.mobile.presentation.ui.components.molecules.dialog

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.helpers.PreviewEnvironment
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.BarcodeFormats
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.ScannerColors
import org.ncgroup.kscan.ScannerView

@Composable
fun BarcodeScannerDialog(
    codeTypes: List<BarcodeFormat> = listOf(
        BarcodeFormats.FORMAT_QR_CODE,
    ),
    marginTop: Dp = BisqUIConstants.ScreenPadding8X,
    onCanceled: () -> Unit = {},
    onFailed: (e: Throwable) -> Unit = {},
    onResult: (barcode: Barcode) -> Unit,
) {
    BisqDialog(
        padding = 14.dp,
        marginTop = marginTop,
        onDismissRequest = onCanceled,
    ) {
        ScannerView(
            modifier = Modifier.fillMaxSize()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp)),
            codeTypes = codeTypes,
            colors = ScannerColors(
                headerContainerColor = BisqTheme.colors.dark_grey10,
                barcodeFrameColor = BisqTheme.colors.primary,
                zoomControllerContainerColor = BisqTheme.colors.dark_grey10,
            )
        ) { result ->
            when (result) {
                is BarcodeResult.OnSuccess -> {
                    onResult(result.barcode)
                }

                is BarcodeResult.OnFailed -> {
                    onFailed(result.exception)
                }

                BarcodeResult.OnCanceled -> {
                    onCanceled()
                }
            }
        }
    }
}

@Composable
@Preview
fun BarcodeScannerDialogPreview() {
    BisqTheme.Preview {
        PreviewEnvironment {
            BarcodeScannerDialog() {}
        }
    }
}

