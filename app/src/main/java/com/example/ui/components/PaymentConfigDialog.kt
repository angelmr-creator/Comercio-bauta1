package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.PaymentConfig
import com.example.util.TransfermovilHelper

@Composable
fun PaymentConfigDialog(
    initialConfig: PaymentConfig,
    onDismiss: () -> Unit,
    onSave: (
        bankName: String,
        bankAccount: String,
        accountHolder: String,
        qrPaymentUrl: String
    ) -> Unit
) {
    val cubanBanks = remember {
        listOf("BANDEC", "BPA", "Banco Metropolitano", "MiTransfer")
    }

    var bankName by remember(initialConfig) {
        mutableStateOf(initialConfig.bankName)
    }
    var bankAccount by remember(initialConfig) {
        mutableStateOf(initialConfig.bankAccount.ifBlank { PaymentConfig.DEFAULT_BAUTA_BANK_ACCOUNT })
    }
    var accountHolder by remember(initialConfig) {
        mutableStateOf(initialConfig.accountHolder.ifBlank { "Empresa Municipal de Comercio Bauta" })
    }
    var qrPaymentUrl by remember(initialConfig) {
        mutableStateOf(initialConfig.qrPaymentUrl)
    }
    var phoneForQr by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.QrCode2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Cobro Digital Transfermóvil",
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
                    text = "Configura la cuenta/tarjeta CUP y el enlace o QR de Bulevar MiTransfer donde los actores pagarán por Transfermóvil (sin efectivo).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    cubanBanks.forEach { bank ->
                        FilterChip(
                            selected = bankName.equals(bank, ignoreCase = true),
                            onClick = { bankName = bank; errorText = null },
                            label = { Text(bank) }
                        )
                    }
                }

                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it; errorText = null },
                    label = { Text("Banco / Pasarela") },
                    placeholder = { Text("Ej: BANDEC, BPA o Metropolitano") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("config_bank_name_input")
                )

                OutlinedTextField(
                    value = bankAccount,
                    onValueChange = { bankAccount = it; errorText = null },
                    label = { Text("Tarjeta / Cuenta bancaria CUP *") },
                    placeholder = { Text("9225 9598 0000 0000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("config_bank_account_input")
                )

                OutlinedTextField(
                    value = accountHolder,
                    onValueChange = { accountHolder = it; errorText = null },
                    label = { Text("Entidad titular") },
                    placeholder = { Text("Empresa Municipal de Comercio Bauta") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("config_account_holder_input")
                )

                OutlinedTextField(
                    value = qrPaymentUrl,
                    onValueChange = { qrPaymentUrl = it; errorText = null },
                    label = { Text("Link QR / Bulevar MiTransfer") },
                    placeholder = { Text("https://... o TRANSFERMOVIL_ETECSA,TRANSFERENCIA,...") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("config_qr_url_input")
                )

                OutlinedTextField(
                    value = phoneForQr,
                    onValueChange = { phoneForQr = it },
                    label = { Text("Teléfono asociado (opcional, para QR)") },
                    placeholder = { Text("5350000000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = {
                        val payload = TransfermovilHelper.buildTransferQrPayload(bankAccount, phoneForQr)
                        if (payload.isEmpty()) {
                            errorText = "Ingresa primero el número de tarjeta o cuenta CUP."
                        } else {
                            qrPaymentUrl = payload
                            errorText = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Generar cadena QR Transfermóvil")
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
                    val cleanAccount = bankAccount.trim()
                    val cleanUrl = qrPaymentUrl.trim()
                    if (cleanAccount.isEmpty() && cleanUrl.isEmpty()) {
                        errorText = "Ingresa al menos la cuenta/tarjeta CUP o el link QR."
                    } else {
                        onSave(
                            bankName.trim().ifBlank { "BANDEC" },
                            cleanAccount,
                            accountHolder.trim().ifBlank { "Empresa Municipal de Comercio Bauta" },
                            cleanUrl
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("save_payment_config_button")
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
