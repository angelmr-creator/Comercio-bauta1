package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.PaymentConfig
import com.example.ui.theme.LocalCommerceColors
import com.example.util.TransfermovilHelper

@Composable
fun ManagerBankAccountCard(
    paymentConfig: PaymentConfig,
    onSavePaymentConfig: (
        bankName: String,
        bankAccount: String,
        accountHolder: String,
        qrPaymentUrl: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val commerceColors = LocalCommerceColors.current
    val cubanBanks = remember {
        listOf("BANDEC", "BPA", "Banco Metropolitano", "MiTransfer")
    }

    var isFormExpanded by rememberSaveable { mutableStateOf(true) }
    var bankName by remember(paymentConfig) {
        mutableStateOf(paymentConfig.bankName.ifBlank { "BANDEC" })
    }
    var bankAccount by remember(paymentConfig) {
        mutableStateOf(paymentConfig.bankAccount.ifBlank { PaymentConfig.DEFAULT_BAUTA_BANK_ACCOUNT })
    }
    var accountHolder by remember(paymentConfig) {
        mutableStateOf(paymentConfig.accountHolder.ifBlank { "Empresa Municipal de Comercio Bauta" })
    }
    var qrPaymentUrl by remember(paymentConfig) {
        mutableStateOf(paymentConfig.qrPaymentUrl)
    }
    var phoneForQr by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val hasUnsavedChanges =
        bankName.trim() != paymentConfig.bankName.trim() ||
            bankAccount.trim() != paymentConfig.bankAccount.trim() ||
            accountHolder.trim() != paymentConfig.accountHolder.trim() ||
            qrPaymentUrl.trim() != paymentConfig.qrPaymentUrl.trim()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_bank_config_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(9.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cuenta Bancaria de la Empresa",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${paymentConfig.bankName.ifBlank { "BANDEC" }} · ${paymentConfig.bankAccount.ifBlank { "Sin definir" }}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (paymentConfig.bankAccount.isNotBlank()) {
                        IconButton(
                            onClick = {
                                TransfermovilHelper.copyToClipboard(
                                    context = context,
                                    label = "Cuenta Comercio Bauta",
                                    text = TransfermovilHelper.cleanCardOrAccountForCopy(paymentConfig.bankAccount),
                                    toastMessage = "Cuenta ${paymentConfig.bankAccount} copiada"
                                )
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("manager_copy_bank_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copiar número de cuenta bancaria"
                            )
                        }
                    }
                    TextButton(
                        onClick = { isFormExpanded = !isFormExpanded },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("dashboard_edit_bank_button")
                    ) {
                        Text(if (isFormExpanded) "Ocultar" else "Editar")
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = if (isFormExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (isFormExpanded) "Ocultar formulario" else "Mostrar formulario",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isFormExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Actualizar datos de cobro digital (Transfermóvil)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        if (!hasUnsavedChanges) {
                            StatusBadge(
                                text = "Vigente",
                                containerColor = commerceColors.collectedContainer,
                                contentColor = commerceColors.collected
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        cubanBanks.forEach { bank ->
                            FilterChip(
                                selected = bankName.equals(bank, ignoreCase = true),
                                onClick = {
                                    bankName = bank
                                    errorText = null
                                },
                                label = { Text(bank) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = bankName,
                        onValueChange = {
                            bankName = it
                            errorText = null
                        },
                        label = { Text("Banco / Pasarela") },
                        placeholder = { Text("Ej: BANDEC, BPA o Metropolitano") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manager_bank_name_input")
                    )

                    OutlinedTextField(
                        value = bankAccount,
                        onValueChange = {
                            bankAccount = it
                            errorText = null
                        },
                        label = { Text("Número de cuenta / tarjeta CUP *") },
                        placeholder = { Text("0618221336820012") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manager_bank_account_input")
                    )

                    OutlinedTextField(
                        value = accountHolder,
                        onValueChange = {
                            accountHolder = it
                            errorText = null
                        },
                        label = { Text("Entidad titular") },
                        placeholder = { Text("Empresa Municipal de Comercio Bauta") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manager_account_holder_input")
                    )

                    OutlinedTextField(
                        value = qrPaymentUrl,
                        onValueChange = {
                            qrPaymentUrl = it
                            errorText = null
                        },
                        label = { Text("Enlace o código QR MiTransfer") },
                        placeholder = { Text("https://... o TRANSFERMOVIL_ETECSA,TRANSFERENCIA,...") },
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manager_qr_url_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = phoneForQr,
                            onValueChange = { phoneForQr = it },
                            label = { Text("Teléfono para QR (opcional)") },
                            placeholder = { Text("5350000000") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = {
                                val payload = TransfermovilHelper.buildTransferQrPayload(bankAccount, phoneForQr)
                                if (payload.isEmpty()) {
                                    errorText = "Ingresa primero el número de cuenta o tarjeta CUP."
                                } else {
                                    qrPaymentUrl = payload
                                    errorText = null
                                }
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("manager_generate_qr_button")
                        ) {
                            Icon(Icons.Rounded.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Generar QR")
                        }
                    }

                    if (errorText != null) {
                        Text(
                            text = errorText!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Button(
                        onClick = {
                            val cleanAccount = bankAccount.trim()
                            val cleanUrl = qrPaymentUrl.trim()
                            if (cleanAccount.isEmpty() && cleanUrl.isEmpty()) {
                                errorText = "Ingresa al menos la cuenta bancaria CUP o el enlace QR."
                            } else {
                                onSavePaymentConfig(
                                    bankName.trim().ifBlank { "BANDEC" },
                                    cleanAccount,
                                    accountHolder.trim().ifBlank { "Empresa Municipal de Comercio Bauta" },
                                    cleanUrl
                                )
                                errorText = null
                            }
                        },
                        enabled = hasUnsavedChanges || !paymentConfig.isConfigured,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("manager_save_bank_button")
                    ) {
                        Icon(
                            imageVector = if (hasUnsavedChanges) Icons.Rounded.Save else Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasUnsavedChanges) {
                                "Actualizar cuenta bancaria"
                            } else {
                                "Cuenta bancaria actualizada"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
