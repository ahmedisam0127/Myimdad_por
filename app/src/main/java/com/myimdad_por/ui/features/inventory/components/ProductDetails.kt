package com.myimdad_por.ui.features.inventory.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myimdad_por.core.utils.CurrencyFormatter
import com.myimdad_por.core.utils.DateTimeUtils
import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.model.StockItem
import com.myimdad_por.domain.model.UnitOfMeasure
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.AppTypography
import com.myimdad_por.ui.theme.BorderColor
import com.myimdad_por.ui.theme.BrandPrimary
import com.myimdad_por.ui.theme.BrandPrimaryTint
import com.myimdad_por.ui.theme.DividerColor
import com.myimdad_por.ui.theme.ErrorColor
import com.myimdad_por.ui.theme.ErrorContainer
import com.myimdad_por.ui.theme.IconColor
import com.myimdad_por.ui.theme.TextPrimaryColor
import com.myimdad_por.ui.theme.TextSecondaryColor
import com.myimdad_por.ui.theme.TextTertiaryColor
import com.myimdad_por.ui.theme.WarningColor
import com.myimdad_por.ui.theme.WarningContainer
import java.time.LocalDate

// ─── Public API ──────────────────────────────────────────────────────────────

/**
 * Full product + stock details panel.
 *
 * Shows the resolved [product] data alongside live [stockItem] information such
 * as quantity, location, unit of measure and expiry status.  When [product] is
 * null only the stock-item section is shown; when [stockItem] is null only the
 * product section is shown.
 *
 * @param stockItem    The stock record to display, or null.
 * @param product      The resolved product catalogue entry, or null.
 * @param modifier     Optional layout modifier.
 * @param today        Reference date used for expiry calculations (defaults to [LocalDate.now]).
 * @param showDivider  Whether to draw a divider between the two sections.
 */
@Composable
fun ProductDetails(
    stockItem: StockItem?,
    product: Product?,
    modifier: Modifier = Modifier,
    today: LocalDate = remember { LocalDate.now() },
    showDivider: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {

        // ── Product catalogue section ────────────────────────────────────────
        AnimatedVisibility(
            visible = product != null,
            enter = fadeIn(tween(220)) + expandVertically(),
            exit = fadeOut(tween(180)) + shrinkVertically()
        ) {
            product?.let { p ->
                ProductCatalogueSection(product = p)
            }
        }

        // ── Divider between sections ─────────────────────────────────────────
        if (showDivider && product != null && stockItem != null) {
            Divider(
                modifier = Modifier.padding(vertical = AppDimens.Spacing.small),
                color = DividerColor,
                thickness = AppDimens.Component.dividerThickness
            )
        }

        // ── Stock item section ───────────────────────────────────────────────
        AnimatedVisibility(
            visible = stockItem != null,
            enter = fadeIn(tween(220)) + expandVertically(),
            exit = fadeOut(tween(180)) + shrinkVertically()
        ) {
            stockItem?.let { si ->
                StockItemSection(stockItem = si, today = today)
            }
        }
    }
}

/**
 * Compact read-only summary row, suitable for list items or card sub-headers.
 *
 * Shows barcode, quantity + unit, and location inline.
 */
@Composable
fun ProductDetailsSummary(
    stockItem: StockItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
    ) {
        BarcodeChip(barcode = stockItem.normalizedBarcode)

        Spacer(modifier = Modifier.weight(1f))

        // Quantity + unit
        Text(
            text = "${formatQuantity(stockItem.quantity)} ${stockItem.unitOfMeasure.symbol}",
            style = AppTypography.bodyMedium,
            color = TextPrimaryColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Location
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.extraSmall)
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(AppDimens.Icon.tiny),
                tint = TextTertiaryColor
            )
            Text(
                text = stockItem.normalizedLocation,
                style = AppTypography.bodySmall,
                color = TextTertiaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── Internal Sections ───────────────────────────────────────────────────────

@Composable
private fun ProductCatalogueSection(product: Product) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
    ) {
        // Section header
        SectionHeader(
            icon = Icons.Outlined.Inventory2,
            title = "معلومات المنتج"
        )

        // Name + display name
        DetailRow(
            icon = Icons.Outlined.Badge,
            label = "الاسم",
            value = product.effectiveName
        )

        if (!product.displayName.isNullOrBlank() &&
            product.displayName.trim() != product.name.trim()
        ) {
            DetailRow(
                icon = Icons.Outlined.Badge,
                label = "الاسم المعروض",
                value = product.displayName.trim()
            )
        }

        // Barcode
        DetailRow(
            icon = Icons.Outlined.Numbers,
            label = "الباركود",
            value = product.normalizedBarcode,
            valueStyle = DetailValueStyle.Monospace
        )

        // Price
        DetailRow(
            icon = Icons.Outlined.Scale,
            label = "السعر",
            value = CurrencyFormatter.formatSDG(product.price)
        )

        // Unit of measure
        DetailRow(
            icon = Icons.Outlined.Category,
            label = "وحدة القياس",
            value = product.unitOfMeasure.displayName
        )

        // Description (optional)
        if (!product.description.isNullOrBlank()) {
            DetailRow(
                icon = Icons.Outlined.Description,
                label = "الوصف",
                value = product.description.trim(),
                maxLines = 3
            )
        }

        // Active badge
        StatusBadge(
            active = product.isActive,
            activeLabel = "متاح للبيع",
            inactiveLabel = "غير متاح"
        )
    }
}

