#!/bin/bash

# Script to manually embed LibTor.framework into the app bundle
# This is a workaround for Xcode not automatically embedding the framework

set -e

# Check if building for simulator
if [[ "${EFFECTIVE_PLATFORM_NAME}" == *"simulator"* ]]; then
    echo "️(!) Skipping LibTor.framework embedding for simulator build"
    echo "   Note: Tor functionality will not be available in the simulator"
    echo "   LibTor.xcframework only contains device (arm64) binaries"
    exit 0
fi

echo "🔧 Embedding LibTor.framework..."

# Path to the xcframework
XCFRAMEWORK_PATH="${PROJECT_DIR}/../build/kmp-tor-resource/LibTor.xcframework"

# The xcframework contains ios/LibTor.framework (for device)
FRAMEWORK_SLICE="${XCFRAMEWORK_PATH}/ios/LibTor.framework"

# Check if the framework slice exists
if [ ! -d "${FRAMEWORK_SLICE}" ]; then
    echo "❌ ERROR: LibTor.framework not found at: ${FRAMEWORK_SLICE}"
    echo "   Make sure kmp-tor resources are generated"
    echo "   Run: ./gradlew :shared:domain:copyKmpTorXCFramework"
    exit 1
fi

SOURCE_FRAMEWORK="${FRAMEWORK_SLICE}"
echo "📍 Using framework from: ${SOURCE_FRAMEWORK}"

# Destination (app bundle Frameworks folder)
DEST_FRAMEWORKS="${BUILT_PRODUCTS_DIR}/${FRAMEWORKS_FOLDER_PATH}"

# Create Frameworks folder if it doesn't exist
mkdir -p "${DEST_FRAMEWORKS}"

# Copy the framework
if [ -n "${SOURCE_FRAMEWORK}" ] && [ -d "${SOURCE_FRAMEWORK}" ]; then
    echo "📦 Copying framework to app bundle..."
    cp -R "${SOURCE_FRAMEWORK}" "${DEST_FRAMEWORKS}/"

    # Code sign the framework
    if [ "${CODE_SIGNING_REQUIRED}" = "YES" ]; then
        echo "✍️  Code signing LibTor.framework..."
        codesign --force --sign "${EXPANDED_CODE_SIGN_IDENTITY}" --preserve-metadata=identifier,entitlements --timestamp=none "${DEST_FRAMEWORKS}/LibTor.framework"
    fi

    echo "✅ LibTor.framework embedded successfully"
else
    echo "❌ ERROR: LibTor.framework not found in any of these locations:"
    for location in "${POSSIBLE_LOCATIONS[@]}"; do
        echo "   - $location"
    done
    echo "   Make sure the xcframework is being processed correctly"
    exit 1
fi

# Generate a UUID-matched dSYM bundle for LibTor.
#
# kmp-tor-resource ships a stripped LibTor binary with no DWARF debug info, so
# Xcode does not produce a dSYM for it automatically. Without a UUID-matched
# dSYM, App Store Connect / Xcode Organizer flags crash reports with a
# "Missing dSYM" warning and refuses to symbolicate the surrounding non-LibTor
# frames (Kotlin/Native, Swift) cleanly. See issue #1404.
#
# Running dsymutil on the stripped binary produces a dSYM bundle with the
# correct UUID and the dynamic symbol table. This is enough to clear the
# warning and unblock symbolication of the rest of the stack. Internal LibTor
# functions remain unsymbolicated (would require building LibTor from source
# upstream — tracked separately).
if [ "${DEBUG_INFORMATION_FORMAT}" = "dwarf-with-dsym" ] && [ -n "${DWARF_DSYM_FOLDER_PATH}" ]; then
    echo "🪪 Generating UUID-matched dSYM for LibTor.framework..."

    EMBEDDED_BINARY="${DEST_FRAMEWORKS}/LibTor.framework/LibTor"
    DSYM_OUTPUT="${DWARF_DSYM_FOLDER_PATH}/LibTor.framework.dSYM"

    mkdir -p "${DWARF_DSYM_FOLDER_PATH}"
    rm -rf "${DSYM_OUTPUT}"

    # dsymutil prints a benign "no debug symbols in executable" warning for
    # stripped binaries but still emits a valid dSYM bundle. Don't fail on it.
    if dsymutil "${EMBEDDED_BINARY}" -o "${DSYM_OUTPUT}"; then
        BINARY_UUID="$(dwarfdump --uuid "${EMBEDDED_BINARY}" 2>/dev/null | awk '{print $2; exit}')"
        DSYM_UUID="$(dwarfdump --uuid "${DSYM_OUTPUT}" 2>/dev/null | awk '{print $2; exit}')"
        if [ -n "${BINARY_UUID}" ] && [ "${BINARY_UUID}" = "${DSYM_UUID}" ]; then
            echo "✅ LibTor dSYM generated at: ${DSYM_OUTPUT} (UUID ${DSYM_UUID})"
        else
            echo "⚠️  LibTor dSYM UUID mismatch (binary=${BINARY_UUID}, dsym=${DSYM_UUID})"
        fi
    else
        echo "⚠️  dsymutil failed for LibTor — continuing without dSYM (crash reports will show 'Missing dSYM' for LibTor)"
    fi
else
    echo "ℹ️  Skipping LibTor dSYM generation (DEBUG_INFORMATION_FORMAT=${DEBUG_INFORMATION_FORMAT:-unset})"
fi

