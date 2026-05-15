package com.myimdad_por.ui.features.inventory.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Scale
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myimdad_por.domain.model.StockItem
import com.myimdad_por.domain.model.UnitOfMeasure
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.AppTypography
import com.myimdad_por.ui.theme.BackgroundColor
import com.myimdad_por.ui.theme.BorderColor
import com.myimdad_por.ui.theme.BrandPrimary
import com.myimdad_por.ui.theme.BrandPrimaryDark
import com.myimdad_por.ui.theme.BrandPrimaryTint
import com.myimdad_por.ui.theme.DividerColor
import com.myimdad_por.ui.theme.ErrorColor
import com.myimdad_por.ui.theme.ErrorContainer
import com.myimdad_por.ui.theme.IconColor
import com.myimdad_por.ui.theme.InfoColor
import com.myimdad_por.ui.theme.InfoContainer
import com.myimdad_por.ui.theme.TextPrimaryColor
import com.myimdad_por.ui.theme.TextSecondaryColor
import com.myimdad_por.ui.theme.TextTertiaryColor
import com.myimdad_por.ui.theme.WarningColor
import com.myimdad_por.ui.theme.WarningContainer
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// ─────────────────────────────────────────────────────────────────────────────
// Data model for quantity conversion between large ↔ small units
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Encodes how many "small" units fit inside one "large" unit.
 * This mapping must match your business domain; adjust as needed.
 *
 * Examples:
 *   CARTON  → BOTTLE   : 12  (كرتونة ← 12 زجاجة)
 *   CARTON  → PIECE    : 24  (كرتونة ← 24 قطعة)
 *   SHAWL   → KILOGRAM : 50  (شوال   ← 50 كجم)
 *   DOZEN   → PIECE    : 12  (دستة   ← 12 قطعة)
 *   GROSS   → PIECE    : 144 (جروس  ← 144 قطعة)
 *   PACK    → PIECE    : 6   (باكيت ← 6 قطعة)
 *   CRATE   → BOTTLE   : 24  (صندوق ← 24 زجاجة)
 *   TRAY    → PIECE    : 30  (صينية ← 30 قطعة)
 *   TIN     → GRAM     : 900 (صفيحة ← 900 جرام)
 *   BUNDLE  → PIECE    : 10  (ربطة  ← 10 قطعة)
 *   TON     → KILOGRAM : 1000
 *   QUINTAL → KILOGRAM : 100
 *   BARREL  → LITER    : 200
 *   JERRICAN→ LITER    : 20
 */
data class UnitConversion(
    val largeUnit: UnitOfMeasure,
    val smallUnit: UnitOfMeasure,
    val factor: Double          // how many smallUnits fit in one largeUnit
)

object UnitConversionTable {
    val conversions: List<UnitConversion> = listOf(
        UnitConversion(UnitOfMeasure.TON,      UnitOfMeasure.KILOGRAM,  1000.0),
        UnitConversion(UnitOfMeasure.QUINTAL,  UnitOfMeasure.KILOGRAM,  100.0),
        UnitConversion(UnitOfMeasure.SHAWL,    UnitOfMeasure.KILOGRAM,  50.0),
        UnitConversion(UnitOfMeasure.CARTON,   UnitOfMeasure.BOTTLE,    12.0),
        UnitConversion(UnitOfMeasure.CARTON,   UnitOfMeasure.PIECE,     24.0),
        UnitConversion(UnitOfMeasure.CARTON,   UnitOfMeasure.BAG,       12.0),
        UnitConversion(UnitOfMeasure.CARTON,   UnitOfMeasure.BOX,       6.0),
        UnitConversion(UnitOfMeasure.DOZEN,    UnitOfMeasure.PIECE,     12.0),
        UnitConversion(UnitOfMeasure.GROSS,    UnitOfMeasure.PIECE,     144.0),
        UnitConversion(UnitOfMeasure.PACK,     UnitOfMeasure.PIECE,     6.0),
        UnitConversion(UnitOfMeasure.BUNDLE,   UnitOfMeasure.PIECE,     10.0),
        UnitConversion(UnitOfMeasure.CRATE,    UnitOfMeasure.BOTTLE,    24.0),
        UnitConversion(UnitOfMeasure.TRAY,     UnitOfMeasure.PIECE,     30.0),
        UnitConversion(UnitOfMeasure.TIN,      UnitOfMeasure.GRAM,      900.0),
        UnitConversion(UnitOfMeasure.BARREL,   UnitOfMeasure.LITER,     200.0),
        UnitConversion(UnitOfMeasure.JERRICAN, UnitOfMeasure.LITER,     20.0),
    )

