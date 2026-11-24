package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DeclarationSource
import com.example.data.model.PaymentEvent
import com.example.data.model.PaymentVerificationStatus
import com.example.ui.components.CloudSyncCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.PaymentEventCard
import com.example.ui.components.StatusBadge
import com.example.ui.state.CommerceUiState
import com.example.ui.theme.LocalCommerceColors
import com.example.util.CurrencyFormatter
import com.example.util.TransfermovilHelper

@Composable
fun LivePaymentsScreen(
    uiState: CommerceUiState,
    onVerifyPaymentEvent: (eventId: String, status: PaymentVerificationStatus, note: String) -> Unit,
    onDownloadReceiptPdf: (PaymentEvent) -> Unit,
    onCreateInvoiceFromEvent: (PaymentEvent) -> Unit,
    onCloudAuthSuccess: () -> Unit,
    onCloudAuthError: (String) -> Unit,
    onJoinWorkspace: (String) -> Unit,
    onCreateNewWorkspace: () -> Unit,
    onForceSync: () -> Unit,
    onCloudSignOutComplete: () -> Unit
) {
    val commerceColors = LocalCommerceColors.current
    var statusFilter by remember { mutableStateOf<PaymentVerificationStatus?>(null) }
    var onlyActorReports by remember { mutableStateOf(false) }
    var showSmsConciliateDialog by remember { mutableStateOf(false) }

    val filteredEvents = remember(uiState.paymentEvents, statusFilter, onlyActorReports) {
        uiState.paymentEvents.filter { event ->
            val matchesActor = !onlyActorReports || event.reportedByEnum() == DeclarationSource.ACTOR
            val matchesStatus = statusFilter == null || event.verificationStatusEnum() == statusFilter
            matchesActor && matchesStatus
        }
    }
    val actorReportsCount = remember(uiState.paymentEvents) {
        uiState.paymentEvents.count { it.reportedByEnum() == DeclarationSource.ACTOR }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    )
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(commerceColors.collected)
                                )
                                Text(
                                    text = "MONITOR EN VIVO · CUBA 100%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            if (uiState.unverifiedPaymentsCount > 0) {
                                StatusBadge(
                                    text = "${uiState.unverifiedPaymentsCount} por verificar",
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }

                        Text(
                            text = if (uiState.cloudSync.isWorkspaceConnected) {
                                "Sincronización activa en la nube para el código ${uiState.cloudSync.workspaceId}. Los pagos Transfermóvil hacia ${uiState.paymentConfig.bankAccount} aparecen aquí al instante aunque lleguen desde otros teléfonos, y se guardan offline si cae ETECSA."
                            } else {
                                "Aquí ves al instante los pagos Transfermóvil hacia la cuenta ${uiState.paymentConfig.bankAccount}. Conecta tu Google arriba para recibir pagos en vivo desde otros teléfonos en toda Cuba."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FilledTonalButton(
                            onClick = { showSmsConciliateDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("conciliate_sms_button")
                        ) {
                            Icon(Icons.Rounded.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Conciliar con SMS de Transfermóvil")
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !onlyActorReports && statusFilter == null,
                        onClick = {
                            onlyActorReports = false
                            statusFilter = null
                        },
                        label = { Text("Todos (${uiState.paymentEvents.size})") }
                    )
                    FilterChip(
                        selected = onlyActorReports,
                        onClick = { onlyActorReports = !onlyActorReports },
                        label = { Text("Solo Actores ($actorReportsCount)") }
                    )
                    PaymentVerificationStatus.entries.forEach { st ->
                        val count = uiState.paymentEvents.count { it.verificationStatusEnum() == st }
                        FilterChip(
                            selected = statusFilter == st,
                            onClick = { statusFilter = if (statusFilter == st) null else st },
                            label = { Text("${st.label} ($count)") }
                        )
                    }
                }
            }

            if (filteredEvents.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.NotificationsActive,
                        title = "Sin movimientos de pago todavía",
                        subtitle = "Cuando un actor económico reporte un pago Transfermóvil desde su teléfono con el Código de Empresa o desde el Portal Actor, aparecerá aquí en tiempo real."
                    )
                }
            } else {
                items(filteredEvents, key = { it.id }) { event ->
                    val alreadyInvoiced = uiState.invoices.any { it.paymentEventId == event.id }
                    PaymentEventCard(
                        event = event,
                        onConfirm = {
                            onVerifyPaymentEvent(
                                event.id,
                                PaymentVerificationStatus.CONFIRMADO,
                                event.note
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
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showSmsConciliateDialog) {
        SmsConciliateDialog(
            pendingEvents = uiState.paymentEvents.filter {
                it.verificationStatusEnum() == PaymentVerificationStatus.POR_VERIFICAR
            },
            onDismiss = { showSmsConciliateDialog = false },
            onConfirmEvent = { event, parsedNote ->
                onVerifyPaymentEvent(
                    event.id,
                    PaymentVerificationStatus.CONFIRMADO,
                    parsedNote
                )
            }
        )
    }
}

@Composable
private fun SmsConciliateDialog(
    pendingEvents: List<PaymentEvent>,
    onDismiss: () -> Unit,
    onConfirmEvent: (event: PaymentEvent, parsedNote: String) -> Unit
) {
    var smsText by remember { mutableStateOf("") }
    val parsed = remember(smsText) { TransfermovilHelper.parseTransfermovilSms(smsText) }

    val matchingEvents = remember(pendingEvents, parsed) {
        pendingEvents.filter { event ->
            val refMatches = !parsed.transactionReference.isNullOrBlank() &&
                event.paymentReference.equals(parsed.transactionReference, ignoreCase = true)
            val amountMatches = parsed.amountCents != null && event.amountCents == parsed.amountCents
            refMatches || amountMatches
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Conciliar SMS Transfermóvil", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Pega el texto del SMS de confirmación recibido (PAGOxMOVIL / BANDEC / BPA / Metropolitano) para verificar automáticamente el monto y código de operación.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = smsText,
                    onValueChange = { smsText = it },
                    label = { Text("Texto del SMS de Transfermóvil") },
                    placeholder = { Text("Ej: Transferencia recibida por 17000.00 CUP. Operación: TM-849201") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                if (parsed.amountCents != null || !parsed.transactionReference.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Detectado: " +
                                    (parsed.amountCents?.let { CurrencyFormatter.format(it) } ?: "Monto no detectado") +
                                    (parsed.transactionReference?.let { " · Ref: $it" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (matchingEvents.isNotEmpty()) {
                    Text(
                        text = "Pagos reportados coincidentes:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    matchingEvents.forEach { ev ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ev.actorName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "${CurrencyFormatter.format(ev.amountCents)} · Ref: ${ev.paymentReference}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Button(
                                onClick = {
                                    onConfirmEvent(ev, "Conciliado con SMS Transfermóvil")
                                    onDismiss()
                                }
                            ) {
                                Text("Confirmar")
                            }
                        }
                    }
                } else if (smsText.isNotBlank()) {
                    Text(
                        text = if (pendingEvents.isEmpty()) {
                            "No hay pagos pendientes de verificar en este momento."
                        } else {
                            "No coincide exactamente con un pago pendiente; revisa la lista principal."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
