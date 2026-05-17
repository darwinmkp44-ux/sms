package com.disparasms.app.sms

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.disparasms.app.data.repository.CampaignRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SmsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val campaignRepository: CampaignRepository,
    private val smsSender: SmsSender
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val campaignId = inputData.getLong("campaign_id", -1L)
        if (campaignId == -1L) return Result.failure()

        return try {
            val campaign = campaignRepository.getById(campaignId) ?: return Result.failure()
            val logs = campaignRepository.getLogsByCampaignAndStatus(
                campaignId, com.disparasms.app.data.local.entity.CampaignLogStatus.PENDING
            )

            campaignRepository.updateStatus(campaignId, com.disparasms.app.data.local.entity.CampaignStatus.SENDING)

            for (log in logs) {
                if (isStopped) break
                val result = smsSender.sendSms(log.phone, log.message, campaign.simSlot)
                if (result.success) {
                    campaignRepository.markLogSent(log.id)
                } else {
                    campaignRepository.markLogFailed(log.id, result.error)
                }
            }

            campaignRepository.updateStatus(campaignId, com.disparasms.app.data.local.entity.CampaignStatus.COMPLETED)
            Result.success()
        } catch (e: Exception) {
            campaignRepository.updateStatus(campaignId, com.disparasms.app.data.local.entity.CampaignStatus.FAILED)
            Result.retry()
        }
    }
}
