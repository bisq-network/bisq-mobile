package network.bisq.mobile.presentation.common.ui.components.atoms

import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals

class AutoResizeStepTest {
    @Test
    fun `text that fits is drawn at its current size`() {
        assertEquals(AutoResizeStep.Draw, autoResizeStep(hasVisualOverflow = false, fontSize = 14.sp, minimumFontSize = 10.sp))
    }

    @Test
    fun `overflowing text shrinks by ten percent`() {
        assertEquals(
            AutoResizeStep.Shrink(14.sp * 0.9f),
            autoResizeStep(hasVisualOverflow = true, fontSize = 14.sp, minimumFontSize = 10.sp),
        )
    }

    @Test
    fun `overflowing text never shrinks below the minimum`() {
        assertEquals(
            AutoResizeStep.Shrink(10.sp),
            autoResizeStep(hasVisualOverflow = true, fontSize = 10.5.sp, minimumFontSize = 10.sp),
        )
    }

    @Test
    fun `text still overflowing at the minimum is drawn clipped by default`() {
        assertEquals(AutoResizeStep.Draw, autoResizeStep(hasVisualOverflow = true, fontSize = 10.sp, minimumFontSize = 10.sp))
    }

    @Test
    fun `text still overflowing at the minimum is hidden when asked to`() {
        assertEquals(
            AutoResizeStep.Hide,
            autoResizeStep(
                hasVisualOverflow = true,
                fontSize = 10.sp,
                minimumFontSize = 10.sp,
                hideWhenOverflowingAtMinimum = true,
            ),
        )
    }

    @Test
    fun `text that fits at the minimum is drawn even when hiding is allowed`() {
        // A wider layout (for example after rotation) brings a hidden label back.
        assertEquals(
            AutoResizeStep.Draw,
            autoResizeStep(
                hasVisualOverflow = false,
                fontSize = 10.sp,
                minimumFontSize = 10.sp,
                hideWhenOverflowingAtMinimum = true,
            ),
        )
    }
}
