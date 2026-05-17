package com.disparasms.app.ui.screen.groups

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.disparasms.app.data.repository.GroupRepository
import com.disparasms.app.ui.theme.Spacing
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val groupRepository = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            GroupRepositoryEntryPoint::class.java
        ).groupRepository()
    }

    fun saveGroup() {
        if (name.isBlank()) return
        isSaving = true
        scope.launch {
            try {
                groupRepository.createWithContactCount(name.trim(), description.trim().ifBlank { null })
                navController.popBackStack()
            } catch (_: Exception) {
                isSaving = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = "Novo Grupo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (!isSaving) navController.popBackStack()
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Cancelar")
                }
            },
            actions = {
                IconButton(
                    onClick = { saveGroup() },
                    enabled = name.isNotBlank() && !isSaving
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Salvar")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.lg)
        ) {
            Text(
                text = "Nome do grupo",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.sm))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ex: Clientes VIP") },
                singleLine = true,
                enabled = !isSaving
            )

            Spacer(Modifier.height(Spacing.lg))

            Text(
                text = "Descrição (opcional)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.sm))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ex: Clientes que compraram em 2025") },
                minLines = 3,
                enabled = !isSaving
            )

            Spacer(Modifier.height(Spacing.xxl))

            Button(
                onClick = { saveGroup() },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && !isSaving
            ) {
                Text(if (isSaving) "A salvar..." else "Criar Grupo")
            }
        }
    }
}
