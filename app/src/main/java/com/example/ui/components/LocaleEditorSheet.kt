package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.data.model.LocaleSpace
import com.example.data.model.LocaleStatus
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LocaleEditorSheet(
    initialLocale: LocaleSpace?,
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        code: String,
        name: String,
        address: String,
        areaSqm: Double,
        monthlyRentCents: Long,
        status: LocaleStatus
    ) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var code by remember(initialLocale) { mutableStateOf(initialLocale?.code ?: "") }
    var name by remember(initialLocale) { mutableStateOf(initialLocale?.name ?: "") }
    var address by remember(initialLocale) { mutableStateOf(initialLocale?.address ?: "") }
    var areaText by remember(initialLocale) {
        mutableStateOf(
            if (initialLocale != null && initialLocale.areaSqm > 0.0) {
                initialLocale.areaSqm.toString()
            } else {
                ""
            }
        )
    }
    var rentText by remember(initialLocale) {
        mutableStateOf(
            if (initialLocale != null) {
                CurrencyFormatter.formatInputValue(initialLocale.monthlyRentCents)
            } else {
                ""
            }
        )
    }
    var status by remember(initialLocale) {
        mutableStateOf(initialLocale?.statusEnum() ?: LocaleStatus.DISPONIBLE)
    }
    var errorText by remember { mutableStateOf<String?>(null) }

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
                text = if (initialLocale == null) "Registrar Local Municipal" else "Editar Local",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it; errorText = null },
                    label = { Text("Código *") },
                    placeholder = { Text("LOC-01") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier
                        .weight(0.42f)
                        .testTag("locale_code_input")
                )
                OutlinedTextField(
                    value = areaText,
                    onValueChange = { areaText = it; errorText = null },
                    label = { Text("Área (m²)") },
                    placeholder = { Text("45.0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(0.58f)
                        .testTag("locale_area_input")
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorText = null },
                label = { Text("Nombre del local *") },
                placeholder = { Text("Ej: Cafetería Boulevard Bauta") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("locale_name_input")
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Dirección en Bauta") },
                placeholder = { Text("Ej: Ave 251 e/ 142 y 144, Bauta") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("locale_address_input")
            )

            OutlinedTextField(
                value = rentText,
                onValueChange = { rentText = it; errorText = null },
                label = { Text("Alquiler mensual base (CUP) *") },
                placeholder = { Text("5000.00") },
                suffix = { Text("CUP") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("locale_rent_input")
            )

            Text(
                text = "Estado de ocupación",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LocaleStatus.entries.forEach { option ->
                    FilterChip(
                        selected = status == option,
                        onClick = { status = option },
                        label = { Text(option.label) },
                        modifier = Modifier.testTag("locale_status_${option.name.lowercase()}")
                    )
                }
            }

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
                if (initialLocale != null && onDelete != null) {
                    OutlinedButton(
                        onClick = {
                            onDelete(initialLocale.id)
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("delete_locale_button")
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
                        val rentCents = if (rentText.isBlank()) 0L else CurrencyFormatter.parseAmountToCents(rentText)
                        val area = if (areaText.isBlank()) 0.0 else areaText.replace(",", ".").toDoubleOrNull()
                        when {
                            code.trim().isEmpty() -> errorText = "Indique el código del local."
                            name.trim().isEmpty() -> errorText = "Indique el nombre del local."
                            rentCents == null -> errorText = "Ingrese un monto de alquiler válido en CUP."
                            area == null || area < 0.0 -> errorText = "El área en m² debe ser un número válido."
                            else -> {
                                onSave(
                                    initialLocale?.id,
                                    code.trim(),
                                    name.trim(),
                                    address.trim(),
                                    area,
                                    rentCents,
                                    status
                                )
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("save_locale_button")
                ) {
                    Text("Guardar")
                }
            }
        }
    }
}
