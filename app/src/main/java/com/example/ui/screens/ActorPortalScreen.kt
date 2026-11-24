package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.example.data.model.DeclarationSource
import com.example.data.model.EconomicActor
import com.example.data.model.LegalType
import com.example.data.model.MonthlySettlement
import com.example.data.model.PaymentChannel
import com.example.data.model.PaymentConfig
import com.example.data.model.PaymentEvent
import com.example.data.model.PaymentStatus
import com.example.data.model.PaymentVerificationStatus
import com.example.ui.components.CloudSyncCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LegalTypeBadge
import com.example.ui.components.MoneyText
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.PaymentStatusBadge
import com.example.ui.components.StatusBadge
import com.example.ui.state.CommerceUiState
import com.example.ui.theme.LocalCommerceColors
import com.example.util.CurrencyFormatter
import com.example.util.TransfermovilHelper
import com.example.util.YearMonthKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ActorHistoryTab(val label: String) {
    ALL("Todo"),
    SETTLEMENTS("Liquidaciones"),
    PAYMENTS("Pagos")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActorPortalScreen(
    uiState: CommerceUiState,
    selectedActorId: String?,
    defaultActorName: String = "",
    onSelectActor: (String) -> Unit,
    onUpdateActorProfile: (
        actor: EconomicActor,
        name: String,
        legalType: LegalType,
        representativeName: String,
        phone: String,
        taxId: String
    ) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDeclareMonth: (EconomicActor) -> Unit,
    onReportDigitalPayment: (MonthlySettlement) -> Unit,
    onReportQuickPayment: (
        actorName: String,
        periodYear: Int,
        periodMonth: Int,
        amountCents: Long,
        channel: PaymentChannel,
        reference: String,
        note: String,
        actorId: String
    ) -> Unit,
    onDownloadEventReceiptPdf: (PaymentEvent) -> Unit,
    onDownloadSettlementReceiptPdf: (MonthlySettlement) -> Unit,
    onCloudAuthSuccess: () -> Unit,
    onCloudAuthError: (String) -> Unit,
    onJoinWorkspace: (String) -> Unit,
    onForceSync: () -> Unit,
    onCloudSignOutComplete: () -> Unit,
    onChangeBusinessSession: () -> Unit,
    onRequestPrivateAccess: () -> Unit
) {
    val commerceColors = LocalCommerceColors.current
    val activeActors = uiState.actors
    val currentActor = remember(activeActors, selectedActorId, uiState.cloudSync.userEmail) {
        activeActors.firstOrNull { it.id == selectedActorId }
            ?: uiState.cloudSync.userEmail.takeIf { it.isNotBlank() }?.let { email ->
                activeActors.firstOrNull { it.actorEmail.equals(email, ignoreCase = true) }
            }
            ?: activeActors.firstOrNull()
    }
    var actorDropdownExpanded by remember { mutableStateOf(false) }
    var showQuickReportDialog by remember { mutableStateOf(false) }
    var editingProfileActor by remember { mutableStateOf<EconomicActor?>(null) }
    var historyTab by remember { mutableStateOf(ActorHistoryTab.ALL) }
    var hiddenFooterTaps by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        if (currentActor == null) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    CloudSyncCard(
                        cloudSync = uiState.cloudSync,
                        isActorPortal = true,
                        onAuthSuccess = onCloudAuthSuccess,
                        onAuthError = onCloudAuthError,
                        onJoinWorkspace = onJoinWorkspace,
                        onCreateNewWorkspace = {},
                        onForceSync = onForceSync,
                        onSignOutComplete = onCloudSignOutComplete
                    )
                }

                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.Storefront,
                        title = "Completa tu registro obligatorio",
                        subtitle = "Para declarar tus operaciones, consultar liquidaciones y descargar recibos PDF debes registrar tu negocio o conectar tu cuenta Google vinculada.",
                        actionLabel = "Ir al registro del negocio",
                        onAction = onChangeBusinessSession,
                        actionTestTag = "portal_complete_registration_button"
                    )
                }

                item {
                    OutlinedButton(
                        onClick = onChangeBusinessSession,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("portal_change_business_button")
                    ) {
                        Text("Registrar / Cambiar negocio")
                    }
                }

                item {
                    Text(
                        text = "Empresa Municipal de Comercio Bauta · Artemisa",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                hiddenFooterTaps += 1
                                if (hiddenFooterTaps >= 5) {
                                    hiddenFooterTaps = 0
                                    onRequestPrivateAccess()
                                }
                            }
                            .padding(vertical = 8.dp)
                    )
                }
            }
        } else {
            val actorSettlements = remember(uiState.allSettlements, currentActor.id) {
                uiState.allSettlements.filter { it.actorId == currentActor.id }
            }
            val actorPaymentEvents = remember(uiState.paymentEvents, currentActor.id, currentActor.name) {
                uiState.paymentEvents.filter {
                    it.actorId == currentActor.id ||
                        it.actorName.equals(currentActor.name, ignoreCase = true)
                }
            }
            val monthSettlement = remember(actorSettlements, uiState.selectedMonth) {
                actorSettlements.firstOrNull {
                    it.periodYear.toInt() == uiState.selectedMonth.year &&
                        it.periodMonth.toInt() == uiState.selectedMonth.month
                }
            }
            val totalPendingAllMonths = actorSettlements.sumOf { it.remainingCents }
            val totalGrossAllMonths = actorSettlements.sumOf { it.grossIncomeCents }
            val totalExpensesAllMonths = actorSettlements.sumOf { it.operatingExpensesCents }
            val totalNetProfitAllMonths = actorSettlements.sumOf { it.netProfitCents }
            val totalPaidAllMonths = actorSettlements.sumOf { it.paidCents }

            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    CloudSyncCard(
                        cloudSync = uiState.cloudSync,
                        isActorPortal = true,
                        onAuthSuccess = onCloudAuthSuccess,
                        onAuthError = onCloudAuthError,
                        onJoinWorkspace = onJoinWorkspace,
                        onCreateNewWorkspace = {},
                        onForceSync = onForceSync,
                        onSignOutComplete = onCloudSignOutComplete
                    )
                }

                item {
                    androidx.compose.material3.Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
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
                                Text(
                                    text = "MI ESPACIO COMERCIAL",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                OutlinedButton(
                                    onClick = onChangeBusinessSession,
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Text("Cambiar negocio")
                                }
                            }

                            ExposedDropdownMenuBox(
                                expanded = actorDropdownExpanded,
                                onExpandedChange = { actorDropdownExpanded = !actorDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = "${currentActor.name} (${currentActor.legalTypeEnum().label})",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Actor conectado") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = actorDropdownExpanded)
                                    },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                        .testTag("portal_actor_selector")
                                )
                                ExposedDropdownMenu(
                                    expanded = actorDropdownExpanded,
                                    onDismissRequest = { actorDropdownExpanded = false }
                                ) {
                                    activeActors.forEach { actor ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text("${actor.name} (${actor.legalTypeEnum().label})", fontWeight = FontWeight.SemiBold)
                                                    Text(
                                                        text = actor.localeName.ifBlank { "Sin local asignado" },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = {
                                                onSelectActor(actor.id)
                                                actorDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Business Registration & Contract Dossier Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Mi Negocio Registrado",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                LegalTypeBadge(type = currentActor.legalTypeEnum())
                            }
                            Text(
                                text = "Titular: ${currentActor.representativeName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Tel: ${currentActor.phone.ifBlank { "Por completar" }} · NIT/Lic: ${currentActor.taxId.ifBlank { "Por completar" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (currentActor.actorEmail.isNotBlank()) {
                                Text(
                                    text = "Correo vinculado: ${currentActor.actorEmail}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "Local arrendado: ${currentActor.localeName.ifBlank { "Por asignar en Comercio Bauta" }}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Alquiler mensual",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    MoneyText(
                                        cents = currentActor.monthlyRentCents,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Aporte utilidad",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${currentActor.profitSharePercent}% neta",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Deuda acumulada",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    MoneyText(
                                        cents = totalPendingAllMonths,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (totalPendingAllMonths > 0L) commerceColors.pending else commerceColors.collected
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { editingProfileActor = currentActor },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("portal_edit_actor_button")
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Editar mis datos de registro")
                            }
                        }
                    }
                }

                // Cumulative Operations Summary for this Actor
                item {
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
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ReceiptLong,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Resumen de Mis Operaciones",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                StatusBadge(
                                    text = "${actorSettlements.size} liq · ${actorPaymentEvents.size} pagos",
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Ingresos totales", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    MoneyText(
                                        cents = totalGrossAllMonths,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Gastos totales", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    MoneyText(
                                        cents = totalExpensesAllMonths,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Utilidad neta", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    MoneyText(
                                        cents = totalNetProfitAllMonths,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Total pagado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    MoneyText(
                                        cents = totalPaidAllMonths,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = commerceColors.collected
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Saldo pendiente", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    MoneyText(
                                        cents = totalPendingAllMonths,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (totalPendingAllMonths > 0L) commerceColors.pending else commerceColors.collected
                                    )
                                }
                            }
                        }
                    }
                }

                // Transfermóvil Digital Payment Instructions Card
                item {
                    TransfermovilInstructionsCard(
                        paymentConfig = uiState.paymentConfig,
                        onOpenQuickReport = { showQuickReportDialog = true }
                    )
                }

                item {
                    MonthSelectorBar(
                        selectedMonth = uiState.selectedMonth,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth
                    )
                }

                // Current Month Action Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (monthSettlement == null) {
                                Text(
                                    text = "Declaración de ${uiState.selectedMonth.label()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Aún no has enviado la declaración de ingresos y gastos de este mes. El sistema calculará tu ${currentActor.profitSharePercent}% de utilidad neta más el alquiler del espacio.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { onDeclareMonth(currentActor) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .testTag("actor_declare_month_button")
                                ) {
                                    Icon(Icons.Rounded.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Declarar utilidad de ${uiState.selectedMonth.label()}")
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Estado de ${uiState.selectedMonth.label()}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    PaymentStatusBadge(status = monthSettlement.paymentStatusEnum())
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total a pagar:", style = MaterialTheme.typography.bodyMedium)
                                    MoneyText(
                                        cents = monthSettlement.totalDueCents,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Pagado a Comercio Bauta:", style = MaterialTheme.typography.bodyMedium)
                                    MoneyText(
                                        cents = monthSettlement.paidCents,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = commerceColors.collected
                                    )
                                }
                                if (monthSettlement.paymentChannelEnum() != PaymentChannel.NO_DEFINIDO) {
                                    val refSuffix = if (monthSettlement.paymentReference.isNotBlank()) {
                                        " · Ref: ${monthSettlement.paymentReference}"
                                    } else {
                                        ""
                                    }
                                    Text(
                                        text = "Vía: ${monthSettlement.paymentChannelEnum().label}$refSuffix",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (monthSettlement.paymentStatusEnum() == PaymentStatus.PAGADO) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = commerceColors.collected,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Mes liquidado al 100%.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = commerceColors.collected,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { onDownloadSettlementReceiptPdf(monthSettlement) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .minimumInteractiveComponentSize()
                                            .testTag("actor_download_month_pdf_button")
                                    ) {
                                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Descargar Recibo de Pago (PDF)")
                                    }
                                } else {
                                    Button(
                                        onClick = { onReportDigitalPayment(monthSettlement) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .minimumInteractiveComponentSize()
                                            .testTag("actor_report_payment_button")
                                    ) {
                                        Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Reportar pago Transfermóvil")
                                    }
                                    if (monthSettlement.paidCents > 0L) {
                                        OutlinedButton(
                                            onClick = { onDownloadSettlementReceiptPdf(monthSettlement) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Descargar Recibo Parcial (PDF)")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Filterable Operations & Receipts History
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Historial de Operaciones y Recibos PDF",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = historyTab == ActorHistoryTab.ALL,
                                onClick = { historyTab = ActorHistoryTab.ALL },
                                label = { Text("Todo (${actorSettlements.size + actorPaymentEvents.size})") }
                            )
                            FilterChip(
                                selected = historyTab == ActorHistoryTab.SETTLEMENTS,
                                onClick = { historyTab = ActorHistoryTab.SETTLEMENTS },
                                label = { Text("Liquidaciones (${actorSettlements.size})") }
                            )
                            FilterChip(
                                selected = historyTab == ActorHistoryTab.PAYMENTS,
                                onClick = { historyTab = ActorHistoryTab.PAYMENTS },
                                label = { Text("Pagos y PDF (${actorPaymentEvents.size})") }
                            )
                        }
                    }
                }

                if ((historyTab == ActorHistoryTab.ALL || historyTab == ActorHistoryTab.PAYMENTS) && actorPaymentEvents.isNotEmpty()) {
                    item {
                        Text(
                            text = "Comprobantes y Pagos Transfermóvil (${actorPaymentEvents.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(actorPaymentEvents, key = { "actor_rcpt_${it.id}" }) { event ->
                        ActorPaymentReceiptCard(
                            event = event,
                            onDownloadReceiptPdf = { onDownloadEventReceiptPdf(event) }
                        )
                    }
                } else if (historyTab == ActorHistoryTab.PAYMENTS) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Rounded.Payments,
                            title = "Sin pagos registrados aún",
                            subtitle = "Cuando reportes un pago por Transfermóvil o se concilie en Comercio Bauta, aparecerá aquí con su recibo PDF."
                        )
                    }
                }

                if (historyTab == ActorHistoryTab.ALL || historyTab == ActorHistoryTab.SETTLEMENTS) {
                    item {
                        Text(
                            text = "Declaraciones y Liquidaciones (${actorSettlements.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (actorSettlements.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Rounded.Calculate,
                                title = "Sin declaraciones registradas",
                                subtitle = "Usa el botón superior para declarar tus ingresos y gastos del mes o espera la sincronización de Comercio Bauta."
                            )
                        }
                    } else {
                        items(actorSettlements, key = { it.id }) { item ->
                            ActorSettlementHistoryCard(
                                settlement = item,
                                onReportDigitalPayment = onReportDigitalPayment,
                                onDownloadReceiptPdf = onDownloadSettlementReceiptPdf
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Empresa Municipal de Comercio Bauta · Artemisa",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                hiddenFooterTaps += 1
                                if (hiddenFooterTaps >= 5) {
                                    hiddenFooterTaps = 0
                                    onRequestPrivateAccess()
                                }
                            }
                            .padding(vertical = 8.dp)
                    )
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    editingProfileActor?.let { actorToEdit ->
        ActorSelfEditDialog(
            actor = actorToEdit,
            onDismiss = { editingProfileActor = null },
            onSave = { name, legalType, repName, phone, taxId ->
                onUpdateActorProfile(actorToEdit, name, legalType, repName, phone, taxId)
                editingProfileActor = null
            }
        )
    }

    if (showQuickReportDialog) {
        QuickTransfermovilReportDialog(
            defaultActorName = currentActor?.name?.ifBlank { defaultActorName } ?: defaultActorName,
            selectedMonth = uiState.selectedMonth,
            onDismiss = { showQuickReportDialog = false },
            onSubmit = { actorName, amountCents, channel, reference, note ->
                onReportQuickPayment(
                    actorName,
                    uiState.selectedMonth.year,
                    uiState.selectedMonth.month,
                    amountCents,
                    channel,
                    reference,
                    note,
                    currentActor?.id.orEmpty()
                )
                showQuickReportDialog = false
            }
        )
    }
}

@Composable
private fun ActorPaymentReceiptCard(
    event: PaymentEvent,
    onDownloadReceiptPdf: () -> Unit
) {
    val commerceColors = LocalCommerceColors.current
    val timeFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val dateText = remember(event.createdAtMillis) { timeFormat.format(Date(event.createdAtMillis)) }
    val status = event.verificationStatusEnum()
    val (bg, fg) = when (status) {
        PaymentVerificationStatus.CONFIRMADO -> commerceColors.collectedContainer to commerceColors.collected
        PaymentVerificationStatus.OBSERVADO -> commerceColors.pendingContainer to commerceColors.pending
        PaymentVerificationStatus.POR_VERIFICAR ->
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.actorName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$dateText · ${event.channelEnum().label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(text = status.label, containerColor = bg, contentColor = fg)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (event.paymentReference.isNotBlank()) "Ref: ${event.paymentReference}" else "Transfermóvil",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                MoneyText(
                    cents = event.amountCents,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = commerceColors.collected
                )
            }

            FilledTonalButton(
                onClick = onDownloadReceiptPdf,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("actor_download_event_pdf_${event.id}")
            ) {
                Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Descargar Recibo PDF")
            }
        }
    }
}

@Composable
private fun TransfermovilInstructionsCard(
    paymentConfig: PaymentConfig,
    onOpenQuickReport: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Pago Digital por Transfermóvil (Sin efectivo)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Realiza tus pagos mediante Transferencia o Pago en Línea (QR / Bulevar MiTransfer) en Transfermóvil hacia la cuenta oficial de la empresa:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (paymentConfig.isConfigured) {
                if (paymentConfig.accountHolder.isNotBlank()) {
                    Text(
                        text = "Beneficiario: ${paymentConfig.accountHolder}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (paymentConfig.bankAccount.isNotBlank()) {
                    val bankPrefix = if (paymentConfig.bankName.isNotBlank()) {
                        "${paymentConfig.bankName} · "
                    } else {
                        ""
                    }
                    Text(
                        text = "${bankPrefix}Cuenta CUP: ${paymentConfig.bankAccount}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (paymentConfig.qrPaymentUrl.isNotBlank()) {
                    Text(
                        text = "QR / MiTransfer: ${paymentConfig.qrPaymentUrl}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (paymentConfig.bankAccount.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                TransfermovilHelper.copyToClipboard(
                                    context = context,
                                    label = "Cuenta Comercio Bauta",
                                    text = TransfermovilHelper.cleanCardOrAccountForCopy(paymentConfig.bankAccount),
                                    toastMessage = "Cuenta ${paymentConfig.bankAccount} copiada"
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("portal_copy_bank_button")
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copiar cuenta", maxLines = 1)
                        }
                    }
                    FilledTonalButton(
                        onClick = {
                            val openedApp = TransfermovilHelper.openTransfermovilApp(context)
                            if (!openedApp) {
                                if (TransfermovilHelper.isWebUrl(paymentConfig.qrPaymentUrl)) {
                                    TransfermovilHelper.openExternalLink(context, paymentConfig.qrPaymentUrl)
                                } else if (paymentConfig.qrPaymentUrl.isNotBlank()) {
                                    TransfermovilHelper.copyToClipboard(
                                        context = context,
                                        label = "QR Transfermóvil",
                                        text = paymentConfig.qrPaymentUrl,
                                        toastMessage = "Datos QR copiados"
                                    )
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Abre Transfermóvil en tu teléfono y pega la cuenta copiada.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("portal_open_transfermovil_button")
                    ) {
                        Icon(
                            imageVector = if (TransfermovilHelper.isWebUrl(paymentConfig.qrPaymentUrl)) {
                                Icons.Rounded.OpenInNew
                            } else {
                                Icons.Rounded.Smartphone
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Abrir Transfermóvil", maxLines = 1)
                    }
                }

                OutlinedButton(
                    onClick = onOpenQuickReport,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("portal_quick_send_payment_button")
                ) {
                    Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Enviar comprobante Transfermóvil")
                }
            } else {
                Text(
                    text = "Los datos de cuenta bancaria o enlace QR aún no están configurados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickTransfermovilReportDialog(
    defaultActorName: String,
    selectedMonth: YearMonthKey,
    onDismiss: () -> Unit,
    onSubmit: (
        actorName: String,
        amountCents: Long,
        channel: PaymentChannel,
        reference: String,
        note: String
    ) -> Unit
) {
    var actorName by remember { mutableStateOf(defaultActorName) }
    var amountInput by remember { mutableStateOf("") }
    var selectedChannel by remember { mutableStateOf(PaymentChannel.TRANSFERENCIA) }
    var reference by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val parsedAmountCents = CurrencyFormatter.parseAmountToCents(amountInput) ?: 0L
    val canSubmit = actorName.isNotBlank() && parsedAmountCents > 0L && reference.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Notificar Pago Transfermóvil",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Período: ${selectedMonth.label()}. Este comprobante se registrará en tiempo real en Comercio Bauta (o quedará en cola offline si no tienes señal).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = actorName,
                    onValueChange = { actorName = it },
                    label = { Text("Nombre del negocio / Actor") },
                    placeholder = { Text("Ej: Cafetería La Plaza (TCP)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Monto pagado (CUP)") },
                    placeholder = { Text("Ej: 17000.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedChannel == PaymentChannel.TRANSFERENCIA,
                        onClick = { selectedChannel = PaymentChannel.TRANSFERENCIA },
                        label = { Text("Transferencia", maxLines = 1) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedChannel == PaymentChannel.QR_LINK,
                        onClick = { selectedChannel = PaymentChannel.QR_LINK },
                        label = { Text("QR MiTransfer", maxLines = 1) },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Código de operación Transfermóvil") },
                    placeholder = { Text("Ej: TM-849201") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota opcional") },
                    placeholder = { Text("Ej: Aporte 30% + alquiler del local") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(actorName, parsedAmountCents, selectedChannel, reference, note)
                },
                enabled = canSubmit
            ) {
                Text("Enviar en vivo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun ActorSettlementHistoryCard(
    settlement: MonthlySettlement,
    onReportDigitalPayment: (MonthlySettlement) -> Unit,
    onDownloadReceiptPdf: (MonthlySettlement) -> Unit
) {
    val commerceColors = LocalCommerceColors.current
    val periodLabel = YearMonthKey(settlement.periodYear.toInt(), settlement.periodMonth.toInt()).label()
    val originLabel = if (settlement.declaredByEnum() == DeclarationSource.ACTOR) {
        "Mi declaración"
    } else {
        "Comercio Bauta"
    }

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
                Text(
                    text = periodLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(
                        text = originLabel,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PaymentStatusBadge(status = settlement.paymentStatusEnum())
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Ingresos brutos:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MoneyText(cents = settlement.grossIncomeCents, style = MaterialTheme.typography.bodySmall)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Gastos deducibles:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MoneyText(cents = settlement.operatingExpensesCents, style = MaterialTheme.typography.bodySmall)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Utilidad neta:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MoneyText(cents = settlement.netProfitCents, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Aporte ${settlement.profitSharePercent}% utilidad:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MoneyText(cents = settlement.profitShareCents, style = MaterialTheme.typography.bodySmall)
            }
            if (settlement.spaceRentCents > 0L) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Alquiler del local:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MoneyText(cents = settlement.spaceRentCents, style = MaterialTheme.typography.bodySmall)
                }
            }
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total liquidado:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                MoneyText(cents = settlement.totalDueCents, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pagado:", style = MaterialTheme.typography.bodySmall, color = commerceColors.collected)
                MoneyText(
                    cents = settlement.paidCents,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = commerceColors.collected
                )
            }
            if (settlement.paymentChannelEnum() != PaymentChannel.NO_DEFINIDO) {
                val refText = if (settlement.paymentReference.isNotBlank()) {
                    " · Ref: ${settlement.paymentReference}"
                } else {
                    ""
                }
                Text(
                    text = "${settlement.paymentChannelEnum().label}$refText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (settlement.notes.isNotBlank()) {
                Text(
                    text = "Nota: ${settlement.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (settlement.remainingCents > 0L) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Pendiente:", style = MaterialTheme.typography.bodySmall, color = commerceColors.pending)
                    MoneyText(
                        cents = settlement.remainingCents,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = commerceColors.pending
                    )
                }
                OutlinedButton(
                    onClick = { onReportDigitalPayment(settlement) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                ) {
                    Text("Reportar pago Transfermóvil")
                }
            }
            if (settlement.paidCents > 0L) {
                FilledTonalButton(
                    onClick = { onDownloadReceiptPdf(settlement) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Descargar Recibo PDF")
                }
            }
        }
    }
}

@Composable
private fun ActorSelfEditDialog(
    actor: EconomicActor,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        legalType: LegalType,
        representativeName: String,
        phone: String,
        taxId: String
    ) -> Unit
) {
    var name by remember(actor) { mutableStateOf(actor.name) }
    var legalType by remember(actor) { mutableStateOf(actor.legalTypeEnum()) }
    var representative by remember(actor) { mutableStateOf(actor.representativeName) }
    var phone by remember(actor) { mutableStateOf(actor.phone) }
    var taxId by remember(actor) { mutableStateOf(actor.taxId) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Actualizar Datos del Negocio",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorText = null },
                    label = { Text("Nombre del negocio *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LegalType.entries.forEach { option ->
                        FilterChip(
                            selected = legalType == option,
                            onClick = { legalType = option },
                            label = { Text(option.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = representative,
                    onValueChange = { representative = it; errorText = null },
                    label = { Text("Titular o representante *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; errorText = null },
                    label = { Text("Teléfono (+53) *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = taxId,
                    onValueChange = { taxId = it; errorText = null },
                    label = { Text("NIT / Licencia Comercial *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        name.trim().isEmpty() -> errorText = "Indique el nombre del negocio."
                        representative.trim().isEmpty() -> errorText = "Indique el nombre del titular."
                        phone.trim().isEmpty() -> errorText = "Indique el teléfono de contacto."
                        taxId.trim().isEmpty() -> errorText = "Indique su NIT o Licencia."
                        else -> onSave(
                            name.trim(),
                            legalType,
                            representative.trim(),
                            phone.trim(),
                            taxId.trim()
                        )
                    }
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
