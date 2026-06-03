package com.disparasms.app.ui.screen.tools

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.disparasms.app.data.local.entity.ContactEntity
import com.disparasms.app.data.local.entity.GroupEntity
import com.disparasms.app.data.repository.ContactRepository
import com.disparasms.app.data.repository.GroupRepository
import com.disparasms.app.ui.components.ModernCard
import com.disparasms.app.ui.components.StatCard
import com.disparasms.app.ui.theme.Spacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class CleaningResult(
    val validNumbers: List<String> = emptyList(),
    val totalCount: Int = 0,
    val validCount: Int = 0,
    val invalidCount: Int = 0
)

data class NumberCleanerUiState(
    val pastedText: String = "",
    val ddiPrefix: String = "+55",
    val cleaningResult: CleaningResult = CleaningResult(),
    val isSavingGroup: Boolean = false,
    val showSaveDialog: Boolean = false,
    val groupName: String = "",
    val error: String? = null
)

@HiltViewModel
class NumberCleanerViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val contactRepository: ContactRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NumberCleanerUiState())
    val uiState: StateFlow<NumberCleanerUiState> = _uiState.asStateFlow()

    fun onPastedTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(pastedText = text)
    }

    fun onDdiPrefixChanged(prefix: String) {
        _uiState.value = _uiState.value.copy(ddiPrefix = prefix)
    }

    fun onGroupNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(groupName = name)
    }

    fun showSaveDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSaveDialog = show, groupName = "")
    }

    fun cleanNumbers() {
        val state = _uiState.value
        val cleanDdi = state.ddiPrefix.replace(Regex("[^\\d]"), "")
        val lines = state.pastedText.split(Regex("[\\n,;\\s]+"))
        val validNumbers = mutableListOf<String>()
        var invalidLinesCount = 0
        var validLinesCount = 0

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val digits = trimmed.replace(Regex("[^\\d]"), "")
            if (digits.isEmpty()) {
                invalidLinesCount++
                continue
            }

            var parsedDigits = digits
            if (cleanDdi.isNotEmpty() && !digits.startsWith(cleanDdi)) {
                parsedDigits = cleanDdi + digits
            }

            if (parsedDigits.length in 10..15) {
                validNumbers.add("+$parsedDigits")
                validLinesCount++
            } else {
                invalidLinesCount++
            }
        }

        _uiState.value = _uiState.value.copy(
            cleaningResult = CleaningResult(
                validNumbers = validNumbers.distinct(),
                totalCount = lines.filter { it.trim().isNotEmpty() }.size,
                validCount = validLinesCount,
                invalidCount = invalidLinesCount
            )
        )
    }

    fun saveAsGroup(onSuccess: () -> Unit) {
        val state = _uiState.value
        val groupName = state.groupName.trim()
        val numbers = state.cleaningResult.validNumbers

        if (groupName.isEmpty() || numbers.isEmpty()) return

        _uiState.value = _uiState.value.copy(isSavingGroup = true)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val group = GroupEntity(
                        name = groupName,
                        contactCount = numbers.size,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    val groupId = groupRepository.insert(group)

                    val contacts = numbers.mapIndexed { index, phone ->
                        ContactEntity(
                            groupId = groupId,
                            phone = phone,
                            fullName = "Contato ${index + 1}",
                            createdAt = System.currentTimeMillis()
                        )
                    }

                    contactRepository.importContacts(contacts)
                    groupRepository.refreshContactCount(groupId)
                }
                _uiState.value = _uiState.value.copy(
                    isSavingGroup = false,
                    showSaveDialog = false,
                    groupName = ""
                )
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSavingGroup = false,
                    error = e.message ?: "Erro desconhecido ao salvar o grupo"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberCleanerScreen(
    navController: NavController,
    viewModel: NumberCleanerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Limpar Números",
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
                    text = "Cole listas brutas contendo números em qualquer formato, adicione o prefixo DDI padrão e limpe os dados instantaneamente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.ddiPrefix,
                        onValueChange = { viewModel.onDdiPrefixChanged(it) },
                        label = { Text("DDI Padrão") },
                        placeholder = { Text("+55") },
                        modifier = Modifier.width(120.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = uiState.pastedText,
                        onValueChange = { viewModel.onPastedTextChanged(it) },
                        label = { Text("Colar Telefones") },
                        placeholder = { Text("Ex: 1199999-8888\n(11) 98888-7777\n977776666") },
                        modifier = Modifier.weight(1f),
                        minLines = 3,
                        maxLines = 5
                    )
                }
            }

            item {
                Button(
                    onClick = { viewModel.cleanNumbers() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = uiState.pastedText.isNotBlank()
                ) {
                    Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null)
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Limpar e Validar")
                }
            }

            if (uiState.cleaningResult.totalCount > 0) {
                item {
                    Text(
                        text = "Resultados da Filtragem",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        StatCard(
                            label = "Válidos",
                            value = uiState.cleaningResult.validNumbers.size.toString(),
                            icon = Icons.Default.Check,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Filtrados",
                            value = uiState.cleaningResult.invalidCount.toString(),
                            icon = Icons.Default.Close,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (uiState.cleaningResult.validNumbers.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val text = uiState.cleaningResult.validNumbers.joinToString("\n")
                                    clipboardManager.setText(AnnotatedString(text))
                                    Toast.makeText(context, "Números copiados!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null)
                                Spacer(Modifier.width(Spacing.xs))
                                Text("Copiar Tudo")
                            }

                            Button(
                                onClick = { viewModel.showSaveDialog(true) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.GroupAdd, contentDescription = null)
                                Spacer(Modifier.width(Spacing.xs))
                                Text("Salvar como Grupo")
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Lista de Telefones Limpos (${uiState.cleaningResult.validNumbers.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(uiState.cleaningResult.validNumbers) { number ->
                        ModernCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = number,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF4CAF50).copy(alpha = 0.1f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Válido",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item {
                        ModernCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.xxl),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(Modifier.height(Spacing.sm))
                                Text(
                                    text = "Nenhum número válido foi encontrado na lista com as configurações atuais.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showSaveDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showSaveDialog(false) },
            title = { Text("Salvar como Grupo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        text = "Crie um novo grupo com estes ${uiState.cleaningResult.validNumbers.size} telefones válidos.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = uiState.groupName,
                        onValueChange = { viewModel.onGroupNameChanged(it) },
                        label = { Text("Nome do Grupo") },
                        placeholder = { Text("Ex: Clientes VIP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveAsGroup {
                            Toast.makeText(context, "Grupo criado com sucesso!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    },
                    enabled = uiState.groupName.isNotBlank() && !uiState.isSavingGroup
                ) {
                    if (uiState.isSavingGroup) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Salvar")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showSaveDialog(false) }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
