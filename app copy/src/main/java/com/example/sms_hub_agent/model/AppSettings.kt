package com.example.sms_hub_agent.model

/**
 * Data class representing app configuration settings
 */
data class AppSettings(
    val webhookUrl: String = "",
    val authUsername: String = "",
    val authPassword: String = "",
    val hmacEnabled: Boolean = false,
    val hmacSecret: String = "",
    val retryCount: Int = 3,
    val timeoutSeconds: Int = 30
) {
    /**
     * Validates if the settings are complete enough to send webhook requests
     */
    fun isValid(): Boolean {
        return webhookUrl.isNotBlank() &&
                webhookUrl.startsWith("http", ignoreCase = true) &&
                authUsername.isNotBlank() &&
                authPassword.isNotBlank() &&
                (!hmacEnabled || hmacSecret.isNotBlank())
    }
}
