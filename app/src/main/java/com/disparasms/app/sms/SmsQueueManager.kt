package com.disparasms.app.sms

import android.util.Log
import com.disparasms.app.data.local.entity.CampaignLogEntity
import com.disparasms.app.data.repository.CampaignRepository
import com.disparasms.app.util.PhoneUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SendProgress(
    val campaignId: Long,
    val total: Int = 0,
    val sent: Int = 0,
    val delivered: Int = 0,
    val failed: Int = 0,
    val pending: Int = 0,
    val isRunning: Boolean = false,
    val currentContact: String? = null
)

class SmsQueueManager(
    private val smsSender: SmsSender,
    private val campaignRepository: CampaignRepository
) {

    companion object {
        private const val TAG = "SmsQueueManager"
        private const val CHUNK_SIZE = 5
        private const val CHUNK_DELAY_MS = 3000L
        private const val PER_MESSAGE_DELAY_MS = 1500L
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var currentJob: Job? = null
    private var sendGeneration = 0L
    private var pauseUpdateJob: Job? = null

    private val _progress = MutableStateFlow<SendProgress?>(null)
    val progress: StateFlow<SendProgress?> = _progress

    fun startSending(
        campaignId: Long,
        logs: List<CampaignLogEntity>,
        simSlot: Int,
        customDelayMs: Long = PER_MESSAGE_DELAY_MS
    ) {
        stopSending()
        val generation = ++sendGeneration

        // Read existing counts so resume doesn't start from zero
        val existing = kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            campaignRepository.getById(campaignId)
        }

        currentJob = scope.launch {
            try {
                val total = logs.size
                var sent = existing?.sentCount ?: 0
                var delivered = existing?.deliveredCount ?: 0
                var failed = existing?.failedCount ?: 0

                _progress.value = SendProgress(
                    campaignId = campaignId,
                    total = total + sent + failed,
                    pending = total,
                    sent = sent,
                    delivered = delivered,
                    failed = failed,
                    isRunning = true
                )

                campaignRepository.updateStatus(campaignId, com.disparasms.app.data.local.entity.CampaignStatus.SENDING)

                val chunks = logs.chunked(CHUNK_SIZE)
                for ((chunkIndex, chunk) in chunks.withIndex()) {
                    for (log in chunk) {
                        if (!isActive) break

                        _progress.value = _progress.value?.copy(
                            currentContact = log.firstName ?: log.phone
                        )

                        val phone = PhoneUtils.clean(log.phone)
                        val result = smsSender.sendSms(phone, log.message, simSlot)

                        if (result.success) {
                            campaignRepository.markLogDelivered(log.id)
                            sent++
                            delivered++
                        } else {
                            campaignRepository.markLogFailed(log.id, result.error)
                            failed++
                            Log.w(TAG, "Failed to send to ${log.phone}: ${result.error}")
                        }

                        _progress.value = _progress.value?.copy(
                            sent = sent,
                            delivered = delivered,
                            failed = failed,
                            pending = total - sent - failed
                        )

                        delay(customDelayMs)
                    }

                    campaignRepository.updateProgress(
                        id = campaignId,
                        sent = sent,
                        delivered = delivered,
                        failed = failed,
                        pending = total - sent - failed,
                        status = com.disparasms.app.data.local.entity.CampaignStatus.SENDING
                    )

                    if (chunkIndex < chunks.size - 1) {
                        delay(CHUNK_DELAY_MS)
                    }
                }

                val finalStatus = if (failed > 0) {
                    if (sent > 0) com.disparasms.app.data.local.entity.CampaignStatus.COMPLETED
                    else com.disparasms.app.data.local.entity.CampaignStatus.FAILED
                } else {
                    com.disparasms.app.data.local.entity.CampaignStatus.COMPLETED
                }

                campaignRepository.updateProgress(
                    id = campaignId,
                    sent = sent,
                    delivered = delivered,
                    failed = failed,
                    pending = 0,
                    status = finalStatus
                )

                _progress.value = _progress.value?.copy(
                    isRunning = false,
                    currentContact = null
                )

            } catch (e: CancellationException) {
                val current = _progress.value
                if (current != null && generation == sendGeneration) {
                    campaignRepository.updateProgress(
                        id = campaignId,
                        sent = current.sent,
                        delivered = current.delivered,
                        failed = current.failed,
                        pending = current.pending,
                        status = com.disparasms.app.data.local.entity.CampaignStatus.PAUSED
                    )
                }
                _progress.value = _progress.value?.copy(isRunning = false, currentContact = null)
            } catch (e: Exception) {
                Log.e(TAG, "Sending error", e)
                campaignRepository.updateStatus(campaignId, com.disparasms.app.data.local.entity.CampaignStatus.FAILED)
                _progress.value = _progress.value?.copy(isRunning = false, currentContact = null)
            }
        }
    }

    fun pauseSending() {
        currentJob?.cancel()
        currentJob = null
        pauseUpdateJob?.cancel()
        val progress = _progress.value ?: return
        pauseUpdateJob = scope.launch {
            campaignRepository.updateProgress(
                id = progress.campaignId,
                sent = progress.sent,
                delivered = progress.delivered,
                failed = progress.failed,
                pending = progress.pending,
                status = com.disparasms.app.data.local.entity.CampaignStatus.PAUSED
            )
            _progress.value = _progress.value?.copy(isRunning = false, currentContact = null)
        }
    }

    fun stopSending() {
        currentJob?.cancel()
        currentJob = null
        pauseUpdateJob?.cancel()
        pauseUpdateJob = null
        _progress.value = null
    }
}
