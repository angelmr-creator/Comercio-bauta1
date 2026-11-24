package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.LegalType
import com.example.data.model.LocaleStatus
import com.example.data.model.PaymentStatus
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalCommerceColors
import com.example.util.CurrencyFormatter
import com.example.util.YearMonthKey

@Composable
fun MonthSelectorBar(
    selectedMonth: YearMonthKey,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("previous_month_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.cd_previous_month)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "PERÍODO DE LIQUIDACIÓN",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = selectedMonth.label(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = onNextMonth,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("next_month_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.cd_next_month)
                )
            }
        }
    }
}

@Composable
fun MoneyText(
    cents: Long,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    includeSuffix: Boolean = true,
    modifier: Modifier = Modifier
) {
    Text(
        text = CurrencyFormatter.format(cents, suffix = if (includeSuffix) "CUP" else ""),
        style = style.copy(
            fontFamily = JetBrainsMonoFamily,
            fontFeatureSettings = "tnum"
        ),
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun StatusBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PaymentStatusBadge(status: PaymentStatus, modifier: Modifier = Modifier) {
    val commerceColors = LocalCommerceColors.current
    val (bg, fg) = when (status) {
        PaymentStatus.PAGADO -> commerceColors.collectedContainer to commerceColors.collected
        PaymentStatus.PARCIAL -> commerceColors.partialContainer to commerceColors.partial
        PaymentStatus.PENDIENTE -> commerceColors.pendingContainer to commerceColors.pending
    }
    StatusBadge(text = status.label, containerColor = bg, contentColor = fg, modifier = modifier)
}

@Composable
fun LocaleStatusBadge(status: LocaleStatus, modifier: Modifier = Modifier) {
    val commerceColors = LocalCommerceColors.current
    val (bg, fg) = when (status) {
        LocaleStatus.ARRENDADO -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        LocaleStatus.DISPONIBLE -> commerceColors.collectedContainer to commerceColors.collected
        LocaleStatus.MANTENIMIENTO -> commerceColors.partialContainer to commerceColors.partial
    }
    StatusBadge(text = status.label, containerColor = bg, contentColor = fg, modifier = modifier)
}

@Composable
fun LegalTypeBadge(type: LegalType, modifier: Modifier = Modifier) {
    StatusBadge(
        text = type.label,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier
    )
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    actionTestTag: String = "empty_action_button",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag(actionTestTag)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}
