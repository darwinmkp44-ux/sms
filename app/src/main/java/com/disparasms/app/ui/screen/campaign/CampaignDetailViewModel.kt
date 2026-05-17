package com.disparasms.app.ui.screen.campaign

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.disparasms.app.data.local.entity.CampaignEntity
import com.disparasms.app.data.local.entity.CampaignLogEntity
import com.disparasms.app.data.repository.CampaignRepository
import com.disparasms.app.sms.SmsQueueManager
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
    val isLoading: Boolean = true
)

@HiltViewModel
class CampaignDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val campaignRepository: CampaignRepository,
    private val smsQueueManager: SmsQueueManager
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
            isLoading = campaign == null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CampaignDetailUiState())

    fun startSending() {
        viewModelScope.launch {
            val state = uiState.value
            val campaign = state.campaign ?: return@launch
            val logs = campaignRepository.getLogsByCampaignAndStatus(
                campaignId, com.disparasms.app.data.local.entity.CampaignLogStatus.PENDING
            )
            smsQueueManager.startSending(campaignId, logs, campaign.simSlot, campaign.delayMs)
        }
    }

    fun pauseSending() {
        smsQueueManager.pauseSending()
    }

    fun resumeSending() {
        startSending()
    }
}