    /** Find a conversion where the item's unit is either the large OR the small side. */
    fun findFor(unit: UnitOfMeasure): UnitConversion? =
        conversions.firstOrNull { it.largeUnit == unit || it.smallUnit == unit }

    fun findLargeFor(smallUnit: UnitOfMeasure): UnitConversion? =
        conversions.firstOrNull { it.smallUnit == smallUnit }

    fun findSmallFor(largeUnit: UnitOfMeasure): UnitConversion? =
        conversions.firstOrNull { it.largeUnit == largeUnit }
}

// ─────────────────────────────────────────────────────────────────────────────
// Display mode enum  (toggled by the user via the flip button)
// ─────────────────────────────────────────────────────────────────────────────

private enum class QuantityDisplayMode {
    /** Show quantity in the unit stored in StockItem (native) */
    NATIVE,
    /** Convert to the *other* side of the conversion pair */
    CONVERTED
}

// ─────────────────────────────────────────────────────────────────────────────
// Public composable – the full inventory details sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryDetails(
    stockItem: StockItem,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current

    // Expiry state
    val today = LocalDate.now()
    val daysUntilExpiry = stockItem.expiryDate?.let { ChronoUnit.DAYS.between(today, it) }
    val isExpired = stockItem.isExpired(today)
    val expiresVerySoon = daysUntilExpiry != null && daysUntilExpiry in 0..7 && !isExpired

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {

        // ── Header gradient strip ──────────────────────────────────────────
        DetailsHeader(
            stockItem = stockItem,
            onEdit = onEdit,
            onDelete = onDelete,
            onClose = onClose
        )

        Spacer(Modifier.height(AppDimens.Spacing.normal))

        // ── Quantity showcase card (the star of the show) ──────────────────
        QuantityShowcase(
            stockItem = stockItem,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.Spacing.normal)
        )

        Spacer(Modifier.height(AppDimens.Spacing.normal))

        // ── Expiry warning banner ──────────────────────────────────────────
        if (isExpired || expiresVerySoon) {
            ExpiryBanner(
                isExpired = isExpired,
                daysUntilExpiry = daysUntilExpiry,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.Spacing.normal)
            )
            Spacer(Modifier.height(AppDimens.Spacing.normal))
        }

        // ── Info grid ─────────────────────────────────────────────────────
        InfoGrid(
            stockItem = stockItem,
            onCopyBarcode = {
                clipboardManager.setText(AnnotatedString(stockItem.normalizedBarcode))
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.Spacing.normal)
        )

        Spacer(Modifier.height(AppDimens.Spacing.huge))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DetailsHeader(
    stockItem: StockItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(
                Brush.verticalGradient(
                    listOf(BrandPrimaryDark, BrandPrimary)
                )
            )
    ) {
        // Decorative circles
        Box(
            Modifier
                .size(200.dp)
                .align(Alignment.TopStart)
                .offset(-60.dp, -60.dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape)
        )
        Box(
            Modifier
                .size(120.dp)
                .align(Alignment.BottomEnd)
                .offset(40.dp, 40.dp)
                .background(Color.White.copy(alpha = 0.07f), CircleShape)
        )

        // Close button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(AppDimens.Spacing.small)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "إغلاق", tint = Color.White, modifier = Modifier.size(20.dp))
        }

        // Name & barcode
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = AppDimens.Spacing.normal, bottom = AppDimens.Spacing.normal, end = 96.dp)
        ) {
            Text(
                text = stockItem.effectiveName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.QrCodeScanner, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(stockItem.normalizedBarcode, color = Color.White.copy(0.75f), fontSize = 12.sp)
            }
        }

        // Action buttons bottom-end
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = AppDimens.Spacing.normal, bottom = AppDimens.Spacing.normal),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HeaderActionButton(icon = Icons.Rounded.EditNote, description = "تعديل", onClick = onEdit)
            HeaderActionButton(icon = Icons.Rounded.DeleteOutline, description = "حذف", tint = ErrorColor.copy(0.9f), onClick = onDelete)
        }
    }
}

@Composable
private fun HeaderActionButton(
    icon: ImageVector,
    description: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.15f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(20.dp))
    }
}

