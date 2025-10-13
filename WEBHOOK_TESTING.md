# Webhook Testing Guide

## Webhook Configuration

Your n8n webhook endpoint is working correctly:

**Endpoint:** `https://zer0-blaze.app.n8n.cloud/webhook/sms`
**Authentication:** Basic Auth (username: `n8n`, password: `n8n`)

## Test Results

The webhook was tested successfully with curl:

```bash
curl -u n8n:n8n -i -X POST "https://zer0-blaze.app.n8n.cloud/webhook/sms" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "sms",
    "sender": "+66812345678",
    "message": "OTP 123456",
    "timestamp": 1728799999000,
    "device": "Pixel 7",
    "osVersion": 34
  }'
```

**Response:** HTTP 200 OK
```json
{"message":"Workflow was started"}
```

## App Configuration

To configure the app to use this webhook:

1. Open the app and tap "Configure Webhook"
2. Enter the following details:
   - **Webhook URL:** `https://zer0-blaze.app.n8n.cloud/webhook/sms`
   - **Username:** `n8n`
   - **Password:** `n8n`
3. (Optional) Enable HMAC signature if needed for additional security
4. Tap "Test" to verify the connection
5. Tap "Save" to store the configuration

## Network Security

The app now includes:
- Network security configuration for HTTPS connections
- Support for system and user certificates
- Proper SSL/TLS handling for n8n.cloud domains

## Troubleshooting

If the webhook doesn't work from the app:

1. **Check permissions:**
   - SMS permission must be granted
   - Internet permission is already declared
   - Disable battery optimization for the app

2. **Check logs:**
   - Use `adb logcat | grep -E "SmsReceiver|SmsForwardWorker|WebhookService"` to see detailed logs

3. **Network issues:**
   - Ensure the device has internet connectivity
   - Check if there's a firewall or VPN blocking the connection
   - Verify the webhook URL is correct and accessible

4. **Test the webhook:**
   - Use the "Test" button in Settings to verify connectivity
   - Check the History screen to see success/failure status of forwarded messages

## History Feature

The app now tracks all SMS forwarding attempts:

- View history by tapping "View History" on the main screen
- See success/failed statistics
- Expand each entry to see full details
- Delete all history if needed

Each history entry shows:
- Sender phone number
- Message content
- Timestamp when forwarded
- Device information
- Webhook URL used
- Status (success/failed)
- Error message (if failed)
