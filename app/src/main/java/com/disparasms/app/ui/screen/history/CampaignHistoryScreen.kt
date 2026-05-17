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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.disparasms.app.data.local.entity.CampaignEntity
import com.disparasms.app.ui.components.EmptyState
import com.disparasms.app.ui.components.ModernCard
import com.disparasms.app.ui.components.StatusBadge
import com.disparasms.app.ui.theme.Spacing
import com.disparasms.app.data.local.entity.CampaignStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignHistoryScreen(
    navController: NavController,
    viewModel: CampaignHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
                    onClick = { navController.navigate("campaigns/${campaign.id}") },
                    onDelete = { viewModel.deleteCampaign(campaign.id) }
                )
            }
        }
    }
}

@Composable
private fun CampaignHistoryItem(
    campaign: CampaignEntity,
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
            Spacer(Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