// Needed for offset inside Box
fun Modifier.offset(x: androidx.compose.ui.unit.Dp, y: androidx.compose.ui.unit.Dp): Modifier =
    this.padding(start = if (x > 0.dp) x else 0.dp, top = if (y > 0.dp) y else 0.dp)

// ─────────────────────────────────────────────────────────────────────────────
// ⭐  Quantity Showcase  – the creative core
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuantityShowcase(
    stockItem: StockItem,
    modifier: Modifier = Modifier
) {
    val conversion = UnitConversionTable.findFor(stockItem.unitOfMeasure)
    var displayMode by remember { mutableStateOf(QuantityDisplayMode.NATIVE) }
    var modeIndex by remember { mutableIntStateOf(0) }       // cycles through 3 views

    val canConvert = conversion != null

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppDimens.Radius.large),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = AppDimens.Elevation.medium
    ) {
        Column(modifier = Modifier.padding(AppDimens.Spacing.normal)) {

            // ── Title row ───────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Rounded.Inventory2,
                    null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "الكمية المتاحة",
                    style = AppTypography.titleSmall,
                    color = TextPrimaryColor,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                if (canConvert) {
                    ToggleModeChip(
                        modeIndex = modeIndex,
                        onClick = { modeIndex = (modeIndex + 1) % 3 }
                    )
                }
            }

            Spacer(Modifier.height(AppDimens.Spacing.normal))

            // ── Main quantity display ─────────────────────────────────
            AnimatedContent(
                targetState = modeIndex,
                transitionSpec = {
                    (slideInVertically { h -> h } + fadeIn(tween(220))).togetherWith(
                        slideOutVertically { h -> -h } + fadeOut(tween(180))
                    )
                },
                label = "quantity_mode"
            ) { idx ->
                when (idx) {
                    0 -> NativeQuantityView(stockItem = stockItem)
                    1 -> if (conversion != null) ConvertedQuantityView(stockItem = stockItem, conversion = conversion, toLarge = stockItem.unitOfMeasure.category == UnitOfMeasure.Category.SMALL)
                         else NativeQuantityView(stockItem = stockItem)
                    else -> if (conversion != null) BreakdownQuantityView(stockItem = stockItem, conversion = conversion)
                            else NativeQuantityView(stockItem = stockItem)
                }
            }

            // ── Stock health bar ─────────────────────────────────────
            if (stockItem.quantity > 0) {
                Spacer(Modifier.height(AppDimens.Spacing.normal))
                StockHealthBar(quantity = stockItem.quantity)
            }
        }
    }
}

// ── Toggle chip (cycles: native → convert → breakdown) ───────────────────────

@Composable
private fun ToggleModeChip(modeIndex: Int, onClick: () -> Unit) {
    val labels = listOf("العدد الأصلي", "التحويل", "التفصيل")
    val icons  = listOf(Icons.Rounded.Scale, Icons.Rounded.Loop, Icons.Rounded.SwapVert)

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "تبديل طريقة العرض" },
        shape = RoundedCornerShape(999.dp),
        color = BrandPrimaryTint,
        contentColor = BrandPrimaryDark
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icons[modeIndex], null, modifier = Modifier.size(14.dp))
            Text(labels[modeIndex], fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ── Mode 0 : Native ──────────────────────────────────────────────────────────

@Composable
private fun NativeQuantityView(stockItem: StockItem) {
    val qty = formatQuantity(stockItem.quantity)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "لديك",
            style = AppTypography.bodyMedium,
            color = TextSecondaryColor
        )
        Spacer(Modifier.height(4.dp))
        AnimatedNumber(
            value = stockItem.quantity,
            suffix = " ${stockItem.unitOfMeasure.symbol}",
            productName = stockItem.effectiveName
        )
    }
}

// ── Mode 1 : Converted (flip large↔small) ────────────────────────────────────

