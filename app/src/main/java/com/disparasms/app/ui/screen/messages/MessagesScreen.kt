package com.disparasms.app.ui.screen.messages

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.disparasms.app.data.local.entity.CampaignLogStatus
import com.disparasms.app.data.local.entity.CampaignLogWithCampaign
import com.disparasms.app.ui.components.EmptyState
import com.disparasms.app.ui.components.LoadingIndicator
import com.disparasms.app.ui.theme.Spacing
import com.disparasms.app.ui.theme.successGreen
import com.disparasms.app.ui.theme.errorRed
import com.disparasms.app.ui.theme.warningOrange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    navController: NavController,
    viewModel: MessagesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hasFailedLogs = state.logs.any { it.status == CampaignLogStatus.FAILED }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mensagens",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (hasFailedLogs) {
                        IconButton(onClick = { viewModel.retryAllFailed() }) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = "Reenviar Todas as Falhas",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (state.isLoading) {
            LoadingIndicator()
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Pesquisar por contato, número ou texto...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(Modifier.height(Spacing.md))

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                val filters = listOf("TODAS", "ENVIADAS", "FALHADAS", "ENTREGUES")
                filters.forEach { filter ->
                    FilterChip(
                        selected = state.selectedStatus == filter,
                        onClick = { viewModel.setSelectedStatus(filter) },
                        label = { Text(filter.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            if (state.logs.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Search,
                    title = "Nenhuma mensagem encontrada",
                    description = "Tente alterar os termos da busca ou os filtros aplicados."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(state.logs, key = { it.logId }) { log ->
                        GlobalLogItem(
                            log = log,
                            onRetryClick = { viewModel.retryMessage(log) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobalLogItem(
    log: CampaignLogWithCampaign,
    onRetryClick: () -> Unit
) {
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

    fun formatTime(timestamp: Long?): String {
        if (timestamp == null) return ""
        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = log.firstName ?: log.phone,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Campaign name tag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = log.campaignName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(4.dp))

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
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${log.phone} ${if (log.sentAt != null) "• " + formatTime(log.sentAt) else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = log.status,
                            tint = statusTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (log.status == CampaignLogStatus.FAILED && !log.errorMessage.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Erro: ${log.errorMessage}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (log.status == CampaignLogStatus.FAILED) {
                Spacer(Modifier.width(Spacing.sm))
                Button(
                    onClick = onRetryClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Reenviar", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
