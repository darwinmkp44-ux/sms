package com.disparasms.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "campaigns")
data class CampaignEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val message: String,
    @ColumnInfo(name = "group_ids")
    val groupIds: String = "[]",
    @ColumnInfo(name = "contact_ids")
    val contactIds: String = "[]",
    @ColumnInfo(name = "total_contacts")
    val totalContacts: Int = 0,
    @ColumnInfo(name = "sent_count")
    val sentCount: Int = 0,
    @ColumnInfo(name = "delivered_count")
    val deliveredCount: Int = 0,
    @ColumnInfo(name = "failed_count")
    val failedCount: Int = 0,
    @ColumnInfo(name = "pending_count")
    val pendingCount: Int = 0,
    val status: String = CampaignStatus.PENDING,
    @ColumnInfo(name = "sim_slot")
    val simSlot: Int = 0,
    @ColumnInfo(name = "delay_ms")
    val delayMs: Long = 1000L,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null
)

object CampaignStatus {
    const val PENDING = "PENDING"
    const val SENDING = "SENDING"
    const val PAUSED = "PAUSED"
    const val COMPLETED = "COMPLETED"
    const val FAILED = "FAILED"
}
