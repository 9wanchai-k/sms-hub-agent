# SMS Hub Agent - Technical Specification

## 1. Executive Summary

### English
**SMS Hub Agent** is an Android application that listens to SMS on Android mobile devices, and forward according to user defined rules to another system via webhook. The application operates autonomously in the background, immediately forwarding incoming SMS messages to a configured webhook endpoint using secure HTTP POST requests. Built with Material 3 Expressive design principles, the app provides an intuitive configuration interface while maintaining robust security through Basic Authentication and HMAC-SHA256 signatures. The system ensures reliable message delivery through WorkManager retry mechanisms and operates independently of phone calls or notifications—SMS only.

### Thai (ภาษาไทย)
**SMS Hub Agent** เป็นแอปพลิเคชัน Android ที่ฟังข้อความ SMS บนอุปกรณ์มือถือ Android และส่งต่อตามกฎที่ผู้ใช้กำหนดไปยังระบบอื่น ผ่านทาง webhook แอปพลิเคชันทำงานอัตโนมัติในเบื้องหลัง โดยส่งต่อข้อความ SMS ที่เข้ามาทันทีไปยัง webhook endpoint ที่กำหนดค่าไว้ผ่าน HTTP POST requests ที่ปลอดภัย สร้างด้วยหลักการออกแบบ Material 3 Expressive แอปมอบอินเทอร์เฟซการตั้งค่าที่ใช้งานง่าย พร้อมรักษาความปลอดภัยที่แข็งแกร่งผ่าน Basic Authentication และลายเซ็น HMAC-SHA256 ระบบรับประกันการส่งข้อความที่เชื่อถือได้ผ่านกลไก retry ของ WorkManager และทำงานโดยไม่ขึ้นกับการโทรหรือการแจ้งเตือน—SMS เท่านั้น

---

## 2. Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     SMS Hub Agent                            │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐         ┌─────────────────┐              │
│  │   System     │         │  MainActivity   │              │
│  │   SMS        │──SMS──▶ │  (Entry Point)  │              │
│  │  Broadcast   │         └─────────────────┘              │
│  └──────────────┘                  │                         │
│         │                           │                         │
│         ▼                           ▼                         │
│  ┌──────────────────────┐  ┌──────────────────────┐        │
│  │  SmsReceiver         │  │ SettingsActivity     │        │
│  │  (BroadcastReceiver) │  │ (Material 3 UI)      │        │
│  └──────────────────────┘  └──────────────────────┘        │
│         │                           │                         │
│         │ Enqueue Work              │ Save Config             │
│         ▼                           ▼                         │
│  ┌──────────────────────┐  ┌──────────────────────┐        │
│  │  SmsForwardWorker    │  │  SharedPreferences   │        │
│  │  (WorkManager)       │◀─│  (Encrypted Store)   │        │
│  └──────────────────────┘  └──────────────────────┘        │
│         │                                                     │
│         │ Build & Sign Payload                               │
│         ▼                                                     │
│  ┌──────────────────────┐                                    │
│  │  WebhookService      │                                    │
│  │  (HTTP Client)       │                                    │
│  └──────────────────────┘                                    │
│         │                                                     │
│         │ HTTPS POST                                          │
│         ▼                                                     │
└─────────────────────────────────────────────────────────────┘
         │
         ▼
  ┌─────────────────┐
  │  Webhook Server │
  │  (External API) │
  └─────────────────┘
