package com.disparasms.app.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.disparasms.app.data.local.entity.CampaignEntity
import com.disparasms.app.data.repository.CampaignRepository
import com.disparasms.app.data.repository.ContactRepository
import com.disparasms.app.data.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val totalSent: Long = 0,
    val totalDelivered: Long = 0,
    val totalFailed: Long = 0,
    val totalCampaigns: Int = 0,
    val totalGroups: Int = 0,
    val totalContacts: Int = 0,
    val recentCampaigns: List<CampaignEntity> = emptyList(),
    val isLoading: Boolean = true
)

private fun Flow<Long?>.orZero(): Flow<Long> = map { it ?: 0L }.catch { emit(0L) }

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val campaignRepository: CampaignRepository,
    private val groupRepository: GroupRepository,
    private val contactRepository: ContactRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        listOf(
            campaignRepository.observeTotalSent().orZero(),
            campaignRepository.observeTotalDelivered().orZero(),
            campaignRepository.observeTotalFailed().orZero(),
            campaignRepository.observeCount().catch { emit(0) },
            groupRepository.observeCount().catch { emit(0) },
            contactRepository.observeTotalCount().catch { emit(0) },
            campaignRepository.observeAll().catch { emit(emptyList()) }
        )
    ) { array ->
        HomeUiState(
            totalSent = array[0] as Long,
            totalDelivered = array[1] as Long,
            totalFailed = array[2] as Long,
            totalCampaigns = array[3] as Int,
            totalGroups = array[4] as Int,
            totalContacts = array[5] as Int,
            recentCampaigns = (array[6] as List<CampaignEntity>).take(5),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
}
