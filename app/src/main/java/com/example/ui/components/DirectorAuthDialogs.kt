package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.ManagerRole
import com.example.data.model.ManagerUser
import kotlinx.coroutines.launch

@Composable
fun ManagerLoginDialog(
    managerUsers: List<ManagerUser>,
    onDismiss: () -> Unit,
    onSignIn: suspend (username: String, password: String) -> Result<ManagerUser>,
    onRegisterFirstManager: suspend (username: String, fullName: String, role: ManagerRole, password: String) -> Result<ManagerUser>
) {
    val scope = rememberCoroutineScope()
    val isFirstRun = managerUsers.isEmpty()

    var fullName by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(ManagerRole.ADMIN) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = if (isFirstRun) "Configuración Inicial" else "Acceso Privado",
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
                    text = if (isFirstRun) {
                        "Registra la cuenta local autorizada para gestionar la empresa."
                    } else {
                        "Ingresa tu usuario y contraseña local."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isFirstRun) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it; errorText = null },
                        label = { Text("Nombre completo *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.trim().lowercase(); errorText = null },
                    label = { Text("Usuario *") },
                    leadingIcon = { Icon(Icons.Rounded.AccountCircle, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_manager_username_input")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorText = null },
                    label = { Text("Contraseña *") },
                    leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_manager_password_input")
                )

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
                    scope.launch {
                        val res = if (isFirstRun) {
                            onRegisterFirstManager(username, fullName, selectedRole, password)
                        } else {
                            onSignIn(username, password)
                        }
                        res.onSuccess { onDismiss() }
                            .onFailure { err -> errorText = err.localizedMessage ?: "Error al iniciar sesión" }
                    }
                },
                modifier = Modifier.testTag("confirm_dialog_manager_login")
            ) {
                Text(if (isFirstRun) "Crear y entrar" else "Entrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerUsersSheet(
    activeUser: ManagerUser?,
    users: List<ManagerUser>,
    onDismiss: () -> Unit,
    onCreateUser: (username: String, fullName: String, role: ManagerRole, password: String) -> Unit,
    onToggleActive: (ManagerUser) -> Unit,
    onResetPassword: (userId: String, newPassword: String) -> Unit,
    onSignOut: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCreateForm by remember { mutableStateOf(false) }
    var newFullName by remember { mutableStateOf("") }
    var newUsername by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf(ManagerRole.ECONOMIST) }
    var newPassword by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    var resetTargetUser by remember { mutableStateOf<ManagerUser?>(null) }
    var resetPasswordInput by remember { mutableStateOf("") }

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ManageAccounts,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Usuarios Locales del Manager",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (activeUser != null) {
                        Text(
                            text = "Sesión activa: ${activeUser.fullName} (@${activeUser.username} · ${activeUser.roleEnum().label})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showCreateForm = !showCreateForm; formError = null },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("toggle_create_manager_user_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showCreateForm) "Ocultar formulario" else "Nuevo Director")
                }
                OutlinedButton(
                    onClick = {
                        onSignOut()
                        onDismiss()
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("manager_sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Salir")
                }
            }

            if (showCreateForm) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Registrar otro usuario Manager",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = newFullName,
                            onValueChange = { newFullName = it; formError = null },
                            label = { Text("Nombre completo *") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newUsername,
                            onValueChange = { newUsername = it.trim().lowercase(); formError = null },
                            label = { Text("Usuario *") },
                            placeholder = { Text("Ej: economica1") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ManagerRole.entries.forEach { role ->
                                FilterChip(
                                    selected = newRole == role,
                                    onClick = { newRole = role },
                                    label = { Text(role.label) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it; formError = null },
                            label = { Text("Contraseña inicial *") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (formError != null) {
                            Text(
                                text = formError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Button(
                            onClick = {
                                when {
                                    newFullName.isBlank() -> formError = "Ingresa el nombre completo."
                                    newUsername.length < 3 -> formError = "El usuario debe tener al menos 3 caracteres."
                                    newPassword.length < 4 -> formError = "La contraseña debe tener al menos 4 caracteres."
                                    else -> {
                                        onCreateUser(newUsername, newFullName, newRole, newPassword)
                                        newFullName = ""
                                        newUsername = ""
                                        newPassword = ""
                                        showCreateForm = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Guardar usuario local")
                        }
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "Equipo registrado (${users.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            users.forEach { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${user.fullName} (@${user.username})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = user.roleEnum().label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = user.active,
                                onCheckedChange = { onToggleActive(user) },
                                enabled = user.id != activeUser?.id
                            )
                        }
                        TextButton(
                            onClick = {
                                resetTargetUser = user
                                resetPasswordInput = ""
                            }
                        ) {
                            Text("Cambiar contraseña")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    resetTargetUser?.let { target ->
        AlertDialog(
            onDismissRequest = { resetTargetUser = null },
            title = { Text("Nueva contraseña para @${target.username}") },
            text = {
                OutlinedTextField(
                    value = resetPasswordInput,
                    onValueChange = { resetPasswordInput = it },
                    label = { Text("Contraseña (mín. 4 caracteres)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetPasswordInput.trim().length >= 4) {
                            onResetPassword(target.id, resetPasswordInput)
                            resetTargetUser = null
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { resetTargetUser = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
