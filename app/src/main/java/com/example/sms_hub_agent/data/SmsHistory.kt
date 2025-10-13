package com.example.sms_hub_agent.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing an SMS message in the history database
 */
@Entity(tableName = "sms_history")
data class SmsHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val message: String,
    val timestamp: Long,
    val device: String,
    val osVersion: Int,
    val forwardedAt: Long,
    val status: String, // "success", "failed", "pending"
    val errorMessage: String? = null,
    val webhookUrl: String
)
