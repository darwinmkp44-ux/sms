package com.disparasms.app.ui.screen.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.disparasms.app.data.local.entity.CampaignEntity
import com.disparasms.app.data.local.entity.CampaignStatus
import com.disparasms.app.ui.components.EmptyState
import com.disparasms.app.ui.components.ModernCard
import com.disparasms.app.ui.components.StatusBadge
import com.disparasms.app.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignHistoryScreen(
    navController: NavController,
    viewModel: CampaignHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var campaignToDelete by remember { mutableStateOf<CampaignEntity?>(null) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }

    campaignToDelete?.let { campaign ->
        AlertDialog(
            onDismissRequest = { campaignToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Apagar Campanha") },
            text = { Text("Tem certeza que deseja apagar \"${campaign.name}\"? Esta acção não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCampaign(campaign.id)
                    campaignToDelete = null
                }) {
                    Text("Apagar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { campaignToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showBulkDeleteConfirm && selectedIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Apagar ${selectedIds.size} campanhas?") },
            text = { Text("Esta acção não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    selectedIds.forEach { viewModel.deleteCampaign(it) }
                    selectedIds = emptySet()
                    showBulkDeleteConfirm = false
                }) {
                    Text("Apagar tudo", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            TopAppBar(
                title = {
                    Text(
                        text = "Campanhas",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (selectedIds.isNotEmpty()) {
                        IconButton(onClick = { showBulkDeleteConfirm = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Apagar seleccionadas",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        TextButton(onClick = { selectedIds = emptySet() }) {
                            Text("Cancelar")
                        }
                    } else if (state.campaigns.isNotEmpty()) {
                        TextButton(onClick = { selectedIds = state.campaigns.map { it.id }.toSet() }) {
                            Icon(
                                Icons.Default.Checklist,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text("Seleccionar")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }

        if (state.campaigns.isEmpty() && !state.isLoading) {
            item {
                EmptyState(
                    icon = Icons.Default.History,
                    title = "Nenhuma campanha",
                    description = "Suas campanhas aparecerão aqui",
                    actionLabel = "Criar Campanha",
                    onAction = { navController.navigate("campaigns/create") }
                )
            }
        } else {
            items(state.campaigns, key = { it.id }) { campaign ->
                CampaignHistoryItem(
                    campaign = campaign,
                    isSelected = campaign.id in selectedIds,
                    onClick = {
                        if (selectedIds.isNotEmpty()) {
                            selectedIds = if (campaign.id in selectedIds)
                                selectedIds - campaign.id
                            else selectedIds + campaign.id
                        } else {
                            navController.navigate("campaigns/${campaign.id}")
                        }
                    },
                    onDelete = { campaignToDelete = campaign }
                )
            }
        }
    }
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
private fun CampaignHistoryItem(
    campaign: CampaignEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    ModernCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelected) {
                    Checkbox(
                        checked = true,
                        onCheckedChange = { onClick() },
                        modifier = Modifier.padding(end = Spacing.sm)
                    )
                }
                Text(
                    text = campaign.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                StatusBadge(status = campaign.status)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = campaign.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDateTime(campaign.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${campaign.totalContacts} contactos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (campaign.status == CampaignStatus.COMPLETED) {
                        "${campaign.sentCount} enviados"
                    } else {
                        "${campaign.sentCount + campaign.failedCount}/${campaign.totalContacts}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 4.dp)
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Apagar campanha",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
