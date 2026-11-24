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
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.LocaleSpace
import com.example.data.model.LocaleStatus
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LocaleStatusBadge
import com.example.ui.components.MoneyText
import com.example.ui.state.CommerceUiState

@Composable
fun LocalesScreen(
    uiState: CommerceUiState,
    onSelectStatusFilter: (LocaleStatus?) -> Unit,
    onAddLocale: () -> Unit,
    onEditLocale: (LocaleSpace) -> Unit
) {
    val tenantByLocaleId = uiState.actors
        .filter { it.localeId.isNotBlank() && it.active }
        .associateBy { it.localeId }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.localeStatusFilter == null,
                    onClick = { onSelectStatusFilter(null) },
                    label = { Text("Todos (${uiState.locales.size})") }
                )
                LocaleStatus.entries.forEach { status ->
                    val count = uiState.locales.count { it.statusEnum() == status }
                    FilterChip(
                        selected = uiState.localeStatusFilter == status,
                        onClick = {
                            onSelectStatusFilter(
                                if (uiState.localeStatusFilter == status) null else status
                            )
                        },
                        label = { Text("${status.label} ($count)") }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.filteredLocales.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Rounded.Storefront,
                            title = if (uiState.locales.isEmpty()) {
                                "Sin locales registrados"
                            } else {
                                "No hay locales en este estado"
                            },
                            subtitle = "Registra los establecimientos y espacios comerciales de Comercio Municipal Bauta con su alquiler mensual en CUP.",
                            actionLabel = if (uiState.locales.isEmpty()) "Registrar local" else null,
                            onAction = if (uiState.locales.isEmpty()) onAddLocale else null,
                            actionTestTag = "empty_add_locale_button"
                        )
                    }
                } else {
                    items(uiState.filteredLocales, key = { it.id }) { locale ->
                        val tenant = tenantByLocaleId[locale.id]
                        Card(
                            onClick = { onEditLocale(locale) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("locale_card_${locale.id}"),
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = locale.code,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Text(
                                            text = locale.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    LocaleStatusBadge(status = locale.statusEnum())
                                }

                                if (locale.address.isNotBlank() || locale.areaSqm > 0.0) {
                                    val details = buildList {
                                        if (locale.address.isNotBlank()) add(locale.address)
                                        if (locale.areaSqm > 0.0) add("${locale.areaSqm} m²")
                                    }.joinToString(" · ")
                                    Text(
                                        text = details,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (tenant != null) {
                                    Text(
                                        text = "Arrendado a: ${tenant.name} (${tenant.legalTypeEnum().label})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                HorizontalDivider()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Alquiler mensual base",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    MoneyText(
                                        cents = locale.monthlyRentCents,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
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
