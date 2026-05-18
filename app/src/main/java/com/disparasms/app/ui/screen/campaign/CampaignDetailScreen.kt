package com.disparasms.app.ui.screen.campaign

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.disparasms.app.data.local.entity.CampaignLogEntity
import com.disparasms.app.data.local.entity.CampaignLogStatus
import com.disparasms.app.ui.components.LoadingIndicator
import com.disparasms.app.ui.components.ModernCard
import com.disparasms.app.ui.components.SectionHeader
import com.disparasms.app.ui.components.StatusBadge
import com.disparasms.app.ui.components.StatusRow
import com.disparasms.app.ui.theme.Spacing
import com.disparasms.app.ui.theme.successGreen
import com.disparasms.app.ui.theme.errorRed
import com.disparasms.app.ui.theme.warningOrange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignDetailScreen(
    navController: NavController,
    viewModel: CampaignDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var simMenuExpanded by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editMessage by remember { mutableStateOf("") }

    if (state.isLoading) {
        LoadingIndicator()
        return
    }

    val campaign = state.campaign ?: return

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            icon = { Icon(Icons.Default.Edit, contentDescription = null) },
            title = { Text("Editar Mensagem") },
            text = {
                OutlinedTextField(
                    value = editMessage,
                    onValueChange = { editMessage = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editMessage.isNotBlank()) {
                        viewModel.updateMessage(editMessage)
                        showEditDialog = false
                    }
                }) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            TopAppBar(
                title = {
                    Text(
                        text = campaign.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (campaign.status == "PENDING") {
                        IconButton(onClick = { viewModel.startSending() }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Iniciar")
                        }
                    } else if (campaign.status == "SENDING") {
                        IconButton(onClick = { viewModel.pauseSending() }) {
                            Icon(Icons.Default.Pause, contentDescription = "Pausar")
                        }
                    } else if (campaign.status == "PAUSED") {
                        IconButton(onClick = { viewModel.resumeSending() }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Continuar")
                        }
                    }
                    IconButton(onClick = {
                        editMessage = campaign.message
                        showEditDialog = true
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar mensagem")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }

        // Progress section
        item {
            ModernCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
            ) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusBadge(status = campaign.status)
                        Text(
                            text = "${campaign.sentCount + campaign.failedCount}/${campaign.totalContacts}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(Spacing.md))

                    val progress = if (campaign.totalContacts > 0)
                        (campaign.sentCount + campaign.failedCount).toFloat() / campaign.totalContacts
                    else 0f

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(Spacing.md))

                    StatusRow(
                        label = "Enviados",
                        icon = Icons.Default.Send,
                        color = MaterialTheme.colorScheme.primary,
                        value = campaign.sentCount.toString()
                    )
                    Spacer(Modifier.height(4.dp))
                    StatusRow(
                        label = "Entregues",
                        icon = Icons.Default.CheckCircle,
                        color = MaterialTheme.colorScheme.successGreen,
                        value = campaign.deliveredCount.toString()
                    )
                    Spacer(Modifier.height(4.dp))
                    StatusRow(
                        label = "Falhas",
                        icon = Icons.Default.Error,
                        color = MaterialTheme.colorScheme.errorRed,
                        value = campaign.failedCount.toString()
                    )
                    Spacer(Modifier.height(4.dp))
                    StatusRow(
                        label = "Pendentes",
                        icon = Icons.Default.HourglassEmpty,
                        color = MaterialTheme.colorScheme.warningOrange,
                        value = campaign.pendingCount.toString()
                    )
                }
            }
        }

        // SIM + date
        item {
            Spacer(Modifier.height(Spacing.lg))
            SectionHeader(title = "SIM Card")
            ModernCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.SimCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(Spacing.md))
                    Text(
                        text = "SIM para envio",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { simMenuExpanded = true }) {
                        Text(
                            text = state.simSlots.getOrNull(campaign.simSlot)?.carrierName
                                ?: "SIM ${campaign.simSlot + 1}"
                        )
                    }
                    DropdownMenu(
                        expanded = simMenuExpanded,
                        onDismissRequest = { simMenuExpanded = false }
                    ) {
                        state.simSlots.forEachIndexed { index, sim ->
                            DropdownMenuItem(
                                text = { Text(sim.carrierName ?: "SIM ${index + 1}") },
                                onClick = {
                                    viewModel.updateSimSlot(index)
                                    simMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Criada em ${formatDateTime(campaign.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Message content
        item {
            Spacer(Modifier.height(Spacing.lg))
            SectionHeader(title = "Mensagem")
            ModernCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
            ) {
                Text(
                    text = campaign.message,
                    modifier = Modifier.padding(Spacing.lg),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Logs
        item {
            Spacer(Modifier.height(Spacing.lg))
            SectionHeader(title = "Mensagens Enviadas")
        }

        if (state.logs.isEmpty()) {
            item {
                Text(
                    text = "Nenhuma mensagem enviada",
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(state.logs.take(100), key = { it.id }) { log ->
                ChatLogItem(log = log)
            }
        }
    }
}

@Composable
private fun ChatLogItem(log: CampaignLogEntity) {
    val bubbleColor = when (log.status) {
        CampaignLogStatus.DELIVERED -> MaterialTheme.colorScheme.primaryContainer
        CampaignLogStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
        CampaignLogStatus.SENT -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val statusIcon = when (log.status) {
        CampaignLogStatus.DELIVERED -> Icons.Default.CheckCircle
        CampaignLogStatus.FAILED -> Icons.Default.Error
        CampaignLogStatus.SENT -> Icons.Default.Done
        else -> Icons.Default.HourglassEmpty
    }
    val statusTint = when (log.status) {
        CampaignLogStatus.DELIVERED -> MaterialTheme.colorScheme.successGreen
        CampaignLogStatus.FAILED -> MaterialTheme.colorScheme.errorRed
        CampaignLogStatus.SENT -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.warningOrange
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = 3.dp)
    ) {
        Text(
            text = log.firstName ?: log.phone,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Card(
                modifier = Modifier.weight(1f, fill = false),
                shape = RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 12.dp,
                    bottomEnd = 12.dp,
                    bottomStart = 2.dp
                ),
                colors = CardDefaults.cardColors(containerColor = bubbleColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = log.message,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = log.phone,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = log.status,
                            tint = statusTint,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
