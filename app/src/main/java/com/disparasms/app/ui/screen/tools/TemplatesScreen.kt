package com.disparasms.app.ui.screen.tools

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.disparasms.app.data.local.entity.MessageTemplateEntity
import com.disparasms.app.data.repository.MessageTemplateRepository
import com.disparasms.app.ui.components.EmptyState
import com.disparasms.app.ui.components.ModernCard
import com.disparasms.app.ui.theme.Spacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TemplatesUiState(
    val templates: List<MessageTemplateEntity> = emptyList(),
    val showDialog: Boolean = false,
    val editingTemplate: MessageTemplateEntity? = null,
    val titleInput: String = "",
    val contentInput: String = "",
    val showDeleteConfirm: MessageTemplateEntity? = null
)

@HiltViewModel
class TemplatesViewModel @Inject constructor(
    private val templateRepository: MessageTemplateRepository
) : ViewModel() {

    val templates: StateFlow<List<MessageTemplateEntity>> = templateRepository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(TemplatesUiState())
    val uiState: StateFlow<TemplatesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            templates.collect { list ->
                _uiState.value = _uiState.value.copy(templates = list)
            }
        }
    }

    fun onTitleChanged(title: String) {
        _uiState.value = _uiState.value.copy(titleInput = title)
    }

    fun onContentChanged(content: String) {
        _uiState.value = _uiState.value.copy(contentInput = content)
    }

    fun openCreateDialog() {
        _uiState.value = _uiState.value.copy(
            showDialog = true,
            editingTemplate = null,
            titleInput = "",
            contentInput = ""
        )
    }

    fun openEditDialog(template: MessageTemplateEntity) {
        _uiState.value = _uiState.value.copy(
            showDialog = true,
            editingTemplate = template,
            titleInput = template.title,
            contentInput = template.content
        )
    }

    fun closeDialog() {
        _uiState.value = _uiState.value.copy(showDialog = false)
    }

    fun showDeleteConfirmation(template: MessageTemplateEntity?) {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = template)
    }

    fun saveTemplate() {
        val state = _uiState.value
        val title = state.titleInput.trim()
        val content = state.contentInput.trim()

        if (title.isEmpty() || content.isEmpty()) return

        viewModelScope.launch {
            if (state.editingTemplate != null) {
                templateRepository.update(
                    state.editingTemplate.copy(title = title, content = content)
                )
            } else {
                templateRepository.create(title, content)
            }
            closeDialog()
        }
    }

    fun deleteTemplate(template: MessageTemplateEntity) {
        viewModelScope.launch {
            templateRepository.delete(template)
            showDeleteConfirmation(null)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    navController: NavController,
    viewModel: TemplatesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Modelos de Mensagens",
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreateDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Novo Modelo")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.templates.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Description,
                    title = "Nenhum modelo criado",
                    description = "Crie templates de reuso para suas campanhas e economize tempo ao redigir mensagens.",
                    actionLabel = "Criar Modelo",
                    onAction = { viewModel.openCreateDialog() }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    items(uiState.templates, key = { it.id }) { template ->
                        ModernCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openEditDialog(template) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.lg)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = template.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Row {
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(template.content))
                                                Toast.makeText(context, "Mensagem copiada!", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copiar",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.showDeleteConfirmation(template) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Excluir",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(Spacing.xs))

                                Text(
                                    text = template.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showDialog) {
        val isEdit = uiState.editingTemplate != null
        AlertDialog(
            onDismissRequest = { viewModel.closeDialog() },
            title = { Text(if (isEdit) "Editar Modelo" else "Novo Modelo") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = uiState.titleInput,
                        onValueChange = { viewModel.onTitleChanged(it) },
                        label = { Text("Título do Modelo") },
                        placeholder = { Text("Ex: Agradecimento") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = uiState.contentInput,
                        onValueChange = { viewModel.onContentChanged(it) },
                        label = { Text("Mensagem") },
                        placeholder = { Text("Olá {nome}, obrigado pela preferência!") },
                        minLines = 4,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.saveTemplate() },
                    enabled = uiState.titleInput.isNotBlank() && uiState.contentInput.isNotBlank()
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeDialog() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    uiState.showDeleteConfirm?.let { template ->
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteConfirmation(null) },
            title = { Text("Excluir Modelo") },
            text = { Text("Tem certeza de que deseja excluir o modelo \"${template.title}\"? Esta ação não pode ser desfeita.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteTemplate(template) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showDeleteConfirmation(null) }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
