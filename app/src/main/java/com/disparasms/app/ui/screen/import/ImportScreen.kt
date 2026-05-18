package com.disparasms.app.ui.screen.import

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.disparasms.app.data.repository.ImportResult
import com.disparasms.app.di.DaoEntryPoint
import com.disparasms.app.ui.components.ModernCard
import com.disparasms.app.ui.theme.Spacing
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(navController: NavController, groupId: Long? = null) {
    var mode by remember { mutableStateOf<String?>(null) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<ImportResult?>(null) }
    var importProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val importRepository = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            DaoEntryPoint::class.java
        ).importRepository()
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedFileUri = it
            selectedFileName = it.lastPathSegment
        }
        mode = "file"
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.lg),
                    verticalArrangement = Arrangement.Center
                ) {
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
                                onClick = {
                                    mode = "phone"
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Importar")
                            }
                        }
                    }

                    Spacer(Modifier.height(Spacing.md))

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
                                text = "Excel, CSV ou TXT",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(Spacing.lg))
                            Button(
                                onClick = {
                                    filePickerLauncher.launch(arrayOf(
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                        "application/vnd.ms-excel",
                                        "text/csv",
                                        "text/comma-separated-values",
                                        "text/plain"
                                    ))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Seleccionar")
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
                    if (selectedFileUri == null) {
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
                                        text = "Seleccionar ficheiro Excel, CSV ou TXT",
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
                                            filePickerLauncher.launch(arrayOf(
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                                "application/vnd.ms-excel",
                                                "text/csv",
                                                "text/comma-separated-values",
                                                "text/plain"
                                            ))
                                        }
                                    ) {
                                        Icon(Icons.Default.TableChart, contentDescription = null)
                                        Text("Escolher Ficheiro", modifier = Modifier.padding(start = 8.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        item {
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
                                            text = selectedFileName ?: "Ficheiro seleccionado",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(Modifier.height(Spacing.md))
                            Button(
                                onClick = {
                                    val uri = selectedFileUri ?: return@Button
                                    isImporting = true
                                    scope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            importRepository.importFromUri(uri, groupId = groupId)
                                        }
                                        importResult = result
                                        isImporting = false
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
                                    Text("Importar Contactos")
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
                        imageVector = if (result.errors.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (result.errors.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                },
                title = {
                    Text(if (result.errors.isEmpty()) "Importação concluída" else "Erro na importação")
                },
                text = {
                    if (result.errors.isNotEmpty()) {
                        Text(result.errors.joinToString("\n"))
                    } else {
                        Text("${result.imported} contactos importados com sucesso.\n${result.skipped} ignorados.\n${result.invalidPhones} inválidos.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        importResult = null
                        if (result.errors.isEmpty()) {
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
