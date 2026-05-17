package com.disparasms.app.data.repository

import com.disparasms.app.data.local.dao.GroupDao
import com.disparasms.app.data.local.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

class GroupRepository(private val groupDao: GroupDao) {

    fun observeAll(): Flow<List<GroupEntity>> = groupDao.observeAll()

    suspend fun getAll(): List<GroupEntity> = groupDao.getAll()

    suspend fun getById(id: Long): GroupEntity? = groupDao.getById(id)

    fun observeById(id: Long): Flow<GroupEntity?> = groupDao.observeById(id)

    fun search(query: String): Flow<List<GroupEntity>> = groupDao.search(query)

    suspend fun insert(group: GroupEntity): Long = groupDao.insert(group)

    suspend fun update(group: GroupEntity) = groupDao.update(group)

    suspend fun delete(group: GroupEntity) = groupDao.delete(group)

    suspend fun deleteById(id: Long) = groupDao.deleteById(id)

    fun observeCount(): Flow<Int> = groupDao.observeCount()

    suspend fun duplicateGroup(id: Long, newName: String): Long {
        return groupDao.duplicateGroup(id, newName)
    }

    suspend fun createWithContactCount(name: String, description: String?) {
        val group = GroupEntity(
            name = name,
            description = description,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        groupDao.insert(group)
    }

    suspend fun refreshContactCount(groupId: Long) {
        groupDao.refreshContactCount(groupId)
    }
}
