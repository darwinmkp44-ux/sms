package com.disparasms.app.ui.screen.campaign

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.disparasms.app.sms.SimInfo
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
                    var simMenuExpanded by remember { mutableStateOf(false) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "SIM para envio",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { simMenuExpanded = true }) {
                            Text(
                                text = state.simSlots.getOrNull(state.simSlot)?.carrierName
                                    ?: "SIM ${state.simSlot + 1}"
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
                                        viewModel.setSimSlot(index)
                                        simMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.md))

                Text(
                    text = "Intervalo entre mensagens",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(Spacing.sm))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = state.delayValue,
                        onValueChange = { viewModel.setDelayValue(it) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        placeholder = { Text("1500") }
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    DelayUnitButton(
                        label = "ms",
                        selected = state.delayUnit == DelayUnit.MS,
                        onClick = { viewModel.setDelayUnit(DelayUnit.MS) }
                    )
                    Spacer(Modifier.width(4.dp))
                    DelayUnitButton(
                        label = "s",
                        selected = state.delayUnit == DelayUnit.S,
                        onClick = { viewModel.setDelayUnit(DelayUnit.S) }
                    )
                }
                if (state.delayValue.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "~${state.delayMs}ms por mensagem | ${state.delayMs * state.totalRecipients}ms total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(Spacing.xxl))
            Button(
                onClick = {
                    viewModel.createCampaign { campaignId ->
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
                Icon(Icons.Default.Check, contentDescription = null)
                Text(
                    text = "Salvar Campanha",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(Modifier.height(Spacing.xxl))
        }
    }
}

@Composable
private fun DelayUnitButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    val bgColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
