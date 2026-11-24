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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.EconomicActor
import com.example.data.model.LegalType
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LegalTypeBadge
import com.example.ui.components.MoneyText
import com.example.ui.components.StatusBadge
import com.example.ui.state.CommerceUiState
import com.example.ui.theme.LocalCommerceColors

@Composable
fun ActorsScreen(
    uiState: CommerceUiState,
    onSearchQueryChange: (String) -> Unit,
    onSelectLegalTypeFilter: (LegalType?) -> Unit,
    onAddActor: () -> Unit,
    onEditActor: (EconomicActor) -> Unit,
    onCreateSettlementForActor: (EconomicActor) -> Unit
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
                OutlinedTextField(
                    value = uiState.actorSearchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Buscar actor, titular o local...") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (uiState.actorSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("actor_search_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.actorLegalTypeFilter == null,
                        onClick = { onSelectLegalTypeFilter(null) },
                        label = { Text("Todos (${uiState.actors.size})") }
                    )
                    LegalType.entries.forEach { type ->
                        FilterChip(
                            selected = uiState.actorLegalTypeFilter == type,
                            onClick = {
                                onSelectLegalTypeFilter(
                                    if (uiState.actorLegalTypeFilter == type) null else type
                                )
                            },
                            label = { Text(type.label) }
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.filteredActors.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Rounded.Groups,
                            title = if (uiState.actors.isEmpty()) {
                                "Sin actores económicos registrados"
                            } else {
                                "Sin resultados para el filtro actual"
                            },
                            subtitle = "Agrega los TCP, MIPYME, CNA o PDL que arriendan locales municipales en Bauta.",
                            actionLabel = if (uiState.actors.isEmpty()) "Registrar actor" else null,
                            onAction = if (uiState.actors.isEmpty()) onAddActor else null,
                            actionTestTag = "empty_add_actor_button"
                        )
                    }
                } else {
                    items(uiState.filteredActors, key = { it.id }) { actor ->
                        Card(
                            onClick = { onEditActor(actor) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("actor_card_${actor.id}"),
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
                                    Text(
                                        text = actor.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        LegalTypeBadge(type = actor.legalTypeEnum())
                                        if (!actor.active) {
                                            StatusBadge(
                                                text = "Inactivo",
                                                containerColor = commerceColors.pendingContainer,
                                                contentColor = commerceColors.pending
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Titular: ${actor.representativeName}" +
                                        if (actor.phone.isNotBlank()) " · Tel: ${actor.phone}" else "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = "Local: ${actor.localeName.ifBlank { "Sin local asignado" }}" +
                                        if (actor.taxId.isNotBlank()) " · NIT: ${actor.taxId}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (actor.actorEmail.isNotBlank()) {
                                    Text(
                                        text = "Google vinculado: ${actor.actorEmail}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
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
                                            text = "Alquiler + ${actor.profitSharePercent}% utilidad",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        MoneyText(
                                            cents = actor.monthlyRentCents,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    FilledTonalButton(
                                        onClick = { onCreateSettlementForActor(actor) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Text("Liquidar mes")
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
