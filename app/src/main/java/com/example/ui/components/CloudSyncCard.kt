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
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.ui.state.CloudSyncState
import com.example.ui.theme.LocalCommerceColors
import com.example.util.TransfermovilHelper

@Composable
fun CloudSyncCard(
    cloudSync: CloudSyncState,
    isActorPortal: Boolean,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    onJoinWorkspace: (String) -> Unit,
    onCreateNewWorkspace: () -> Unit,
    onForceSync: () -> Unit,
    onSignOutComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val commerceColors = LocalCommerceColors.current
    var showJoinDialog by remember { mutableStateOf(false) }

    val statusText: String
    val statusBg: androidx.compose.ui.graphics.Color
    val statusFg: androidx.compose.ui.graphics.Color
    val statusIcon = when {
        !cloudSync.isAuthenticated -> {
            statusText = "Modo local (Cuba)"
            statusBg = MaterialTheme.colorScheme.surfaceVariant
            statusFg = MaterialTheme.colorScheme.onSurfaceVariant
            Icons.Rounded.CloudOff
        }
        !cloudSync.isOnline && cloudSync.hasPendingWrites -> {
            statusText = "Sin señal · En cola local"
            statusBg = commerceColors.pendingContainer
            statusFg = commerceColors.pending
            Icons.Rounded.CloudQueue
        }
        !cloudSync.isOnline -> {
            statusText = "Sin señal · Caché offline"
            statusBg = commerceColors.pendingContainer
            statusFg = commerceColors.pending
            Icons.Rounded.CloudOff
        }
        cloudSync.hasPendingWrites -> {
            statusText = "Subiendo cola..."
            statusBg = MaterialTheme.colorScheme.secondaryContainer
            statusFg = MaterialTheme.colorScheme.onSecondaryContainer
            Icons.Rounded.CloudSync
        }
        cloudSync.isFromCache -> {
            statusText = "Conectando nube..."
            statusBg = MaterialTheme.colorScheme.secondaryContainer
            statusFg = MaterialTheme.colorScheme.onSecondaryContainer
            Icons.Rounded.CloudSync
        }
        else -> {
            statusText = "Nube En Vivo"
            statusBg = commerceColors.collectedContainer
            statusFg = commerceColors.collected
            Icons.Rounded.CloudDone
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusFg,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isActorPortal) "Sincronización con Comercio Bauta" else "Sincronización Nube · Cuba 100%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(
                    text = statusText,
                    containerColor = statusBg,
                    contentColor = statusFg
                )
            }

            if (!cloudSync.isAuthenticated) {
                Text(
                    text = if (isActorPortal) {
                        "Conecta tu cuenta de Google para enlazar tu negocio automáticamente con Comercio Bauta, enviar tus declaraciones y reportar pagos Transfermóvil en tiempo real. Si pierdes señal (ETECSA), se guardan en cola local hasta que vuelva."
                    } else {
                        "Conecta tu cuenta de Google para ver en vivo a todos los actores económicos, sus declaraciones mensuales y sus pagos Transfermóvil desde otros teléfonos. El caché offline sigue funcionando aunque caiga ETECSA o Wi-Fi."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                GoogleSignInButton(
                    onAuthSuccess = onAuthSuccess,
                    onAuthError = onAuthError,
                    label = "Conectar En Vivo con Google"
                )
            } else {
                if (cloudSync.userEmail.isNotBlank()) {
                    Text(
                        text = "Cuenta: ${cloudSync.userEmail}" +
                            if (cloudSync.memberCount > 0) " · ${cloudSync.memberCount} dispositivo(s)" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (cloudSync.workspaceId.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Código de Empresa",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = cloudSync.workspaceId,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = when {
                                        isActorPortal -> "Conectado en tiempo real · Cuenta CUP sincronizada"
                                        cloudSync.isWorkspaceOwner ->
                                            "Rol nube: Propietario (comparte este código con tus dispositivos)"
                                        else ->
                                            "Conectado al espacio compartido · Cuenta CUP sincronizada"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    TransfermovilHelper.copyToClipboard(
                                        context = context,
                                        label = "Código Comercio Bauta",
                                        text = cloudSync.workspaceId,
                                        toastMessage = "Código ${cloudSync.workspaceId} copiado"
                                    )
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("copy_workspace_code_button")
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copiar")
                            }
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isActorPortal) {
                                    "Enlace pendiente con Comercio Bauta"
                                } else {
                                    "Espacio compartido por activar"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isActorPortal) {
                                    "Tu cuenta Google ya está conectada. Pulsa Sincronizar para enlazar con comercio-bauta en cuanto esté activo, o usa Unirse si recibiste un código distinto."
                                } else {
                                    "Pulsa Sincronizar para activar el espacio compartido comercio-bauta y recibir todas las operaciones de los actores."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!cloudSync.syncError.isNullOrBlank()) {
                    Text(
                        text = cloudSync.syncError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { showJoinDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("join_workspace_button")
                    ) {
                        Icon(Icons.Rounded.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Unirse", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onForceSync,
                        enabled = !cloudSync.isSyncing,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("force_sync_button")
                    ) {
                        Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sincronizar", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            signOutCloud(
                                credentialManager = credentialManager,
                                scope = scope,
                                onSignOutComplete = onSignOutComplete
                            )
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("cloud_sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Logout,
                            contentDescription = "Desconectar nube",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    if (showJoinDialog) {
        JoinWorkspaceDialog(
            currentWorkspaceId = cloudSync.workspaceId,
            isActorPortal = isActorPortal,
            onDismiss = { showJoinDialog = false },
            onJoin = { code ->
                onJoinWorkspace(code)
                showJoinDialog = false
            },
            onCreateNew = {
                onCreateNewWorkspace()
                showJoinDialog = false
            }
        )
    }
}

@Composable
private fun JoinWorkspaceDialog(
    currentWorkspaceId: String,
    isActorPortal: Boolean,
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit,
    onCreateNew: () -> Unit
) {
    var codeInput by remember { mutableStateOf("") }
    val normalized = codeInput.trim().lowercase()
    val valid = normalized.matches(Regex("^[a-z0-9_\\-]{4,64}$"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Conectar con Código de Empresa",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "El espacio principal de la empresa es comercio-bauta (se enlaza automáticamente). Si utilizas una sucursal con otro código (por ejemplo: bauta-7k3m9p), escríbelo abajo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it },
                    label = { Text("Código de Empresa") },
                    placeholder = { Text("comercio-bauta o bauta-xxxxxx") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("workspace_code_input")
                )
                if (currentWorkspaceId.isNotBlank()) {
                    Text(
                        text = "Espacio actual: $currentWorkspaceId",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!isActorPortal) {
                    TextButton(onClick = onCreateNew) {
                        Text("Crear un espacio nuevo propio")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onJoin(normalized) },
                enabled = valid,
                modifier = Modifier.testTag("confirm_join_workspace_button")
            ) {
                Text("Conectar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
