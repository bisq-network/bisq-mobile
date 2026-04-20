#!/bin/bash

# Test script for the Notification Service Extension (NSE) on iOS Simulator.
# Generates an AES-256-GCM encrypted notification payload and pushes it
# directly to the simulator using xcrun simctl.
#
# Prerequisites:
#   - iOS Simulator running with the app installed
#   - Python 3 with pycryptodome: pip3 install pycryptodome
#
# Usage:
#   ./support/test_nse_simulator.sh
#   ./support/test_nse_simulator.sh "Custom title" "Custom message"
#   SYMMETRIC_KEY="your_base64_key" ./support/test_nse_simulator.sh

BUNDLE_ID="${BUNDLE_ID:-network.bisq.mobile.ios}"
TITLE="${1:-Test notification}"
MESSAGE="${2:-Push notification pipeline is working}"
NOTIFICATION_ID="test-$(date +%s)"

# Use env var or default test key.
# Replace with the actual key from device registration logs:
#   "symmetricKeyBase64": "MqybkhyRZoc3k8ZcXYxyfN6N49vUNaD5lw5zzIIgxbI="
SYMMETRIC_KEY="${SYMMETRIC_KEY:-MqybkhyRZoc3k8ZcXYxyfN6N49vUNaD5lw5zzIIgxbI=}"

echo "=========================================="
echo "Testing NSE on iOS Simulator"
echo "=========================================="
echo ""
echo "Bundle ID:  $BUNDLE_ID"
echo "Title:      $TITLE"
echo "Message:    $MESSAGE"
echo "Key:        ${SYMMETRIC_KEY:0:10}..."
echo ""

# Generate encrypted payload using Swift (no external dependencies needed on macOS)
ENCRYPTED_BASE64=$(swift -e "
import Foundation
import CryptoKit

let keyData = Data(base64Encoded: \"$SYMMETRIC_KEY\")!
precondition(keyData.count == 32, \"Key must be 32 bytes\")

let payload = \"{\\\"id\\\":\\\"$NOTIFICATION_ID\\\",\\\"title\\\":\\\"$TITLE\\\",\\\"message\\\":\\\"$MESSAGE\\\"}\"
let plaintextData = payload.data(using: .utf8)!

let symmetricKey = SymmetricKey(data: keyData)
let sealedBox = try! AES.GCM.seal(plaintextData, using: symmetricKey)

// Output format: nonce (12) + ciphertext + tag (16)
var result = Data()
result.append(contentsOf: sealedBox.nonce)
result.append(sealedBox.ciphertext)
result.append(sealedBox.tag)
print(result.base64EncodedString())
" 2>&1)

if [ $? -ne 0 ]; then
    echo "ERROR: Failed to generate encrypted payload."
    echo "$ENCRYPTED_BASE64"
    exit 1
fi

echo "Encrypted payload: ${ENCRYPTED_BASE64:0:40}..."
echo ""

# Create APNs payload JSON
PAYLOAD_FILE=$(mktemp /tmp/bisq_apns_XXXXXX.json)
cat > "$PAYLOAD_FILE" << EOF
{
    "aps": {
        "alert": {
            "loc-key": "notification"
        },
        "content-available": 1,
        "mutable-content": 1
    },
    "encrypted": "$ENCRYPTED_BASE64"
}
EOF

echo "APNs payload written to: $PAYLOAD_FILE"
echo ""
cat "$PAYLOAD_FILE"
echo ""
echo ""

# Push to simulator
echo "Pushing to simulator..."
xcrun simctl push booted "$BUNDLE_ID" "$PAYLOAD_FILE"
RESULT=$?

rm -f "$PAYLOAD_FILE"

if [ $RESULT -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "SUCCESS - Notification pushed to simulator"
    echo "=========================================="
    echo ""
    echo "Expected behavior:"
    echo "  - NSE intercepts (mutable-content: 1)"
    echo "  - Decrypts AES-GCM payload"
    echo "  - Shows privacy-safe banner: 'Bisq - New notification'"
    echo "  - Full content stored for in-app display"
else
    echo ""
    echo "FAILED - simctl push returned error code $RESULT"
    echo "Make sure the simulator is running with the app installed."
fi
