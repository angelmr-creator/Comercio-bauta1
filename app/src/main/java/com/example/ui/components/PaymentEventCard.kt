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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DeclarationSource
import com.example.data.model.PaymentEvent
import com.example.data.model.PaymentVerificationStatus
import com.example.ui.theme.LocalCommerceColors
import com.example.util.YearMonthKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PaymentEventCard(
    event: PaymentEvent,
    onConfirm: () -> Unit,
    onObserve: () -> Unit,
    modifier: Modifier = Modifier,
    onDownloadReceiptPdf: (() -> Unit)? = null,
    onCreateInvoice: (() -> Unit)? = null,
    alreadyInvoiced: Boolean = false
) {
    val commerceColors = LocalCommerceColors.current
    val timeFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
    val dateText = remember(event.createdAtMillis) { timeFormat.format(Date(event.createdAtMillis)) }
    val periodLabel = YearMonthKey(event.periodYear.toInt(), event.periodMonth.toInt()).label()
    val status = event.verificationStatusEnum()
    val reportedByActor = event.reportedByEnum() == DeclarationSource.ACTOR

    val (badgeBg, badgeFg) = when (status) {
        PaymentVerificationStatus.POR_VERIFICAR ->
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        PaymentVerificationStatus.CONFIRMADO ->
            commerceColors.collectedContainer to commerceColors.collected
        PaymentVerificationStatus.OBSERVADO ->
            commerceColors.pendingContainer to commerceColors.pending
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("payment_event_card_${event.id}"),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.actorName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$dateText · $periodLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StatusBadge(text = status.label, containerColor = badgeBg, contentColor = badgeFg)
                    StatusBadge(
                        text = if (reportedByActor) "Portal Actor" else "Manager",
                        containerColor = if (reportedByActor) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = if (reportedByActor) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.channelEnum().label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (event.paymentReference.isNotBlank()) {
                        Text(
                            text = "Ref: ${event.paymentReference}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "Reportado por: ${event.reportedByEnum().label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                MoneyText(
                    cents = event.amountCents,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = commerceColors.collected
                )
            }

            if (event.verifiedByUsername.isNotBlank()) {
                Text(
                    text = "Revisado por @${event.verifiedByUsername}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (event.note.isNotBlank()) {
                Text(
                    text = event.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (status != PaymentVerificationStatus.CONFIRMADO) {
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onObserve,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.WarningAmber, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Observar")
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("confirm_event_${event.id}")
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Confirmar")
                    }
                }
            }
            if (onDownloadReceiptPdf != null || onCreateInvoice != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onDownloadReceiptPdf != null) {
                        OutlinedButton(
                            onClick = onDownloadReceiptPdf,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("download_receipt_pdf_${event.id}")
                        ) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Recibo PDF", maxLines = 1)
                        }
                    }
                    if (onCreateInvoice != null) {
                        OutlinedButton(
                            onClick = onCreateInvoice,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("create_invoice_from_event_${event.id}")
                        ) {
                            Icon(Icons.Rounded.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (alreadyInvoiced) "Facturada" else "Facturar", maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
