package com.disparasms.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.disparasms.app.data.local.entity.MessageTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageTemplateDao {
    @Query("SELECT * FROM message_templates ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MessageTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: MessageTemplateEntity): Long

    @Update
    suspend fun update(template: MessageTemplateEntity)

    @Delete
    suspend fun delete(template: MessageTemplateEntity)
}
