package org.ncgroup.kscan

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.test.filters.SmallTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class CameraProviderDisposeInstrumentedTest {

    @Test
    fun unbindAll_is_idempotent() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val provider = ProcessCameraProvider.getInstance(context).get()
        // Should not throw when nothing is bound
        provider.unbindAll()
        // And repeated calls should remain safe
        provider.unbindAll()
    }
}