```

### Component Interactions

1. **SMS Reception Flow**
   - System broadcasts SMS → SmsReceiver catches intent
   - SmsReceiver extracts message data → enqueues SmsForwardWorker
   - WorkManager schedules background execution (even if app closed)

2. **Configuration Flow**
   - User opens SettingsActivity → configures webhook URL, credentials, HMAC
   - Settings saved to SharedPreferences (encrypted via EncryptedSharedPreferences)
   - Test button validates configuration immediately

3. **Forwarding Flow**
   - SmsForwardWorker reads config from SharedPreferences
   - WebhookService builds JSON payload + adds auth headers + computes HMAC signature
   - HTTP POST to webhook URL with retry/backoff on failure

---

## 3. Feature List

### Core Features
- ✅ **SMS Reception**: Listens to incoming SMS via BroadcastReceiver
- ✅ **Webhook Forwarding**: Immediately POSTs JSON payload to configured URL
- ✅ **Background Operation**: Works when app is closed (WorkManager)
- ✅ **Material 3 Expressive UI**: Modern, adaptive design with dynamic colors
- ✅ **Secure Storage**: Encrypted SharedPreferences for sensitive data
- ✅ **Basic Authentication**: Username/password credentials
- ✅ **HMAC Signature**: SHA256 signature in X-Signature header
- ✅ **Retry Logic**: Exponential backoff on network failures
- ✅ **Device Metadata**: Includes device model, OS version in payload

### Security Features
- 🔒 Basic Auth (username:password)
- 🔒 HMAC-SHA256 request signing
- 🔒 Optional IP allowlist validation
- 🔒 Optional TLS certificate pinning
- 🔒 EncryptedSharedPreferences (AES256)

### Reliability Features
- 🔄 WorkManager retry with exponential backoff
- 🔄 Battery optimization whitelist prompt
- 🔄 Optional offline message queue
- 🔄 Connection timeout handling (30s default)

### Excluded Features
- ❌ NO phone call handling
- ❌ NO notification management
- ❌ SMS only—no MMS support

---

## 4. Material 3 UI Design Specification

### SettingsActivity Layout

```
┌────────────────────────────────────────────────┐
│  SMS Hub Agent Settings              [≡] Menu  │
├────────────────────────────────────────────────┤
│                                                 │
│  Webhook Configuration                          │
│  ┌──────────────────────────────────────────┐ │
│  │ Webhook URL                               │ │
│  │ https://api.example.com/webhook     [🌐] │ │
│  └──────────────────────────────────────────┘ │
│                                                 │
│  Authentication                                 │
│  ┌──────────────────────────────────────────┐ │
│  │ Username                                  │ │
│  │ admin                               [👤] │ │
│  └──────────────────────────────────────────┘ │
│  ┌──────────────────────────────────────────┐ │
│  │ Password                                  │ │
│  │ ••••••••                            [🔒] │ │
│  └──────────────────────────────────────────┘ │
│                                                 │
│  Security                                       │
│  ┌──────────────────────────────────────────┐ │
│  │ Enable HMAC Signature            [○ OFF] │ │
│  └──────────────────────────────────────────┘ │
│  ┌──────────────────────────────────────────┐ │
│  │ HMAC Secret Key                           │ │
│  │ ••••••••••••••••••••                [🔑] │ │
│  └──────────────────────────────────────────┘ │
│                                                 │
│  ┌──────────────┐  ┌────────────────────────┐ │
│  │  Test        │  │  Save Configuration    │ │
│  └──────────────┘  └────────────────────────┘ │
│                                                 │
│  Status: ✓ Last forwarded 2 minutes ago       │
└────────────────────────────────────────────────┘
```

### Color Scheme (Material 3 Expressive)

```kotlin
// Dynamic color from system (Android 12+)
val lightColorScheme = lightColorScheme(
    primary = Color(0xFF006C51),        // Deep teal
    onPrimary = Color(0xFFFFFFFF),      // White
    primaryContainer = Color(0xFF7DF8D3), // Light teal
    onPrimaryContainer = Color(0xFF002116), // Dark teal
    secondary = Color(0xFF4A6360),      // Muted green
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xCCE8E4),
    error = Color(0xFFBA1A1A),
    surface = Color(0xFFFBFDFA),
    surfaceVariant = Color(0xFFDBE5E1)
)

