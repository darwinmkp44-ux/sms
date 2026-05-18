package com.disparasms.app.ui.screen.campaign

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.disparasms.app.data.local.entity.CampaignLogEntity
import com.disparasms.app.data.local.entity.ContactEntity
import com.disparasms.app.data.local.entity.GroupEntity
import com.disparasms.app.data.repository.CampaignRepository
import com.disparasms.app.data.repository.ContactRepository
import com.disparasms.app.data.repository.GroupRepository
import com.disparasms.app.sms.SimInfo
import com.disparasms.app.sms.SmsSender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateCampaignUiState(
    val name: String = "",
    val message: String = "",
    val selectedGroupIds: List<Long> = emptyList(),
    val selectedContactIds: List<Long> = emptyList(),
    val groups: List<GroupEntity> = emptyList(),
    val simSlot: Int = 0,
    val delayMs: Long = 1500L,
    val estimatedSmsCount: Int = 0,
    val estimatedParts: Int = 1,
    val totalRecipients: Int = 0,
    val isLoading: Boolean = false,
    val simSlots: List<SimInfo> = emptyList()
)

@HiltViewModel
class CreateCampaignViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val contactRepository: ContactRepository,
    private val campaignRepository: CampaignRepository,
    private val smsSender: SmsSender
) : ViewModel() {

    val uiState: StateFlow<CreateCampaignUiState> = MutableStateFlow(CreateCampaignUiState())

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val groups = groupRepository.getAll()
            val sims = smsSender.getAvailableSimSlots()
            (uiState as MutableStateFlow).value = uiState.value.copy(
                groups = groups,
                simSlots = sims
            )
        }
    }

    fun updateName(name: String) { updateState { it.copy(name = name) } }

    fun updateMessage(message: String) {
        val parts = smsSender.getMessageCount(message)
        val estimatedCount = if (parts > 1) parts else 1
        updateState {
            it.copy(
                message = message,
                estimatedSmsCount = estimatedCount,
                estimatedParts = parts
            )
        }
    }

    fun toggleGroup(groupId: Long) {
        val current = uiState.value.selectedGroupIds.toMutableList()
        if (groupId in current) current.remove(groupId) else current.add(groupId)
        updateState { it.copy(selectedGroupIds = current) }
        recalculateRecipients()
    }

    fun setSimSlot(slot: Int) {
        updateState { it.copy(simSlot = slot) }
    }

    fun setDelayMs(delay: Long) {
        updateState { it.copy(delayMs = delay) }
    }

    fun createCampaign(onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            val state = uiState.value
            val campaignId = campaignRepository.create(
                name = state.name.ifBlank { "Campanha ${System.currentTimeMillis()}" },
                message = state.message,
                groupIds = state.selectedGroupIds,
                contactIds = emptyList(),
                totalContacts = state.totalRecipients,
                simSlot = state.simSlot,
                delayMs = state.delayMs
            )

            val contacts = mutableListOf<ContactEntity>()
            for (groupId in state.selectedGroupIds) {
                contacts.addAll(contactRepository.getByGroup(groupId))
            }

            val logs = contacts.map { contact ->
                val resolvedMessage = state.message
                    .replace("{first_name}", contact.firstName ?: "")
                    .replace("{last_name}", contact.lastName ?: "")
                    .replace("{phone}", contact.phone)
                CampaignLogEntity(
                    campaignId = campaignId,
                    contactId = contact.id,
                    phone = contact.phone,
                    firstName = contact.firstName,
                    message = resolvedMessage
                )
            }

            campaignRepository.insertLogs(logs)
            onComplete(campaignId)
        }
    }

    private fun recalculateRecipients() {
        viewModelScope.launch {
            var total = 0
            for (groupId in uiState.value.selectedGroupIds) {
                total += contactRepository.getByGroup(groupId).size
            }
            updateState { it.copy(totalRecipients = total) }
        }
    }

    private fun updateState(transform: (CreateCampaignUiState) -> CreateCampaignUiState) {
        (uiState as MutableStateFlow).value = transform(uiState.value)
    }
}
