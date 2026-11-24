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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.LegalType
import com.example.data.model.LocaleSpace
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ActorEditorSheet(
    initialActor: EconomicActor?,
    locales: List<LocaleSpace>,
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        previousLocaleId: String?,
        name: String,
        legalType: LegalType,
        representativeName: String,
        phone: String,
        taxId: String,
        actorEmail: String,
        localeId: String,
        localeName: String,
        monthlyRentCents: Long,
        profitSharePercent: Long,
        active: Boolean
    ) -> Unit,
    onDelete: ((EconomicActor) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(initialActor) { mutableStateOf(initialActor?.name ?: "") }
    var legalType by remember(initialActor) {
        mutableStateOf(initialActor?.legalTypeEnum() ?: LegalType.TCP)
    }
    var representativeName by remember(initialActor) {
        mutableStateOf(initialActor?.representativeName ?: "")
    }
    var phone by remember(initialActor) { mutableStateOf(initialActor?.phone ?: "") }
    var taxId by remember(initialActor) { mutableStateOf(initialActor?.taxId ?: "") }
    var actorEmail by remember(initialActor) { mutableStateOf(initialActor?.actorEmail ?: "") }

    var selectedLocaleId by remember(initialActor) { mutableStateOf(initialActor?.localeId ?: "") }
    var selectedLocaleName by remember(initialActor) { mutableStateOf(initialActor?.localeName ?: "") }
    var localeDropdownExpanded by remember { mutableStateOf(false) }

    var rentText by remember(initialActor) {
        mutableStateOf(
            if (initialActor != null) {
                CurrencyFormatter.formatInputValue(initialActor.monthlyRentCents)
            } else {
                ""
            }
        )
    }
    var percentText by remember(initialActor) {
        mutableStateOf((initialActor?.profitSharePercent ?: 30L).toString())
    }
    var active by remember(initialActor) { mutableStateOf(initialActor?.active ?: true) }
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
                text = if (initialActor == null) "Registrar Actor Económico" else "Editar Actor Económico",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorText = null },
                label = { Text("Nombre del negocio o actor *") },
                placeholder = { Text("Ej: Panadería El Trigal") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("actor_name_input")
            )

            Text(
                text = "Forma de gestión económica",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LegalType.entries.forEach { option ->
                    FilterChip(
                        selected = legalType == option,
                        onClick = { legalType = option },
                        label = { Text(option.label) },
                        modifier = Modifier.testTag("actor_type_${option.name.lowercase()}")
                    )
                }
            }

            OutlinedTextField(
                value = representativeName,
                onValueChange = { representativeName = it; errorText = null },
                label = { Text("Titular o representante *") },
                placeholder = { Text("Ej: Carlos Manuel Pérez") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("actor_rep_input")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Teléfono (+53)") },
                    placeholder = { Text("5555 1234") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("actor_phone_input")
                )
                OutlinedTextField(
                    value = taxId,
                    onValueChange = { taxId = it },
                    label = { Text("NIT / Licencia") },
                    placeholder = { Text("NIT-...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("actor_nit_input")
                )
            }

            OutlinedTextField(
                value = actorEmail,
                onValueChange = { actorEmail = it; errorText = null },
                label = { Text("Correo Google del actor (opcional)") },
                placeholder = { Text("Ej: negocio.bauta@gmail.com") },
                supportingText = {
                    Text("Enlaza el correo Google del actor para que vea sus liquidaciones, pagos y recibos en su teléfono.")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("actor_email_input")
            )

            ExposedDropdownMenuBox(
                expanded = localeDropdownExpanded,
                onExpandedChange = { localeDropdownExpanded = !localeDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = if (selectedLocaleName.isBlank()) "Sin local asignado" else selectedLocaleName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Local arrendado") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = localeDropdownExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                        .testTag("actor_locale_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = localeDropdownExpanded,
                    onDismissRequest = { localeDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Sin local asignado") },
                        onClick = {
                            selectedLocaleId = ""
                            selectedLocaleName = ""
                            localeDropdownExpanded = false
                        }
                    )
                    locales.forEach { locale ->
                        val display = "${locale.code} · ${locale.name}"
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(display, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = "${locale.statusEnum().label} · ${CurrencyFormatter.format(locale.monthlyRentCents)}/mes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                selectedLocaleId = locale.id
                                selectedLocaleName = display
                                if (rentText.isBlank() || initialActor == null) {
                                    rentText = CurrencyFormatter.formatInputValue(locale.monthlyRentCents)
                                }
                                localeDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = rentText,
                    onValueChange = { rentText = it; errorText = null },
                    label = { Text("Alquiler mensual (CUP)") },
                    placeholder = { Text("5000.00") },
                    suffix = { Text("CUP") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("actor_rent_input")
                )
                OutlinedTextField(
                    value = percentText,
                    onValueChange = { percentText = it; errorText = null },
                    label = { Text("% Utilidad") },
                    placeholder = { Text("30") },
                    suffix = { Text("%") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(0.8f)
                        .testTag("actor_percent_input")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Contrato vigente",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Incluye al actor en los cobros mensuales de Bauta",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = active,
                    onCheckedChange = { active = it },
                    modifier = Modifier.testTag("actor_active_switch")
                )
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
                if (initialActor != null && onDelete != null) {
                    OutlinedButton(
                        onClick = {
                            onDelete(initialActor)
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("delete_actor_button")
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
                        val percent = percentText.trim().toLongOrNull()
                        val cleanEmail = actorEmail.trim().lowercase()
                        when {
                            name.trim().isEmpty() -> errorText = "Indique el nombre del actor económico."
                            representativeName.trim().isEmpty() -> errorText = "Indique el nombre del titular."
                            cleanEmail.isNotEmpty() && (!cleanEmail.contains("@") || !cleanEmail.contains(".")) ->
                                errorText = "Ingresa un correo válido o déjalo vacío."
                            rentCents == null -> errorText = "Ingrese un alquiler válido en CUP."
                            percent == null || percent !in 0L..100L -> errorText = "El porcentaje debe estar entre 0 y 100."
                            else -> {
                                onSave(
                                    initialActor?.id,
                                    initialActor?.localeId,
                                    name.trim(),
                                    legalType,
                                    representativeName.trim(),
                                    phone.trim(),
                                    taxId.trim(),
                                    cleanEmail,
                                    selectedLocaleId,
                                    selectedLocaleName,
                                    rentCents,
                                    percent,
                                    active
                                )
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("save_actor_button")
                ) {
                    Text("Guardar")
                }
            }
        }
    }
}
