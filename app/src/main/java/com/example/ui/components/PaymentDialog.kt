package com.example.ui.components

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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.MonthlySettlement
import com.example.data.model.PaymentChannel
import com.example.data.model.PaymentConfig
import com.example.util.CurrencyFormatter
import com.example.util.TransfermovilHelper

@Composable
fun PaymentDialog(
    settlement: MonthlySettlement,
    paymentConfig: PaymentConfig,
    isActorPortalMode: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmPayment: (
        newTotalPaidCents: Long,
        channel: PaymentChannel,
        reference: String
    ) -> Unit
) {
    val context = LocalContext.current

    var paidInput by remember(settlement) {
        mutableStateOf(
            CurrencyFormatter.formatInputValue(
                if (settlement.totalDueCents > 0L) settlement.totalDueCents else settlement.paidCents
            )
        )
    }
    var selectedChannel by remember(settlement) {
        mutableStateOf(
            when (settlement.paymentChannelEnum()) {
                PaymentChannel.QR_LINK -> PaymentChannel.QR_LINK
                else -> PaymentChannel.TRANSFERENCIA
            }
        )
    }
    var referenceInput by remember(settlement) {
        mutableStateOf(settlement.paymentReference)
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    val channelOptions = remember {
        listOf(PaymentChannel.TRANSFERENCIA, PaymentChannel.QR_LINK)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isActorPortalMode) "Reportar Pago Transfermóvil" else "Registrar Cobro Digital",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = settlement.actorName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total liquidación:", style = MaterialTheme.typography.bodyMedium)
                    MoneyText(cents = settlement.totalDueCents, style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Cobrado previo:", style = MaterialTheme.typography.bodyMedium)
                    MoneyText(cents = settlement.paidCents, style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Saldo pendiente:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    MoneyText(
                        cents = settlement.remainingCents,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Transfermóvil receiving details card
                if (paymentConfig.isConfigured) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "DESTINO TRANSFERMÓVIL (SIN EFECTIVO)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            if (paymentConfig.bankAccount.isNotBlank()) {
                                val bankPrefix = if (paymentConfig.bankName.isNotBlank()) {
                                    "${paymentConfig.bankName} · "
                                } else {
                                    ""
                                }
                                Text(
                                    text = "${bankPrefix}Cuenta: ${paymentConfig.bankAccount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (paymentConfig.accountHolder.isNotBlank()) {
                                Text(
                                    text = paymentConfig.accountHolder,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (paymentConfig.bankAccount.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            TransfermovilHelper.copyToClipboard(
                                                context = context,
                                                label = "Cuenta Comercio Bauta",
                                                text = TransfermovilHelper.cleanCardOrAccountForCopy(paymentConfig.bankAccount),
                                                toastMessage = "Cuenta copiada para Transfermóvil"
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copiar cuenta", maxLines = 1)
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        val launched = TransfermovilHelper.openTransfermovilApp(context)
                                        if (!launched) {
                                            if (TransfermovilHelper.isWebUrl(paymentConfig.qrPaymentUrl)) {
                                                TransfermovilHelper.openExternalLink(context, paymentConfig.qrPaymentUrl)
                                            } else {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Transfermóvil no está instalado en este equipo; copia la cuenta manualmente.",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
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
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Transfermóvil", maxLines = 1)
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Modalidad digital Transfermóvil",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    channelOptions.forEachIndexed { index, channel ->
                        SegmentedButton(
                            selected = selectedChannel == channel,
                            onClick = { selectedChannel = channel; errorText = null },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = channelOptions.size),
                            modifier = Modifier.testTag("payment_channel_${channel.name}")
                        ) {
                            Text(
                                text = if (channel == PaymentChannel.TRANSFERENCIA) "Transferencia" else "QR / MiTransfer",
                                maxLines = 1
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = paidInput,
                    onValueChange = { paidInput = it; errorText = null },
                    label = { Text("Total pagado acumulado (CUP)") },
                    suffix = { Text("CUP") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_amount_input")
                )

                OutlinedTextField(
                    value = referenceInput,
                    onValueChange = { referenceInput = it; errorText = null },
                    label = {
                        Text(
                            if (isActorPortalMode) {
                                "No. operación Transfermóvil *"
                            } else {
                                "No. operación / referencia Transfermóvil"
                            }
                        )
                    },
                    placeholder = { Text("Ej: TM-849201 o código del SMS") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_reference_input")
                )

                if (!isActorPortalMode) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = {
                                paidInput = CurrencyFormatter.formatInputValue(settlement.totalDueCents)
                                errorText = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Liquidar total")
                        }
                        FilledTonalButton(
                            onClick = {
                                paidInput = "0.00"
                                referenceInput = ""
                                errorText = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Marcar pendiente")
                        }
                    }
                }

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
                    val cents = CurrencyFormatter.parseAmountToCents(paidInput)
                    val cleanRef = referenceInput.trim()
                    when {
                        cents == null -> errorText = "Ingrese un monto válido en CUP."
                        isActorPortalMode && cents <= 0L -> errorText = "El monto pagado debe ser mayor que cero."
                        isActorPortalMode && cleanRef.isEmpty() -> errorText = "Ingresa el número de operación del comprobante Transfermóvil."
                        else -> {
                            onConfirmPayment(cents, selectedChannel, cleanRef)
                            onDismiss()
                        }
                    }
                },
                modifier = Modifier.testTag("confirm_payment_button")
            ) {
                Text(if (isActorPortalMode) "Reportar pago" else "Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
