package org.ncgroup.kscan

import android.os.SystemClock
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
 * A barcode must be detected twice within [DETECTION_WINDOW_MILLIS] before it is reported, to
 * filter out misreads.
 */
class BarcodeAnalyzer(
    private val codeTypes: List<BarcodeFormat>,
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

    // Callback-thread state. Stale entries are dropped, so counts cannot pile up over a long
    // session (for example while a barcode the filter rejects stays in view)
    private val detections = mutableMapOf<String, Detection>()

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
            } catch (t: Throwable) {
                // Decoding crosses JNI, where a native failure surfaces as an Error rather than an
                // Exception. Catching it keeps the frame loop reporting instead of dying silently
                callbackExecutor.execute { if (!closed) onFailed(t as? Exception ?: RuntimeException(t)) }
                return
            }

        val relevantResults =
            results.filter { BarcodeFormatMapper.isRequested(BarcodeFormatMapper.toAppFormat(it.format), codeTypes) }
        if (relevantResults.isNotEmpty()) {
            callbackExecutor.execute { if (!closed) processFoundBarcodes(relevantResults) }
        }
    }

    private fun processFoundBarcodes(results: List<BarcodeReader.Result>) {
        if (hasSuccessfullyProcessedBarcode) return

        val now = SystemClock.elapsedRealtime()
        detections.values.removeAll { now - it.lastSeenAt > DETECTION_WINDOW_MILLIS }

        for (result in results) {
            val text = result.text ?: continue
            val rawBytes = result.bytes ?: text.encodeToByteArray()

            val detection = detections.getOrPut(text) { Detection() }
            detection.count++
            detection.lastSeenAt = now
            if (detection.count < REQUIRED_DETECTIONS) continue

            val barcode =
                Barcode(
                    data = text,
                    format = BarcodeFormatMapper.toAppFormat(result.format).toString(),
                    rawBytes = rawBytes,
                )

            if (!filter(barcode)) continue

            hasSuccessfullyProcessedBarcode = true
            detections.clear()
            onSuccess(listOf(barcode))
            return
        }
    }

    fun close() {
        closed = true
    }

    private class Detection(
        var count: Int = 0,
        var lastSeenAt: Long = 0,
    )

    private companion object {
        const val REQUIRED_DETECTIONS = 2

        // Detections further apart than this are treated as unrelated sightings, not a confirmation
        const val DETECTION_WINDOW_MILLIS = 2_000L
    }
}
