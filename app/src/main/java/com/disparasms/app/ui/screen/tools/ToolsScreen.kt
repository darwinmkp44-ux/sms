package com.disparasms.app.ui.screen.tools

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Quickreply
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.disparasms.app.ui.navigation.NavRoutes
import com.disparasms.app.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ferramentas",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Text(
                text = "Utilitários premium para otimizar seus disparos de mensagens em massa.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)
            )

            Spacer(Modifier.height(Spacing.md))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    ToolDashboardCard(
                        title = "Limpar Números",
                        description = "Formate e valide listas de telefones rapidamente.",
                        icon = Icons.Default.CleaningServices,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { navController.navigate(NavRoutes.ToolsNumberCleaner.route) }
                    )
                }
                item {
                    ToolDashboardCard(
                        title = "Modelos",
                        description = "Crie e gerencie templates de mensagens de reuso.",
                        icon = Icons.Default.Description,
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = { navController.navigate(NavRoutes.ToolsTemplates.route) }
                    )
                }
                item {
                    ToolDashboardCard(
                        title = "Teste Rápido",
                        description = "Valide o sinal enviando SMS individual de teste.",
                        icon = Icons.Default.Quickreply,
                        color = MaterialTheme.colorScheme.tertiary,
                        onClick = { navController.navigate(NavRoutes.ToolsQuickTest.route) }
                    )
                }
                item {
                    ToolDashboardCard(
                        title = "Relatórios",
                        description = "Gráficos de desempenho e exportação em CSV.",
                        icon = Icons.Default.BarChart,
                        color = Color(0xFF4CAF50), // Green for success / graph
                        onClick = { navController.navigate(NavRoutes.ToolsReports.route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolDashboardCard(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
