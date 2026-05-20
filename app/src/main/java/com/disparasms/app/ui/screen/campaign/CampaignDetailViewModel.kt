package com.disparasms.app.ui.screen.campaign

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.disparasms.app.data.local.entity.CampaignEntity
import com.disparasms.app.data.local.entity.CampaignLogEntity
import com.disparasms.app.data.local.entity.CampaignLogStatus
import com.disparasms.app.data.repository.CampaignRepository
import com.disparasms.app.sms.SimInfo
import com.disparasms.app.sms.SmsQueueManager
import com.disparasms.app.sms.SmsSender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CampaignDetailUiState(
    val campaign: CampaignEntity? = null,
    val logs: List<CampaignLogEntity> = emptyList(),
    val isSending: Boolean = false,
    val isLoading: Boolean = true,
    val simSlots: List<SimInfo> = emptyList()
)

@HiltViewModel
class CampaignDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val campaignRepository: CampaignRepository,
    private val smsQueueManager: SmsQueueManager,
    private val smsSender: SmsSender
) : ViewModel() {

    private val campaignId: Long = savedStateHandle["campaignId"] ?: -1L

    val uiState: StateFlow<CampaignDetailUiState> = combine(
        campaignRepository.observeById(campaignId).catch { emit(null) },
        campaignRepository.observeLogsByCampaign(campaignId).catch { emit(emptyList()) },
        smsQueueManager.progress.catch { emit(null) }
    ) { campaign, logs, progress ->
        CampaignDetailUiState(
            campaign = campaign,
            logs = logs,
            isSending = progress?.isRunning == true && progress.campaignId == campaignId,
            isLoading = campaign == null,
            simSlots = smsSender.getAvailableSimSlots()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CampaignDetailUiState())

    fun updateSimSlot(slot: Int) {
        viewModelScope.launch {
            val campaign = uiState.value.campaign ?: return@launch
            campaignRepository.update(campaign.copy(simSlot = slot))
        }
    }

    fun startSending() {
        viewModelScope.launch {
            val state = uiState.value
            val campaign = state.campaign ?: return@launch
            val logs = campaignRepository.getLogsByCampaignAndStatus(
                campaignId, CampaignLogStatus.PENDING
            )
            smsQueueManager.startSending(campaignId, logs, campaign.simSlot, campaign.messagesPerInterval, campaign.intervalMs)
        }
    }

    fun pauseSending() {
        smsQueueManager.pauseSending()
    }

    fun resumeSending() {
        startSending()
    }

    fun updateMessage(newMessage: String) {
        viewModelScope.launch {
            val campaign = uiState.value.campaign ?: return@launch
            campaignRepository.update(campaign.copy(message = newMessage))
        }
    }
}
