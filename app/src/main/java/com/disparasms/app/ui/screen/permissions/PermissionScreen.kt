package com.disparasms.app.ui.screen.permissions

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.disparasms.app.ui.theme.CornerRadius
import com.disparasms.app.ui.theme.Spacing
import com.disparasms.app.ui.theme.successGreen

data class AppPermission(
    val icon: ImageVector,
    val name: String,
    val description: String,
    val permission: String
)

private val requiredPermissions = listOf(
    AppPermission(
        icon = Icons.Default.Send,
        name = "Enviar SMS",
        description = "Necessário para enviar as campanhas de SMS",
        permission = Manifest.permission.SEND_SMS
    ),
    AppPermission(
        icon = Icons.Default.SimCard,
        name = "Estado do Telefone",
        description = "Necessário para detectar os SIMs e operadoras",
        permission = Manifest.permission.READ_PHONE_STATE
    ),
    AppPermission(
        icon = Icons.Default.Contacts,
        name = "Contactos",
        description = "Necessário para importar contactos do telefone",
        permission = Manifest.permission.READ_CONTACTS
    )
)

@Composable
fun PermissionScreen(onAllGranted: () -> Unit) {
    val context = LocalContext.current
    var grantedPermissions by remember {
        mutableStateOf(
            requiredPermissions.map { it.permission to false }.toMap().toMutableMap()
        )
    }
    var showRationale by remember { mutableStateOf(false) }
    val allGranted = requiredPermissions.all {
        ContextCompat.checkSelfPermission(context, it.permission) == PackageManager.PERMISSION_GRANTED
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grantResults ->
        grantResults.forEach { (perm, granted) ->
            grantedPermissions[perm] = granted
        }
        if (requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it.permission) == PackageManager.PERMISSION_GRANTED
            }) {
            onAllGranted()
        }
    }

    if (allGranted) {
        onAllGranted()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Permissões Necessárias",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "O Dispara SMS precisa das seguintes permissões para funcionar:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        requiredPermissions.forEach { perm ->
            val isGranted = ContextCompat.checkSelfPermission(context, perm.permission) == PackageManager.PERMISSION_GRANTED
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(CornerRadius.md),
                colors = CardDefaults.cardColors(
                    containerColor = if (isGranted)
                        MaterialTheme.colorScheme.successGreen.copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isGranted) MaterialTheme.colorScheme.successGreen.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isGranted) Icons.Default.CheckCircle else perm.icon,
                            contentDescription = null,
                            tint = if (isGranted) MaterialTheme.colorScheme.successGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(Spacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = perm.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = perm.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                val needed = requiredPermissions
                    .filter { ContextCompat.checkSelfPermission(context, it.permission) != PackageManager.PERMISSION_GRANTED }
                    .map { it.permission }
                    .toTypedArray()
                permissionLauncher.launch(needed)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(CornerRadius.md)
        ) {
            Text(
                text = "Conceder Permissões",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
