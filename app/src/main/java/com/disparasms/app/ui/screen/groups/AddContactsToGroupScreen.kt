package com.disparasms.app.ui.screen.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.disparasms.app.data.local.entity.ContactEntity
import com.disparasms.app.di.DaoEntryPoint
import com.disparasms.app.ui.components.EmptyState
import com.disparasms.app.ui.theme.Spacing
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactsToGroupScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val groupId = navController
        .currentBackStackEntry
        ?.arguments
        ?.getLong("groupId", -1L)
        .takeIf { it != -1L }

    val contactRepository = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            DaoEntryPoint::class.java
        ).contactRepository()
    }

    var allContacts by remember { mutableStateOf<List<ContactEntity>>(emptyList()) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        contactRepository.observeAll().collect { contacts ->
            allContacts = contacts
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = "Adicionar Contactos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.Close, contentDescription = "Voltar")
                }
            },
            actions = {
                if (allContacts.isNotEmpty()) {
                    val allSelected = selectedIds.size == allContacts.size
                    TextButton(
                        onClick = {
                            selectedIds = if (allSelected) emptySet()
                            else allContacts.map { it.id }.toSet()
                        }
                    ) {
                        Icon(
                            imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (allSelected) "Nenhum" else "Todos",
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        if (allContacts.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Person,
                title = "Sem contactos disponíveis",
                description = "Importe contactos primeiro para adicionar a este grupo"
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(allContacts, key = { it.id }) { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = contact.id in selectedIds,
                            onCheckedChange = {
                                selectedIds = if (contact.id in selectedIds)
                                    selectedIds - contact.id
                                else selectedIds + contact.id
                            }
                        )
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.width(Spacing.sm))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contact.fullName ?: contact.firstName ?: "Sem nome",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = contact.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (groupId != null && selectedIds.isNotEmpty()) {
                        isSaving = true
                        scope.launch {
                            try {
                                contactRepository.assignToGroup(selectedIds.toList(), groupId)
                                navController.popBackStack()
                            } catch (_: Exception) {
                                isSaving = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                enabled = selectedIds.isNotEmpty() && !isSaving && groupId != null
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Text(
                    text = "Adicionar ${selectedIds.size} contactos",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