@Composable
private fun ConvertedQuantityView(
    stockItem: StockItem,
    conversion: UnitConversion,
    toLarge: Boolean
) {
    // toLarge=true  → user stored small units; show in large
    // toLarge=false → user stored large units; show in small
    val convertedQty: Double
    val targetUnit: UnitOfMeasure
    val equation: String

    if (toLarge) {
        // small → large  e.g. 500 bottle ÷ 12 = 41.67 carton
        convertedQty = stockItem.quantity / conversion.factor
        targetUnit = conversion.largeUnit
        equation = "${formatQuantity(stockItem.quantity)} ${stockItem.unitOfMeasure.symbol} ÷ ${formatQuantity(conversion.factor)} = "
    } else {
        // large → small  e.g. 50 carton × 12 = 600 bottle
        convertedQty = stockItem.quantity * conversion.factor
        targetUnit = conversion.smallUnit
        equation = "${formatQuantity(stockItem.quantity)} ${stockItem.unitOfMeasure.symbol} × ${formatQuantity(conversion.factor)} = "
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("يعادل", style = AppTypography.bodyMedium, color = TextSecondaryColor)
        Spacer(Modifier.height(4.dp))
        AnimatedNumber(
            value = convertedQty,
            suffix = " ${targetUnit.symbol}",
            productName = stockItem.effectiveName
        )
        Spacer(Modifier.height(6.dp))
        // equation chips
        EquationRow(equation = equation, resultQty = convertedQty, resultUnit = targetUnit)
    }
}

// ── Mode 2 : Breakdown (whole large + remainder small) ───────────────────────

@Composable
private fun BreakdownQuantityView(
    stockItem: StockItem,
    conversion: UnitConversion
) {
    // Always express as:  whole large-units  +  remainder small-units
    val totalInSmall: Double
    val largeUnit: UnitOfMeasure
    val smallUnit: UnitOfMeasure
    val factor = conversion.factor

    when (stockItem.unitOfMeasure.category) {
        UnitOfMeasure.Category.LARGE -> {
            largeUnit = stockItem.unitOfMeasure
            smallUnit = conversion.smallUnit
            totalInSmall = stockItem.quantity * factor
        }
        UnitOfMeasure.Category.SMALL -> {
            largeUnit = conversion.largeUnit
            smallUnit = stockItem.unitOfMeasure
            totalInSmall = stockItem.quantity
        }
    }

    val wholeLarge = (totalInSmall / factor).toLong()
    val remainSmall = totalInSmall - wholeLarge * factor

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("لديك", style = AppTypography.bodyMedium, color = TextSecondaryColor)
        Spacer(Modifier.height(AppDimens.Spacing.small))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Large part
            BreakdownChip(
                quantity = wholeLarge.toDouble(),
                unit = largeUnit,
                productName = stockItem.effectiveName,
                color = BrandPrimary
            )

            if (remainSmall > 0.001) {
                Spacer(Modifier.width(8.dp))
                Text("و", color = TextSecondaryColor, fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                BreakdownChip(
                    quantity = remainSmall,
                    unit = smallUnit,
                    productName = stockItem.effectiveName,
                    color = InfoColor
                )
            }
        }

        Spacer(Modifier.height(AppDimens.Spacing.small))

        // Equivalency note
        val note = buildAnnotatedString {
            withStyle(SpanStyle(color = TextTertiaryColor, fontSize = 12.sp)) { append("( ${formatQuantity(totalInSmall)} ${smallUnit.symbol} ${stockItem.effectiveName} )") }
        }
        Text(note)
    }
}

@Composable
private fun BreakdownChip(
    quantity: Double,
    unit: UnitOfMeasure,
    productName: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = formatQuantity(quantity),
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(unit.displayName, fontSize = 13.sp, color = color.copy(0.8f), fontWeight = FontWeight.Medium)
        Text(productName, fontSize = 11.sp, color = TextTertiaryColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ── Animated big number ───────────────────────────────────────────────────────

@Composable
private fun AnimatedNumber(value: Double, suffix: String, productName: String) {
    val animatable = remember { Animatable(0f) }
    LaunchedEffect(value) {
        animatable.snapTo(0f)
        animatable.animateTo(1f, animationSpec = tween(600, easing = EaseOutCubic))
    }
    val displayed = (value * animatable.value)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimaryColor)) {
                    append(formatQuantity(displayed))
                }
                withStyle(SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium, color = BrandPrimary)) {
                    append(suffix)
                }
            }
        )
        Text(productName, style = AppTypography.bodyMedium, color = TextSecondaryColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ── Equation helper row ───────────────────────────────────────────────────────

@Composable
private fun EquationRow(equation: String, resultQty: Double, resultUnit: UnitOfMeasure) {
    Surface(
        color = BackgroundColor,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = TextSecondaryColor, fontSize = 12.sp)) { append(equation) }
                withStyle(SpanStyle(color = BrandPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)) {
                    append("${formatQuantity(resultQty)} ${resultUnit.symbol}")
                }
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(8.dp)
        )
    }
}

