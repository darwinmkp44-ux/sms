package com.disparasms.app.data.repository

import com.disparasms.app.data.local.dao.MessageTemplateDao
import com.disparasms.app.data.local.entity.MessageTemplateEntity
import kotlinx.coroutines.flow.Flow

class MessageTemplateRepository(private val messageTemplateDao: MessageTemplateDao) {
    fun observeAll(): Flow<List<MessageTemplateEntity>> = messageTemplateDao.observeAll()
    suspend fun create(title: String, content: String): Long =
        messageTemplateDao.insert(MessageTemplateEntity(title = title, content = content))
    suspend fun update(template: MessageTemplateEntity) = messageTemplateDao.update(template)
    suspend fun delete(template: MessageTemplateEntity) = messageTemplateDao.delete(template)
}
