package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DeclarationSource
import com.example.data.model.EconomicActor
import com.example.data.model.InvoiceRecord
import com.example.data.model.MonthlySettlement
import com.example.data.model.PaymentEvent
import com.example.data.model.PaymentVerificationStatus
import com.example.ui.components.CloudSyncCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LegalTypeBadge
import com.example.ui.components.ManagerBankAccountCard
import com.example.ui.components.MoneyText
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.PaymentEventCard
import com.example.ui.components.PaymentStatusBadge
import com.example.ui.components.StatusBadge
import com.example.ui.components.VersatInvoicesCard
import com.example.ui.state.CommerceUiState
import com.example.ui.theme.LocalCommerceColors

private enum class ManagerPaymentFilter(val label: String) {
    ALL("Todos"),
    ACTORS_ONLY("Solo Actores"),
    UNVERIFIED("Por verificar"),
    CONFIRMED("Confirmados")
}

@Composable
fun DashboardScreen(
    uiState: CommerceUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onAddLocale: () -> Unit,
    onAddActor: () -> Unit,
    onAddSettlement: (EconomicActor?) -> Unit,
    onOpenPaymentDialog: (MonthlySettlement) -> Unit,
    onEditSettlement: (MonthlySettlement) -> Unit,
    onSavePaymentConfig: (
        bankName: String,
        bankAccount: String,
        accountHolder: String,
        qrPaymentUrl: String
    ) -> Unit,
    onNavigateToActors: () -> Unit,
    onNavigateToLocales: () -> Unit,
    onNavigateToSettlements: () -> Unit,
    onNavigateToLiveMonitor: () -> Unit,
    onNavigateToMonthlySummary: () -> Unit,
    onVerifyPaymentEvent: (eventId: String, status: PaymentVerificationStatus, note: String) -> Unit,
    onDownloadReceiptPdf: (PaymentEvent) -> Unit,
    onAddInvoice: () -> Unit,
    onCreateInvoiceFromEvent: (PaymentEvent) -> Unit,
    onEditInvoice: (InvoiceRecord) -> Unit,
    onDeleteInvoice: (String) -> Unit,
    onExportVersatCsv: () -> Unit,
    onExportVersatXml: () -> Unit,
    onCloudAuthSuccess: () -> Unit,
    onCloudAuthError: (String) -> Unit,
    onJoinWorkspace: (String) -> Unit,
    onCreateNewWorkspace: () -> Unit,
    onForceSync: () -> Unit,
    onCloudSignOutComplete: () -> Unit
) {
    val commerceColors = LocalCommerceColors.current
    val metrics = uiState.metrics
    val firestoreSummary = remember(uiState.cloudSettlements, uiState.selectedMonth) {
        com.example.util.SettlementCalculator.summarizeSettlements(
            settlements = uiState.cloudSettlements,
            selectedYear = uiState.selectedMonth.year,
            selectedMonth = uiState.selectedMonth.month
        )
    }
    val collectionProgress = if (metrics.totalDueCents > 0L) {
        (metrics.totalPaidCents.toFloat() / metrics.totalDueCents.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    var paymentFilter by remember { mutableStateOf(ManagerPaymentFilter.ALL) }
    val filteredPaymentEvents = remember(uiState.paymentEvents, paymentFilter) {
        when (paymentFilter) {
            ManagerPaymentFilter.ALL -> uiState.paymentEvents
            ManagerPaymentFilter.ACTORS_ONLY -> uiState.paymentEvents.filter {
                it.reportedByEnum() == DeclarationSource.ACTOR
            }
            ManagerPaymentFilter.UNVERIFIED -> uiState.paymentEvents.filter {
                it.verificationStatusEnum() == PaymentVerificationStatus.POR_VERIFICAR
            }
            ManagerPaymentFilter.CONFIRMED -> uiState.paymentEvents.filter {
                it.verificationStatusEnum() == PaymentVerificationStatus.CONFIRMADO
            }
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
            // Hero Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(136.dp)
                        .clip(MaterialTheme.shapes.large)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_bauta_header),
                        contentDescription = stringResource(R.string.cd_bauta_banner),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF0B3B2E).copy(alpha = 0.92f),
                                        Color(0xFF0B3B2E).copy(alpha = 0.55f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "EMPRESA MUNICIPAL DE COMERCIO · PANEL DEL MANAGER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF5B731)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Bauta · Control de Arrendamientos",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "30% de utilidad neta + alquiler de espacio (CUP)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                        uiState.activeManagerUser?.let { manager ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Conectado: ${manager.fullName} (@${manager.username} · ${manager.roleEnum().label})",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF5B731),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Inline Company Bank Account & Transfermóvil Form
            item {
                ManagerBankAccountCard(
                    paymentConfig = uiState.paymentConfig,
                    onSavePaymentConfig = onSavePaymentConfig
                )
            }

            // Cloud Sync Card for immediate live visibility on Manager Dashboard
            item {
                CloudSyncCard(
                    cloudSync = uiState.cloudSync,
                    isActorPortal = false,
                    onAuthSuccess = onCloudAuthSuccess,
                    onAuthError = onCloudAuthError,
                    onJoinWorkspace = onJoinWorkspace,
                    onCreateNewWorkspace = onCreateNewWorkspace,
                    onForceSync = onForceSync,
                    onSignOutComplete = onCloudSignOutComplete
                )
            }

            item {
                MonthSelectorBar(
                    selectedMonth = uiState.selectedMonth,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth
                )
            }

            // Monthly Financial Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Total a cobrar en ${uiState.selectedMonth.label()}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            MoneyText(
                                cents = metrics.totalDueCents,
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        LinearProgressIndicator(
                            progress = { collectionProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = commerceColors.collected,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Cobrado",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                MoneyText(
                                    cents = metrics.totalPaidCents,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = commerceColors.collected
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Pendiente",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                MoneyText(
                                    cents = metrics.totalPendingCents,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (metrics.totalPendingCents > 0L) commerceColors.pending else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "30% Utilidad Neta",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    MoneyText(
                                        cents = metrics.totalProfitShareCents,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Alquiler de Espacio",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    MoneyText(
                                        cents = metrics.totalSpaceRentCents,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Firestore Monthly Summary Entry Card
            item {
                Card(
                    onClick = onNavigateToMonthlySummary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_monthly_summary_card"),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                    ),
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
                                    imageVector = Icons.Rounded.Assessment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Resumen Mensual · Firestore",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            TextButton(onClick = onNavigateToMonthlySummary) {
                                Text("Ver detalle")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Ingresos mes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                MoneyText(
                                    cents = firestoreSummary.monthGrossIncomeCents,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = commerceColors.collected
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Gastos mes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                MoneyText(
                                    cents = firestoreSummary.monthExpensesCents,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = commerceColors.pending
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Utilidad acumulada",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                MoneyText(
                                    cents = firestoreSummary.accumulatedNetProfitCents,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Quick Stats Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickStatCard(
                        icon = Icons.Rounded.Groups,
                        value = "${metrics.activeActorsCount}",
                        label = "Actores activos",
                        onClick = onNavigateToActors,
                        modifier = Modifier.weight(1f)
                    )
                    QuickStatCard(
                        icon = Icons.Rounded.Storefront,
                        value = "${metrics.leasedLocalesCount}/${metrics.totalLocalesCount}",
                        label = "Locales arrendados",
                        onClick = onNavigateToLocales,
                        modifier = Modifier.weight(1f)
                    )
                    QuickStatCard(
                        icon = Icons.Rounded.Payments,
                        value = "${metrics.pendingCount}",
                        label = "Por cobrar",
                        onClick = onNavigateToSettlements,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Action Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onAddLocale,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("quick_add_locale_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Rounded.Apartment, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Local", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    FilledTonalButton(
                        onClick = onAddActor,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("quick_add_actor_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Actor", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = { onAddSettlement(null) },
                        modifier = Modifier
                            .weight(1.25f)
                            .minimumInteractiveComponentSize()
                            .testTag("quick_add_settlement_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Rounded.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Liquidar mes", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // Actor-Registered Payments List Section inside Manager Panel
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(commerceColors.collected)
                            )
                            Text(
                                text = "Pagos Registrados por Actores (${uiState.paymentEvents.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (uiState.unverifiedPaymentsCount > 0) {
                            StatusBadge(
                                text = "${uiState.unverifiedPaymentsCount} por verificar",
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        } else {
                            TextButton(onClick = onNavigateToLiveMonitor) {
                                Text("Monitor En Vivo")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ManagerPaymentFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = paymentFilter == filter,
                                onClick = { paymentFilter = filter },
                                label = { Text(filter.label) },
                                modifier = Modifier.testTag("manager_payments_filter_${filter.name}")
                            )
                        }
                    }
                }
            }

            if (filteredPaymentEvents.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.NotificationsActive,
                        title = "Sin pagos registrados en esta vista",
                        subtitle = "Cuando los actores económicos reporten transferencias o pagos por QR Transfermóvil desde el Portal Actor, aparecerán en esta lista en tiempo real.",
                        actionLabel = "Abrir Monitor En Vivo",
                        onAction = onNavigateToLiveMonitor,
                        actionTestTag = "dashboard_open_live_monitor_button"
                    )
                }
            } else {
                items(filteredPaymentEvents.take(8), key = { "dash_pay_${it.id}" }) { event ->
                    val alreadyInvoiced = uiState.invoices.any { it.paymentEventId == event.id }
                    PaymentEventCard(
                        event = event,
                        onConfirm = {
                            onVerifyPaymentEvent(
                                event.id,
                                PaymentVerificationStatus.CONFIRMADO,
                                event.note.ifBlank { "Confirmado desde Panel del Manager" }
                            )
                        },
                        onObserve = {
                            onVerifyPaymentEvent(
                                event.id,
                                PaymentVerificationStatus.OBSERVADO,
                                "Referencia pendiente de revisión bancaria"
                            )
                        },
                        onDownloadReceiptPdf = { onDownloadReceiptPdf(event) },
                        onCreateInvoice = { onCreateInvoiceFromEvent(event) },
                        alreadyInvoiced = alreadyInvoiced
                    )
                }
                if (filteredPaymentEvents.size > 8) {
                    item {
                        OutlinedButton(
                            onClick = onNavigateToLiveMonitor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                        ) {
                            Text("Ver todos los pagos (${filteredPaymentEvents.size}) en Monitor En Vivo")
                        }
                    }
                }
            }

            // Versat Sarasola Invoicing & Export Card
            item {
                VersatInvoicesCard(
                    invoices = uiState.invoices,
                    pendingExportCount = uiState.pendingExportInvoicesCount,
                    onAddInvoice = onAddInvoice,
                    onEditInvoice = onEditInvoice,
                    onDeleteInvoice = onDeleteInvoice,
                    onExportCsv = onExportVersatCsv,
                    onExportXml = onExportVersatXml
                )
            }

            // First-run onboarding guide if empty
            if (uiState.locales.isEmpty() && uiState.actors.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.Storefront,
                        title = "Comienza registrando tus locales y actores",
                        subtitle = "1) Registra los locales municipales disponibles.\n2) Crea tus actores económicos (TCP, MIPYME, CNA, PDL).\n3) Liquida cada mes el 30% de utilidad neta + alquiler en CUP.",
                        actionLabel = "Registrar primer local",
                        onAction = onAddLocale,
                        actionTestTag = "onboarding_add_locale_button"
                    )
                }
            }

            // Pending settlements for the month
            if (uiState.pendingMonthSettlements.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cobros pendientes (${uiState.pendingMonthSettlements.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onNavigateToSettlements) {
                            Text("Ver todos")
                        }
                    }
                }

                items(uiState.pendingMonthSettlements, key = { it.id }) { settlement ->
                    Card(
                        onClick = { onEditSettlement(settlement) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = settlement.actorName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    PaymentStatusBadge(status = settlement.paymentStatusEnum())
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Saldo pendiente:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                MoneyText(
                                    cents = settlement.remainingCents,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = commerceColors.pending
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            OutlinedButton(
                                onClick = { onOpenPaymentDialog(settlement) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text("Cobrar")
                            }
                        }
                    }
                }
            }

            // Actors without settlement for selected month
            if (uiState.actorsWithoutMonthSettlement.isNotEmpty()) {
                item {
                    Text(
                        text = "Actores sin liquidar en ${uiState.selectedMonth.label()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(uiState.actorsWithoutMonthSettlement, key = { it.id }) { actor ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = actor.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    LegalTypeBadge(type = actor.legalTypeEnum())
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = actor.localeName.ifBlank { "Sin local asignado" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = { onAddSettlement(actor) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text("Liquidar")
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun QuickStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