// ── Stock health bar ──────────────────────────────────────────────────────────

@Composable
private fun StockHealthBar(quantity: Double) {
    // Purely visual: green full, yellow half, red low (thresholds are illustrative)
    val fraction = (quantity / 100.0).coerceIn(0.0, 1.0).toFloat()
    val color = when {
        fraction > 0.5f -> BrandPrimary
        fraction > 0.2f -> WarningColor
        else            -> ErrorColor
    }
    val label = when {
        fraction > 0.5f -> "مخزون جيد"
        fraction > 0.2f -> "مخزون متوسط"
        else            -> "مخزون منخفض"
    }
    val animFraction by animateFloatAsState(targetValue = fraction, animationSpec = tween(800), label = "bar")

    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("مستوى المخزون", fontSize = 11.sp, color = TextTertiaryColor)
            Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(DividerColor)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animFraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Brush.horizontalGradient(listOf(color.copy(0.7f), color)))
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Expiry banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExpiryBanner(
    isExpired: Boolean,
    daysUntilExpiry: Long?,
    modifier: Modifier = Modifier
) {
    val bgColor    = if (isExpired) ErrorContainer   else WarningContainer
    val borderClr  = if (isExpired) ErrorColor       else WarningColor
    val textColor  = if (isExpired) ErrorColor       else WarningColor
    val icon       = if (isExpired) Icons.Rounded.WarningAmber else Icons.Rounded.CalendarMonth
    val message    = when {
        isExpired                 -> "هذا المنتج منتهي الصلاحية!"
        daysUntilExpiry == 0L    -> "ينتهي صلاحيته اليوم"
        daysUntilExpiry == 1L    -> "ينتهي غداً"
        daysUntilExpiry != null  -> "ينتهي خلال $daysUntilExpiry أيام"
        else                      -> ""
    }

    Surface(
        modifier = modifier.border(1.dp, borderClr.copy(0.4f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = textColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(message, color = textColor, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Info grid
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InfoGrid(
    stockItem: StockItem,
    onCopyBarcode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppDimens.Radius.large),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = AppDimens.Elevation.low
    ) {
        Column(modifier = Modifier.padding(AppDimens.Spacing.normal)) {
            Text("معلومات المنتج", style = AppTypography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimaryColor)
            Spacer(Modifier.height(AppDimens.Spacing.normal))

            // Barcode (with copy)
            InfoRow(
                icon = Icons.Rounded.QrCodeScanner,
                label = "الباركود",
                value = stockItem.normalizedBarcode,
                action = { CopyIcon(onClick = onCopyBarcode) }
            )
            RowDivider()

            // Location
            InfoRow(
                icon = Icons.Rounded.LocationOn,
                label = "الموقع",
                value = stockItem.normalizedLocation
            )
            RowDivider()

            // Unit
            InfoRow(
                icon = Icons.Rounded.Scale,
                label = "وحدة القياس",
                value = "${stockItem.unitOfMeasure.displayName} (${stockItem.unitOfMeasure.symbol})"
            )
            RowDivider()

            // Category
            InfoRow(
                icon = Icons.Rounded.Category,
                label = "تصنيف الوحدة",
                value = when (stockItem.unitOfMeasure.category) {
                    UnitOfMeasure.Category.LARGE -> "وحدة كبيرة"
                    UnitOfMeasure.Category.SMALL -> "وحدة صغيرة"
                }
            )

            // Expiry
            stockItem.expiryDate?.let { expiry ->
                RowDivider()
                InfoRow(
                    icon = Icons.Rounded.CalendarMonth,
                    label = "تاريخ الانتهاء",
                    value = expiry.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    valueColor = if (stockItem.isExpired()) ErrorColor else TextPrimaryColor
                )
            }

            // Description (if any)
            // (StockItem doesn't have description, but Product does – wire if needed)
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = TextPrimaryColor,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(34.dp)
                .background(BrandPrimaryTint, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = BrandPrimary, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = TextTertiaryColor)
            Text(value, fontSize = 14.sp, color = valueColor, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        action?.invoke()
    }
}

@Composable
private fun RowDivider() {
    Divider(
        color = DividerColor,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 46.dp)
    )
}

@Composable
private fun CopyIcon(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.ContentCopy, contentDescription = "نسخ", tint = IconColor, modifier = Modifier.size(16.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

/** Format a double removing unnecessary decimals */
private fun formatQuantity(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        BigDecimal(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    }
}
