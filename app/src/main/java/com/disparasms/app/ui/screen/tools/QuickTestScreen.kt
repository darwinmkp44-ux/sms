package com.disparasms.app.ui.screen.tools

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.disparasms.app.sms.SimInfo
import com.disparasms.app.sms.SmsResult
import com.disparasms.app.sms.SmsSender
import com.disparasms.app.ui.components.ModernCard
import com.disparasms.app.ui.theme.Spacing
import com.disparasms.app.ui.theme.successGreen
import com.disparasms.app.ui.theme.errorRed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TestStatus {
    IDLE, PENDING, SUCCESS, FAILURE
}

data class QuickTestUiState(
    val recipientPhone: String = "",
    val messageContent: String = "DisparaSMS: Este é um SMS de teste rápido para validar o canal de envio.",
    val simSlots: List<SimInfo> = emptyList(),
    val selectedSim: SimInfo? = null,
    val isDropdownExpanded: Boolean = false,
    val testStatus: TestStatus = TestStatus.IDLE,
    val errorLog: String? = null,
    val smsResult: SmsResult? = null
)

@HiltViewModel
class QuickTestViewModel @Inject constructor(
    private val smsSender: SmsSender
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickTestUiState())
    val uiState: StateFlow<QuickTestUiState> = _uiState.asStateFlow()

    init {
        loadSimSlots()
    }

    fun loadSimSlots() {
        val slots = smsSender.getAvailableSimSlots()
        _uiState.value = _uiState.value.copy(
            simSlots = slots,
            selectedSim = slots.firstOrNull()
        )
    }

    fun onPhoneChanged(phone: String) {
        _uiState.value = _uiState.value.copy(recipientPhone = phone)
    }

    fun onMessageChanged(message: String) {
        _uiState.value = _uiState.value.copy(messageContent = message)
    }

    fun onSimSelected(sim: SimInfo) {
        _uiState.value = _uiState.value.copy(selectedSim = sim, isDropdownExpanded = false)
    }

    fun setDropdownExpanded(expanded: Boolean) {
        _uiState.value = _uiState.value.copy(isDropdownExpanded = expanded)
    }

    fun sendTestSms() {
        val state = _uiState.value
        val phone = state.recipientPhone.trim()
        val message = state.messageContent.trim()
        val simIndex = state.selectedSim?.slotIndex ?: 0

        if (phone.isEmpty() || message.isEmpty()) return

        _uiState.value = _uiState.value.copy(
            testStatus = TestStatus.PENDING,
            errorLog = null,
            smsResult = null
        )

        viewModelScope.launch {
            try {
                val result = smsSender.sendSms(phone, message, simIndex)
                if (result.success) {
                    _uiState.value = _uiState.value.copy(
                        testStatus = TestStatus.SUCCESS,
                        smsResult = result
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        testStatus = TestStatus.FAILURE,
                        errorLog = result.error ?: "Falha no envio de SMS. Erro desconhecido.",
                        smsResult = result
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    testStatus = TestStatus.FAILURE,
                    errorLog = e.message ?: "Exceção inesperada durante o disparo."
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickTestScreen(
    navController: NavController,
    viewModel: QuickTestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val hasSmsPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.SEND_SMS
    ) == PackageManager.PERMISSION_GRANTED

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Teste Rápido de Envio",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item {
                Text(
                    text = "Envie uma mensagem de teste individual usando qualquer slot SIM configurado no celular para verificar a conectividade da operadora de forma isolada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!hasSmsPermission) {
                item {
                    ModernCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.lg),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Permissão de SMS Ausente",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Conceda permissões de envio de SMS nas definições ou reinicie a aplicação para continuar.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // SIM Card Dropdown Selector
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Selecione o Chip SIM",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = Spacing.xs)
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = uiState.selectedSim?.let { "${it.displayName} (${it.carrierName})" } ?: "Procurando SIM...",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { viewModel.setDropdownExpanded(!uiState.isDropdownExpanded) }) {
                                    Icon(
                                        imageVector = if (uiState.isDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = null
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setDropdownExpanded(!uiState.isDropdownExpanded) }
                        )

                        DropdownMenu(
                            expanded = uiState.isDropdownExpanded,
                            onDismissRequest = { viewModel.setDropdownExpanded(false) },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            if (uiState.simSlots.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Nenhum chip detectado") },
                                    onClick = {}
                                )
                            } else {
                                uiState.simSlots.forEach { sim ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(sim.displayName, fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = sim.carrierName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = { viewModel.onSimSelected(sim) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Destination phone input
            item {
                OutlinedTextField(
                    value = uiState.recipientPhone,
                    onValueChange = { viewModel.onPhoneChanged(it) },
                    label = { Text("Número de Destinatário") },
                    placeholder = { Text("Ex: +5511999998888") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                    )
                )
            }

            // Message Body input
            item {
                OutlinedTextField(
                    value = uiState.messageContent,
                    onValueChange = { viewModel.onMessageChanged(it) },
                    label = { Text("Mensagem de Teste") },
                    placeholder = { Text("Redija a sua mensagem de teste...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }

            // Send Test SMS trigger
            item {
                Button(
                    onClick = { viewModel.sendTestSms() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = hasSmsPermission &&
                            uiState.recipientPhone.isNotBlank() &&
                            uiState.messageContent.isNotBlank() &&
                            uiState.testStatus != TestStatus.PENDING
                ) {
                    if (uiState.testStatus == TestStatus.PENDING) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        Text("Enviando SMS...")
                    } else {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null)
                        Spacer(Modifier.width(Spacing.sm))
                        Text("Enviar Teste")
                    }
                }
            }

            // Dispatch Result Log
            if (uiState.testStatus != TestStatus.IDLE) {
                item {
                    Text(
                        text = "Status do Disparo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = Spacing.sm)
                    )
                }

                item {
                    val statusColor = when (uiState.testStatus) {
                        TestStatus.PENDING -> MaterialTheme.colorScheme.primary
                        TestStatus.SUCCESS -> MaterialTheme.colorScheme.successGreen
                        TestStatus.FAILURE -> MaterialTheme.colorScheme.errorRed
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    ModernCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.lg),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                Icon(
                                    imageVector = when (uiState.testStatus) {
                                        TestStatus.PENDING -> Icons.Default.Info
                                        TestStatus.SUCCESS -> Icons.Default.CheckCircle
                                        else -> Icons.Default.Error
                                    },
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = when (uiState.testStatus) {
                                        TestStatus.PENDING -> "A processar..."
                                        TestStatus.SUCCESS -> "Disparado com Sucesso"
                                        else -> "Falha no Envio"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }

                            Divider(color = MaterialTheme.colorScheme.outlineVariant)

                            if (uiState.testStatus == TestStatus.SUCCESS) {
                                Text(
                                    text = "O sinal de rede foi validado com êxito! O SMS foi programado com ID: ${uiState.smsResult?.messageId}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (uiState.testStatus == TestStatus.FAILURE) {
                                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                    Text(
                                        text = "Log de Erro Detalhado:",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = uiState.errorLog ?: "Ocorreu um erro desconhecido no subsistema Android.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                                            .padding(Spacing.md)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
