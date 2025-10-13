package com.example.sms_hub_agent.worker

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sms_hub_agent.model.SmsPayload
import com.example.sms_hub_agent.repository.SettingsRepository
import com.example.sms_hub_agent.service.WebhookService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager worker that forwards SMS to webhook in background
 */
class SmsForwardWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val settingsRepository = SettingsRepository(context)
    private val webhookService = WebhookService()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting SMS forward work...")

        try {
            // Extract SMS data from input
            val sender = inputData.getString(KEY_SENDER) ?: return@withContext Result.failure()
            val message = inputData.getString(KEY_MESSAGE) ?: return@withContext Result.failure()
            val timestamp = inputData.getLong(KEY_TIMESTAMP, System.currentTimeMillis())

            Log.i(TAG, "Processing SMS from $sender")

            // Load settings
            val settings = settingsRepository.loadSettings()

            if (!settings.isValid()) {
                Log.e(TAG, "Invalid settings configuration, cannot forward SMS")
                return@withContext Result.failure()
            }

            // Build SMS payload
            val payload = SmsPayload(
                sender = sender,
                message = message,
                timestamp = timestamp,
                device = Build.MODEL,
                osVersion = Build.VERSION.SDK_INT
            )

            // Send to webhook with retry logic
            var success = false
            var attempts = 0
            val maxAttempts = settings.retryCount

            while (!success && attempts < maxAttempts) {
                attempts++
                Log.d(TAG, "Attempt $attempts of $maxAttempts")

                success = webhookService.sendSmsToWebhook(payload, settings)

                if (!success && attempts < maxAttempts) {
                    // Exponential backoff: wait before retry
                    val delayMs = (1000L * (1 shl (attempts - 1))).coerceAtMost(30000L)
                    Log.d(TAG, "Retrying in ${delayMs}ms...")
                    kotlinx.coroutines.delay(delayMs)
                }
            }

            if (success) {
                // Save last forward time
                settingsRepository.saveLastForwardTime(System.currentTimeMillis())
                Log.i(TAG, "SMS forwarded successfully")
                Result.success()
            } else {
                Log.e(TAG, "Failed to forward SMS after $attempts attempts")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in SmsForwardWorker", e)
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "SmsForwardWorker"
        const val KEY_SENDER = "sender"
        const val KEY_MESSAGE = "message"
        const val KEY_TIMESTAMP = "timestamp"
    }
}
