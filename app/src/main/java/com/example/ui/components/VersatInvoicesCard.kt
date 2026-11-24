package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ReceiptLong
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.EconomicActor
import com.example.data.model.InvoiceRecord
import com.example.data.model.PaymentChannel
import com.example.data.model.PaymentEvent
import com.example.ui.theme.LocalCommerceColors
import com.example.util.CurrencyFormatter
import com.example.util.VersatSarasolaExporter
import com.example.util.YearMonthKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VersatInvoicesCard(
    invoices: List<InvoiceRecord>,
    pendingExportCount: Int,
    onAddInvoice: () -> Unit,
    onEditInvoice: (InvoiceRecord) -> Unit,
    onDeleteInvoice: (String) -> Unit,
    onExportCsv: () -> Unit,
    onExportXml: () -> Unit,
    modifier: Modifier = Modifier
) {
    val commerceColors = LocalCommerceColors.current
    val totalInvoicedCents = remember(invoices) { invoices.sumOf { it.totalAmountCents } }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        imageVector = Icons.Rounded.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Facturación · Versat Sarasola",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${invoices.size} factura(s) registrada(s) · Total ${CurrencyFormatter.format(totalInvoicedCents)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (pendingExportCount > 0) {
                    StatusBadge(
                        text = "$pendingExportCount sin exportar",
                        containerColor = commerceColors.pendingContainer,
                        contentColor = commerceColors.pending
                    )
                }
            }

            Text(
                text = "Registra aquí las facturas contables de los cobros Transfermóvil para exportarlas directo hacia Versat Sarasola (formato CSV estándar cubano o XML).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddInvoice,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("manager_add_invoice_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nueva Factura", maxLines = 1)
                }
                FilledTonalButton(
                    onClick = onExportCsv,
                    enabled = invoices.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("export_versat_csv_button")
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Exportar CSV", maxLines = 1)
                }
            }

            if (invoices.isNotEmpty()) {
                OutlinedButton(
                    onClick = onExportXml,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("export_versat_xml_button")
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exportar Comprobantes XML para Versat Sarasola")
                }

                HorizontalDivider()

                invoices.take(6).forEach { inv ->
                    Surface(
                        onClick = { onEditInvoice(inv) },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("invoice_row_${inv.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${inv.invoiceNumber} · ${inv.actorName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                StatusBadge(
                                    text = if (inv.exportedToVersat) "Exportada" else "Pendiente",
                                    containerColor = if (inv.exportedToVersat) {
                                        commerceColors.collectedContainer
                                    } else {
                                        commerceColors.pendingContainer
                                    },
                                    contentColor = if (inv.exportedToVersat) {
                                        commerceColors.collected
                                    } else {
                                        commerceColors.pending
                                    }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${dateFormat.format(Date(inv.issueDateMillis))} · Ctas: ${inv.debitAccountCode} → ${inv.creditAccountCode}" +
                                            if (inv.paymentReference.isNotBlank()) " · Ref: ${inv.paymentReference}" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = CurrencyFormatter.format(inv.totalAmountCents),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = commerceColors.collected
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteInvoice(inv.id) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Eliminar factura ${inv.invoiceNumber}",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceEditorDialog(
    initialInvoice: InvoiceRecord?,
    prefillEvent: PaymentEvent?,
    actors: List<EconomicActor>,
    selectedMonth: YearMonthKey,
    nextInvoiceSequence: Int,
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        invoiceNumber: String,
        paymentEventId: String,
        settlementId: String,
        actorId: String,
        actorName: String,
        actorTaxId: String,
        concept: String,
        periodYear: Int,
        periodMonth: Int,
        profitShareCents: Long,
        spaceRentCents: Long,
        totalAmountCents: Long,
        paymentChannel: PaymentChannel,
        paymentReference: String,
        debitAccountCode: String,
        creditAccountCode: String,
        costCenter: String
    ) -> Unit
) {
    val defaultNumber = remember(initialInvoice, selectedMonth, nextInvoiceSequence) {
        initialInvoice?.invoiceNumber ?: "FAC-${selectedMonth.year}-${nextInvoiceSequence.toString().padStart(4, '0')}"
    }
    val matchedActor = remember(actors, initialInvoice, prefillEvent) {
        val targetId = initialInvoice?.actorId?.ifBlank { null } ?: prefillEvent?.actorId?.ifBlank { null }
        val targetName = initialInvoice?.actorName ?: prefillEvent?.actorName
        actors.firstOrNull { it.id == targetId }
            ?: actors.firstOrNull { it.name.equals(targetName, ignoreCase = true) }
    }

    var invoiceNumber by remember { mutableStateOf(defaultNumber) }
    var actorId by remember { mutableStateOf(initialInvoice?.actorId ?: prefillEvent?.actorId ?: matchedActor?.id.orEmpty()) }
    var actorName by remember { mutableStateOf(initialInvoice?.actorName ?: prefillEvent?.actorName ?: matchedActor?.name.orEmpty()) }
    var actorTaxId by remember { mutableStateOf(initialInvoice?.actorTaxId ?: matchedActor?.taxId.orEmpty()) }
    var actorDropdownExpanded by remember { mutableStateOf(false) }

    val periodYear = initialInvoice?.periodYear?.toInt()
        ?: prefillEvent?.periodYear?.toInt()
        ?: selectedMonth.year
    val periodMonth = initialInvoice?.periodMonth?.toInt()
        ?: prefillEvent?.periodMonth?.toInt()
        ?: selectedMonth.month

    var concept by remember {
        mutableStateOf(
            initialInvoice?.concept
                ?: "Aporte 30% utilidad neta + alquiler municipal (${YearMonthKey(periodYear, periodMonth).label()})"
        )
    }

    val initialTotalCents = initialInvoice?.totalAmountCents ?: prefillEvent?.amountCents ?: 0L
    val initialRentCents = initialInvoice?.spaceRentCents ?: matchedActor?.monthlyRentCents?.coerceAtMost(initialTotalCents) ?: 0L
    val initialProfitCents = initialInvoice?.profitShareCents ?: (initialTotalCents - initialRentCents).coerceAtLeast(0L)

    var totalAmountInput by remember {
        mutableStateOf(if (initialTotalCents > 0L) VersatSarasolaExporter.formatDecimalCents(initialTotalCents) else "")
    }
    var spaceRentInput by remember {
        mutableStateOf(if (initialRentCents > 0L) VersatSarasolaExporter.formatDecimalCents(initialRentCents) else "0.00")
    }
    var channel by remember {
        mutableStateOf(
            initialInvoice?.channelEnum()
                ?: prefillEvent?.channelEnum()?.takeIf { it != PaymentChannel.NO_DEFINIDO }
                ?: PaymentChannel.TRANSFERENCIA
        )
    }
    var reference by remember { mutableStateOf(initialInvoice?.paymentReference ?: prefillEvent?.paymentReference.orEmpty()) }
    var debitAccount by remember { mutableStateOf(initialInvoice?.debitAccountCode ?: InvoiceRecord.DEFAULT_VERSAT_DEBIT_ACCOUNT) }
    var creditAccount by remember { mutableStateOf(initialInvoice?.creditAccountCode ?: InvoiceRecord.DEFAULT_VERSAT_CREDIT_ACCOUNT) }
    var costCenter by remember { mutableStateOf(initialInvoice?.costCenter ?: InvoiceRecord.DEFAULT_VERSAT_COST_CENTER) }

    val parsedTotal = CurrencyFormatter.parseAmountToCents(totalAmountInput) ?: 0L
    val parsedRent = (CurrencyFormatter.parseAmountToCents(spaceRentInput) ?: 0L).coerceIn(0L, parsedTotal)
    val parsedProfitShare = (parsedTotal - parsedRent).coerceAtLeast(0L)
    val canSave = invoiceNumber.isNotBlank() && actorName.isNotBlank() && parsedTotal > 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialInvoice == null) "Registrar Factura (Versat Sarasola)" else "Editar Factura",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = invoiceNumber,
                    onValueChange = { invoiceNumber = it },
                    label = { Text("No. de Factura") },
                    placeholder = { Text("FAC-2026-0001") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("invoice_number_input")
                )

                if (actors.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = actorDropdownExpanded,
                        onExpandedChange = { actorDropdownExpanded = !actorDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = actorName,
                            onValueChange = {
                                actorName = it
                                actorId = ""
                            },
                            label = { Text("Actor Económico / Cliente") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = actorDropdownExpanded)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = actorDropdownExpanded,
                            onDismissRequest = { actorDropdownExpanded = false }
                        ) {
                            actors.forEach { actor ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${actor.name} (${actor.legalTypeEnum().label})")
                                    },
                                    onClick = {
                                        actorId = actor.id
                                        actorName = actor.name
                                        actorTaxId = actor.taxId
                                        if (spaceRentInput == "0.00" && actor.monthlyRentCents > 0L) {
                                            spaceRentInput = VersatSarasolaExporter.formatDecimalCents(actor.monthlyRentCents)
                                        }
                                        actorDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = actorName,
                        onValueChange = { actorName = it },
                        label = { Text("Actor Económico / Cliente") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = actorTaxId,
                    onValueChange = { actorTaxId = it },
                    label = { Text("NIT / Código Cliente (Versat)") },
                    placeholder = { Text("Ej: 12345678901") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = concept,
                    onValueChange = { concept = it },
                    label = { Text("Concepto de Factura") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = totalAmountInput,
                        onValueChange = { totalAmountInput = it },
                        label = { Text("Importe Total (CUP)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("invoice_total_input")
                    )
                    OutlinedTextField(
                        value = spaceRentInput,
                        onValueChange = { spaceRentInput = it },
                        label = { Text("Alquiler (CUP)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Desglose Versat: Aporte 30% (${CurrencyFormatter.format(parsedProfitShare)}) + Alquiler (${CurrencyFormatter.format(parsedRent)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = channel == PaymentChannel.TRANSFERENCIA,
                        onClick = { channel = PaymentChannel.TRANSFERENCIA },
                        label = { Text("Transferencia", maxLines = 1) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = channel == PaymentChannel.QR_LINK,
                        onClick = { channel = PaymentChannel.QR_LINK },
                        label = { Text("QR MiTransfer", maxLines = 1) },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("No. Operación Transfermóvil") },
                    placeholder = { Text("Ej: TM-849201") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = debitAccount,
                        onValueChange = { debitAccount = it },
                        label = { Text("Cta. Débito") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = creditAccount,
                        onValueChange = { creditAccount = it },
                        label = { Text("Cta. Crédito") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = costCenter,
                        onValueChange = { costCenter = it },
                        label = { Text("Centro Costo") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initialInvoice?.id,
                        invoiceNumber,
                        initialInvoice?.paymentEventId ?: prefillEvent?.id.orEmpty(),
                        initialInvoice?.settlementId ?: prefillEvent?.settlementId.orEmpty(),
                        actorId,
                        actorName,
                        actorTaxId,
                        concept,
                        periodYear,
                        periodMonth,
                        parsedProfitShare,
                        parsedRent,
                        parsedTotal,
                        channel,
                        reference,
                        debitAccount,
                        creditAccount,
                        costCenter
                    )
                },
                enabled = canSave,
                modifier = Modifier.testTag("save_invoice_button")
            ) {
                Text("Guardar Factura")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
