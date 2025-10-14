package network.bisq.mobile.presentation.ui.components.molecules.bottom_sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import network.bisq.mobile.presentation.isAffectedBottomSheetDevice
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BisqBottomSheet(
    onDismissRequest: () -> Unit,
    containerColor: Color = BisqTheme.colors.dark_grey40,
    content: @Composable () -> Unit
) {
    if (isAffectedBottomSheetDevice()) {
        // Non-dialog fallback to avoid window flag updates on affected devices
        NonDialogBottomSheet(
            onDismissRequest = onDismissRequest,
            containerColor = containerColor,
            content = content
        )
    } else {
        ModalBottomSheet (
            onDismissRequest = onDismissRequest,
            shape = RoundedCornerShape(16.dp),
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
            ),
            containerColor = containerColor,
            dragHandle = { DragHandle() }
        ) {
            content()
        }
    }
}

@Composable
private fun DragHandle() {
    Box(
        modifier = Modifier.padding(top = 20.dp)
            .clip(shape = RoundedCornerShape(4.dp))
    ) {
        Box(
            modifier = Modifier.height(4.dp).width(60.dp)
                .background(Color(0xFF6F6F6F))
        )
    }
}

@Composable
private fun NonDialogBottomSheet(
    onDismissRequest: () -> Unit,
    containerColor: Color,
    content: @Composable () -> Unit
) {
    Popup(
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            clippingEnabled = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Scrim
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(onClick = onDismissRequest)
            )
            // Sheet content
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 12.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = containerColor,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DragHandle()
                        Spacer(modifier = Modifier.height(8.dp))
                        content()
                    }
                }
            }
        }
    }
}