val darkColorScheme = darkColorScheme(
    primary = Color(0xFF5EDCB7),
    onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF00513D),
    secondary = Color(0xFFB1CCC8),
    error = Color(0xFFFFB4AB),
    surface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFF3F4945)
)
```

### Typography

```kotlin
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W400,
        fontSize = 57.sp,
        lineHeight = 64.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.W600,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)
```

### Components

| Component | Material 3 Widget | Properties |
|-----------|-------------------|------------|
| Webhook URL Field | OutlinedTextField | `keyboardType = Uri`, leadingIcon = Icons.Language |
| Username Field | OutlinedTextField | `keyboardType = Text`, leadingIcon = Icons.Person |
| Password Field | OutlinedTextField | `visualTransformation = PasswordVisualTransformation()`, leadingIcon = Icons.Lock |
| HMAC Switch | Switch | `checked = enableHmac`, thumbContent with checkmark icon |
| Test Button | OutlinedButton | `onClick = { testWebhook() }`, outline stroke 2dp |
| Save Button | FilledButton | `onClick = { saveSettings() }`, containerColor = primary |

### Motion & Transitions

- **Screen Enter**: Slide in from right with 300ms duration + fade in
- **Save Success**: Button → Checkmark icon morph (200ms) + Green tint pulse
- **Test Request**: Loading spinner overlay with scrim (alpha 0.6)
- **Field Focus**: Elevation raise 2dp → 4dp with 150ms easing
- **Error State**: Shake animation (300ms) + Red outline pulse

---

## 5. Permissions & Security Explanation

### Required Permissions

#### RECEIVE_SMS (Dangerous Permission)
```xml
<uses-permission android:name="android.permission.RECEIVE_SMS" />
```
**Why?** Listen to incoming SMS messages via BroadcastReceiver.
**User Impact:** User must grant this at runtime (Android 6+).
**Risk:** Access to all incoming messages—must be secured properly.

#### INTERNET (Normal Permission)
```xml
<uses-permission android:name="android.permission.INTERNET" />
```
**Why?** Send HTTP POST requests to webhook URL.
**User Impact:** Automatically granted, no user prompt.
**Risk:** None—standard network access.

#### POST_NOTIFICATIONS (Android 13+)
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```
**Why?** Show foreground service notification (WorkManager requirement).
**User Impact:** User must grant at runtime (Android 13+).

### Security Measures

#### 1. Basic Authentication
```kotlin
val credentials = "$username:$password".toByteArray()
val encodedAuth = Base64.encodeToString(credentials, Base64.NO_WRAP)
request.addHeader("Authorization", "Basic $encodedAuth")
```
**Purpose:** Authenticate requests to webhook server.

#### 2. HMAC-SHA256 Signature
```kotlin
val hmac = Mac.getInstance("HmacSHA256")
hmac.init(SecretKeySpec(hmacSecret.toByteArray(), "HmacSHA256"))
val signature = hmac.doFinal(payload.toByteArray())
val hexSignature = signature.joinToString("") { "%02x".format(it) }
request.addHeader("X-Signature", hexSignature)
```
**Purpose:** Verify request integrity—server recomputes and compares.
**Server Validation:**
```python
import hmac
import hashlib

def verify_signature(payload, signature, secret):
    computed = hmac.new(secret.encode(), payload.encode(), hashlib.sha256).hexdigest()
    return hmac.compare_digest(computed, signature)
```

#### 3. Encrypted Storage
```kotlin
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val sharedPreferences = EncryptedSharedPreferences.create(
    context,
    "sms_hub_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```
**Purpose:** Protect webhook credentials stored on device.

#### 4. Optional TLS Pinning
```kotlin
val certificatePinner = CertificatePinner.Builder()
    .add("api.example.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
    .build()

val client = OkHttpClient.Builder()
    .certificatePinner(certificatePinner)
    .build()
```
**Purpose:** Prevent MITM attacks by validating server certificate.

