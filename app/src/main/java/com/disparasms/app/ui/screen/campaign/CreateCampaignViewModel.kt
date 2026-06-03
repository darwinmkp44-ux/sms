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

enum class DelayUnit { MS, S }

data class CreateCampaignUiState(
    val name: String = "",
    val message: String = "",
    val selectedGroupIds: List<Long> = emptyList(),
    val selectedContactIds: List<Long> = emptyList(),
    val groups: List<GroupEntity> = emptyList(),
    val simSlot: Int = 0,
    val messagesPerInterval: String = "1",
    val intervalValue: String = "1000",
    val intervalUnit: DelayUnit = DelayUnit.MS,
    val estimatedSmsCount: Int = 0,
    val estimatedParts: Int = 1,
    val totalRecipients: Int = 0,
    val isLoading: Boolean = false,
    val simSlots: List<SimInfo> = emptyList(),
    val maxRetries: Int = 3,
    val limitRecipients: Boolean = false,
    val recipientLimitValue: String = ""
) {
    val intervalMs: Long
        get() = when (intervalUnit) {
            DelayUnit.MS -> intervalValue.toLongOrNull() ?: 1000L
            DelayUnit.S -> (intervalValue.toLongOrNull() ?: 1L) * 1000L
        }

    val messagesPerIntervalInt: Int
        get() = messagesPerInterval.toIntOrNull() ?: 1
}

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

    fun setMessagesPerInterval(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            updateState { it.copy(messagesPerInterval = value) }
        }
    }

    fun setIntervalValue(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            updateState { it.copy(intervalValue = value) }
        }
    }

    fun setIntervalUnit(unit: DelayUnit) {
        updateState { it.copy(intervalUnit = unit) }
    }

    fun setMaxRetries(value: Int) {
        updateState { it.copy(maxRetries = value.coerceIn(0, 15)) }
    }

    fun toggleLimitRecipients(enabled: Boolean) {
        updateState { it.copy(limitRecipients = enabled) }
        recalculateRecipients()
    }

    fun setRecipientLimitValue(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            updateState { it.copy(recipientLimitValue = value) }
            recalculateRecipients()
        }
    }

    fun createCampaign(onComplete: (Long) -> Unit) {
        if (uiState.value.isLoading) return
        updateState { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
            val state = uiState.value
            val contacts = mutableListOf<ContactEntity>()
            for (groupId in state.selectedGroupIds) {
                contacts.addAll(contactRepository.getByGroup(groupId))
            }

            val finalContacts = if (state.limitRecipients) {
                val limit = state.recipientLimitValue.toIntOrNull() ?: contacts.size
                if (limit > 0 && limit < contacts.size) {
                    contacts.shuffled().take(limit)
                } else {
                    contacts
                }
            } else {
                contacts
            }

            val campaignId = campaignRepository.create(
                name = state.name.ifBlank { "Campanha ${System.currentTimeMillis()}" },
                message = state.message,
                groupIds = state.selectedGroupIds,
                contactIds = emptyList(),
                totalContacts = finalContacts.size,
                simSlot = state.simSlot,
                messagesPerInterval = state.messagesPerIntervalInt.coerceAtLeast(1),
                intervalMs = state.intervalMs.coerceIn(100L, 3600000L),
                maxRetries = state.maxRetries
            )

            val logs = finalContacts.map { contact ->
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
            updateState { it.copy(isLoading = false) }
            onComplete(campaignId)
            } catch (e: Exception) {
                updateState { it.copy(isLoading = false) }
            }
        }
    }

    private fun recalculateRecipients() {
        viewModelScope.launch {
            var total = 0
            for (groupId in uiState.value.selectedGroupIds) {
                total += contactRepository.getByGroup(groupId).size
            }
            val limit = if (uiState.value.limitRecipients) {
                uiState.value.recipientLimitValue.toIntOrNull()
            } else null
            
            val finalTotal = if (limit != null && limit > 0) {
                total.coerceAtMost(limit)
            } else {
                total
            }
            updateState { it.copy(totalRecipients = finalTotal) }
        }
    }

    private fun updateState(transform: (CreateCampaignUiState) -> CreateCampaignUiState) {
        (uiState as MutableStateFlow).value = transform(uiState.value)
    }
}
