package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.MonthlySettlement
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GoogleSignInButton
import com.example.ui.components.MoneyText
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.PaymentStatusBadge
import com.example.ui.components.StatusBadge
import com.example.ui.state.CommerceUiState
import com.example.ui.theme.LocalCommerceColors
import com.example.util.FinancialPeriodSummary
import com.example.util.SettlementCalculator
import kotlin.math.abs
import kotlin.math.max

@Composable
fun MonthlySummaryScreen(
    uiState: CommerceUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCloudAuthSuccess: () -> Unit,
    onCloudAuthError: (String) -> Unit,
    onSyncSettlementsToFirestore: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val commerceColors = LocalCommerceColors.current
    val cloudSync = uiState.cloudSync

    val firestoreSummary = remember(
        uiState.cloudSettlements,
        uiState.selectedMonth
    ) {
        SettlementCalculator.summarizeSettlements(
            settlements = uiState.cloudSettlements,
            selectedYear = uiState.selectedMonth.year,
            selectedMonth = uiState.selectedMonth.month
        )
    }

    val selectedMonthCloudSettlements = remember(uiState.cloudSettlements, uiState.selectedMonth) {
        uiState.cloudSettlements.filter {
            it.periodYear.toInt() == uiState.selectedMonth.year &&
                it.periodMonth.toInt() == uiState.selectedMonth.month
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Firestore Connection Card
            item {
                FirestoreSummaryHeaderCard(
                    uiState = uiState,
                    onCloudAuthSuccess = onCloudAuthSuccess,
                    onCloudAuthError = onCloudAuthError,
                    onSyncSettlementsToFirestore = onSyncSettlementsToFirestore
                )
            }

            // Month selector
            item {
                MonthSelectorBar(
                    selectedMonth = uiState.selectedMonth,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth
                )
            }

            if (!cloudSync.isAuthenticated) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.Assessment,
                        title = "Conecta Firestore para calcular el resumen",
                        subtitle = "Inicia sesión con tu cuenta Google en la tarjeta superior para consultar ingresos, gastos y utilidad neta acumulada directamente desde Firestore."
                    )
                }
            } else {
                // Hero Accumulated Net Profit Card (Firestore)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("firestore_accumulated_summary_card"),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "UTILIDAD NETA ACUMULADA (FIRESTORE)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                StatusBadge(
                                    text = "${firestoreSummary.totalSettlementsCount} registro(s)",
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            }

                            MoneyText(
                                cents = firestoreSummary.accumulatedNetProfitCents,
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (firestoreSummary.accumulatedNetProfitCents >= 0L) {
                                    commerceColors.collected
                                } else {
                                    commerceColors.pending
                                }
                            )

                            Text(
                                text = "Acumulado hasta ${uiState.selectedMonth.label()}: " +
                                    com.example.util.CurrencyFormatter.format(
                                        firestoreSummary.accumulatedNetProfitUpToSelectedMonthCents
                                    ),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SummaryMiniTile(
                                    label = "Ingresos acumulados",
                                    cents = firestoreSummary.accumulatedGrossIncomeCents,
                                    valueColor = commerceColors.collected,
                                    modifier = Modifier.weight(1f)
                                )
                                SummaryMiniTile(
                                    label = "Gastos acumulados",
                                    cents = firestoreSummary.accumulatedExpensesCents,
                                    valueColor = commerceColors.pending,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Selected Month Breakdown Card + Canvas Bar Chart
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("firestore_monthly_breakdown_card"),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Resumen de ${uiState.selectedMonth.label()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                StatusBadge(
                                    text = "${firestoreSummary.monthSettlementsCount} actor(es)",
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                KpiColumnBox(
                                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                                    label = "Ingresos",
                                    cents = firestoreSummary.monthGrossIncomeCents,
                                    accentColor = commerceColors.collected,
                                    modifier = Modifier.weight(1f)
                                )
                                KpiColumnBox(
                                    icon = Icons.AutoMirrored.Rounded.TrendingDown,
                                    label = "Gastos",
                                    cents = firestoreSummary.monthExpensesCents,
                                    accentColor = commerceColors.pending,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Utilidad Neta del Mes",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    MoneyText(
                                        cents = firestoreSummary.monthNetProfitCents,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (firestoreSummary.monthNetProfitCents >= 0L) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            commerceColors.pending
                                        }
                                    )
                                }
                            }

                            // Proportional Bar Chart (Ingresos vs Gastos vs Utilidad Neta)
                            FinancialComparisonChart(
                                incomeCents = firestoreSummary.monthGrossIncomeCents,
                                expensesCents = firestoreSummary.monthExpensesCents,
                                netProfitCents = firestoreSummary.monthNetProfitCents,
                                incomeColor = commerceColors.collected,
                                expenseColor = commerceColors.pending,
                                profitColor = MaterialTheme.colorScheme.primary
                            )

                            HorizontalDivider()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Aporte + Alquiler del mes",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    MoneyText(
                                        cents = firestoreSummary.monthTotalDueCents,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Cobrado en el mes",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    MoneyText(
                                        cents = firestoreSummary.monthPaidCents,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = commerceColors.collected
                                    )
                                }
                            }
                        }
                    }
                }

                // Period-by-Period Firestore History
                if (firestoreSummary.periodHistory.isNotEmpty()) {
                    item {
                        Text(
                            text = "Evolución Mensual en Firestore (${firestoreSummary.periodHistory.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(
                        firestoreSummary.periodHistory,
                        key = { "period_${it.period.year}_${it.period.month}" }
                    ) { periodSummary ->
                        PeriodHistoryCard(
                            summary = periodSummary,
                            isSelected = periodSummary.period == uiState.selectedMonth
                        )
                    }
                } else {
                    item {
                        EmptyStateCard(
                            icon = Icons.Rounded.Assessment,
                            title = "Aún no hay liquidaciones en Firestore",
                            subtitle = if (uiState.allSettlements.isNotEmpty()) {
                                "Tienes ${uiState.allSettlements.size} liquidación(es) locales listas para subir a Firestore."
                            } else {
                                "Cuando registres liquidaciones mensuales de ingresos y gastos, aparecerán aquí calculadas en tiempo real."
                            },
                            actionLabel = if (uiState.allSettlements.isNotEmpty()) "Sincronizar a Firestore" else null,
                            onAction = if (uiState.allSettlements.isNotEmpty()) onSyncSettlementsToFirestore else null,
                            actionTestTag = "summary_empty_sync_button"
                        )
                    }
                }

                // Selected month detail per actor
                if (selectedMonthCloudSettlements.isNotEmpty()) {
                    item {
                        Text(
                            text = "Detalle de ${uiState.selectedMonth.label()} en Firestore (${selectedMonthCloudSettlements.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(selectedMonthCloudSettlements, key = { "cloud_set_${it.id}" }) { settlement ->
                        FirestoreActorSettlementCard(settlement = settlement)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun FirestoreSummaryHeaderCard(
    uiState: CommerceUiState,
    onCloudAuthSuccess: () -> Unit,
    onCloudAuthError: (String) -> Unit,
    onSyncSettlementsToFirestore: () -> Unit
) {
    val commerceColors = LocalCommerceColors.current
    val cloudSync = uiState.cloudSync

    val (badgeText, badgeBg, badgeFg, icon) = when {
        !cloudSync.isAuthenticated -> Quad(
            "Desconectado",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Rounded.CloudOff
        )
        cloudSync.isSettlementsPendingWrites -> Quad(
            "Subiendo cambios...",
            commerceColors.pendingContainer,
            commerceColors.pending,
            Icons.Rounded.CloudSync
        )
        cloudSync.isSettlementsFromCache && !cloudSync.isOnline -> Quad(
            "Caché Offline Firestore",
            commerceColors.pendingContainer,
            commerceColors.pending,
            Icons.Rounded.CloudOff
        )
        cloudSync.isSettlementsFromCache -> Quad(
            "Sincronizando Firestore...",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            Icons.Rounded.CloudSync
        )
        else -> Quad(
            "Firestore En Vivo",
            commerceColors.collectedContainer,
            commerceColors.collected,
            Icons.Rounded.CloudDone
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeFg,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Datos Financieros Firestore",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(text = badgeText, containerColor = badgeBg, contentColor = badgeFg)
            }

            if (!cloudSync.isAuthenticated) {
                Text(
                    text = "Conecta tu cuenta de Google para leer ingresos, gastos y utilidad neta acumulada desde Firestore.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                GoogleSignInButton(
                    onAuthSuccess = onCloudAuthSuccess,
                    onAuthError = onCloudAuthError,
                    label = "Conectar Google / Firestore"
                )
            } else {
                Text(
                    text = "Cuenta: ${cloudSync.userEmail} · ${uiState.cloudSettlements.size} liquidación(es) en Firestore",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                FilledTonalButton(
                    onClick = onSyncSettlementsToFirestore,
                    enabled = !cloudSync.isSyncing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("summary_sync_firestore_button")
                ) {
                    Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sincronizar liquidaciones con Firestore")
                }
            }
        }
    }
}

@Composable
private fun SummaryMiniTile(
    label: String,
    cents: Long,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            MoneyText(
                cents = cents,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = valueColor
            )
        }
    }
}

@Composable
private fun KpiColumnBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    cents: Long,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            MoneyText(
                cents = cents,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
        }
    }
}

