package com.disparasms.app.ui.screen.import

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.disparasms.app.data.local.entity.ContactEntity
import com.disparasms.app.data.repository.ImportResult
import com.disparasms.app.di.DaoEntryPoint
import com.disparasms.app.ui.components.ModernCard
import com.disparasms.app.ui.theme.Spacing
import com.disparasms.app.util.PhoneUtils
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(navController: NavController, groupId: Long? = null) {
    var mode by remember { mutableStateOf<String?>(null) }
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedFileNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var isImporting by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<ImportResult?>(null) }
    var importProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var totalFiles by remember { mutableStateOf(0) }
    var currentFileIndex by remember { mutableStateOf(0) }
    var manualPhone by remember { mutableStateOf("") }
    var manualName by remember { mutableStateOf("") }
    var manualPhoneError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val contactRepository = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            DaoEntryPoint::class.java
        ).contactRepository()
    }
    val importRepository = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            DaoEntryPoint::class.java
        ).importRepository()
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris = uris
            selectedFileNames = uris.mapNotNull { it.lastPathSegment }
            mode = "file"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isImporting = true
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    importRepository.importFromPhoneContacts(groupId = groupId) { current, total ->
                        importProgress = current to total
                    }
                }
                importResult = result
                isImporting = false
                importProgress = null
            }
        } else {
            importResult = ImportResult(errors = listOf(
                "Permissão para ler contactos não concedida. " +
                "Vá a Definições > Aplicações > DisparaSMS > Permissões e ative 'Contactos'."
            ))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = when (mode) {
                        "phone" -> "Importar do Telefone"
                        "file" -> "Importar de Ficheiro"
                        else -> "Importar Contactos"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (mode != null) mode = null
                    else navController.popBackStack()
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Voltar")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        when (mode) {
            null -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    // Manual input card
                    item {
                        ModernCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(Spacing.lg)) {
                                Text(
                                    text = "Adicionar manualmente",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(Spacing.sm))
                                OutlinedTextField(
                                    value = manualName,
                                    onValueChange = { manualName = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Nome (opcional)") },
                                    singleLine = true
                                )
                                Spacer(Modifier.height(Spacing.sm))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                ) {
                                    OutlinedTextField(
                                        value = manualPhone,
                                        onValueChange = {
                                            manualPhone = it
                                            manualPhoneError = null
                                        },
                                        modifier = Modifier.weight(1f),
                                        placeholder = { Text("+258XXXXXXXX") },
                                        singleLine = true,
                                        isError = manualPhoneError != null,
                                        supportingText = manualPhoneError?.let { { Text(it) } }
                                    )
                                    IconButton(
                                        onClick = {
                                            val phone = PhoneUtils.clean(manualPhone)
                                            if (phone.isEmpty()) {
                                                manualPhoneError = "Número inválido"
                                                return@IconButton
                                            }
                                            if (!PhoneUtils.isValidMzPhone(phone)) {
                                                manualPhoneError = "Número Moçambicano inválido"
                                                return@IconButton
                                            }
                                            manualPhoneError = null
                                            val name = manualName.ifBlank { phone }
                                            manualPhone = ""
                                            manualName = ""
                                            scope.launch {
                                                withContext(Dispatchers.IO) {
                                                    contactRepository.insert(
                                                        ContactEntity(phone = phone, fullName = name)
                                                    )
                                                }
                                            }
                                        },
                                        enabled = manualPhone.isNotBlank()
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Adicionar contacto",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Import from Phone
                    item {
                        ModernCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.xxl),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(Spacing.md))
                                Text(
                                    text = "Importar do Telefone",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(Spacing.sm))
                                Text(
                                    text = "Lê os contactos do seu celular",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(Spacing.lg))
                                Button(
                                    onClick = { mode = "phone" },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Importar")
                                }
                            }
                        }
                    }

                    // Import from File
                    item {
                        ModernCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.xxl),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(Spacing.md))
                                Text(
                                    text = "Importar de Ficheiro",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(Spacing.sm))
                                Text(
                                    text = "Excel, CSV ou TXT (vários ficheiros)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(Spacing.lg))
                                Button(
                                    onClick = {
                                        filePickerLauncher.launch(arrayOf("*/*"))
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Seleccionar Ficheiros")
                                }
                            }
                        }
                    }
                }
            }

            "phone" -> {
                if (isImporting) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        LinearProgressIndicator(
                            progress = {
                                if (importProgress != null)
                                    importProgress!!.first.toFloat() / importProgress!!.second.toFloat()
                                else 0f
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(Spacing.md))
                        Text(
                            text = if (importProgress != null)
                                "Importados ${importProgress!!.first} de ${importProgress!!.second} contactos..."
                            else
                                "A preparar importação...",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            text = if (importProgress != null)
                                "${((importProgress!!.first.toFloat() / importProgress!!.second.toFloat()) * 100).toInt()}%"
                            else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LaunchedEffect(Unit) {
                        val permission = Manifest.permission.READ_CONTACTS
                        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                            isImporting = true
                            val result = withContext(Dispatchers.IO) {
                                importRepository.importFromPhoneContacts(groupId = groupId) { current, total ->
                                    importProgress = current to total
                                }
                            }
                            importResult = result
                            isImporting = false
                            importProgress = null
                        } else {
                            permissionLauncher.launch(permission)
                        }
                    }
                }
            }

            "file" -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    if (selectedUris.isEmpty()) {
                        item {
                            ModernCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Spacing.xxl),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileUpload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = Spacing.md)
                                    )
                                    Text(
                                        text = "Seleccionar ficheiros Excel, CSV ou TXT",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(Spacing.sm))
                                    Text(
                                        text = "Formatos suportados: .xlsx, .xls, .csv, .txt",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(Spacing.lg))
                                    Button(
                                        onClick = {
                                            filePickerLauncher.launch(arrayOf("*/*"))
                                        }
                                    ) {
                                        Icon(Icons.Default.TableChart, contentDescription = null)
                                        Text("Escolher Ficheiros", modifier = Modifier.padding(start = 8.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Text(
                                text = "${selectedUris.size} ficheiro(s) seleccionado(s)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        items(selectedUris.mapIndexed { i, uri -> uri to (selectedFileNames.getOrElse(i) { "Ficheiro ${i+1}" }) }) { (uri, name) ->
                            ModernCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Spacing.lg),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.InsertDriveFile,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column(modifier = Modifier.weight(1f).padding(horizontal = Spacing.md)) {
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        if (isImporting) {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (totalFiles > 1) {
                                        Text(
                                            text = "Ficheiro ${currentFileIndex + 1} de $totalFiles",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(Spacing.sm))
                                    }
                                    LinearProgressIndicator(
                                        progress = {
                                            if (importProgress != null && importProgress!!.second > 0)
                                                importProgress!!.first.toFloat() / importProgress!!.second.toFloat()
                                            else 0f
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(Spacing.sm))
                                    Text(
                                        text = if (importProgress != null)
                                            "${importProgress!!.first} de ${importProgress!!.second} contactos"
                                        else "A importar...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            item {
                                Spacer(Modifier.height(Spacing.md))
                                Button(
                                    onClick = {
                                        isImporting = true
                                        totalFiles = selectedUris.size
                                        currentFileIndex = 0
                                        scope.launch {
                                            var combinedResult = ImportResult()
                                            for ((i, uri) in selectedUris.withIndex()) {
                                                currentFileIndex = i
                                                val result = withContext(Dispatchers.IO) {
                                                    importRepository.importFromUri(uri, groupId) { current, total ->
                                                        importProgress = current to total
                                                    }
                                                }
                                                combinedResult = combinedResult.copy(
                                                    imported = combinedResult.imported + result.imported,
                                                    skipped = combinedResult.skipped + result.skipped,
                                                    invalidPhones = combinedResult.invalidPhones + result.invalidPhones,
                                                    totalFound = combinedResult.totalFound + result.totalFound,
                                                    errors = combinedResult.errors + result.errors
                                                )
                                            }
                                            importResult = combinedResult
                                            isImporting = false
                                            importProgress = null
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isImporting
                                ) {
                                    if (isImporting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("A importar...")
                                    } else {
                                        Text("Importar ${selectedUris.size} ficheiro(s)")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        importResult?.let { result ->
            AlertDialog(
                onDismissRequest = { importResult = null },
                icon = {
                    Icon(
                        imageVector = if (result.errors.isEmpty() || result.errors.all { it.isEmpty() }) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (result.errors.isEmpty() || result.errors.all { it.isEmpty() }) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                },
                title = {
                    Text(if (result.errors.isEmpty() || result.errors.all { it.isEmpty() }) "Importação concluída" else "Erro na importação")
                },
                text = {
                    if (result.errors.isNotEmpty() && result.errors.any { it.isNotEmpty() }) {
                        Text(result.errors.filter { it.isNotEmpty() }.joinToString("\n"))
                    } else {
                        Text("${result.imported} contactos importados com sucesso.\n${result.skipped} ignorados.\n${result.invalidPhones} inválidos.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        importResult = null
                        if (result.errors.isEmpty() || result.errors.all { it.isEmpty() }) {
                            navController.popBackStack()
                        } else {
                            mode = null
                        }
                    }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}
