package com.disparasms.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.disparasms.app.data.local.entity.CampaignEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignDao {

    @Query("SELECT * FROM campaigns ORDER BY created_at DESC")
    fun observeAll(): Flow<List<CampaignEntity>>

    @Query("SELECT * FROM campaigns WHERE id = :id")
    suspend fun getById(id: Long): CampaignEntity?

    @Query("SELECT * FROM campaigns WHERE id = :id")
    fun observeById(id: Long): Flow<CampaignEntity?>

    @Query("SELECT * FROM campaigns WHERE name LIKE '%' || :query || '%' ORDER BY created_at DESC")
    fun search(query: String): Flow<List<CampaignEntity>>

    @Query("SELECT * FROM campaigns WHERE status = :status ORDER BY created_at DESC")
    fun observeByStatus(status: String): Flow<List<CampaignEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(campaign: CampaignEntity): Long

    @Update
    suspend fun update(campaign: CampaignEntity)

    @Delete
    suspend fun delete(campaign: CampaignEntity)

    @Query("DELETE FROM campaigns WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE campaigns SET status = :status, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE campaigns SET sent_count = :sent, delivered_count = :delivered, failed_count = :failed, pending_count = :pending, status = :status, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateProgress(
        id: Long,
        sent: Int,
        delivered: Int,
        failed: Int,
        pending: Int,
        status: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT COUNT(*) FROM campaigns")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM campaigns WHERE status = :status")
    fun observeCountByStatus(status: String): Flow<Int>

    @Query("SELECT SUM(sent_count) FROM campaigns")
    fun observeTotalSent(): Flow<Long?>

    @Query("SELECT SUM(delivered_count) FROM campaigns")
    fun observeTotalDelivered(): Flow<Long?>

    @Query("SELECT SUM(failed_count) FROM campaigns")
    fun observeTotalFailed(): Flow<Long?>

    @Query("DELETE FROM campaigns")
    suspend fun deleteAll()
}
