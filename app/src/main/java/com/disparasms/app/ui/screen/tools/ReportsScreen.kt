package com.disparasms.app.ui.screen.tools

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.disparasms.app.data.local.entity.CampaignEntity
import com.disparasms.app.data.repository.CampaignRepository
import com.disparasms.app.ui.components.ModernCard
import com.disparasms.app.ui.components.SectionHeader
import com.disparasms.app.ui.theme.Spacing
import com.disparasms.app.ui.theme.successGreen
import com.disparasms.app.ui.theme.errorRed
import com.disparasms.app.ui.theme.warningOrange
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ReportsUiState(
    val campaigns: List<CampaignEntity> = emptyList(),
    val totalSent: Int = 0,
    val totalDelivered: Int = 0,
    val totalFailed: Int = 0,
    val totalPending: Int = 0,
    val isExporting: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val campaignRepository: CampaignRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val uiState: StateFlow<ReportsUiState> = campaignRepository.observeAll()
        .map { campaigns ->
            ReportsUiState(
                campaigns = campaigns,
                totalSent = campaigns.sumOf { it.sentCount },
                totalDelivered = campaigns.sumOf { it.deliveredCount },
                totalFailed = campaigns.sumOf { it.failedCount },
                totalPending = campaigns.sumOf { it.pendingCount },
                isLoading = false
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportsUiState())

    private val _isExporting = MutableStateFlow(false)

    fun exportCsv() {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val campaigns = uiState.value.campaigns
                val csvContent = buildString {
                    appendLine("Campanha,Status,Total,Enviados,Entregues,Falhas,Pendentes,Criada Em")
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    campaigns.forEach { c ->
                        appendLine("\"${c.name}\",${c.status},${c.totalContacts},${c.sentCount},${c.deliveredCount},${c.failedCount},${c.pendingCount},${sdf.format(Date(c.createdAt))}")
                    }
                }

                withContext(Dispatchers.IO) {
                    val file = File(appContext.cacheDir, "relatorio_campanhas_${System.currentTimeMillis()}.csv")
                    file.writeText(csvContent)

                    val uri = FileProvider.getUriForFile(
                        appContext,
                        "${appContext.packageName}.fileprovider",
                        file
                    )

                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    appContext.startActivity(Intent.createChooser(shareIntent, "Exportar Relatório").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            } catch (e: Exception) {
                // silently handle
            } finally {
                _isExporting.value = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    navController: NavController,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Relatórios",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
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
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Stats overview
            item {
                Spacer(Modifier.height(Spacing.sm))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    StatsBox(
                        label = "Enviados",
                        value = state.totalSent.toString(),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatsBox(
                        label = "Entregues",
                        value = state.totalDelivered.toString(),
                        color = MaterialTheme.colorScheme.successGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatsBox(
                        label = "Falhas",
                        value = state.totalFailed.toString(),
                        color = MaterialTheme.colorScheme.errorRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Donut chart
            item {
                Spacer(Modifier.height(Spacing.xl))
                SectionHeader(title = "Taxa de Entrega")
                ModernCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg)
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val delivered = state.totalDelivered.toFloat()
                        val failed = state.totalFailed.toFloat()
                        val sent = state.totalSent.toFloat()
                        val total = (delivered + failed + sent).coerceAtLeast(1f)
                        val deliveredAngle = (delivered / total) * 360f
                        val failedAngle = (failed / total) * 360f
                        val sentAngle = (sent / total) * 360f

                        val deliveredColor = MaterialTheme.colorScheme.successGreen
                        val failedColor = MaterialTheme.colorScheme.errorRed
                        val sentColor = MaterialTheme.colorScheme.primary

                        Box(
                            modifier = Modifier.size(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(180.dp)) {
                                val strokeWidth = 32f
                                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                                val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                                drawArc(
                                    color = deliveredColor,
                                    startAngle = -90f,
                                    sweepAngle = deliveredAngle,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth)
                                )
                                drawArc(
                                    color = failedColor,
                                    startAngle = -90f + deliveredAngle,
                                    sweepAngle = failedAngle,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth)
                                )
                                drawArc(
                                    color = sentColor,
                                    startAngle = -90f + deliveredAngle + failedAngle,
                                    sweepAngle = sentAngle,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth)
                                )
                            }

                            val rate = if (total > 0) ((delivered / total) * 100).toInt() else 0
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$rate%",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "entregues",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(Spacing.lg))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
                        ) {
                            ChartLegendItem("Entregues", deliveredColor)
                            ChartLegendItem("Falhas", failedColor)
                            ChartLegendItem("Enviados", sentColor)
                        }
                    }
                }
            }

            // Export CSV Button
            item {
                Spacer(Modifier.height(Spacing.lg))
                Button(
                    onClick = { viewModel.exportCsv() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Exportar CSV")
                }
            }

            // Campaign list
            item {
                Spacer(Modifier.height(Spacing.xl))
                SectionHeader(title = "Campanhas (${state.campaigns.size})")
            }

            items(state.campaigns, key = { it.id }) { campaign ->
                CampaignReportCard(campaign = campaign)
            }
        }
    }
}

@Composable
private fun StatsBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChartLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CampaignReportCard(campaign: CampaignEntity) {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    ModernCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = campaign.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = campaign.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SmallStat(Icons.Default.Send, campaign.sentCount.toString(), MaterialTheme.colorScheme.primary)
                SmallStat(Icons.Default.CheckCircle, campaign.deliveredCount.toString(), MaterialTheme.colorScheme.successGreen)
                SmallStat(Icons.Default.Error, campaign.failedCount.toString(), MaterialTheme.colorScheme.errorRed)
                Text(
                    text = sdf.format(Date(campaign.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SmallStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}
