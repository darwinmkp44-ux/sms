package com.disparasms.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contacts",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["group_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["group_id"]),
        Index(value = ["phone"]),
        Index(value = ["phone", "group_id"], unique = true)
    ]
)
data class ContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "group_id")
    val groupId: Long? = null,
    val phone: String,
    @ColumnInfo(name = "first_name")
    val firstName: String? = null,
    @ColumnInfo(name = "last_name")
    val lastName: String? = null,
    @ColumnInfo(name = "full_name")
    val fullName: String? = null,
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,
    @ColumnInfo(name = "imported_from")
    val importedFrom: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
