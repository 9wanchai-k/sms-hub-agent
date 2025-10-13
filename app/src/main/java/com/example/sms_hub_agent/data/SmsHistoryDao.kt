package com.example.sms_hub_agent.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for SMS history
 */
@Dao
interface SmsHistoryDao {

    @Insert
    suspend fun insert(smsHistory: SmsHistory): Long

    @Query("SELECT * FROM sms_history ORDER BY forwardedAt DESC")
    fun getAllHistory(): Flow<List<SmsHistory>>

    @Query("SELECT * FROM sms_history ORDER BY forwardedAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 50): Flow<List<SmsHistory>>

    @Query("SELECT * FROM sms_history WHERE status = :status ORDER BY forwardedAt DESC")
    fun getHistoryByStatus(status: String): Flow<List<SmsHistory>>

    @Query("SELECT COUNT(*) FROM sms_history WHERE status = 'success'")
    fun getSuccessCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sms_history WHERE status = 'failed'")
    fun getFailedCount(): Flow<Int>

    @Query("DELETE FROM sms_history WHERE forwardedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    @Query("DELETE FROM sms_history")
    suspend fun deleteAll()
}
