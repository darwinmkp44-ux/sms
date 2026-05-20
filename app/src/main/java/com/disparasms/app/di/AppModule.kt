package com.disparasms.app.di

import android.content.Context
import androidx.room.Room
import com.disparasms.app.data.local.AppDatabase
import com.disparasms.app.data.local.dao.CampaignDao
import com.disparasms.app.data.local.dao.CampaignLogDao
import com.disparasms.app.data.local.dao.ContactDao
import com.disparasms.app.data.local.dao.GroupDao
import com.disparasms.app.data.repository.CampaignRepository
import com.disparasms.app.data.repository.ContactRepository
import com.disparasms.app.data.repository.GroupRepository
import com.disparasms.app.sms.SmsQueueManager
import com.disparasms.app.sms.SmsSender
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "disparasms.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides fun provideGroupDao(db: AppDatabase): GroupDao = db.groupDao()
    @Provides fun provideContactDao(db: AppDatabase): ContactDao = db.contactDao()
    @Provides fun provideCampaignDao(db: AppDatabase): CampaignDao = db.campaignDao()
    @Provides fun provideCampaignLogDao(db: AppDatabase): CampaignLogDao = db.campaignLogDao()

    @Provides
    @Singleton
    fun provideGroupRepository(groupDao: GroupDao): GroupRepository = GroupRepository(groupDao)

    @Provides
    @Singleton
    fun provideContactRepository(contactDao: ContactDao): ContactRepository = ContactRepository(contactDao)

    @Provides
    @Singleton
    fun provideCampaignRepository(
        campaignDao: CampaignDao,
        campaignLogDao: CampaignLogDao
    ): CampaignRepository = CampaignRepository(campaignDao, campaignLogDao)

    @Provides
    @Singleton
    fun provideSmsSender(@ApplicationContext context: Context): SmsSender = SmsSender(context)

    @Provides
    @Singleton
    fun provideSmsQueueManager(
        smsSender: SmsSender,
        campaignRepository: CampaignRepository
    ): SmsQueueManager = SmsQueueManager(smsSender, campaignRepository)
}
