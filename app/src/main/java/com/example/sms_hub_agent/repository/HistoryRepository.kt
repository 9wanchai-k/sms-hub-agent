package com.example.sms_hub_agent.repository

import com.example.sms_hub_agent.data.SmsHistory
import com.example.sms_hub_agent.data.SmsHistoryDao
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing SMS history
 */
class HistoryRepository(private val smsHistoryDao: SmsHistoryDao) {

    fun getAllHistory(): Flow<List<SmsHistory>> {
        return smsHistoryDao.getAllHistory()
    }

    fun getRecentHistory(limit: Int = 50): Flow<List<SmsHistory>> {
        return smsHistoryDao.getRecentHistory(limit)
    }

    fun getHistoryByStatus(status: String): Flow<List<SmsHistory>> {
        return smsHistoryDao.getHistoryByStatus(status)
    }

    fun getSuccessCount(): Flow<Int> {
        return smsHistoryDao.getSuccessCount()
    }

    fun getFailedCount(): Flow<Int> {
        return smsHistoryDao.getFailedCount()
    }

    suspend fun insert(smsHistory: SmsHistory): Long {
        return smsHistoryDao.insert(smsHistory)
    }

    suspend fun deleteOlderThan(timestamp: Long) {
        smsHistoryDao.deleteOlderThan(timestamp)
    }

    suspend fun deleteAll() {
        smsHistoryDao.deleteAll()
    }
}
