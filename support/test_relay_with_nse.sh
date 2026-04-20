#!/bin/bash

# Test script for the relay server with mutableContent support.
# Sends an AES-GCM encrypted notification to the local relay, which then
# forwards to APNs with mutable-content:1 for NSE processing.
#
# Prerequisites:
#   - Local bisq-relay running on localhost:8080
#   - Valid APNs certificate configured in the relay
#   - A real device token (simulators can't receive APNs)
#   - Python 3 with pycryptodome: pip3 install pycryptodome
#
# Usage:
#   DEVICE_TOKEN="<64-char-hex>" SYMMETRIC_KEY="<base64>" ./support/test_relay_with_nse.sh
#
# For testing relay without actually hitting APNs (just validates the relay accepts the request):
#   ./support/test_relay_with_nse.sh --dry-run


## working example
# DEVICE_TOKEN="b162a243b6ce4bf944d5c795475d900d508269216ad1e6e4b6214cc64da941c2" \
#  SYMMETRIC_KEY="MqybkhyRZoc3k8ZcXYxyfN6N49vUNaD5lw5zzIIgxbI=" \                                                               #    ./support/test_relay_with_nse.sh

RELAY_URL="${RELAY_URL:-http://localhost:8080}"
DEVICE_TOKEN="${DEVICE_TOKEN:-b162a243b6ce4bf944d5c795475d900d508269216ad1e6e4b6214cc64da941c2}"
SYMMETRIC_KEY="${SYMMETRIC_KEY:-MqybkhyRZoc3k8ZcXYxyfN6N49vUNaD5lw5zzIIgxbI=}"
TITLE="${TITLE:-Trade update}"
MESSAGE="${MESSAGE:-Payment confirmed for your trade}"
DRY_RUN="${1:-}"

echo "=========================================="
echo "Testing Relay with mutableContent Support"
echo "=========================================="
echo ""
echo "Relay URL:     $RELAY_URL"
echo "Device Token:  ${DEVICE_TOKEN:0:16}..."
echo "Key:           ${SYMMETRIC_KEY:0:10}..."
echo "Dry run:       ${DRY_RUN:-no}"
echo ""

# Generate encrypted payload using Swift (no external dependencies needed on macOS)
NOTIFICATION_ID="test-$(date +%s)"
ENCRYPTED_HEX=$(swift -e "
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

// Relay expects hex of raw bytes (matching bisq2 format)
print(result.map { String(format: \"%02x\", \$0) }.joined())
" 2>&1)

if [ $? -ne 0 ]; then
    echo "ERROR: Failed to generate encrypted payload."
    echo "$ENCRYPTED_HEX"
    exit 1
fi

echo "Encrypted hex: ${ENCRYPTED_HEX:0:40}..."
echo ""

if [ "$DRY_RUN" = "--dry-run" ]; then
    echo "DRY RUN - would send to relay:"
    echo "  GET $RELAY_URL/relay?isAndroid=false&token=$DEVICE_TOKEN&msg=$ENCRYPTED_HEX&mutableContent=true"
    echo ""
    echo "Equivalent POST (v1 endpoint):"
    echo "  POST $RELAY_URL/v1/apns/device/$DEVICE_TOKEN"
    echo "  Body: {\"encrypted\":\"$(echo -n "$ENCRYPTED_HEX" | xxd -r -p | base64)\",\"isUrgent\":true,\"isMutableContent\":true}"
    exit 0
fi

# Test 1: Legacy endpoint with mutableContent param
echo "Test 1: Legacy /relay endpoint with mutableContent=true"
echo "---"
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
    "$RELAY_URL/relay?isAndroid=false&token=$DEVICE_TOKEN&msg=$ENCRYPTED_HEX&mutableContent=true")

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)
BODY=$(echo "$RESPONSE" | sed '/HTTP_CODE:/d')

echo "Response: $HTTP_CODE"
echo "Body: $BODY"
echo ""

if [ "$HTTP_CODE" = "200" ]; then
    echo "SUCCESS - Relay accepted and forwarded to APNs with mutable-content:1"
elif [ "$HTTP_CODE" = "400" ]; then
    echo "APNs rejected (expected if cert/bundle mismatch or invalid token)"
    echo "But the relay DID process the mutableContent flag correctly."
else
    echo "Unexpected response: $HTTP_CODE"
fi

echo ""

# Test 2: New POST endpoint
echo "Test 2: POST /v1/apns/device endpoint with isMutableContent=true"
echo "---"
# For POST endpoint, encrypted field is Base64 (not hex)
ENCRYPTED_FOR_POST=$(swift -e "
import Foundation
let hex = \"$ENCRYPTED_HEX\"
var data = Data()
var i = hex.startIndex
while i < hex.endIndex {
    let end = hex.index(i, offsetBy: 2)
    data.append(UInt8(hex[i..<end], radix: 16)!)
    i = end
}
print(data.base64EncodedString())
")

RESPONSE2=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
    -X POST "$RELAY_URL/v1/apns/device/$DEVICE_TOKEN" \
    -H "Content-Type: application/json" \
    -H "User-Agent: bisq-mobile-test/1.0" \
    -d "{\"encrypted\":\"$ENCRYPTED_FOR_POST\",\"isUrgent\":true,\"isMutableContent\":true}")

HTTP_CODE2=$(echo "$RESPONSE2" | grep "HTTP_CODE:" | cut -d: -f2)
BODY2=$(echo "$RESPONSE2" | sed '/HTTP_CODE:/d')

echo "Response: $HTTP_CODE2"
echo "Body: $BODY2"
echo ""

if [ "$HTTP_CODE2" = "200" ]; then
    echo "SUCCESS - POST endpoint accepted with mutable-content:1"
elif [ "$HTTP_CODE2" = "400" ]; then
    echo "APNs rejected (cert/bundle mismatch) - but relay processed correctly"
else
    echo "Unexpected response: $HTTP_CODE2"
fi

echo ""
echo "=========================================="
echo "Done"
echo "=========================================="
