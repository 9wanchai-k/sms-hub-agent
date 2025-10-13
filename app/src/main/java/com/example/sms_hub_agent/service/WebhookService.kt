package com.example.sms_hub_agent.service

import android.util.Base64
import android.util.Log
import com.example.sms_hub_agent.model.AppSettings
import com.example.sms_hub_agent.model.SmsPayload
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Service for sending SMS data to webhook endpoint
 */
class WebhookService {

    private val json = Json { prettyPrint = false }

    /**
     * Send SMS payload to configured webhook URL
     * @return true if successful, false otherwise
     */
    fun sendSmsToWebhook(payload: SmsPayload, settings: AppSettings): Boolean {
        if (!settings.isValid()) {
            Log.e(TAG, "Invalid settings configuration")
            return false
        }

        return try {
            val jsonPayload = json.encodeToString(payload)
            Log.d(TAG, "Sending payload: $jsonPayload")

            // Build HTTP client with timeout
            val client = OkHttpClient.Builder()
                .connectTimeout(settings.timeoutSeconds.toLong(), TimeUnit.SECONDS)
                .readTimeout(settings.timeoutSeconds.toLong(), TimeUnit.SECONDS)
                .writeTimeout(settings.timeoutSeconds.toLong(), TimeUnit.SECONDS)
                .build()

            // Build request
            val requestBuilder = Request.Builder()
                .url(settings.webhookUrl)
                .post(jsonPayload.toRequestBody(JSON_MEDIA_TYPE))
                .addHeader("Content-Type", "application/json")

            // Add Basic Auth header
            val basicAuth = buildBasicAuthHeader(settings.authUsername, settings.authPassword)
            requestBuilder.addHeader("Authorization", basicAuth)

            // Add HMAC signature if enabled
            if (settings.hmacEnabled && settings.hmacSecret.isNotBlank()) {
                val signature = computeHmacSignature(jsonPayload, settings.hmacSecret)
                requestBuilder.addHeader("X-Signature", signature)
                Log.d(TAG, "Added HMAC signature: $signature")
            }

            // Execute request
            val request = requestBuilder.build()
            val response = client.newCall(request).execute()

            val success = response.isSuccessful
            if (success) {
                Log.i(TAG, "Webhook request successful: ${response.code}")
                Log.d(TAG, "Response body: ${response.body?.string()}")
            } else {
                Log.e(TAG, "Webhook request failed: ${response.code} ${response.message}")
                Log.e(TAG, "Response body: ${response.body?.string()}")
            }

            response.close()
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error sending webhook request", e)
            false
        }
    }

    /**
     * Build Basic Authentication header
     */
    private fun buildBasicAuthHeader(username: String, password: String): String {
        val credentials = "$username:$password"
        val encodedCredentials = Base64.encodeToString(
            credentials.toByteArray(),
            Base64.NO_WRAP
        )
        return "Basic $encodedCredentials"
    }

    /**
     * Compute HMAC-SHA256 signature for payload
     */
    private fun computeHmacSignature(payload: String, secret: String): String {
        return try {
            val hmac = Mac.getInstance("HmacSHA256")
            val secretKey = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
            hmac.init(secretKey)
            val signature = hmac.doFinal(payload.toByteArray())
            signature.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error computing HMAC signature", e)
            ""
        }
    }

    companion object {
        private const val TAG = "WebhookService"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
