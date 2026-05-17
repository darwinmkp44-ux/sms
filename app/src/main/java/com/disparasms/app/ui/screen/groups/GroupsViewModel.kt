package com.disparasms.app.ui.screen.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.disparasms.app.data.local.entity.GroupEntity
import com.disparasms.app.data.repository.ContactRepository
import com.disparasms.app.data.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupsUiState(
    val groups: List<GroupEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val contactRepository: ContactRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _allGroups = groupRepository.observeAll()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<GroupsUiState> = combine(
        _searchQuery, _allGroups
    ) { query, groups ->
        GroupsUiState(
            groups = if (query.isBlank()) groups
            else groups.filter { it.name.contains(query, ignoreCase = true) },
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GroupsUiState())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun duplicateGroup(groupId: Long) {
        viewModelScope.launch {
            val group = groupRepository.getById(groupId) ?: return@launch
            val newName = "${group.name} (cópia)"
            groupRepository.duplicateGroup(groupId, newName)
        }
    }

    fun deleteGroup(groupId: Long) {
        viewModelScope.launch { groupRepository.deleteById(groupId) }
    }

    fun toggleFavorite(group: GroupEntity) {
        viewModelScope.launch {
            groupRepository.update(group.copy(isFavorite = !group.isFavorite))
        }
    }
}
