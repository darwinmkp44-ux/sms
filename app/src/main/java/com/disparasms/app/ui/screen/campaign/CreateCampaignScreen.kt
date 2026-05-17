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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.disparasms.app.ui.components.ModernCard
import com.disparasms.app.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCampaignScreen(
    navController: NavController,
    viewModel: CreateCampaignViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            TopAppBar(
                title = {
                    Text(
                        text = "Nova Campanha",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancelar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }

        item {
            Column(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                Text(
                    text = "Nome da Campanha",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Spacing.sm))
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { viewModel.updateName(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ex: Promoção Natal") },
                    singleLine = true
                )
            }
        }

        item {
            Spacer(Modifier.height(Spacing.lg))
            Column(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                Text(
                    text = "Mensagem",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Use {first_name}, {last_name}, {phone} para personalizar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Spacing.sm))
                OutlinedTextField(
                    value = state.message,
                    onValueChange = { viewModel.updateMessage(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Olá {first_name}, temos uma oferta para si...") },
                    minLines = 4,
                    maxLines = 8
                )
                if (state.estimatedParts > 1) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "~${state.estimatedSmsCount} SMS (${state.estimatedParts} partes concatenadas)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(Spacing.lg))
            Column(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                Text(
                    text = "Grupos para enviar",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Spacing.sm))
            }
        }

        if (state.groups.isEmpty()) {
            item {
                Text(
                    text = "Nenhum grupo disponível. Crie grupos primeiro.",
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(state.groups, key = { it.id }) { group ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = group.id in state.selectedGroupIds,
                        onCheckedChange = { viewModel.toggleGroup(group.id) }
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${group.contactCount} contactos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(Spacing.lg))
            Column(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                Text(
                    text = "Configurações de Envio",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Spacing.sm))

                // SIM selection
                if (state.simSlots.size > 1) {
                    Text(
                        text = "SIM: ${state.simSlots.getOrNull(state.simSlot)?.carrierName ?: "Padrão"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(Modifier.height(Spacing.md))

                Text(
                    text = "Intervalo: ${state.delayMs}ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = state.delayMs.toFloat(),
                    onValueChange = { viewModel.setDelayMs(it.toLong()) },
                    valueRange = 500f..5000f,
                    steps = 8
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("500ms", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("5s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Spacer(Modifier.height(Spacing.xxl))
            Button(
                onClick = {
                    viewModel.createAndSend { campaignId ->
                        navController.navigate("campaigns/$campaignId") {
                            popUpTo("campaigns") { inclusive = false }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                enabled = state.message.isNotBlank() && state.selectedGroupIds.isNotEmpty()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Icon(Icons.Default.Send, contentDescription = null)
                Text(
                    text = "Iniciar Disparo (${state.totalRecipients} contactos)",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(Modifier.height(Spacing.xxl))
        }
    }
}
