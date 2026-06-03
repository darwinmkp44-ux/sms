package com.disparasms.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.disparasms.app.data.local.entity.CampaignLogEntity
import com.disparasms.app.data.local.entity.CampaignLogWithCampaign
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignLogDao {

    @Query("SELECT * FROM campaign_logs WHERE campaign_id = :campaignId ORDER BY id ASC")
    fun observeByCampaign(campaignId: Long): Flow<List<CampaignLogEntity>>

    @Query("SELECT * FROM campaign_logs WHERE campaign_id = :campaignId AND status = :status ORDER BY id ASC")
    fun observeByCampaignAndStatus(campaignId: Long, status: String): Flow<List<CampaignLogEntity>>

    @Query("SELECT * FROM campaign_logs WHERE campaign_id = :campaignId")
    suspend fun getByCampaign(campaignId: Long): List<CampaignLogEntity>

    @Query("SELECT * FROM campaign_logs WHERE campaign_id = :campaignId AND status = :status")
    suspend fun getByCampaignAndStatus(campaignId: Long, status: String): List<CampaignLogEntity>

    @Query("SELECT * FROM campaign_logs WHERE id = :id")
    suspend fun getById(id: Long): CampaignLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<CampaignLogEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: CampaignLogEntity): Long

    @Query("UPDATE campaign_logs SET status = :status, sent_at = :sentAt WHERE id = :id")
    suspend fun markSent(id: Long, status: String, sentAt: Long = System.currentTimeMillis())

    @Query("UPDATE campaign_logs SET status = :status, error_message = :error WHERE id = :id")
    suspend fun markFailed(id: Long, status: String, error: String?)

    @Query("UPDATE campaign_logs SET status = :status, error_message = :error WHERE id = :id")
    suspend fun updateStatusAndError(id: Long, status: String, error: String?)

    @Query("UPDATE campaign_logs SET status = 'PENDING', error_message = NULL WHERE campaign_id = :campaignId AND status = 'FAILED'")
    suspend fun resetFailedLogsToPending(campaignId: Long)

    @Query("""
        SELECT 
            l.id as logId, 
            l.campaign_id as campaignId, 
            c.name as campaignName, 
            l.phone as phone, 
            l.first_name as firstName, 
            l.message as message, 
            l.status as status, 
            l.error_message as errorMessage, 
            l.sent_at as sentAt, 
            l.delivered_at as deliveredAt
        FROM campaign_logs l
        INNER JOIN campaigns c ON l.campaign_id = c.id
        ORDER BY l.id DESC
    """)
    fun observeAllWithCampaign(): Flow<List<CampaignLogWithCampaign>>

    @Query("UPDATE campaign_logs SET status = :status, delivered_at = :deliveredAt WHERE id = :id")
    suspend fun markDelivered(id: Long, status: String, deliveredAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM campaign_logs WHERE campaign_id = :campaignId")
    suspend fun countByCampaign(campaignId: Long): Int

    @Query("SELECT COUNT(*) FROM campaign_logs WHERE campaign_id = :campaignId AND status = :status")
    suspend fun countByCampaignAndStatus(campaignId: Long, status: String): Int

    @Query("SELECT COUNT(*) FROM campaign_logs WHERE campaign_id = :campaignId AND status = :status")
    fun observeCountByCampaignAndStatus(campaignId: Long, status: String): Flow<Int>

    @Query("DELETE FROM campaign_logs WHERE campaign_id = :campaignId")
    suspend fun deleteByCampaign(campaignId: Long)

    @Query("DELETE FROM campaign_logs WHERE campaign_id IN (SELECT id FROM campaigns WHERE status = :status)")
    suspend fun deleteByCampaignStatus(status: String)
}
