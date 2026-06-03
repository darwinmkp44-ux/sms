package com.disparasms.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_templates")
data class MessageTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
