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

    suspend fun assignToGroup(contactIds: List<Long>, groupId: Long) {
        contactDao.assignToGroup(contactIds, groupId)
    }

    suspend fun removeFromGroup(contactIds: List<Long>) {
        contactDao.removeFromGroup(contactIds)
    }

    suspend fun getPhonesWithoutGroup(): Set<String> =
        contactDao.getPhonesWithoutGroup().toSet()

    suspend fun importContacts(contacts: List<ContactEntity>): Pair<Int, Int> {
        if (contacts.isEmpty()) return Pair(0, 0)
        val existingPhones = contactDao.getPhonesWithoutGroup().toSet()
        return importContactsWithExisting(contacts, existingPhones)
    }

    suspend fun importContactsWithExisting(
        contacts: List<ContactEntity>,
        existingPhones: Set<String>
    ): Pair<Int, Int> {
        if (contacts.isEmpty()) return Pair(0, 0)
        val newContacts = contacts.filter { it.groupId != null || it.phone !in existingPhones }
        if (newContacts.isEmpty()) return Pair(0, contacts.size)
        val ids = contactDao.insertAll(newContacts)
        val imported = ids.count { it > 0 }
        val skipped = contacts.size - imported
        return Pair(imported, skipped)
    }
}