---

## 6. Example Payload and cURL Test

### JSON Payload Schema

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

### Field Descriptions

| Field | Type | Description |
|-------|------|-------------|
| `type` | string | Always "sms" (future: could support "mms") |
| `sender` | string | Phone number of SMS sender (E.164 format preferred) |
| `message` | string | Full SMS message body (UTF-8 encoded) |
| `timestamp` | integer | Unix timestamp in milliseconds (UTC) |
| `device` | string | Android device model (from Build.MODEL) |
| `osVersion` | integer | Android API level (from Build.VERSION.SDK_INT) |

### cURL Test Request

#### Without HMAC
```bash
curl -X POST https://api.example.com/webhook \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic YWRtaW46cGFzc3dvcmQ=" \
  -d '{
    "type": "sms",
    "sender": "+66812345678",
    "message": "Test message",
    "timestamp": 1728799999000,
    "device": "Pixel 7",
    "osVersion": 34
  }'
```

#### With HMAC Signature
```bash
# Compute HMAC (using openssl)
PAYLOAD='{"type":"sms","sender":"+66812345678","message":"Test message","timestamp":1728799999000,"device":"Pixel 7","osVersion":34}'
SECRET="your_hmac_secret_key"
SIGNATURE=$(echo -n "$PAYLOAD" | openssl dgst -sha256 -hmac "$SECRET" | awk '{print $2}')

curl -X POST https://api.example.com/webhook \
  -H "Content-Type: application/json" \
  -H "Authorization: Basic YWRtaW46cGFzc3dvcmQ=" \
  -H "X-Signature: $SIGNATURE" \
  -d "$PAYLOAD"
```

### Expected Server Response
```json
{
  "status": "success",
  "message": "SMS forwarded successfully",
  "id": "msg_1234567890"
}
```

---

## 7. Installation & Setup Guide

### Gradle Dependencies (app/build.gradle.kts)

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.sms_hub_agent"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.sms_hub_agent"
        minSdk = 35
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Jetpack Compose with Material 3
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")

    // WorkManager for background tasks
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Encrypted SharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // HTTP Client (OkHttp)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
```

### AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Permissions -->
    <uses-permission android:name="android.permission.RECEIVE_SMS" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.SmshubAgent">

        <!-- Main Activity -->
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.SmshubAgent">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Settings Activity -->
        <activity
            android:name=".SettingsActivity"
            android:exported="false"
            android:label="@string/settings_title" />

        <!-- SMS Receiver -->
        <receiver
            android:name=".receiver.SmsReceiver"
            android:enabled="true"
            android:exported="true"
            android:permission="android.permission.BROADCAST_SMS">
            <intent-filter android:priority="999">
                <action android:name="android.provider.Telephony.SMS_RECEIVED" />
            </intent-filter>
        </receiver>

    </application>

</manifest>
```

### Initial Setup Steps

1. **Clone/Create Project**
   ```bash
   git clone https://github.com/yourusername/sms-hub-agent.git
   cd sms-hub-agent
   ```

2. **Sync Gradle Dependencies**
   ```bash
   ./gradlew sync
   ```

3. **Build APK**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Install on Device**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

5. **Grant Permissions**
   - Open app → Settings → Grant SMS permission
   - Settings → Battery → Unrestricted (disable optimization)

6. **Configure Webhook**
   - Open Settings screen
   - Enter webhook URL: `https://api.example.com/webhook`
   - Enter username/password for Basic Auth
   - (Optional) Enable HMAC and enter secret key
   - Click "Test" to validate configuration
   - Click "Save"

---

## 8. Testing Checklist

### Unit Tests

#### SmsReceiverTest.kt
- [ ] Test SMS intent parsing extracts sender and message correctly
- [ ] Test malformed SMS intent is handled gracefully
- [ ] Test WorkManager is enqueued when SMS received
- [ ] Test empty message body is rejected

