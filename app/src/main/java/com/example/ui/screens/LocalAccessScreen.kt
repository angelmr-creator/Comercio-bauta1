package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.EconomicActor
import com.example.data.model.LegalType
import com.example.ui.components.GoogleSignInButton
import com.example.ui.state.CloudSyncState
import com.example.ui.theme.LocalCommerceColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LocalAccessScreen(
    actors: List<EconomicActor>,
    cloudSync: CloudSyncState,
    onCloudAuthSuccess: () -> Unit,
    onCloudAuthError: (String) -> Unit,
    onEnterActorPortal: (
        selectedActorId: String?,
        actorNameInput: String,
        legalType: LegalType,
        representativeName: String,
        phone: String,
        taxId: String
    ) -> Unit,
    onRequestPrivateAccess: () -> Unit
) {
    val commerceColors = LocalCommerceColors.current
    val activeActors = remember(actors) { actors.filter { it.active } }
    var selectedActor by remember { mutableStateOf(activeActors.firstOrNull()) }
    var actorDropdownExpanded by remember { mutableStateOf(false) }
    var registerNewMode by remember { mutableStateOf(activeActors.isEmpty()) }
    var businessNameInput by remember { mutableStateOf("") }
    var selectedLegalType by remember { mutableStateOf(LegalType.TCP) }
    var representativeInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var taxIdInput by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }
    var secretTapCount by remember { mutableIntStateOf(0) }
    val silentInteraction = remember { MutableInteractionSource() }

    LaunchedEffect(activeActors, cloudSync.userEmail) {
        if (activeActors.isNotEmpty() && selectedActor == null && businessNameInput.isBlank()) {
            val matchedByEmail = cloudSync.userEmail.takeIf { it.isNotBlank() }?.let { email ->
                activeActors.firstOrNull { it.actorEmail.equals(email, ignoreCase = true) }
            }
            selectedActor = matchedByEmail ?: activeActors.firstOrNull()
            registerNewMode = false
            formError = null
        } else if (activeActors.isEmpty()) {
            selectedActor = null
            registerNewMode = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .combinedClickable(
                        interactionSource = silentInteraction,
                        indication = null,
                        onClick = {},
                        onLongClick = onRequestPrivateAccess
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_bauta_header),
                    contentDescription = stringResource(R.string.cd_bauta_banner),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.12f),
                                    Color(0xFF0B3B2E).copy(alpha = 0.90f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp)
                ) {
                    Surface(
                        color = Color(0xFFF5B731),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "BAUTA · ARTEMISA · PORTAL COMERCIAL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF281A00),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Comercio Bauta",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhoneAndroid,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Operaciones, Transfermóvil y Recibos PDF · Cuba 100%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Storefront,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (registerNewMode) {
                                    "Registro Obligatorio · Actor Económico"
                                } else {
                                    "Acceso · Actor Económico"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (registerNewMode) {
                                    "Registra todos los datos obligatorios de tu negocio (TCP, MIPYME, CNA o PDL) para consultar tus operaciones, reportar pagos Transfermóvil y descargar recibos PDF."
                                } else {
                                    "Selecciona tu negocio inscrito o registra uno nuevo para ver tus declaraciones, pagos y recibos PDF."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Optional cloud lookup without exposing internal management roles
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (cloudSync.isAuthenticated) {
                            commerceColors.collectedContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!cloudSync.isAuthenticated) {
                                Text(
                                    text = "¿Tu negocio ya está inscrito en Comercio Bauta? Conecta tu cuenta Google para cargar automáticamente tus datos y operaciones.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                GoogleSignInButton(
                                    onAuthSuccess = onCloudAuthSuccess,
                                    onAuthError = onCloudAuthError,
                                    label = "Cargar mi negocio con Google"
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = commerceColors.collected,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Google conectado: ${cloudSync.userEmail}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = commerceColors.collected
                                        )
                                        Text(
                                            text = if (activeActors.isNotEmpty()) {
                                                "Tu registro se vinculará automáticamente con tu correo."
                                            } else {
                                                "Completa el registro de tu negocio abajo para activar tu expediente."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    if (activeActors.isNotEmpty() && !registerNewMode) {
                        ExposedDropdownMenuBox(
                            expanded = actorDropdownExpanded,
                            onExpandedChange = { actorDropdownExpanded = !actorDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedActor?.let { "${it.name} (${it.legalTypeEnum().label})" }
                                    ?: "Seleccionar negocio registrado",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Negocio / Actor Económico") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = actorDropdownExpanded)
                                },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    .testTag("actor_login_dropdown")
                            )
                            ExposedDropdownMenu(
                                expanded = actorDropdownExpanded,
                                onDismissRequest = { actorDropdownExpanded = false }
                            ) {
                                activeActors.forEach { actor ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = "${actor.name} (${actor.legalTypeEnum().label})",
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = buildString {
                                                        append("Titular: ${actor.representativeName}")
                                                        if (actor.localeName.isNotBlank()) append(" · ${actor.localeName}")
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedActor = actor
                                            formError = null
                                            actorDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        selectedActor?.let { chosen ->
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Titular: ${chosen.representativeName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Teléfono: ${chosen.phone.ifBlank { "Por completar" }} · NIT/Lic: ${chosen.taxId.ifBlank { "Por completar" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (chosen.localeName.isNotBlank()) {
                                        Text(
                                            text = "Local: ${chosen.localeName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        TextButton(
                            onClick = {
                                registerNewMode = true
                                selectedActor = null
                                businessNameInput = ""
                                representativeInput = ""
                                phoneInput = ""
                                taxIdInput = ""
                                formError = null
                            },
                            modifier = Modifier.testTag("actor_switch_new_business_button")
                        ) {
                            Text("¿Negocio nuevo? Registrar datos obligatorios aquí")
                        }
                    } else {
                        OutlinedTextField(
                            value = businessNameInput,
                            onValueChange = { businessNameInput = it; formError = null },
                            label = { Text("Nombre de tu negocio *") },
                            placeholder = { Text("Ej: Cafetería La Plaza") },
                            leadingIcon = { Icon(Icons.Rounded.Storefront, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("actor_login_name_input")
                        )

                        Text(
                            text = "Forma de gestión económica *",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LegalType.entries.forEach { type ->
                                FilterChip(
                                    selected = selectedLegalType == type,
                                    onClick = { selectedLegalType = type },
                                    label = { Text(type.label) },
                                    modifier = Modifier.testTag("actor_login_legal_${type.name}")
                                )
                            }
                        }

                        OutlinedTextField(
                            value = representativeInput,
                            onValueChange = { representativeInput = it; formError = null },
                            label = { Text("Titular / Representante legal *") },
                            placeholder = { Text("Ej: Carlos Pérez") },
                            leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("actor_login_rep_input")
                        )

                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it; formError = null },
                            label = { Text("Teléfono de contacto (+53) *") },
                            placeholder = { Text("Ej: 5555 1234") },
                            leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("actor_login_phone_input")
                        )

                        OutlinedTextField(
                            value = taxIdInput,
                            onValueChange = { taxIdInput = it; formError = null },
                            label = { Text("NIT / Licencia Comercial o Carnet *") },
                            placeholder = { Text("Ej: NIT-900123") },
                            leadingIcon = { Icon(Icons.Rounded.Badge, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("actor_login_tax_input")
                        )

                        if (activeActors.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    registerNewMode = false
                                    selectedActor = activeActors.firstOrNull()
                                    formError = null
                                }
                            ) {
                                Text("Seleccionar negocio ya registrado")
                            }
                        }
                    }

                    if (!formError.isNullOrBlank()) {
                        Text(
                            text = formError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("actor_login_error_text")
                        )
                    }

                    Button(
                        onClick = {
                            if (registerNewMode || activeActors.isEmpty()) {
                                val cleanName = businessNameInput.trim()
                                val cleanRep = representativeInput.trim()
                                val cleanPhone = phoneInput.trim()
                                val cleanTax = taxIdInput.trim()
                                when {
                                    cleanName.isEmpty() ->
                                        formError = "El nombre de tu negocio es obligatorio."
                                    cleanRep.isEmpty() ->
                                        formError = "El nombre del titular o representante es obligatorio."
                                    cleanPhone.isEmpty() ->
                                        formError = "El teléfono de contacto es obligatorio."
                                    cleanTax.isEmpty() ->
                                        formError = "El NIT, Licencia Comercial o Carnet es obligatorio."
                                    else -> {
                                        formError = null
                                        val matchedExisting = activeActors.firstOrNull { actor ->
                                            actor.taxId.equals(cleanTax, ignoreCase = true) ||
                                                actor.name.equals(cleanName, ignoreCase = true)
                                        }
                                        onEnterActorPortal(
                                            matchedExisting?.id,
                                            cleanName,
                                            selectedLegalType,
                                            cleanRep,
                                            cleanPhone,
                                            cleanTax
                                        )
                                    }
                                }
                            } else {
                                val chosen = selectedActor
                                if (chosen == null) {
                                    formError = "Selecciona un negocio registrado o registra uno nuevo."
                                } else {
                                    formError = null
                                    onEnterActorPortal(
                                        chosen.id,
                                        chosen.name,
                                        chosen.legalTypeEnum(),
                                        chosen.representativeName,
                                        chosen.phone,
                                        chosen.taxId
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("enter_actor_portal_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Login,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (registerNewMode || activeActors.isEmpty()) {
                                "Registrar y Entrar"
                            } else {
                                "Entrar al Portal"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = "Empresa Municipal de Comercio Bauta · Artemisa",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        secretTapCount += 1
                        if (secretTapCount >= 5) {
                            secretTapCount = 0
                            onRequestPrivateAccess()
                        }
                    }
                    .padding(vertical = 8.dp, horizontal = 12.dp)
            )
        }
    }
}
