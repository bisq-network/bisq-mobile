package org.ncgroup.kscan

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import zxingcpp.BarcodeReader
import java.util.concurrent.Executor

/**
 * Analyzes camera frames for barcodes using zxing-cpp.
 *
 * zxing-cpp decodes on the calling thread, so this is bound to a background executor and hands
 * what it finds to [callbackExecutor]. [filter], [onSuccess] and [onFailed] run there, which keeps
 * them on the main thread and confines the detection state to it.
 *
 * A barcode must be detected twice before it is reported, to filter out misreads.
 */
class BarcodeAnalyzer(
    codeTypes: List<BarcodeFormat>,
    private val callbackExecutor: Executor,
    private val onSuccess: (List<Barcode>) -> Unit,
    private val onFailed: (Exception) -> Unit,
    private val filter: (Barcode) -> Boolean,
) : ImageAnalysis.Analyzer {
    private val reader =
        BarcodeReader(
            BarcodeReader.Options(
                formats = BarcodeFormatMapper.toZxingFormats(codeTypes),
                // Retries close most of the gap to ML Kit on awkward frames; decode time is not the bottleneck
                tryHarder = true,
                tryRotate = true,
                tryInvert = true,
                tryDownscale = true,
                // Match ML Kit's displayValue: no HRI formatting of the decoded text
                textMode = BarcodeReader.TextMode.PLAIN,
            ),
        )

    // Callback-thread state
    private val barcodesDetected = mutableMapOf<String, Int>()

    // Written on the callback thread, read on the analysis thread
    @Volatile
    private var hasSuccessfullyProcessedBarcode = false

    // Callbacks are queued, so the caller can leave before one runs
    @Volatile
    private var closed = false

    override fun analyze(imageProxy: ImageProxy) {
        if (closed || hasSuccessfullyProcessedBarcode) {
            imageProxy.close()
            return
        }

        val results =
            try {
                imageProxy.use { reader.read(it) }
            } catch (e: Exception) {
                callbackExecutor.execute { if (!closed) onFailed(e) }
                return
            }

        val relevantResults = results.filter { BarcodeFormatMapper.isKnownFormat(it.format) }
        if (relevantResults.isNotEmpty()) {
            callbackExecutor.execute { if (!closed) processFoundBarcodes(relevantResults) }
        }
    }

    private fun processFoundBarcodes(results: List<BarcodeReader.Result>) {
        if (hasSuccessfullyProcessedBarcode) return

        for (result in results) {
            val text = result.text ?: continue
            val rawBytes = result.bytes ?: text.encodeToByteArray()

            barcodesDetected[text] = (barcodesDetected[text] ?: 0) + 1
            if ((barcodesDetected[text] ?: 0) >= REQUIRED_DETECTIONS) {
                val barcode =
                    Barcode(
                        data = text,
                        format = BarcodeFormatMapper.toAppFormat(result.format).toString(),
                        rawBytes = rawBytes,
                    )

                if (!filter(barcode)) return

                hasSuccessfullyProcessedBarcode = true
                barcodesDetected.clear()
                onSuccess(listOf(barcode))
                return
            }
        }
    }

    fun close() {
        closed = true
    }

    private companion object {
        const val REQUIRED_DETECTIONS = 2
    }
}
