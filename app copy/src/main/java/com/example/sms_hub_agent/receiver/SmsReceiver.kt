package com.example.sms_hub_agent.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.sms_hub_agent.worker.SmsForwardWorker

/**
 * BroadcastReceiver that listens for incoming SMS messages
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        Log.d(TAG, "SMS received, processing...")

        try {
            // Extract SMS messages from intent
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            if (messages.isEmpty()) {
                Log.w(TAG, "No messages found in intent")
                return
            }

            // Process each SMS message
            for (smsMessage in messages) {
                val sender = smsMessage.displayOriginatingAddress ?: ""
                val messageBody = smsMessage.messageBody ?: ""
                val timestamp = smsMessage.timestampMillis

                if (sender.isBlank() || messageBody.isBlank()) {
                    Log.w(TAG, "Skipping invalid SMS: sender=$sender, message=$messageBody")
                    continue
                }

                Log.i(TAG, "SMS from $sender: ${messageBody.take(50)}...")

                // Enqueue WorkManager task to forward SMS
                enqueueForwardWork(context, sender, messageBody, timestamp)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS", e)
        }
    }

    /**
     * Enqueue a WorkManager task to forward the SMS to webhook
     */
    private fun enqueueForwardWork(
        context: Context,
        sender: String,
        message: String,
        timestamp: Long
    ) {
        // Build work data
        val inputData = Data.Builder()
            .putString(SmsForwardWorker.KEY_SENDER, sender)
            .putString(SmsForwardWorker.KEY_MESSAGE, message)
            .putLong(SmsForwardWorker.KEY_TIMESTAMP, timestamp)
            .build()

        // Create work request
        val workRequest = OneTimeWorkRequestBuilder<SmsForwardWorker>()
            .setInputData(inputData)
            .build()

        // Enqueue work
        WorkManager.getInstance(context).enqueue(workRequest)
        Log.d(TAG, "Enqueued WorkManager task: ${workRequest.id}")
    }

    companion object {
        private const val TAG = "SmsReceiver"
    }
}