#### WebhookServiceTest.kt
- [ ] Test JSON payload serialization matches expected format
- [ ] Test Basic Auth header is correctly encoded
- [ ] Test HMAC signature is computed correctly (SHA256)
- [ ] Test HTTP request includes all required headers
- [ ] Test retry logic triggers on network failure (3 attempts)
- [ ] Test timeout handling (30 seconds default)

#### SettingsRepositoryTest.kt
- [ ] Test settings are saved to SharedPreferences correctly
- [ ] Test settings are retrieved without data loss
- [ ] Test EncryptedSharedPreferences encryption/decryption works
- [ ] Test empty/null values are handled safely

### Instrumented Tests (Android Device/Emulator)

#### SmsFlowTest.kt
- [ ] Test end-to-end: Simulate SMS → Verify webhook POST sent
- [ ] Test background execution: Close app → Send SMS → Verify delivery
- [ ] Test network failure: Disable WiFi → Send SMS → Verify retry
- [ ] Test battery optimization: Restricted mode → Verify WorkManager executes

#### SettingsActivityTest.kt
- [ ] Test UI elements are displayed correctly
- [ ] Test "Save" button updates SharedPreferences
- [ ] Test "Test" button sends mock webhook request
- [ ] Test validation: Empty URL shows error message
- [ ] Test HMAC toggle enables/disables secret field
- [ ] Test password field masks input correctly

### Manual Testing Checklist

- [ ] Send SMS from another phone → Verify forwarded to webhook
- [ ] Test with special characters in message (emoji, Thai script)
- [ ] Test with long message (>160 characters, multipart SMS)
- [ ] Test with different senders (+66, +1, shortcode 12345)
- [ ] Test app closed state: Force stop → Send SMS → Verify delivery
- [ ] Test airplane mode: Enable → Send SMS → Disable → Verify retry
- [ ] Test invalid webhook URL: Save → Send SMS → Verify error logged
- [ ] Test HMAC validation: Server rejects wrong signature

### Performance Testing

- [ ] Test 10 SMS received within 1 second → All forwarded
- [ ] Test battery drain: Monitor over 24 hours with 100 SMS
- [ ] Test memory usage: No leaks after 1000 SMS processed

---

## 9. JSON Schema for Webhook Payload

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "$id": "https://sms-hub-agent.example.com/schemas/sms-payload.json",
  "title": "SMS Webhook Payload",
  "description": "JSON payload sent by SMS Hub Agent when SMS is received",
  "type": "object",
  "required": ["type", "sender", "message", "timestamp", "device", "osVersion"],
  "properties": {
    "type": {
      "type": "string",
      "const": "sms",
      "description": "Message type identifier (always 'sms')"
    },
    "sender": {
      "type": "string",
      "pattern": "^\\+?[1-9]\\d{1,14}$",
      "description": "Phone number of SMS sender in E.164 format (e.g., +66812345678)",
      "examples": ["+66812345678", "+12025551234", "12345"]
    },
    "message": {
      "type": "string",
      "minLength": 1,
      "maxLength": 1600,
      "description": "Full SMS message body (UTF-8 encoded, max 1600 chars for concatenated SMS)"
    },
    "timestamp": {
      "type": "integer",
      "minimum": 0,
      "description": "Unix timestamp in milliseconds (UTC) when SMS was received",
      "examples": [1728799999000]
    },
    "device": {
      "type": "string",
      "description": "Android device model name (from Build.MODEL)",
      "examples": ["Pixel 7", "SM-G991B", "OnePlus 9 Pro"]
    },
    "osVersion": {
      "type": "integer",
      "minimum": 21,
      "maximum": 99,
      "description": "Android API level (from Build.VERSION.SDK_INT)",
      "examples": [34, 33, 31]
    }
  },
  "additionalProperties": false
}
```

### Validation Example (Node.js)

```javascript
const Ajv = require('ajv');
const ajv = new Ajv();

