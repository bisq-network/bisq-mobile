# KScan

A lightweight, cross-platform barcode scanning library for Kotlin Multiplatform. This is a
security-focused minimal clone of [KScan](https://github.com/ismai117/KScan), customized for Bisq
Mobile.

Based on:

- Commit: [754b88e8bf](https://github.com/ismai117/KScan/commit/754b88e8bf3841d608308d5bc3f97b7e208cea65)
- Release tag: [0.9.2](https://github.com/ismai117/KScan/releases/tag/0.9.2)

Differences from upstream:

- Android decodes with [zxing-cpp](https://github.com/zxing-cpp/zxing-cpp) instead of ML Kit, so
  every dependency is open source and no Google Play Services libraries are pulled in.
- ML Kit's auto-zoom suggestions are removed. Zoom is manual only via `ScannerController` or the
  built-in zoom controls.
