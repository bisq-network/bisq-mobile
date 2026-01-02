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

# Source framework (extracted by Xcode from xcframework)
# Try multiple possible locations where Xcode might extract the framework
POSSIBLE_LOCATIONS=(
    "${BUILD_DIR}/${CONFIGURATION}${EFFECTIVE_PLATFORM_NAME}/LibTor.framework"
    "${PROJECT_DIR}/build/${CONFIGURATION}${EFFECTIVE_PLATFORM_NAME}/LibTor.framework"
    "${SRCROOT}/build/${CONFIGURATION}${EFFECTIVE_PLATFORM_NAME}/LibTor.framework"
)

SOURCE_FRAMEWORK=""
for location in "${POSSIBLE_LOCATIONS[@]}"; do
    if [ -d "$location" ]; then
        SOURCE_FRAMEWORK="$location"
        break
    fi
done

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

