package com.disparasms.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.disparasms.app.data.local.converter.Converters
import com.disparasms.app.data.local.dao.CampaignDao
import com.disparasms.app.data.local.dao.CampaignLogDao
import com.disparasms.app.data.local.dao.ContactDao
import com.disparasms.app.data.local.dao.GroupDao
import com.disparasms.app.data.local.entity.CampaignEntity
import com.disparasms.app.data.local.entity.CampaignLogEntity
import com.disparasms.app.data.local.entity.ContactEntity
import com.disparasms.app.data.local.entity.GroupEntity

@Database(
    entities = [
        GroupEntity::class,
        ContactEntity::class,
        CampaignEntity::class,
        CampaignLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
    abstract fun contactDao(): ContactDao
    abstract fun campaignDao(): CampaignDao
    abstract fun campaignLogDao(): CampaignLogDao
}
