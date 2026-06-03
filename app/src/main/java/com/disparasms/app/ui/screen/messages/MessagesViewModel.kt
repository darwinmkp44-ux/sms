package com.disparasms.app.ui.screen.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.disparasms.app.data.local.entity.CampaignLogStatus
import com.disparasms.app.data.local.entity.CampaignLogWithCampaign
import com.disparasms.app.data.repository.CampaignRepository
import com.disparasms.app.sms.SmsQueueManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MessagesUiState(
    val logs: List<CampaignLogWithCampaign> = emptyList(),
    val searchQuery: String = "",
    val selectedStatus: String = "TODAS",
    val isLoading: Boolean = true
)

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val campaignRepository: CampaignRepository,
    private val smsQueueManager: SmsQueueManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedStatus = MutableStateFlow("TODAS")
    val selectedStatus: StateFlow<String> = _selectedStatus

    val uiState: StateFlow<MessagesUiState> = combine(
        campaignRepository.observeAllLogsWithCampaign(),
        _searchQuery,
        _selectedStatus
    ) { logs, query, status ->
        val filteredLogs = logs.filter { log ->
            val matchesQuery = log.phone.contains(query, ignoreCase = true) ||
                    (log.firstName?.contains(query, ignoreCase = true) == true) ||
                    log.message.contains(query, ignoreCase = true) ||
                    log.campaignName.contains(query, ignoreCase = true)

            val matchesStatus = when (status) {
                "TODAS" -> true
                "ENVIADAS" -> log.status == CampaignLogStatus.SENT
                "FALHADAS" -> log.status == CampaignLogStatus.FAILED
                "ENTREGUES" -> log.status == CampaignLogStatus.DELIVERED
                else -> true
            }

            matchesQuery && matchesStatus
        }

        MessagesUiState(
            logs = filteredLogs,
            searchQuery = query,
            selectedStatus = status,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MessagesUiState())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedStatus(status: String) {
        _selectedStatus.value = status
    }

    fun retryMessage(log: CampaignLogWithCampaign) {
        viewModelScope.launch {
            campaignRepository.retryFailedLog(log.logId)
            val campaign = campaignRepository.getById(log.campaignId) ?: return@launch
            val pendingLogs = campaignRepository.getLogsByCampaignAndStatus(log.campaignId, CampaignLogStatus.PENDING)
            smsQueueManager.startSending(
                campaignId = log.campaignId,
                logs = pendingLogs,
                simSlot = campaign.simSlot,
                messagesPerInterval = campaign.messagesPerInterval,
                intervalMs = campaign.intervalMs
            )
        }
    }

    fun retryAllFailed() {
        viewModelScope.launch {
            val failedLogs = uiState.value.logs.filter { it.status == CampaignLogStatus.FAILED }
            val campaignIds = failedLogs.map { it.campaignId }.distinct()

            for (campaignId in campaignIds) {
                campaignRepository.retryAllFailedLogs(campaignId)
                val campaign = campaignRepository.getById(campaignId) ?: continue
                val pendingLogs = campaignRepository.getLogsByCampaignAndStatus(campaignId, CampaignLogStatus.PENDING)
                smsQueueManager.startSending(
                    campaignId = campaignId,
                    logs = pendingLogs,
                    simSlot = campaign.simSlot,
                    messagesPerInterval = campaign.messagesPerInterval,
                    intervalMs = campaign.intervalMs
                )
            }
        }
    }
}
