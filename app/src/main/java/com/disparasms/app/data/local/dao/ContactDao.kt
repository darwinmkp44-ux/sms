package com.disparasms.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.disparasms.app.data.local.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    @Query("SELECT * FROM contacts WHERE group_id = :groupId ORDER BY full_name ASC")
    fun observeByGroup(groupId: Long): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE group_id IS NULL ORDER BY full_name ASC")
    fun observeUngrouped(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts ORDER BY full_name ASC")
    fun observeAll(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id IN (:ids)")
    fun observeByIds(ids: List<Long>): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getById(id: Long): ContactEntity?

    @Query("SELECT * FROM contacts WHERE group_id = :groupId")
    suspend fun getByGroup(groupId: Long): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE id IN (:contactIds)")
    suspend fun getByIds(contactIds: List<Long>): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE phone LIKE '%' || :query || '%' OR full_name LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(contact: ContactEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(contacts: List<ContactEntity>): List<Long>

    @Update
    suspend fun update(contact: ContactEntity)

    @Delete
    suspend fun delete(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM contacts WHERE group_id = :groupId")
    suspend fun deleteByGroup(groupId: Long)

    @Query("SELECT COUNT(*) FROM contacts WHERE group_id = :groupId")
    fun observeCountByGroup(groupId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM contacts")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM contacts WHERE phone = :phone AND (group_id = :groupId OR (:groupId IS NULL AND group_id IS NULL)))")
    suspend fun existsByPhone(phone: String, groupId: Long?): Boolean

    @Query("SELECT * FROM contacts WHERE is_favorite = 1 ORDER BY full_name ASC")
    fun observeFavorites(): Flow<List<ContactEntity>>
}
