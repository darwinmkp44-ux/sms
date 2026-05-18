package com.disparasms.app.ui.screen.campaign

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.disparasms.app.data.local.entity.CampaignLogEntity
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

    if (state.isLoading) {
        LoadingIndicator()
        return
    }

    val campaign = state.campaign ?: return

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
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

        // SIM selection
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
            SectionHeader(title = "Logs de Envio")
        }

        if (state.logs.isEmpty()) {
            item {
                Text(
                    text = "Nenhum log disponível",
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(state.logs.take(50), key = { it.id }) { log ->
                LogItem(log = log)
            }
        }
    }
}

@Composable
private fun LogItem(log: CampaignLogEntity) {
    ModernCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.firstName ?: log.phone,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = log.phone,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusBadge(status = log.status)
        }
    }
}
