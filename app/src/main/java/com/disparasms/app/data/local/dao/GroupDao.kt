package com.disparasms.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.disparasms.app.data.local.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    @Query("SELECT * FROM `groups` ORDER BY is_favorite DESC, name ASC")
    fun observeAll(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM `groups` ORDER BY is_favorite DESC, name ASC")
    suspend fun getAll(): List<GroupEntity>

    @Query("SELECT * FROM `groups` WHERE id = :id")
    suspend fun getById(id: Long): GroupEntity?

    @Query("SELECT * FROM `groups` WHERE id = :id")
    fun observeById(id: Long): Flow<GroupEntity?>

    @Query("SELECT * FROM `groups` WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun search(query: String): Flow<List<GroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: GroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(groups: List<GroupEntity>): List<Long>

    @Update
    suspend fun update(group: GroupEntity)

    @Delete
    suspend fun delete(group: GroupEntity)

    @Query("DELETE FROM `groups` WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM `groups`")
    fun observeCount(): Flow<Int>

    @Transaction
    suspend fun duplicateGroup(id: Long, newName: String): Long {
        val original = getById(id) ?: return -1
        val duplicate = original.copy(
            id = 0,
            name = newName,
            contactCount = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return insert(duplicate)
    }

    @Query("UPDATE `groups` SET contact_count = (SELECT COUNT(*) FROM contacts WHERE group_id = :groupId) WHERE id = :groupId")
    suspend fun refreshContactCount(groupId: Long)
}
