package com.example.sms_hub_agent.model

import kotlinx.serialization.Serializable

/**
 * Data class representing the SMS payload sent to webhook
 */
@Serializable
data class SmsPayload(
    val type: String = "sms",
    val sender: String,
    val message: String,
    val timestamp: Long,
    val device: String,
    val osVersion: Int
)
