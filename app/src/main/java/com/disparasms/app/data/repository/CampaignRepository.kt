package com.disparasms.app.data.repository

import com.disparasms.app.data.local.dao.CampaignDao
import com.disparasms.app.data.local.dao.CampaignLogDao
import com.disparasms.app.data.local.entity.CampaignEntity
import com.disparasms.app.data.local.entity.CampaignLogEntity
import kotlinx.coroutines.flow.Flow

class CampaignRepository(
    private val campaignDao: CampaignDao,
    private val campaignLogDao: CampaignLogDao
) {

    fun observeAll(): Flow<List<CampaignEntity>> = campaignDao.observeAll()

    suspend fun getById(id: Long): CampaignEntity? = campaignDao.getById(id)

    fun observeById(id: Long): Flow<CampaignEntity?> = campaignDao.observeById(id)

    fun search(query: String): Flow<List<CampaignEntity>> = campaignDao.search(query)

    fun observeByStatus(status: String): Flow<List<CampaignEntity>> =
        campaignDao.observeByStatus(status)

    suspend fun create(
        name: String,
        message: String,
        groupIds: List<Long>,
        contactIds: List<Long>,
        totalContacts: Int,
        simSlot: Int,
        messagesPerInterval: Int,
        intervalMs: Long
    ): Long {
        val campaign = CampaignEntity(
            name = name,
            message = message,
            groupIds = groupIds.joinToString(",", "[", "]"),
            contactIds = contactIds.joinToString(",", "[", "]"),
            totalContacts = totalContacts,
            pendingCount = totalContacts,
            simSlot = simSlot,
            messagesPerInterval = messagesPerInterval,
            intervalMs = intervalMs
        )
        return campaignDao.insert(campaign)
    }

    suspend fun update(campaign: CampaignEntity) = campaignDao.update(campaign)

    suspend fun deleteById(id: Long) = campaignDao.deleteById(id)

    suspend fun updateStatus(id: Long, status: String) {
        campaignDao.updateStatus(id, status)
    }

    suspend fun updateProgress(
        id: Long,
        sent: Int,
        delivered: Int,
        failed: Int,
        pending: Int,
        status: String
    ) {
        campaignDao.updateProgress(id, sent, delivered, failed, pending, status)
    }

    fun observeCount(): Flow<Int> = campaignDao.observeCount()

    fun observeCountByStatus(status: String): Flow<Int> =
        campaignDao.observeCountByStatus(status)

    fun observeTotalSent(): Flow<Long?> = campaignDao.observeTotalSent()

    fun observeTotalDelivered(): Flow<Long?> = campaignDao.observeTotalDelivered()

    fun observeTotalFailed(): Flow<Long?> = campaignDao.observeTotalFailed()

    fun observeLogsByCampaign(campaignId: Long): Flow<List<CampaignLogEntity>> =
        campaignLogDao.observeByCampaign(campaignId)

    suspend fun getLogsByCampaign(campaignId: Long): List<CampaignLogEntity> =
        campaignLogDao.getByCampaign(campaignId)

    suspend fun getLogsByCampaignAndStatus(campaignId: Long, status: String): List<CampaignLogEntity> =
        campaignLogDao.getByCampaignAndStatus(campaignId, status)

    suspend fun insertLogs(logs: List<CampaignLogEntity>) =
        campaignLogDao.insertAll(logs)

    suspend fun insertLog(log: CampaignLogEntity): Long =
        campaignLogDao.insert(log)

    suspend fun markLogSent(id: Long) =
        campaignLogDao.markSent(id, com.disparasms.app.data.local.entity.CampaignLogStatus.SENT)

    suspend fun markLogFailed(id: Long, error: String?) =
        campaignLogDao.markFailed(id, com.disparasms.app.data.local.entity.CampaignLogStatus.FAILED, error)

    suspend fun markLogDelivered(id: Long) =
        campaignLogDao.markDelivered(id, com.disparasms.app.data.local.entity.CampaignLogStatus.DELIVERED)

    fun observeLogCountByStatus(campaignId: Long, status: String): Flow<Int> =
        campaignLogDao.observeCountByCampaignAndStatus(campaignId, status)
}
