package com.disparasms.app.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.disparasms.app.data.local.entity.CampaignEntity
import com.disparasms.app.ui.components.EmptyState
import com.disparasms.app.ui.components.ModernCard
import com.disparasms.app.ui.components.SectionHeader
import com.disparasms.app.ui.components.StatCard
import com.disparasms.app.ui.components.StatusBadge
import com.disparasms.app.ui.theme.CornerRadius
import com.disparasms.app.ui.theme.Spacing
import com.disparasms.app.ui.theme.successGreen
import com.disparasms.app.ui.theme.errorRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            TopAppBar(
                title = {
                    Text(
                        text = "Dispara SMS",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }

        item {
            Spacer(Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Enviados",
                    value = formatNumber(state.totalSent),
                    icon = Icons.Default.Send,
                    color = MaterialTheme.colorScheme.primary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Entregues",
                    value = formatNumber(state.totalDelivered),
                    icon = Icons.Default.CheckCircle,
                    color = MaterialTheme.colorScheme.successGreen
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Falhas",
                    value = formatNumber(state.totalFailed),
                    icon = Icons.Default.Error,
                    color = MaterialTheme.colorScheme.errorRed
                )
            }
        }

        item {
            Spacer(Modifier.height(Spacing.xxl))
            SectionHeader(title = "Acção Rápida")
            Spacer(Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Button(
                    onClick = { navController.navigate("campaigns/create") },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Nova Campanha", modifier = Modifier.padding(start = 8.dp))
                }
                Button(
                    onClick = { navController.navigate("import") },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                    Text("Importar", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        item {
            Spacer(Modifier.height(Spacing.xxl))
            SectionHeader(
                title = "Campanhas Recentes",
                action = "Ver todas",
                onAction = { navController.navigate("campaigns") }
            )
        }

        if (state.recentCampaigns.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.Send,
                    title = "Nenhuma campanha",
                    description = "Crie sua primeira campanha de SMS",
                    actionLabel = "Criar Campanha",
                    onAction = { navController.navigate("campaigns/create") }
                )
            }
        } else {
            items(state.recentCampaigns, key = { it.id }) { campaign ->
                CampaignListItem(
                    campaign = campaign,
                    onClick = { navController.navigate("campaigns/${campaign.id}") }
                )
            }
        }

        item {
            Spacer(Modifier.height(Spacing.xxl))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Grupos",
                    value = state.totalGroups.toString(),
                    icon = Icons.Default.Add,
                    color = MaterialTheme.colorScheme.tertiary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Contactos",
                    value = formatNumber(state.totalContacts.toLong()),
                    icon = Icons.Default.Add,
                    color = MaterialTheme.colorScheme.successGreen
                )
            }
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun CampaignListItem(
    campaign: CampaignEntity,
    onClick: () -> Unit
) {
    ModernCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg)
        ) {
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
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = campaign.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                Text(
                    text = "${campaign.totalContacts} contactos",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (campaign.sentCount > 0) {
                    Text(
                        text = "${campaign.sentCount} enviados",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun formatNumber(n: Long): String {
    return when {
        n >= 1_000_000 -> "${n / 1_000_000}M"
        n >= 1_000 -> "${n / 1_000}K"
        else -> n.toString()
    }
}
