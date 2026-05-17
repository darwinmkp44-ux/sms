package com.disparasms.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "campaign_logs",
    foreignKeys = [
        ForeignKey(
            entity = CampaignEntity::class,
            parentColumns = ["id"],
            childColumns = ["campaign_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["campaign_id"]),
        Index(value = ["phone"]),
        Index(value = ["campaign_id", "phone"])
    ]
)
data class CampaignLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "campaign_id")
    val campaignId: Long,
    @ColumnInfo(name = "contact_id")
    val contactId: Long? = null,
    val phone: String,
    @ColumnInfo(name = "first_name")
    val firstName: String? = null,
    val message: String,
    val status: String = CampaignLogStatus.PENDING,
    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,
    @ColumnInfo(name = "sent_at")
    val sentAt: Long? = null,
    @ColumnInfo(name = "delivered_at")
    val deliveredAt: Long? = null
)

object CampaignLogStatus {
    const val PENDING = "PENDING"
    const val SENT = "SENT"
    const val DELIVERED = "DELIVERED"
    const val FAILED = "FAILED"
}