@Composable
private fun FinancialComparisonChart(
    incomeCents: Long,
    expensesCents: Long,
    netProfitCents: Long,
    incomeColor: Color,
    expenseColor: Color,
    profitColor: Color
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val maxVal = max(1L, maxOf(incomeCents, expensesCents, abs(netProfitCents)))
    val incomeFraction = (incomeCents.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
    val expenseFraction = (expensesCents.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
    val profitFraction = (abs(netProfitCents).toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "COMPARATIVA MENSUAL (INGRESOS VS GASTOS VS UTILIDAD)",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            val barHeight = (size.height - 16.dp.toPx()) / 3f
            val gap = 8.dp.toPx()
            val radius = CornerRadius(barHeight / 2f, barHeight / 2f)

            val entries = listOf(
                incomeFraction to incomeColor,
                expenseFraction to expenseColor,
                profitFraction to if (netProfitCents >= 0L) profitColor else expenseColor
            )

            entries.forEachIndexed { index, (fraction, color) ->
                val top = index * (barHeight + gap)
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(0f, top),
                    size = Size(size.width, barHeight),
                    cornerRadius = radius
                )
                val fillWidth = (size.width * fraction).coerceAtLeast(if (fraction > 0f) barHeight else 0f)
                if (fillWidth > 0f) {
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(0f, top),
                        size = Size(fillWidth, barHeight),
                        cornerRadius = radius
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ChartLegendDot(color = incomeColor, label = "Ingresos")
            ChartLegendDot(color = expenseColor, label = "Gastos")
            ChartLegendDot(
                color = if (netProfitCents >= 0L) profitColor else expenseColor,
                label = if (netProfitCents >= 0L) "Utilidad Neta" else "Pérdida Neta"
            )
        }
    }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PeriodHistoryCard(
    summary: FinancialPeriodSummary,
    isSelected: Boolean
) {
    val commerceColors = LocalCommerceColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = summary.period.label(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                StatusBadge(
                    text = "${summary.settlementsCount} liq.",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Ingresos:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MoneyText(
                    cents = summary.grossIncomeCents,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = commerceColors.collected
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Gastos:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MoneyText(
                    cents = summary.operatingExpensesCents,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = commerceColors.pending
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Utilidad neta del mes:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MoneyText(
                    cents = summary.netProfitCents,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Utilidad neta acumulada:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                MoneyText(
                    cents = summary.accumulatedNetProfitUpToPeriodCents,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (summary.accumulatedNetProfitUpToPeriodCents >= 0L) {
                        commerceColors.collected
                    } else {
                        commerceColors.pending
                    }
                )
            }
        }
    }
}

@Composable
private fun FirestoreActorSettlementCard(settlement: MonthlySettlement) {
    val commerceColors = LocalCommerceColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = settlement.actorName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (settlement.localeName.isNotBlank()) {
                        Text(
                            text = settlement.localeName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                PaymentStatusBadge(status = settlement.paymentStatusEnum())
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Ingresos:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MoneyText(cents = settlement.grossIncomeCents, style = MaterialTheme.typography.bodySmall, color = commerceColors.collected)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Gastos:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MoneyText(cents = settlement.operatingExpensesCents, style = MaterialTheme.typography.bodySmall, color = commerceColors.pending)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Utilidad neta:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                MoneyText(
                    cents = settlement.netProfitCents,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
