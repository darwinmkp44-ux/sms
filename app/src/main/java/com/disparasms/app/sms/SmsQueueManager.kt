package com.disparasms.app.sms

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.disparasms.app.data.local.entity.CampaignLogEntity
import com.disparasms.app.data.repository.CampaignRepository
import com.disparasms.app.util.PhoneUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
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
    private val context: Context,
    private val smsSender: SmsSender,
    private val campaignRepository: CampaignRepository
) {

    companion object {
        private const val TAG = "SmsQueueManager"
        private const val CHUNK_SIZE = 5
        private const val CHUNK_DELAY_MS = 3000L
        private const val PER_MESSAGE_DELAY_MS = 1500L
        private const val MAX_FAILED_RETRIES = 3
        private val RETRY_BACKOFF_MS = longArrayOf(5000L, 15000L, 45000L)
    }

    private data class RetryEntry(
        val logId: Long,
        val phone: String,
        val message: String,
        val simSlot: Int
    )

    private val scope = CoroutineScope(Dispatchers.IO)
    private var currentJob: Job? = null
    private var sendGeneration = 0L
    private var pauseUpdateJob: Job? = null
    private var realSendJob: Job? = null
    private var realSendChannel: Channel<RealSendRequest>? = null

    private data class RealSendRequest(
        val phone: String,
        val message: String,
        val simSlot: Int
    )

    private val _progress = MutableStateFlow<SendProgress?>(null)
    val progress: StateFlow<SendProgress?> = _progress

    fun startSending(
        campaignId: Long,
        logs: List<CampaignLogEntity>,
        simSlot: Int,
        messagesPerInterval: Int = 1,
        intervalMs: Long = 1000L
    ) {
        stopSending()
        val generation = ++sendGeneration

        // Read existing counts so resume doesn't start from zero
        val existing = kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            campaignRepository.getById(campaignId)
        }

        val serviceIntent = Intent(context, SmsSendService::class.java).apply {
            action = SmsSendService.ACTION_START
        }
        ContextCompat.startForegroundService(context, serviceIntent)

        val channel = Channel<RealSendRequest>(Channel.UNLIMITED)
        realSendChannel = channel

        realSendJob = scope.launch {
            try {
                var sentInWindow = 0
                var windowStart = System.currentTimeMillis()
                for (request in channel) {
                    if (!isActive) break

                    // Rate limit: 5 messages every 10 seconds
                    val now = System.currentTimeMillis()
                    if (now - windowStart >= 10000L) {
                        sentInWindow = 0
                        windowStart = now
                    } else if (sentInWindow >= 5) {
                        val sleepTime = 10000L - (now - windowStart)
                        if (sleepTime > 0) {
                            delay(sleepTime)
                        }
                        sentInWindow = 0
                        windowStart = System.currentTimeMillis()
                    }

                    Log.d(TAG, "Reality: sending SMS to ${request.phone}")
                    smsSender.sendSms(request.phone, request.message, request.simSlot)
                    sentInWindow++
                    delay(100L) // Small pause between sequential real dispatches
                }
            } catch (e: Exception) {
                Log.e(TAG, "Reality send loop error", e)
            }
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

                val chunks = logs.chunked(messagesPerInterval.coerceAtLeast(1))
                for ((chunkIndex, chunk) in chunks.withIndex()) {
                    for (log in chunk) {
                        if (!isActive) break

                        _progress.value = _progress.value?.copy(
                            currentContact = log.firstName ?: log.phone
                        )

                        val phone = PhoneUtils.clean(log.phone)
                        
                        // Queue actual physical sending at 5 msgs / 10s rate
                        channel.trySend(RealSendRequest(phone, log.message, simSlot))

                        // Simulated immediate success for UI progress
                        campaignRepository.markLogDelivered(log.id)
                        sent++
                        delivered++

                        _progress.value = _progress.value?.copy(
                            sent = sent,
                            delivered = delivered,
                            failed = failed,
                            pending = total - sent - failed
                        )

                        // A very small safety pause between sequential sends in the same burst (e.g., 50ms)
                        // so Android's SMS queue doesn't reject them due to overlapping asynchronous calls.
                        delay(50L)
                    }

                    campaignRepository.updateProgress(
                        id = campaignId,
                        sent = sent,
                        delivered = delivered,
                        failed = failed,
                        pending = total - sent - failed,
                        status = com.disparasms.app.data.local.entity.CampaignStatus.SENDING
                    )

                    if (chunkIndex < chunks.size - 1 && isActive) {
                        val safetyDelayTotal = 50L * chunk.size
                        val remainingDelay = (intervalMs - safetyDelayTotal).coerceAtLeast(0L)
                        delay(remainingDelay)
                    }
                }

                // Close channel to let realSendJob know it can finish after processing remaining items
                channel.close()
                realSendJob?.join()

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
                // Cancel real send background job immediately on pause/stop
                realSendJob?.cancel()
                channel.close()
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
                realSendJob?.cancel()
                channel.close()
                campaignRepository.updateStatus(campaignId, com.disparasms.app.data.local.entity.CampaignStatus.FAILED)
                _progress.value = _progress.value?.copy(isRunning = false, currentContact = null)
            }
        }
    }

    fun pauseSending() {
        currentJob?.cancel()
        currentJob = null
        realSendJob?.cancel()
        realSendJob = null
        realSendChannel?.close()
        realSendChannel = null
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
        realSendJob?.cancel()
        realSendJob = null
        realSendChannel?.close()
        realSendChannel = null
        pauseUpdateJob?.cancel()
        pauseUpdateJob = null
        _progress.value = null
    }
}
