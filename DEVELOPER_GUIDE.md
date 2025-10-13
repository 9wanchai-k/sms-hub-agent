# SMS Hub Agent - Developer Guide

## Quick Start

### Prerequisites
- Android Studio Hedgehog or later
- Kotlin 1.9.0+
- Android SDK 34
- JDK 17 or later
- Git

### Project Setup
```bash
# Clone the repository
git clone https://github.com/reb4ck/sms-hub-agent.git
cd sms-hub-agent

# Open in Android Studio
# File → Open → Select the project directory

# Sync Gradle dependencies
./gradlew build
```

## Development Environment

### Build Configuration
```kotlin
// app/build.gradle.kts
android {
    namespace = "com.smshub.agent"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.smshub.agent"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}
```

### Dependencies Management
```kotlin
dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Data Storage
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // JSON Processing
    implementation("com.google.code.gson:gson:2.10.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.work:work-testing:2.9.0")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

## Architecture Implementation

### Core Components

#### 1. SMS Receiver
```kotlin
class SmsReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "SmsReceiver"
        private const val SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SMS_RECEIVED_ACTION) return

        try {
            val messages = extractSmsMessages(intent)
            messages.forEach { smsMessage ->
                scheduleWebhookDelivery(context, smsMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process SMS", e)
        }
    }

    private fun extractSmsMessages(intent: Intent): List<SmsMessage> {
        val pdus = intent.extras?.get("pdus") as? Array<*> ?: return emptyList()
        val format = intent.getStringExtra("format")

        return pdus.mapNotNull { pdu ->
            try {
                val smsMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    android.telephony.SmsMessage.createFromPdu(pdu as ByteArray, format)
                } else {
                    @Suppress("DEPRECATION")
                    android.telephony.SmsMessage.createFromPdu(pdu as ByteArray)
                }

                SmsMessage(
                    sender = smsMessage.displayOriginatingAddress,
                    message = smsMessage.messageBody,
                    timestamp = smsMessage.timestampMillis,
                    messageId = UUID.randomUUID().toString()
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse SMS PDU", e)
                null
            }
        }
    }

    private fun scheduleWebhookDelivery(context: Context, smsMessage: SmsMessage) {
        val workRequest = OneTimeWorkRequestBuilder<WebhookWorker>()
            .setInputData(smsMessage.toWorkData())
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
```

#### 2. Data Models
```kotlin
@Parcelize
data class SmsMessage(
    val sender: String,
    val message: String,
    val timestamp: Long,
    val messageId: String = UUID.randomUUID().toString(),
    val device: String = Build.MODEL,
    val osVersion: Int = Build.VERSION.SDK_INT
) : Parcelable {
    
    fun toWorkData(): Data = Data.Builder()
        .putString("sender", sender)
        .putString("message", message)
        .putLong("timestamp", timestamp)
        .putString("messageId", messageId)
        .putString("device", device)
        .putInt("osVersion", osVersion)
        .build()

    fun toJsonPayload(appVersion: String): String {
        val payload = mapOf(
            "type" to "sms",
            "sender" to sender,
            "message" to message,
            "timestamp" to timestamp,
            "device" to device,
            "osVersion" to osVersion,
            "appVersion" to appVersion,
            "messageId" to messageId
        )
        return Gson().toJson(payload)
    }

    companion object {
        fun fromWorkData(data: Data): SmsMessage? {
            return try {
                SmsMessage(
                    sender = data.getString("sender") ?: return null,
                    message = data.getString("message") ?: return null,
                    timestamp = data.getLong("timestamp", 0),
                    messageId = data.getString("messageId") ?: UUID.randomUUID().toString(),
                    device = data.getString("device") ?: Build.MODEL,
                    osVersion = data.getInt("osVersion", Build.VERSION.SDK_INT)
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
```

#### 3. Configuration Management
```kotlin
@Singleton
class ConfigurationManager @Inject constructor(
    private val context: Context,
    private val encryptedPreferences: EncryptedSharedPreferences
) {
    
    companion object {
        private const val PREF_WEBHOOK_URL = "webhook_url"
        private const val PREF_ENABLE_AUTH = "enable_auth"
        private const val PREF_AUTH_USERNAME = "auth_username"
        private const val PREF_AUTH_PASSWORD = "auth_password"
        private const val PREF_ENABLE_HMAC = "enable_hmac"
        private const val PREF_HMAC_SECRET = "hmac_secret"
        private const val PREF_RETRY_ATTEMPTS = "retry_attempts"
        private const val PREF_TIMEOUT_SECONDS = "timeout_seconds"
    }

    fun getConfiguration(): Configuration {
        return Configuration(
            webhookUrl = encryptedPreferences.getString(PREF_WEBHOOK_URL, "") ?: "",
            enableAuth = encryptedPreferences.getBoolean(PREF_ENABLE_AUTH, false),
            authUsername = encryptedPreferences.getString(PREF_AUTH_USERNAME, "") ?: "",
            authPassword = encryptedPreferences.getString(PREF_AUTH_PASSWORD, "") ?: "",
            enableHmac = encryptedPreferences.getBoolean(PREF_ENABLE_HMAC, false),
            hmacSecret = encryptedPreferences.getString(PREF_HMAC_SECRET, "") ?: "",
            retryAttempts = encryptedPreferences.getInt(PREF_RETRY_ATTEMPTS, 3),
            timeoutSeconds = encryptedPreferences.getInt(PREF_TIMEOUT_SECONDS, 30)
        )
    }

    fun saveConfiguration(config: Configuration) {
        encryptedPreferences.edit {
            putString(PREF_WEBHOOK_URL, config.webhookUrl)
            putBoolean(PREF_ENABLE_AUTH, config.enableAuth)
            putString(PREF_AUTH_USERNAME, config.authUsername)
            putString(PREF_AUTH_PASSWORD, config.authPassword)
            putBoolean(PREF_ENABLE_HMAC, config.enableHmac)
            putString(PREF_HMAC_SECRET, config.hmacSecret)
            putInt(PREF_RETRY_ATTEMPTS, config.retryAttempts)
            putInt(PREF_TIMEOUT_SECONDS, config.timeoutSeconds)
        }
    }

    fun validateConfiguration(config: Configuration): ValidationResult {
        val errors = mutableListOf<String>()

        if (config.webhookUrl.isBlank()) {
            errors.add("Webhook URL is required")
        } else if (!config.webhookUrl.startsWith("https://")) {
            errors.add("Webhook URL must use HTTPS")
        }

        if (config.enableAuth) {
            if (config.authUsername.isBlank()) {
                errors.add("Username is required when authentication is enabled")
            }
            if (config.authPassword.isBlank()) {
                errors.add("Password is required when authentication is enabled")
            }
        }

        if (config.enableHmac) {
            if (config.hmacSecret.length < 16) {
                errors.add("HMAC secret must be at least 16 characters")
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
}
```

#### 4. Webhook Worker
```kotlin
class WebhookWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "WebhookWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            val smsMessage = SmsMessage.fromWorkData(inputData)
                ?: return Result.failure()

            val configManager = ConfigurationManager(applicationContext)
            val configuration = configManager.getConfiguration()

            if (!configuration.isValid()) {
                Log.w(TAG, "Invalid configuration, skipping webhook delivery")
                return Result.failure()
            }

            val success = deliverWebhook(smsMessage, configuration)
            if (success) {
                Log.i(TAG, "Webhook delivered successfully")
                Result.success()
            } else {
                Log.w(TAG, "Webhook delivery failed, will retry")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in webhook worker", e)
            Result.failure()
        }
    }

    private suspend fun deliverWebhook(
        smsMessage: SmsMessage,
        configuration: Configuration
    ): Boolean {
        return try {
            val httpClient = createHttpClient(configuration)
            val payload = smsMessage.toJsonPayload(BuildConfig.VERSION_NAME)
            
            val requestBuilder = Request.Builder()
                .url(configuration.webhookUrl)
                .post(payload.toRequestBody("application/json".toMediaType()))
                .header("User-Agent", "SMSHubAgent/${BuildConfig.VERSION_NAME}")
                .header("Content-Type", "application/json")

            // Add Basic Auth if enabled
            if (configuration.enableAuth) {
                val credentials = Credentials.basic(
                    configuration.authUsername,
                    configuration.authPassword
                )
                requestBuilder.header("Authorization", credentials)
            }

            // Add HMAC signature if enabled
            if (configuration.enableHmac) {
                val signature = HMACGenerator.generateSignature(payload, configuration.hmacSecret)
                requestBuilder.header("X-Signature", signature)
            }

            val response = httpClient.newCall(requestBuilder.build()).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deliver webhook", e)
            false
        }
    }

    private fun createHttpClient(configuration: Configuration): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(configuration.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout(configuration.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .writeTimeout(configuration.timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .build()
    }
}
```

## Testing Guidelines

### Unit Testing Examples

#### Testing SMS Message Processing
```kotlin
class SmsMessageTest {

    @Test
    fun `toJsonPayload creates valid JSON structure`() {
        val smsMessage = SmsMessage(
            sender = "+66812345678",
            message = "Test message",
            timestamp = 1640995200000,
            messageId = "test-id",
            device = "TestDevice",
            osVersion = 33
        )

        val json = smsMessage.toJsonPayload("1.0.0")
        val payload = Gson().fromJson(json, Map::class.java)

        assertEquals("sms", payload["type"])
        assertEquals("+66812345678", payload["sender"])
        assertEquals("Test message", payload["message"])
        assertEquals(1640995200000.0, payload["timestamp"])
        assertEquals("TestDevice", payload["device"])
        assertEquals(33.0, payload["osVersion"])
        assertEquals("1.0.0", payload["appVersion"])
        assertEquals("test-id", payload["messageId"])
    }

    @Test
    fun `fromWorkData reconstructs SmsMessage correctly`() {
        val original = SmsMessage(
            sender = "+66812345678",
            message = "Test message",
            timestamp = 1640995200000
        )

        val workData = original.toWorkData()
        val reconstructed = SmsMessage.fromWorkData(workData)

        assertNotNull(reconstructed)
        assertEquals(original.sender, reconstructed!!.sender)
        assertEquals(original.message, reconstructed.message)
        assertEquals(original.timestamp, reconstructed.timestamp)
    }
}
```

#### Testing HMAC Generation
```kotlin
class HMACGeneratorTest {

    @Test
    fun `generateSignature produces consistent results`() {
        val payload = """{"type":"sms","sender":"+66812345678","message":"test"}"""
        val secret = "test-secret-key"

        val signature1 = HMACGenerator.generateSignature(payload, secret)
        val signature2 = HMACGenerator.generateSignature(payload, secret)

        assertEquals(signature1, signature2)
        assertTrue(signature1.startsWith("sha256="))
    }

    @Test
    fun `verifySignature validates correctly`() {
        val payload = """{"type":"sms","message":"test"}"""
        val secret = "test-secret-key"
        val signature = HMACGenerator.generateSignature(payload, secret)

        assertTrue(HMACGenerator.verifySignature(payload, secret, signature))
        assertFalse(HMACGenerator.verifySignature(payload, "wrong-secret", signature))
        assertFalse(HMACGenerator.verifySignature("wrong-payload", secret, signature))
    }
}
```

### Integration Testing

#### Testing WorkManager Integration
```kotlin
@RunWith(AndroidJUnit4::class)
class WebhookWorkerTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var workManager: TestWorkManagerImpl
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
        
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context) as TestWorkManagerImpl
    }

    @Test
    fun `webhook worker processes SMS message successfully`() {
        val smsMessage = SmsMessage(
            sender = "+66812345678",
            message = "Test SMS",
            timestamp = System.currentTimeMillis()
        )

        val request = OneTimeWorkRequestBuilder<WebhookWorker>()
            .setInputData(smsMessage.toWorkData())
            .build()

        workManager.enqueue(request).result.get()

        val workInfo = workManager.getWorkInfoById(request.id).get()
        assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)
    }
}
```

## Code Style and Best Practices

### Kotlin Coding Standards
```kotlin
// Use explicit types for public APIs
class ConfigurationManager {
    fun getConfiguration(): Configuration { /* implementation */ }
}

// Use extension functions for utility methods
fun String.isValidPhoneNumber(): Boolean {
    return matches(Regex("^\\+[1-9]\\d{1,14}$"))
}

// Use data classes for immutable data
data class Configuration(
    val webhookUrl: String,
    val enableAuth: Boolean,
    val authUsername: String = "",
    val authPassword: String = ""
) {
    fun isValid(): Boolean = webhookUrl.isNotBlank() && webhookUrl.startsWith("https://")
}

// Use sealed classes for result types
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val messages: List<String>) : ValidationResult()
}
```

### Compose UI Best Practices
```kotlin
// Use state hoisting
@Composable
fun ConfigurationScreen(
    uiState: ConfigurationUiState,
    onEvent: (ConfigurationEvent) -> Unit
) {
    // UI implementation
}

// Extract reusable components
@Composable
fun ConfigurationSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}
```

## Performance Optimization

### Memory Management
```kotlin
// Use WeakReference for callbacks
class SmsProcessor {
    private var callbackRef: WeakReference<Callback>? = null
    
    fun setCallback(callback: Callback) {
        callbackRef = WeakReference(callback)
    }
}

// Dispose resources properly
class NetworkClient : Closeable {
    private val okHttpClient = OkHttpClient()
    
    override fun close() {
        okHttpClient.dispatcher.executorService.shutdown()
        okHttpClient.connectionPool.evictAll()
    }
}
```

### Battery Optimization
```kotlin
// Use efficient WorkManager constraints
val constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .setRequiresBatteryNotLow(true)
    .build()

// Batch multiple SMS messages when possible
class SMSBatcher {
    private val pendingMessages = mutableListOf<SmsMessage>()
    private val batchDelay = 5000L // 5 seconds
    
    fun addMessage(message: SmsMessage) {
        pendingMessages.add(message)
        scheduleFlush()
    }
    
    private fun scheduleFlush() {
        handler.removeCallbacks(flushRunnable)
        handler.postDelayed(flushRunnable, batchDelay)
    }
}
```

## Security Considerations

### Secure Storage
```kotlin
// Use Android Keystore for sensitive data
object SecureStorage {
    private const val KEYSTORE_ALIAS = "SmsHubAgentKey"
    
    fun encryptData(data: String): String {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .build()
        
        keyGenerator.init(keyGenParameterSpec)
        val secretKey = keyGenerator.generateKey()
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        
        val encryptedData = cipher.doFinal(data.toByteArray())
        return Base64.encodeToString(encryptedData, Base64.DEFAULT)
    }
}
```

### Network Security
```kotlin
// Implement certificate pinning
class SecureHttpClient {
    private val certificatePinner = CertificatePinner.Builder()
        .add("api.example.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        .build()
    
    private val client = OkHttpClient.Builder()
        .certificatePinner(certificatePinner)
        .build()
}
```

## Debugging and Logging

### Structured Logging
```kotlin
object Logger {
    private const val TAG = "SMSHubAgent"
    
    fun logSMSReceived(sender: String, messageLength: Int) {
        Log.i(TAG, "SMS received from $sender, length: $messageLength")
    }
    
    fun logWebhookDelivery(success: Boolean, responseCode: Int?) {
        if (success) {
            Log.i(TAG, "Webhook delivered successfully, response: $responseCode")
        } else {
            Log.w(TAG, "Webhook delivery failed, response: $responseCode")
        }
    }
    
    fun logError(operation: String, error: Throwable) {
        Log.e(TAG, "Error in $operation", error)
    }
}
```

### Debug Build Configuration
```kotlin
// build.gradle.kts
android {
    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            buildConfigField("boolean", "ENABLE_LOGGING", "true")
            buildConfigField("String", "DEFAULT_WEBHOOK_URL", "\"https://webhook.site/debug\"")
        }
        release {
            buildConfigField("boolean", "ENABLE_LOGGING", "false")
            buildConfigField("String", "DEFAULT_WEBHOOK_URL", "\"\"")
        }
    }
}
```

## Deployment

### Release Build Configuration
```kotlin
// Proguard rules for release builds
-keep class com.smshub.agent.data.** { *; }
-keep class com.smshub.agent.network.** { *; }
-keepclassmembers class ** {
    @com.google.gson.annotations.SerializedName <fields>;
}

// Keep WorkManager classes
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.InputMerger
```

### Continuous Integration
```yaml
# .github/workflows/android.yml
name: Android CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
    - name: Run tests
      run: ./gradlew test
      
    - name: Run lint
      run: ./gradlew lint
      
    - name: Build APK
      run: ./gradlew assembleDebug
```

This developer guide provides comprehensive information for setting up, developing, testing, and deploying the SMS Hub Agent application. Follow these guidelines to maintain code quality and ensure robust functionality.