@Composable
private fun StockItemSection(stockItem: StockItem, today: LocalDate) {
    val isExpired = stockItem.isExpired(today)
    val expiresWithin7 = stockItem.expiresWithin(7, today)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
    ) {
        // Section header
        SectionHeader(
            icon = Icons.Outlined.Inventory2,
            title = "بيانات المخزون"
        )

        // Barcode
        DetailRow(
            icon = Icons.Outlined.Numbers,
            label = "الباركود",
            value = stockItem.normalizedBarcode,
            valueStyle = DetailValueStyle.Monospace
        )

        // Product name
        DetailRow(
            icon = Icons.Outlined.Badge,
            label = "اسم المنتج",
            value = stockItem.effectiveName
        )

        // Quantity
        DetailRow(
            icon = Icons.Outlined.Scale,
            label = "الكمية",
            value = "${formatQuantity(stockItem.quantity)} ${stockItem.unitOfMeasure.symbol}"
        )

        // Unit of measure
        DetailRow(
            icon = Icons.Outlined.Category,
            label = "وحدة القياس",
            value = buildUomLabel(stockItem.unitOfMeasure)
        )

        // Location
        DetailRow(
            icon = Icons.Outlined.LocationOn,
            label = "الموقع",
            value = stockItem.normalizedLocation
        )

        // Expiry date
        stockItem.expiryDate?.let { date ->
            val formattedDate = DateTimeUtils.formatDateForDisplay(date)
            val expiryColor = when {
                isExpired -> ErrorColor
                expiresWithin7 -> WarningColor
                else -> null
            }
            DetailRow(
                icon = Icons.Outlined.CalendarMonth,
                label = "تاريخ الانتهاء",
                value = formattedDate,
                valueColor = expiryColor
            )

            // Expiry warning banner
            if (isExpired || expiresWithin7) {
                ExpiryWarningBanner(isExpired = isExpired)
            }
        }

        // Out-of-stock badge
        if (stockItem.isOutOfStock) {
            StatusBadge(
                active = false,
                activeLabel = "متوفر",
                inactiveLabel = "نفد المخزون"
            )
        }
    }
}

// ─── Reusable Sub-Components ─────────────────────────────────────────────────

private enum class DetailValueStyle { Default, Monospace }

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(BrandPrimaryTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(AppDimens.Icon.small),
                tint = BrandPrimary
            )
        }
        Text(
            text = title,
            style = AppTypography.titleSmall,
            color = TextPrimaryColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color? = null,
    valueStyle: DetailValueStyle = DetailValueStyle.Default,
    maxLines: Int = 2
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
    ) {
        // Icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(AppDimens.Icon.small),
            tint = IconColor
        )

        // Label
        Text(
            text = "$label:",
            style = AppTypography.bodyMedium,
            color = TextSecondaryColor,
            modifier = Modifier.width(90.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Value
        val resolvedTextStyle = if (valueStyle == DetailValueStyle.Monospace) {
            AppTypography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
        } else {
            AppTypography.bodyMedium
        }

        Text(
            text = value,
            style = resolvedTextStyle,
            color = valueColor ?: TextPrimaryColor,
            fontWeight = FontWeight.Medium,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BarcodeChip(barcode: String) {
    Surface(
        shape = RoundedCornerShape(AppDimens.Radius.small),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = barcode,
            style = AppTypography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = TextSecondaryColor,
            modifier = Modifier.padding(
                horizontal = AppDimens.Spacing.small,
                vertical = AppDimens.Spacing.extraSmall
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusBadge(
    active: Boolean,
    activeLabel: String,
    inactiveLabel: String
) {
    val bgColor by animateColorAsState(
        targetValue = if (active) BrandPrimaryTint else ErrorContainer,
        animationSpec = tween(200),
        label = "statusBadgeBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (active) BrandPrimary else ErrorColor,
        animationSpec = tween(200),
        label = "statusBadgeText"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(AppDimens.Radius.small))
            .background(bgColor)
            .border(
                width = AppDimens.Component.borderThickness,
                color = textColor.copy(alpha = 0.25f),
                shape = RoundedCornerShape(AppDimens.Radius.small)
            )
            .padding(horizontal = AppDimens.Spacing.small, vertical = AppDimens.Spacing.extraSmall)
    ) {
        Text(
            text = if (active) activeLabel else inactiveLabel,
            style = AppTypography.bodySmall,
            color = textColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ExpiryWarningBanner(isExpired: Boolean) {
    val bgColor = if (isExpired) ErrorContainer else WarningContainer
    val iconTint = if (isExpired) ErrorColor else WarningColor
    val message = if (isExpired) "المنتج منتهي الصلاحية" else "ينتهي خلال 7 أيام أو أقل"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.Radius.small))
            .background(bgColor)
            .padding(
                horizontal = AppDimens.Spacing.small,
                vertical = AppDimens.Spacing.extraSmall
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.extraSmall)
    ) {
        Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            modifier = Modifier.size(AppDimens.Icon.small),
            tint = iconTint
        )
        Text(
            text = message,
            style = AppTypography.bodySmall,
            color = iconTint,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

/** Formats quantity without trailing zeros for whole numbers. */
private fun formatQuantity(value: Double): String {
    return if (value == kotlin.math.floor(value) && !value.isInfinite()) {
        value.toLong().toString()
    } else {
        value.toBigDecimal().stripTrailingZeros().toPlainString()
    }
}

/** Returns a human-readable UoM label with category context. */
private fun buildUomLabel(uom: UnitOfMeasure): String {
    return buildString {
        append(uom.displayName)
        append(" (")
        append(uom.symbol)
        append(")")
    }
}
