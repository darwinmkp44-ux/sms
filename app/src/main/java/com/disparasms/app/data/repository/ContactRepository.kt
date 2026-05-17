package com.disparasms.app.data.repository

import com.disparasms.app.data.local.dao.ContactDao
import com.disparasms.app.data.local.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

class ContactRepository(private val contactDao: ContactDao) {

    fun observeByGroup(groupId: Long): Flow<List<ContactEntity>> =
        contactDao.observeByGroup(groupId)

    fun observeAll(): Flow<List<ContactEntity>> = contactDao.observeAll()

    fun observeByIds(ids: List<Long>): Flow<List<ContactEntity>> =
        contactDao.observeByIds(ids)

    suspend fun getById(id: Long): ContactEntity? = contactDao.getById(id)

    suspend fun getByGroup(groupId: Long): List<ContactEntity> =
        contactDao.getByGroup(groupId)

    suspend fun getByIds(ids: List<Long>): List<ContactEntity> =
        contactDao.getByIds(ids)

    fun search(query: String): Flow<List<ContactEntity>> = contactDao.search(query)

    suspend fun insert(contact: ContactEntity): Long = contactDao.insert(contact)

    suspend fun insertAll(contacts: List<ContactEntity>): List<Long> =
        contactDao.insertAll(contacts)

    suspend fun update(contact: ContactEntity) = contactDao.update(contact)

    suspend fun delete(contact: ContactEntity) = contactDao.delete(contact)

    suspend fun deleteById(id: Long) = contactDao.deleteById(id)

    suspend fun deleteByGroup(groupId: Long) = contactDao.deleteByGroup(groupId)

    fun observeCountByGroup(groupId: Long): Flow<Int> =
        contactDao.observeCountByGroup(groupId)

    fun observeTotalCount(): Flow<Int> = contactDao.observeTotalCount()

    fun observeFavorites(): Flow<List<ContactEntity>> = contactDao.observeFavorites()

    suspend fun existsByPhone(phone: String, groupId: Long?): Boolean =
        contactDao.existsByPhone(phone, groupId)

    suspend fun importContacts(contacts: List<ContactEntity>): Pair<Int, Int> {
        var imported = 0
        var skipped = 0
        contacts.forEach { contact ->
            val id = contactDao.insert(contact)
            if (id > 0) imported++ else skipped++
        }
        return Pair(imported, skipped)
    }
}