const schema = {
  type: "object",
  required: ["type", "sender", "message", "timestamp", "device", "osVersion"],
  properties: {
    type: { const: "sms" },
    sender: { type: "string", pattern: "^\\+?[1-9]\\d{1,14}$" },
    message: { type: "string", minLength: 1, maxLength: 1600 },
    timestamp: { type: "integer", minimum: 0 },
    device: { type: "string" },
    osVersion: { type: "integer", minimum: 21, maximum: 99 }
  },
  additionalProperties: false
};

const validate = ajv.compile(schema);

// Example payload
const payload = {
  type: "sms",
  sender: "+66812345678",
  message: "Your OTP is 123456",
  timestamp: 1728799999000,
  device: "Pixel 7",
  osVersion: 34
};

const valid = validate(payload);
if (!valid) {
  console.error('Validation errors:', validate.errors);
}
```

---

## 10. Deployment & Production Considerations

### Play Store Preparation

1. **ProGuard Rules** (app/proguard-rules.pro)
   ```proguard
   # Keep OkHttp
   -dontwarn okhttp3.**
   -keep class okhttp3.** { *; }

   # Keep Kotlinx Serialization
   -keep class kotlinx.serialization.** { *; }
   -keep class com.example.sms_hub_agent.model.** { *; }

   # Keep WorkManager
   -keep class androidx.work.** { *; }
   ```

2. **Signing Configuration**
   ```kotlin
   android {
       signingConfigs {
           release {
               storeFile file("release.keystore")
               storePassword System.getenv("KEYSTORE_PASSWORD")
               keyAlias "sms-hub-agent"
               keyPassword System.getenv("KEY_PASSWORD")
           }
       }
       buildTypes {
           release {
               signingConfig signingConfigs.release
               isMinifyEnabled true
               proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
           }
       }
   }
   ```

3. **Build Release APK**
   ```bash
   ./gradlew assembleRelease
   ```

### Privacy Policy (Required for SMS Permission)

You MUST provide a privacy policy URL when publishing to Play Store (SMS permission requirement).

**Example Privacy Policy Statement:**
> "SMS Hub Agent collects incoming SMS messages only for the purpose of forwarding them to your configured webhook server. Messages are transmitted immediately and not stored locally. No data is shared with third parties beyond your specified webhook endpoint. All credentials are encrypted on device using AES-256."

---

## Appendix A: Server-Side Implementation Example

### Express.js Webhook Endpoint

```javascript
const express = require('express');
const crypto = require('crypto');
const app = express();

app.use(express.json());

// Environment variables
const WEBHOOK_USER = process.env.WEBHOOK_USER || 'admin';
const WEBHOOK_PASS = process.env.WEBHOOK_PASS || 'password';
const HMAC_SECRET = process.env.HMAC_SECRET || 'your_secret_key';

// Middleware: Basic Auth
app.use((req, res, next) => {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Basic ')) {
        return res.status(401).json({ error: 'Unauthorized' });
    }

    const credentials = Buffer.from(authHeader.slice(6), 'base64').toString();
    const [username, password] = credentials.split(':');

    if (username !== WEBHOOK_USER || password !== WEBHOOK_PASS) {
        return res.status(401).json({ error: 'Invalid credentials' });
    }

    next();
});

// Middleware: HMAC Verification
app.use((req, res, next) => {
    const signature = req.headers['x-signature'];
    if (!signature) {
        return next(); // Optional: Allow requests without HMAC
    }

    const payload = JSON.stringify(req.body);
    const computed = crypto.createHmac('sha256', HMAC_SECRET)
                          .update(payload)
                          .digest('hex');

    if (!crypto.timingSafeEqual(Buffer.from(signature), Buffer.from(computed))) {
        return res.status(403).json({ error: 'Invalid signature' });
    }

    next();
});

