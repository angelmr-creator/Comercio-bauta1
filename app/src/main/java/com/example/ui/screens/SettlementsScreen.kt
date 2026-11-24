package com.example.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.MonthlySettlement
import com.example.data.model.PaymentStatus
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MoneyText
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.PaymentStatusBadge
import com.example.ui.state.CommerceUiState
import com.example.ui.theme.LocalCommerceColors

@Composable
fun SettlementsScreen(
    uiState: CommerceUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectStatusFilter: (PaymentStatus?) -> Unit,
    onAddSettlement: () -> Unit,
    onEditSettlement: (MonthlySettlement) -> Unit,
    onOpenPaymentDialog: (MonthlySettlement) -> Unit
) {
    val commerceColors = LocalCommerceColors.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MonthSelectorBar(
                    selectedMonth = uiState.selectedMonth,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.settlementStatusFilter == null,
                        onClick = { onSelectStatusFilter(null) },
                        label = { Text("Todos (${uiState.monthSettlements.size})") }
                    )
                    PaymentStatus.entries.forEach { status ->
                        val count = uiState.monthSettlements.count { it.paymentStatusEnum() == status }
                        FilterChip(
                            selected = uiState.settlementStatusFilter == status,
                            onClick = {
                                onSelectStatusFilter(
                                    if (uiState.settlementStatusFilter == status) null else status
                                )
                            },
                            label = { Text("${status.label} ($count)") }
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.filteredMonthSettlements.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Rounded.ReceiptLong,
                            title = if (uiState.monthSettlements.isEmpty()) {
                                "Sin liquidaciones en ${uiState.selectedMonth.label()}"
                            } else {
                                "Sin liquidaciones en este estado"
                            },
                            subtitle = "Calcula y registra el 30% de utilidad neta más el alquiler de espacio para cada actor económico.",
                            actionLabel = if (uiState.actors.isNotEmpty() && uiState.monthSettlements.isEmpty()) {
                                "Crear liquidación"
                            } else {
                                null
                            },
                            onAction = if (uiState.actors.isNotEmpty() && uiState.monthSettlements.isEmpty()) {
                                onAddSettlement
                            } else {
                                null
                            },
                            actionTestTag = "empty_add_settlement_button"
                        )
                    }
                } else {
                    items(uiState.filteredMonthSettlements, key = { it.id }) { settlement ->
                        Card(
                            onClick = { onEditSettlement(settlement) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settlement_card_${settlement.id}"),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = settlement.actorName,
                                            style = MaterialTheme.typography.titleMedium,
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
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (settlement.declaredByEnum() == com.example.data.model.DeclarationSource.ACTOR) {
                                            com.example.ui.components.StatusBadge(
                                                text = "Portal Actor",
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                        PaymentStatusBadge(status = settlement.paymentStatusEnum())
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Utilidad neta:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    MoneyText(
                                        cents = settlement.netProfitCents,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Aporte ${settlement.profitSharePercent}% utilidad:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    MoneyText(
                                        cents = settlement.profitShareCents,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Alquiler de espacio:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    MoneyText(
                                        cents = settlement.spaceRentCents,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }

                                if (settlement.notes.isNotBlank()) {
                                    Text(
                                        text = "Nota: ${settlement.notes}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                HorizontalDivider()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Total a pagar",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        MoneyText(
                                            cents = settlement.totalDueCents,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (settlement.paidCents > 0L) {
                                            Text(
                                                text = "Cobrado: ${com.example.util.CurrencyFormatter.format(settlement.paidCents)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = commerceColors.collected
                                            )
                                            if (settlement.paymentChannelEnum() != com.example.data.model.PaymentChannel.NO_DEFINIDO) {
                                                val refPart = if (settlement.paymentReference.isNotBlank()) {
                                                    " · Ref: ${settlement.paymentReference}"
                                                } else {
                                                    ""
                                                }
                                                Text(
                                                    text = "${settlement.paymentChannelEnum().label}$refPart",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { onOpenPaymentDialog(settlement) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Text(
                                            if (settlement.paymentStatusEnum() == PaymentStatus.PAGADO) {
                                                "Editar cobro"
                                            } else {
                                                "Registrar cobro"
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }
}
