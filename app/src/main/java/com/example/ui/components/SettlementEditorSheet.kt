package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.EconomicActor
import com.example.data.model.MonthlySettlement
import com.example.data.model.PaymentChannel
import com.example.ui.theme.LocalCommerceColors
import com.example.util.CurrencyFormatter
import com.example.util.SettlementCalculator
import com.example.util.YearMonthKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementEditorSheet(
    initialSettlement: MonthlySettlement?,
    preselectedActor: EconomicActor?,
    actors: List<EconomicActor>,
    selectedMonth: YearMonthKey,
    isActorPortalMode: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        actor: EconomicActor,
        periodYear: Int,
        periodMonth: Int,
        grossIncomeCents: Long,
        operatingExpensesCents: Long,
        netProfitCents: Long,
        profitSharePercent: Long,
        spaceRentCents: Long,
        paidCents: Long,
        notes: String,
        paymentChannel: PaymentChannel,
        paymentReference: String
    ) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initialActor = remember(initialSettlement, preselectedActor, actors) {
        when {
            initialSettlement != null -> actors.firstOrNull { it.id == initialSettlement.actorId }
            preselectedActor != null -> preselectedActor
            else -> actors.firstOrNull { it.active } ?: actors.firstOrNull()
        }
    }

    var chosenActor by remember(initialActor) { mutableStateOf(initialActor) }
    var actorDropdownExpanded by remember { mutableStateOf(false) }

    // 0 = Ingresos - Gastos, 1 = Utilidad neta directa
    var inputMode by remember(initialSettlement) {
        mutableIntStateOf(
            if (initialSettlement != null && initialSettlement.grossIncomeCents == 0L && initialSettlement.netProfitCents != 0L) 1 else 0
        )
    }

    var grossIncomeText by remember(initialSettlement) {
        mutableStateOf(
            if (initialSettlement != null) CurrencyFormatter.formatInputValue(initialSettlement.grossIncomeCents) else ""
        )
    }
    var expensesText by remember(initialSettlement) {
        mutableStateOf(
            if (initialSettlement != null) CurrencyFormatter.formatInputValue(initialSettlement.operatingExpensesCents) else ""
        )
    }
    var directNetProfitText by remember(initialSettlement) {
        mutableStateOf(
            if (initialSettlement != null) CurrencyFormatter.formatInputValue(initialSettlement.netProfitCents) else ""
        )
    }
    var rentText by remember(initialSettlement, initialActor) {
        mutableStateOf(
            when {
                initialSettlement != null -> CurrencyFormatter.formatInputValue(initialSettlement.spaceRentCents)
                initialActor != null -> CurrencyFormatter.formatInputValue(initialActor.monthlyRentCents)
                else -> ""
            }
        )
    }
    var percentText by remember(initialSettlement, initialActor) {
        mutableStateOf(
            (initialSettlement?.profitSharePercent ?: initialActor?.profitSharePercent ?: 30L).toString()
        )
    }
    var paidText by remember(initialSettlement) {
        mutableStateOf(
            if (initialSettlement != null && initialSettlement.paidCents > 0L) {
                CurrencyFormatter.formatInputValue(initialSettlement.paidCents)
            } else {
                ""
            }
        )
    }
    var paymentChannel by remember(initialSettlement) {
        mutableStateOf(
            when (initialSettlement?.paymentChannelEnum()) {
                PaymentChannel.QR_LINK -> PaymentChannel.QR_LINK
                else -> PaymentChannel.TRANSFERENCIA
            }
        )
    }
    var paymentReference by remember(initialSettlement) {
        mutableStateOf(initialSettlement?.paymentReference ?: "")
    }
    var notes by remember(initialSettlement) { mutableStateOf(initialSettlement?.notes ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val parsedGross = CurrencyFormatter.parseAmountToCents(grossIncomeText) ?: 0L
    val parsedExpenses = CurrencyFormatter.parseAmountToCents(expensesText) ?: 0L
    val parsedDirectProfit = CurrencyFormatter.parseAmountToCents(directNetProfitText, allowNegative = true) ?: 0L
    val parsedRent = CurrencyFormatter.parseAmountToCents(rentText) ?: 0L
    val parsedPercent = percentText.trim().toLongOrNull()?.coerceIn(0L, 100L) ?: 30L
    val parsedPaid = CurrencyFormatter.parseAmountToCents(paidText) ?: 0L

    val previewBreakdown = remember(
        inputMode,
        parsedGross,
        parsedExpenses,
        parsedDirectProfit,
        parsedPercent,
        parsedRent,
        parsedPaid
    ) {
        if (inputMode == 0) {
            SettlementCalculator.calculateFromIncomeAndExpenses(
                grossIncomeCents = parsedGross,
                operatingExpensesCents = parsedExpenses,
                profitSharePercent = parsedPercent,
                spaceRentCents = parsedRent,
                paidCents = parsedPaid
            )
        } else {
            SettlementCalculator.calculateFromNetProfit(
                grossIncomeCents = 0L,
                operatingExpensesCents = 0L,
                netProfitCents = parsedDirectProfit,
                profitSharePercent = parsedPercent,
                spaceRentCents = parsedRent,
                paidCents = parsedPaid
            )
        }
    }

    val periodLabel = if (initialSettlement != null) {
        YearMonthKey(initialSettlement.periodYear.toInt(), initialSettlement.periodMonth.toInt()).label()
    } else {
        selectedMonth.label()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = when {
                    isActorPortalMode -> "Declarar Utilidad del Mes"
                    initialSettlement == null -> "Liquidación Mensual"
                    else -> "Editar Liquidación"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Período: $periodLabel · ${parsedPercent}% utilidad neta + alquiler",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!isActorPortalMode) {
                ExposedDropdownMenuBox(
                    expanded = actorDropdownExpanded,
                    onExpandedChange = { actorDropdownExpanded = !actorDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = chosenActor?.let { "${it.name} (${it.legalTypeEnum().label})" } ?: "Seleccionar actor económico",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Actor económico *") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = actorDropdownExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("settlement_actor_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = actorDropdownExpanded,
                        onDismissRequest = { actorDropdownExpanded = false }
                    ) {
                        actors.forEach { actor ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("${actor.name} (${actor.legalTypeEnum().label})", fontWeight = FontWeight.Medium)
                                        val locLabel = actor.localeName.ifBlank { "Sin local asignado" }
                                        Text(
                                            text = "$locLabel · Alquiler ${CurrencyFormatter.format(actor.monthlyRentCents)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    chosenActor = actor
                                    rentText = CurrencyFormatter.formatInputValue(actor.monthlyRentCents)
                                    percentText = actor.profitSharePercent.toString()
                                    actorDropdownExpanded = false
                                    errorText = null
                                }
                            )
                        }
                    }
                }
            } else if (chosenActor != null) {
                Text(
                    text = "${chosenActor!!.name} (${chosenActor!!.legalTypeEnum().label})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = inputMode == 0,
                    onClick = { inputMode = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Ingresos y gastos")
                }
                SegmentedButton(
                    selected = inputMode == 1,
                    onClick = { inputMode = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Utilidad directa")
                }
            }

            if (inputMode == 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = grossIncomeText,
                        onValueChange = { grossIncomeText = it; errorText = null },
                        label = { Text("Ingresos brutos") },
                        placeholder = { Text("100000.00") },
                        suffix = { Text("CUP") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settlement_gross_input")
                    )
                    OutlinedTextField(
                        value = expensesText,
                        onValueChange = { expensesText = it; errorText = null },
                        label = { Text("Gastos deducibles") },
                        placeholder = { Text("60000.00") },
                        suffix = { Text("CUP") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settlement_expenses_input")
                    )
                }
            } else {
                OutlinedTextField(
                    value = directNetProfitText,
                    onValueChange = { directNetProfitText = it; errorText = null },
                    label = { Text("Utilidad neta del mes (CUP)") },
                    placeholder = { Text("40000.00") },
                    suffix = { Text("CUP") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settlement_net_profit_input")
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = rentText,
                    onValueChange = { if (!isActorPortalMode) { rentText = it; errorText = null } },
                    readOnly = isActorPortalMode,
                    label = { Text("Alquiler de espacio") },
                    placeholder = { Text("5000.00") },
                    suffix = { Text("CUP") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("settlement_rent_input")
                )
                OutlinedTextField(
                    value = percentText,
                    onValueChange = { if (!isActorPortalMode) { percentText = it; errorText = null } },
                    readOnly = isActorPortalMode,
                    label = { Text("% Aporte") },
                    placeholder = { Text("30") },
                    suffix = { Text("%") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(0.75f)
                        .testTag("settlement_percent_input")
                )
            }

            // Live Breakdown Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "DESGLOSE CALCULADO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    BreakdownRow(
                        label = "Utilidad neta",
                        cents = previewBreakdown.netProfitCents
                    )
                    BreakdownRow(
                        label = "Aporte (${previewBreakdown.profitSharePercent}% utilidad neta)",
                        cents = previewBreakdown.profitShareCents
                    )
                    BreakdownRow(
                        label = "Alquiler de espacio",
                        cents = previewBreakdown.spaceRentCents
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total a pagar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        MoneyText(
                            cents = previewBreakdown.totalDueCents,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (!isActorPortalMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = paidText,
                        onValueChange = { paidText = it; errorText = null },
                        label = { Text("Monto cobrado (opcional)") },
                        placeholder = { Text("0.00") },
                        suffix = { Text("CUP") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settlement_paid_input")
                    )
                    FilterChip(
                        selected = previewBreakdown.totalDueCents > 0 && parsedPaid >= previewBreakdown.totalDueCents,
                        onClick = {
                            paidText = CurrencyFormatter.formatInputValue(previewBreakdown.totalDueCents)
                        },
                        label = { Text("Pagado total") },
                        modifier = Modifier.testTag("settlement_pay_full_chip")
                    )
                }

                if (parsedPaid > 0L) {
                    val digitalChannels = listOf(PaymentChannel.TRANSFERENCIA, PaymentChannel.QR_LINK)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        digitalChannels.forEachIndexed { idx, channel ->
                            SegmentedButton(
                                selected = paymentChannel == channel,
                                onClick = { paymentChannel = channel },
                                shape = SegmentedButtonDefaults.itemShape(index = idx, count = digitalChannels.size)
                            ) {
                                Text(
                                    text = if (channel == PaymentChannel.TRANSFERENCIA) "Transferencia" else "QR / MiTransfer",
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = paymentReference,
                        onValueChange = { paymentReference = it },
                        label = { Text("No. operación Transfermóvil") },
                        placeholder = { Text("Ej: TM-849201") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settlement_reference_input")
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Observaciones") },
                placeholder = { Text("Ej: Liquidación mensual vía Transfermóvil / MiTransfer") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settlement_notes_input")
            )

            if (errorText != null) {
                Text(
                    text = errorText!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (initialSettlement != null && onDelete != null) {
                    OutlinedButton(
                        onClick = {
                            onDelete(initialSettlement.id)
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("delete_settlement_button")
                    ) {
                        Text("Eliminar")
                    }
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = {
                        val actor = chosenActor
                        val rawPercent = percentText.trim().toLongOrNull()
                        val rawRent = if (rentText.isBlank()) 0L else CurrencyFormatter.parseAmountToCents(rentText)
                        val rawPaid = if (paidText.isBlank()) 0L else CurrencyFormatter.parseAmountToCents(paidText)
                        val netProfit = if (inputMode == 0) parsedGross - parsedExpenses else parsedDirectProfit

                        when {
                            actor == null -> errorText = "Primero registre o seleccione un actor económico."
                            rawPercent == null || rawPercent !in 0L..100L -> errorText = "El porcentaje debe estar entre 0 y 100."
                            rawRent == null -> errorText = "Ingrese un alquiler válido en CUP."
                            rawPaid == null -> errorText = "Ingrese un monto cobrado válido."
                            else -> {
                                val resolvedChannel = if (!isActorPortalMode && rawPaid > 0L) {
                                    paymentChannel
                                } else {
                                    initialSettlement?.paymentChannelEnum() ?: PaymentChannel.NO_DEFINIDO
                                }
                                val resolvedRef = if (!isActorPortalMode && rawPaid > 0L) {
                                    paymentReference.trim()
                                } else {
                                    initialSettlement?.paymentReference.orEmpty()
                                }
                                onSave(
                                    initialSettlement?.id,
                                    actor,
                                    initialSettlement?.periodYear?.toInt() ?: selectedMonth.year,
                                    initialSettlement?.periodMonth?.toInt() ?: selectedMonth.month,
                                    if (inputMode == 0) parsedGross else 0L,
                                    if (inputMode == 0) parsedExpenses else 0L,
                                    netProfit,
                                    rawPercent,
                                    rawRent,
                                    rawPaid,
                                    notes.trim(),
                                    resolvedChannel,
                                    resolvedRef
                                )
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("save_settlement_button")
                ) {
                    Text("Guardar")
                }
            }
        }
    }
}

@Composable
private fun BreakdownRow(label: String, cents: Long) {
    val commerceColors = LocalCommerceColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        MoneyText(
            cents = cents,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (cents < 0) commerceColors.pending else MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