// Webhook endpoint
app.post('/webhook', (req, res) => {
    const { type, sender, message, timestamp, device, osVersion } = req.body;

    // Validate required fields
    if (!type || !sender || !message || !timestamp) {
        return res.status(400).json({ error: 'Missing required fields' });
    }

    // Process SMS (e.g., save to database, trigger automation)
    console.log(`[SMS] From: ${sender} | Message: ${message}`);

    // Example: Extract OTP and store
    const otpMatch = message.match(/\b\d{6}\b/);
    if (otpMatch) {
        console.log(`Detected OTP: ${otpMatch[0]}`);
        // Store OTP in Redis with 5-minute TTL
    }

    res.json({
        status: 'success',
        message: 'SMS forwarded successfully',
        id: `msg_${Date.now()}`
    });
});

app.listen(3000, () => {
    console.log('Webhook server listening on port 3000');
});
```

---

## Appendix B: Troubleshooting Guide

### SMS Not Forwarding

**Symptom:** SMS received but no webhook request sent.

**Solutions:**
1. Check WorkManager logs: `adb logcat | grep WorkManager`
2. Verify internet connection: Test webhook URL in browser
3. Check battery optimization: Settings → Battery → Unrestricted
4. Verify RECEIVE_SMS permission granted
5. Test with mock SMS: `adb emu sms send +66812345678 "Test message"`

### Webhook Returns 401 Unauthorized

**Symptom:** Server rejects request with 401 status.

**Solutions:**
1. Verify Basic Auth credentials in app settings
2. Test with cURL: `curl -u username:password https://webhook-url`
3. Check server logs for authentication errors
4. Ensure username:password encoding is correct (Base64, no padding issues)

### HMAC Signature Mismatch

**Symptom:** Server returns 403 with "Invalid signature" error.

**Solutions:**
1. Verify HMAC secret matches on both client and server
2. Ensure payload is serialized identically (no whitespace differences)
3. Test signature generation manually:
   ```bash
   echo -n '{"type":"sms",...}' | openssl dgst -sha256 -hmac "secret"
   ```
4. Check for charset encoding issues (UTF-8 vs ASCII)

### High Battery Drain

**Symptom:** App consumes >5% battery per day.

**Solutions:**
1. Check WorkManager constraints: Only run on WiFi/charging
2. Reduce retry attempts from 5 to 3
3. Increase backoff delay from 10s to 30s
4. Profile with Android Studio Battery Profiler

---

## Appendix C: Future Enhancements

### Roadmap (v2.0)

- [ ] **Rule-Based Filtering**: Forward only SMS matching regex patterns
- [ ] **Multiple Webhooks**: Route different senders to different URLs
- [ ] **Offline Queue**: Store failed requests in Room database
- [ ] **Analytics Dashboard**: Display forwarding stats (success rate, latency)
- [ ] **MMS Support**: Forward multimedia messages (images, videos)
- [ ] **Custom Headers**: Allow user-defined HTTP headers
- [ ] **Webhook Response Actions**: Trigger actions based on server response
- [ ] **Export Logs**: Share forwarding history via email/cloud

### API Version 2 Payload (Future)

```json
{
  "version": "2.0",
  "type": "sms",
  "sender": {
    "phone": "+66812345678",
    "contactName": "John Doe"
  },
  "message": {
    "body": "Your OTP is 123456",
    "encoding": "utf-8",
    "partCount": 1
  },
  "timestamp": {
    "received": 1728799999000,
    "forwarded": 1728800005000
  },
  "device": {
    "model": "Pixel 7",
    "manufacturer": "Google",
    "osVersion": 34,
    "appVersion": "2.0.0"
  },
  "metadata": {
    "ruleId": "otp-filter",
    "retryCount": 0
  }
}
```

---

**Document Version:** 1.0.0
**Last Updated:** 2025-01-13
**Author:** SMS Hub Agent Team
**License:** MIT
