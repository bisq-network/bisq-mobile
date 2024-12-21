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
import network.bisq.mobile.presentation.ui.components.atoms.icons.FlashLightIcon
import network.bisq.mobile.presentation.ui.components.atoms.icons.GalleryIcon
import network.bisq.mobile.presentation.ui.components.atoms.icons.StarFillIcon
import network.bisq.mobile.presentation.ui.components.atoms.icons.UserIcon
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.ui.uicases.startup.ITrustedNodeSetupPresenter
import org.koin.compose.koinInject
import qrscanner.CameraLens
import qrscanner.OverlayShape
import qrscanner.QrScanner

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
                onClick = {
                    openImagePicker = true
                },
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
                Icon(
                    Icons.Filled.Close,
                    "close",
                    modifier = Modifier.size(24.dp).clickable {
                        presenter.goBack()
                    },
                    tint = Color.White
                )
                IconButton(
                    modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(
                        if (flashlightOn) {
                            BisqTheme.colors.light1
                        } else {
                            Color.Transparent
                        }
                    ),
                    onClick = {
                        flashlightOn = !flashlightOn
                    }
                ) {
                    FlashLightIcon()
                }
            }
        }
    }
}


private fun DrawScope.drawQrBorderCanvas(
    borderColor: Color = Color.White,
    curve: Dp,
    strokeWidth: Dp,
    capSize: Dp,
    cap: StrokeCap = StrokeCap.Square,
    lineCap: StrokeCap = StrokeCap.Round,
    width: Float = 0f,
    height: Float = 0f,
    top: Float = 0f,
    left: Float = 0f
) {

    val curvePx = curve.toPx()

    val mCapSize = capSize.toPx()

    val sweepAngle = 90 / 2f

    val mCurve = curvePx * 2

    val borderOutline = 5
    // bottom-right Arc
    drawArc(
        color = borderColor,
        style = Stroke(strokeWidth.toPx(), cap = cap),
        startAngle = 0f,
        sweepAngle = sweepAngle,
        useCenter = false,
        size = Size(mCurve, mCurve),
        topLeft = Offset(
            width + left + borderOutline - mCurve, height + top + borderOutline - mCurve
        )
    )
    drawArc(
        color = borderColor,
        style = Stroke(strokeWidth.toPx(), cap = cap),
        startAngle = 90 - sweepAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        size = Size(mCurve, mCurve),
        topLeft = Offset(
            width + left + borderOutline - mCurve, height + top + borderOutline - mCurve
        )
    )

    //bottom-left Arc
    drawArc(
        color = borderColor,
        style = Stroke(strokeWidth.toPx(), cap = cap),
        startAngle = 90f,
        sweepAngle = sweepAngle,
        useCenter = false,
        size = Size(mCurve, mCurve),
        topLeft = Offset(
            left - borderOutline, height + top + borderOutline - mCurve
        )
    )
    drawArc(
        color = borderColor,
        style = Stroke(strokeWidth.toPx(), cap = cap),
        startAngle = 180 - sweepAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        size = Size(mCurve, mCurve),
        topLeft = Offset(
            left - borderOutline, height + top + borderOutline - mCurve
        )
    )

    //Top-Left Arc
    drawArc(
        color = borderColor,
        style = Stroke(strokeWidth.toPx(), cap = cap),
        startAngle = 180f,
        sweepAngle = sweepAngle,
        useCenter = false,
        size = Size(mCurve, mCurve),
        topLeft = Offset(
            left - borderOutline, top - borderOutline
        )
    )

    drawArc(
        color = borderColor,
        style = Stroke(strokeWidth.toPx(), cap = cap),
        startAngle = 270 - sweepAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        size = Size(mCurve, mCurve),
        topLeft = Offset(
            left - borderOutline, top - borderOutline
        )
    )

    // Top-right Arc
    drawArc(
        color = borderColor,
        style = Stroke(strokeWidth.toPx(), cap = cap),
        startAngle = 270f,
        sweepAngle = sweepAngle,
        useCenter = false,
        size = Size(mCurve, mCurve),
        topLeft = Offset(
            width + left + borderOutline - mCurve, top - borderOutline
        )
    )

    drawArc(
        color = borderColor,
        style = Stroke(strokeWidth.toPx(), cap = cap),
        startAngle = 360 - sweepAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        size = Size(mCurve, mCurve),
        topLeft = Offset(
            width + left + borderOutline - mCurve, top - borderOutline
        )
    )

    //bottom-right Line
    drawLine(
        SolidColor(borderColor),
        Offset(width + left + borderOutline, height + top - curvePx),
        Offset(width + left + borderOutline, height + top - mCapSize),
        strokeWidth.toPx(),
        lineCap,
    )

    drawLine(
        SolidColor(borderColor),
        Offset(width + left - mCapSize, height + top + borderOutline),
        Offset(width + left - curvePx, height + top + borderOutline),
        strokeWidth.toPx(),
        lineCap,
    )

    //bottom-left Line
    drawLine(
        SolidColor(borderColor),
        Offset(left + mCapSize, height + top + borderOutline),
        Offset(left + curvePx, height + top + borderOutline),
        strokeWidth.toPx(),
        lineCap,
    )

    drawLine(
        SolidColor(borderColor),
        Offset(left - borderOutline, height + top - curvePx),
        Offset(left - borderOutline, height + top - mCapSize),
        strokeWidth.toPx(),
        lineCap
    )

    // Top-left line
    drawLine(
        SolidColor(borderColor),
        Offset(left - borderOutline, curvePx + top),
        Offset(left - borderOutline, mCapSize + top),
        strokeWidth.toPx(),
        lineCap,
    )

    drawLine(
        SolidColor(borderColor),
        Offset(curvePx + left, top - borderOutline),
        Offset(left + mCapSize, top - borderOutline),
        strokeWidth.toPx(),
        lineCap,
    )

    // Top-right line
    drawLine(
        SolidColor(borderColor),
        Offset(width + left - curvePx, top - borderOutline),
        Offset(width + left - mCapSize, top - borderOutline),
        strokeWidth.toPx(),
        lineCap,
    )

    drawLine(
        SolidColor(borderColor),
        Offset(width + left + borderOutline, curvePx + top),
        Offset(width + left + borderOutline, mCapSize + top),
        strokeWidth.toPx(),
        lineCap
    )

}
