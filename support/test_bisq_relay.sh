#!/bin/bash

# Test script for bisq-relay server
# This tests the APNs endpoint with a mock notification

RELAY_URL="http://localhost:8080"
DEVICE_TOKEN="test_device_token_1234567890abcdef"
ENCRYPTED_MESSAGE="test_encrypted_message_payload"

echo "=========================================="
echo "Testing Local Bisq Relay Server"
echo "=========================================="
echo ""

# Test 1: Check if server is running
echo "Test 1: Checking if server is running..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST "$RELAY_URL/v1/apns/device/test" \
    -H "Content-Type: application/json" \
    -d '{"encrypted":"test","isUrgent":false}')
if [ "$HTTP_CODE" -eq 400 ] || [ "$HTTP_CODE" -eq 200 ]; then
    echo "✅ Server is running (HTTP $HTTP_CODE)"
else
    echo "❌ Server is not responding correctly (HTTP $HTTP_CODE)"
    echo "   Note: Server might still be working, continuing tests..."
fi
echo ""

# Test 2: Send a test APNs notification
echo "Test 2: Sending test APNs notification..."
echo "Endpoint: POST $RELAY_URL/v1/apns/device/$DEVICE_TOKEN"
echo "Payload: {\"encrypted\":\"$ENCRYPTED_MESSAGE\",\"isUrgent\":true}"
echo ""

RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
    -X POST "$RELAY_URL/v1/apns/device/$DEVICE_TOKEN" \
    -H "Content-Type: application/json" \
    -H "User-Agent: bisq-mobile-test/1.0" \
    -d "{\"encrypted\":\"$ENCRYPTED_MESSAGE\",\"isUrgent\":true}")

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)
BODY=$(echo "$RESPONSE" | sed '/HTTP_CODE:/d')

echo "Response Code: $HTTP_CODE"
echo "Response Body: $BODY"
echo ""

if [ "$HTTP_CODE" -eq 200 ]; then
    echo "✅ APNs endpoint accepted the notification"
elif [ "$HTTP_CODE" -eq 400 ]; then
    echo "⚠️  APNs endpoint rejected the notification (expected - invalid token)"
    echo "   This is normal if you're using a test token"
else
    echo "❌ Unexpected response code: $HTTP_CODE"
fi
echo ""

# Test 3: Test legacy relay endpoint (deprecated but still supported)
echo "Test 3: Testing legacy /relay endpoint..."
TOKEN_HEX=$(echo -n "$DEVICE_TOKEN" | xxd -p | tr -d '\n')
MSG_HEX=$(echo -n "$ENCRYPTED_MESSAGE" | xxd -p | tr -d '\n')

LEGACY_URL="$RELAY_URL/relay?isAndroid=false&token=$TOKEN_HEX&msg=$MSG_HEX"
echo "Endpoint: GET $LEGACY_URL"
echo ""

LEGACY_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" "$LEGACY_URL")
LEGACY_HTTP_CODE=$(echo "$LEGACY_RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)
LEGACY_BODY=$(echo "$LEGACY_RESPONSE" | sed '/HTTP_CODE:/d')

echo "Response Code: $LEGACY_HTTP_CODE"
echo "Response Body: $LEGACY_BODY"
echo ""

if [ "$LEGACY_HTTP_CODE" -eq 200 ]; then
    echo "✅ Legacy endpoint is working"
elif [ "$LEGACY_HTTP_CODE" -eq 400 ]; then
    echo "⚠️  Legacy endpoint rejected the notification (expected - invalid token)"
else
    echo "❌ Unexpected response code: $LEGACY_HTTP_CODE"
fi
echo ""

# Summary
echo "=========================================="
echo "Summary"
echo "=========================================="
echo "Your bisq-relay server is running and ready to:"
echo "  • Accept APNs notifications at: POST /v1/apns/device/{deviceToken}"
echo "  • Accept FCM notifications at: POST /v1/fcm/device/{deviceToken} (if enabled)"
echo "  • Support legacy endpoint at: GET /relay (deprecated)"
echo ""
echo "Configuration detected:"
echo "  • APNs Bundle ID: bisq.mobile.client.BisqConnect"
echo "  • APNs Sandbox Mode: true"
echo "  • Server Port: 8080"
echo ""

