package com.disparasms.app.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.disparasms.app.data.local.entity.CampaignEntity
import com.disparasms.app.data.repository.CampaignRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CampaignHistoryUiState(
    val campaigns: List<CampaignEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CampaignHistoryViewModel @Inject constructor(
    private val campaignRepository: CampaignRepository
) : ViewModel() {

    val uiState: StateFlow<CampaignHistoryUiState> = campaignRepository.observeAll()
        .catch { emit(emptyList()) }
        .map { campaigns -> CampaignHistoryUiState(campaigns = campaigns, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CampaignHistoryUiState())

    fun deleteCampaign(campaignId: Long) {
        viewModelScope.launch { campaignRepository.deleteById(campaignId) }
    }
}
