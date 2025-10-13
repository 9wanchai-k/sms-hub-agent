# SMS Hub Agent 📱➡️🌐

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android)](https://www.android.com/)
[![API Level](https://img.shields.io/badge/API-35+-brightgreen)](https://developer.android.com/studio/releases/platforms)
[![Material 3](https://img.shields.io/badge/Material-3%20Expressive-6200EE?logo=material-design)](https://m3.material.io/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF?logo=kotlin)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> **SMS Hub Agent** listens to SMS on Android mobile devices, and forward according to user defined rules to another system via secure webhook—NO phone call or notification features, SMS only.

<p align="center">
  <img src="docs/screenshots/settings_screen.png" width="250" alt="Settings Screen" />
  <img src="docs/screenshots/test_webhook.png" width="250" alt="Test Webhook" />
  <img src="docs/screenshots/material3_theme.png" width="250" alt="Material 3 Theme" />
</p>

---

## ✨ Features

### Core Functionality
- 📨 **Automatic SMS Forwarding** — Instantly forwards incoming SMS to configured webhook
- 🌙 **Background Operation** — Works even when app is closed (powered by WorkManager)
- 🎨 **Material 3 Expressive UI** — Modern design with dynamic colors and smooth animations
- 🔒 **Secure by Default** — Basic Auth + HMAC-SHA256 signatures + encrypted storage
- 🔄 **Reliable Delivery** — Exponential backoff retry on network failures
- ⚡ **Lightweight** — Minimal battery drain with optimized background tasks

### Security Features
- 🔐 Basic Authentication (username:password)
- ✍️ HMAC-SHA256 request signing (X-Signature header)
- 🛡️ AES-256 encrypted credentials storage
- 🔗 Optional TLS certificate pinning
- 🌐 Optional IP allowlist validation

### What's NOT Included
- ❌ NO phone call handling
- ❌ NO notification management
- ❌ NO MMS support (SMS only)

---

## 🚀 Quick Start

### Prerequisites
- Android device with API 35+ (Android 15+)
- Webhook server endpoint (HTTPS recommended)
- Basic Auth credentials (username/password)

### Installation

#### Option 1: Install from Release (Recommended)
1. Download the latest APK from [Releases](https://github.com/yourusername/sms-hub-agent/releases)
2. Enable "Install from unknown sources" in device settings
3. Install APK and open the app
4. Grant SMS and notification permissions when prompted

#### Option 2: Build from Source
```bash
# Clone repository
git clone https://github.com/yourusername/sms-hub-agent.git
cd sms-hub-agent

# Build debug APK
./gradlew assembleDebug

# Install on connected device
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Configuration

1. **Open Settings Screen**
   - Launch app → Tap "Settings" button

2. **Configure Webhook**
   ```
   Webhook URL: https://api.example.com/webhook
   Username: admin
   Password: your_secure_password
   ```

3. **Enable HMAC Signature (Optional but Recommended)**
   - Toggle "Enable HMAC Signature"
   - Enter HMAC secret key (shared with your server)

4. **Test Configuration**
   - Tap "Test" button to send mock SMS
   - Verify server receives request successfully

5. **Save & Activate**
   - Tap "Save Configuration"
   - Send test SMS to your device → Check webhook logs

### Disable Battery Optimization (Important!)
To ensure SMS forwarding works when screen is off:

1. Go to **Settings** → **Battery** → **Battery Optimization**
2. Find "SMS Hub Agent" → Select "Don't optimize"
3. Restart the app

---

## 📡 Webhook Integration

### Expected JSON Payload

Every incoming SMS is immediately forwarded as a JSON POST request:

```json
{
  "type": "sms",
  "sender": "+66812345678",
  "message": "Your OTP code is 123456. Valid for 5 minutes.",
  "timestamp": 1728799999000,
  "device": "Pixel 7",
  "osVersion": 34
}
```

| Field | Type | Description |
|-------|------|-------------|
| `type` | string | Always "sms" |
| `sender` | string | Phone number of SMS sender (E.164 format) |
| `message` | string | Full SMS message body (UTF-8) |
| `timestamp` | integer | Unix timestamp in milliseconds (UTC) |
| `device` | string | Android device model |
| `osVersion` | integer | Android API level |

### HTTP Headers

#### Basic Authentication (Always Sent)
```
Authorization: Basic YWRtaW46cGFzc3dvcmQ=
```
(Base64 encoded `username:password`)

#### HMAC Signature (If Enabled)
```
X-Signature: a3f5e8c9d2b1f7e4a8c6d9e2b5f8c1a4d7e0b3f6c9a2e5d8b1f4a7c0d3e6b9
```
HMAC-SHA256 hex digest of request body using configured secret key.

#### Content Type
```
Content-Type: application/json
```

### Server-Side Verification

#### Node.js Example
```javascript
const crypto = require('crypto');

function verifyHmacSignature(payload, signature, secret) {
    const computed = crypto
        .createHmac('sha256', secret)
        .update(JSON.stringify(payload))
        .digest('hex');

    return crypto.timingSafeEqual(
        Buffer.from(signature),
        Buffer.from(computed)
    );
}

app.post('/webhook', (req, res) => {
    const signature = req.headers['x-signature'];

    if (!verifyHmacSignature(req.body, signature, process.env.HMAC_SECRET)) {
        return res.status(403).json({ error: 'Invalid signature' });
    }

    console.log('SMS received:', req.body);
    res.json({ status: 'success' });
});
```

#### Python Example
```python
import hmac
import hashlib
from flask import Flask, request, jsonify

app = Flask(__name__)

def verify_hmac(payload: str, signature: str, secret: str) -> bool:
    computed = hmac.new(
        secret.encode(),
        payload.encode(),
        hashlib.sha256
    ).hexdigest()
    return hmac.compare_digest(computed, signature)

@app.route('/webhook', methods=['POST'])
def webhook():
    signature = request.headers.get('X-Signature')
    payload = request.get_data(as_text=True)

    if not verify_hmac(payload, signature, os.getenv('HMAC_SECRET')):
        return jsonify({'error': 'Invalid signature'}), 403

    data = request.get_json()
    print(f"SMS from {data['sender']}: {data['message']}")
    return jsonify({'status': 'success'})
```

### Expected Response
Your webhook should return HTTP 200 with JSON body:

```json
{
  "status": "success",
  "message": "SMS processed successfully",
  "id": "msg_1234567890"
}
```

Any non-2xx response triggers automatic retry with exponential backoff (max 3 attempts).

---

## 🧪 Testing

### Send Test SMS via ADB
```bash
# Send test SMS to emulator/rooted device
adb emu sms send +66812345678 "Test message from ADB"

# Monitor logs
adb logcat | grep -E "SmsReceiver|WorkManager|WebhookService"
```

### Manual Testing with cURL
```bash
# Test webhook endpoint manually
curl -X POST https://api.example.com/webhook \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic YWRtaW46cGFzc3dvcmQ=" \
  -d '{
    "type": "sms",
    "sender": "+66812345678",
    "message": "Test OTP 123456",
    "timestamp": 1728799999000,
    "device": "TestDevice",
    "osVersion": 34
  }'
```

### Unit Tests
```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests com.example.sms_hub_agent.WebhookServiceTest
```

### Instrumented Tests (Requires Device/Emulator)
```bash
# Run all instrumented tests
./gradlew connectedAndroidTest

# Run specific test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.sms_hub_agent.SmsFlowTest
```

---

## 🏗️ Architecture

### High-Level Flow

```
📱 SMS Received
    ↓
📡 SmsReceiver (BroadcastReceiver)
    ↓
📋 Parse message data
    ↓
⚙️ Enqueue SmsForwardWorker (WorkManager)
    ↓
🔐 Load config from EncryptedSharedPreferences
    ↓
🔒 Build JSON + Add Auth headers + Compute HMAC
    ↓
🌐 HTTP POST to Webhook URL (OkHttp)
    ↓
✅ Success: Log completion
❌ Failure: Retry with backoff (max 3x)
```

### Key Components

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **SmsReceiver** | BroadcastReceiver | Listens to `SMS_RECEIVED` system broadcast |
| **SmsForwardWorker** | WorkManager | Background task that forwards SMS (survives app close) |
| **WebhookService** | OkHttp | HTTP client for POST requests with retry logic |
| **SettingsActivity** | Jetpack Compose + Material 3 | Configuration UI with dynamic theme |
| **SettingsRepository** | EncryptedSharedPreferences | Secure credential storage (AES-256) |

---

## 🎨 Material 3 Design

### Color Scheme
Uses **Material 3 Expressive** theme with dynamic color extraction from system wallpaper (Android 12+).

#### Light Mode
- **Primary**: Deep Teal (`#006C51`)
- **Primary Container**: Light Teal (`#7DF8D3`)
- **Secondary**: Muted Green (`#4A6360`)
- **Surface**: Off-White (`#FBFDFFA`)

#### Dark Mode
- **Primary**: Bright Teal (`#5EDCB7`)
- **Primary Container**: Dark Teal (`#00513D`)
- **Secondary**: Sage Green (`#B1CCC8`)
- **Surface**: Near Black (`#191C1B`)

### Typography
- **Display**: 57sp, Regular (Headlines)
- **Headline**: 28sp, SemiBold (Section titles)
- **Body**: 16sp, Regular (Content text)
- **Label**: 14sp, Medium (Buttons, labels)

### Motion Transitions
- **Screen Entry**: 300ms slide-in + fade
- **Button Success**: 200ms checkmark morph + color pulse
- **Field Focus**: 150ms elevation raise (2dp → 4dp)
- **Error State**: 300ms shake animation + red outline

---

## 🔒 Security & Privacy

### Data Storage
- All credentials (webhook URL, username, password, HMAC secret) are encrypted using **AES-256-GCM** via `EncryptedSharedPreferences`
- Master key is stored in Android Keystore (hardware-backed on supported devices)
- SMS messages are **never stored locally**—forwarded immediately and discarded

### Network Security
- **TLS 1.2+** enforced for HTTPS connections
- Optional certificate pinning to prevent MITM attacks
- Connection timeout: 30 seconds (configurable)

### Permissions
App requires these Android permissions:

| Permission | Type | Purpose |
|------------|------|---------|
| `RECEIVE_SMS` | Dangerous | Listen to incoming SMS broadcasts |
| `INTERNET` | Normal | Send HTTP requests to webhook |
| `POST_NOTIFICATIONS` | Dangerous (Android 13+) | Show foreground service notifications |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Normal | Ensure background execution |

**Privacy Note:** This app does NOT:
- Upload data to third-party analytics
- Store SMS history in database
- Access contacts or call logs
- Run any telemetry/tracking

---

## 📝 Configuration Reference

### SharedPreferences Keys

| Key | Type | Description |
|-----|------|-------------|
| `webhook_url` | String | HTTPS endpoint URL |
| `auth_username` | String | Basic Auth username |
| `auth_password` | String | Basic Auth password (encrypted) |
| `hmac_enabled` | Boolean | Enable/disable HMAC signatures |
| `hmac_secret` | String | HMAC secret key (encrypted) |
| `retry_count` | Int | Max retry attempts (default: 3) |
| `timeout_seconds` | Int | HTTP timeout (default: 30) |

### WorkManager Constraints

```kotlin
val constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED) // Any internet connection
    .setRequiresBatteryNotLow(false) // Allow on low battery
    .build()
```

---

## 🛠️ Development

### Project Structure
```
sms-hub-agent/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/sms_hub_agent/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── SettingsActivity.kt
│   │   │   │   ├── receiver/
│   │   │   │   │   └── SmsReceiver.kt
│   │   │   │   ├── worker/
│   │   │   │   │   └── SmsForwardWorker.kt
│   │   │   │   ├── service/
│   │   │   │   │   └── WebhookService.kt
│   │   │   │   ├── repository/
│   │   │   │   │   └── SettingsRepository.kt
│   │   │   │   ├── model/
│   │   │   │   │   └── SmsPayload.kt
│   │   │   │   └── ui/theme/
│   │   │   │       ├── Color.kt
│   │   │   │       ├── Theme.kt
│   │   │   │       └── Type.kt
│   │   │   └── AndroidManifest.xml
│   │   ├── test/
│   │   │   └── java/com/example/sms_hub_agent/
│   │   │       ├── WebhookServiceTest.kt
│   │   │       └── SmsReceiverTest.kt
│   │   └── androidTest/
│   │       └── java/com/example/sms_hub_agent/
│   │           ├── SmsFlowTest.kt
│   │           └── SettingsActivityTest.kt
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

### Building

```bash
# Debug build (with signing)
./gradlew assembleDebug

# Release build (requires keystore)
export KEYSTORE_PASSWORD=your_password
export KEY_PASSWORD=your_key_password
./gradlew assembleRelease

# Clean build
./gradlew clean assembleDebug

# Lint checks
./gradlew lint

# All checks (lint + test)
./gradlew check
```

### Code Style
- **Kotlin Official Style** (enforced via `ktlint`)
- **Java 11** source/target compatibility
- **Material 3 Components** only (no legacy Material 2)

---

## 🐛 Troubleshooting

### SMS Not Forwarding

**Problem:** SMS received but webhook not called.

**Solutions:**
1. Check logs: `adb logcat | grep SmsReceiver`
2. Verify battery optimization disabled (Settings → Battery)
3. Test webhook URL manually with cURL
4. Confirm RECEIVE_SMS permission granted
5. Check WorkManager status: `adb shell dumpsys jobscheduler | grep SmsForward`

### 401 Unauthorized Error

**Problem:** Webhook returns 401 status code.

**Solutions:**
1. Verify username/password in Settings are correct
2. Test Basic Auth with: `curl -u username:password https://webhook-url`
3. Check server logs for authentication failures
4. Ensure no special characters in password (or URL-encode them)

### HMAC Signature Mismatch

**Problem:** Server rejects request with "Invalid signature" (403).

**Solutions:**
1. Confirm HMAC secret matches on both app and server
2. Verify payload serialization (no whitespace/formatting differences)
3. Test HMAC computation manually:
   ```bash
   echo -n '{"type":"sms",...}' | openssl dgst -sha256 -hmac "secret"
   ```
4. Check character encoding (UTF-8 on both sides)

### High Battery Drain

**Problem:** App uses >5% battery per day.

**Solutions:**
1. Reduce retry count from 3 to 2 (Settings screen)
2. Increase backoff delay from 10s to 30s
3. Check for stuck WorkManager tasks: `adb shell dumpsys jobscheduler`
4. Profile with Android Studio Battery Profiler

---

## 📚 API Documentation

### JSON Schema

Full JSON Schema is available at: [TECHNICAL_SPECIFICATION.md](TECHNICAL_SPECIFICATION.md#9-json-schema-for-webhook-payload)

**Quick Reference:**
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["type", "sender", "message", "timestamp", "device", "osVersion"],
  "properties": {
    "type": { "const": "sms" },
    "sender": { "type": "string", "pattern": "^\\+?[1-9]\\d{1,14}$" },
    "message": { "type": "string", "minLength": 1, "maxLength": 1600 },
    "timestamp": { "type": "integer", "minimum": 0 },
    "device": { "type": "string" },
    "osVersion": { "type": "integer", "minimum": 21 }
  }
}
```

---

## 🗺️ Roadmap

### Version 1.1 (Next Release)
- [ ] Rule-based SMS filtering (regex patterns)
- [ ] Multiple webhook endpoints (routing rules)
- [ ] Offline queue with Room database
- [ ] Retry history dashboard

### Version 2.0 (Future)
- [ ] MMS support (images, videos)
- [ ] Custom HTTP headers configuration
- [ ] Webhook response actions (trigger automation)
- [ ] Export/import configuration (JSON backup)
- [ ] Analytics dashboard (success rate, latency graphs)

---

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. **Fork** the repository
2. Create a **feature branch**: `git checkout -b feature/amazing-feature`
3. **Commit** changes: `git commit -m 'Add amazing feature'`
4. **Push** to branch: `git push origin feature/amazing-feature`
5. Open a **Pull Request**

### Development Setup
```bash
# Install pre-commit hooks
cp scripts/pre-commit .git/hooks/
chmod +x .git/hooks/pre-commit

# Run linter
./gradlew ktlintCheck

# Run all tests before committing
./gradlew test connectedAndroidTest
```

---

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2025 SMS Hub Agent Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/yourusername/sms-hub-agent/issues)
- **Documentation**: [Technical Specification](TECHNICAL_SPECIFICATION.md)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/sms-hub-agent/discussions)

---

## 🙏 Acknowledgments

- **Material Design Team** — For Material 3 Expressive design system
- **AndroidX Team** — For WorkManager and Compose libraries
- **OkHttp Contributors** — For robust HTTP client
- **Android Community** — For security best practices

---

<p align="center">
  <strong>Made with ❤️ by the SMS Hub Agent Team</strong>
</p>

<p align="center">
  <a href="#-features">Features</a> •
  <a href="#-quick-start">Quick Start</a> •
  <a href="#-webhook-integration">Webhook</a> •
  <a href="#-architecture">Architecture</a> •
  <a href="#-troubleshooting">Troubleshooting</a>
</p>
