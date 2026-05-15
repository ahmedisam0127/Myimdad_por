package com.myimdad_por.ui.features.inventory.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.myimdad_por.domain.model.UnitOfMeasure
import com.myimdad_por.ui.components.AppButton
import com.myimdad_por.ui.components.AppButtonVariant
import com.myimdad_por.ui.components.AppTextField
import com.myimdad_por.ui.features.inventory.contract.InventoryFormState
import com.myimdad_por.ui.features.inventory.contract.InventoryUiEvent
import com.myimdad_por.ui.features.inventory.contract.InventoryUiState
import com.myimdad_por.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ══════════════════════════════════════════════════════════════════════════════
//  Design Tokens — Emerald Crystal
// ══════════════════════════════════════════════════════════════════════════════

private val Crystal900  = Color(0xFF064E3B)
private val Crystal800  = Color(0xFF065F46)
private val Crystal700  = Color(0xFF047857)
private val Crystal600  = Color(0xFF059669)
private val Crystal500  = Color(0xFF10B981)
private val Crystal400  = Color(0xFF34D399)
private val Crystal300  = Color(0xFF6EE7B7)
private val Crystal200  = Color(0xFFA7F3D0)
private val Crystal100  = Color(0xFFD1FAE5)
private val Crystal50   = Color(0xFFECFDF5)

private val ErrorRed    = Color(0xFFEF4444)
private val ErrorRedBg  = Color(0xFFFEF2F2)
private val SuccessGreen= Color(0xFF22C55E)
private val TextDark    = Color(0xFF111827)
private val TextMid     = Color(0xFF6B7280)
private val TextLight   = Color(0xFF9CA3AF)
private val SurfaceWht  = Color(0xFFFFFFFF)

private val HeaderGradient = Brush.linearGradient(
    colorStops = arrayOf(
        0.0f  to Color(0xFF047857),
        0.45f to Color(0xFF059669),
        1.0f  to Color(0xFF10B981)
    ),
    start = Offset(0f, 0f),
    end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

private val GlassOverlay = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.0f))
)

private val ExpiryDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd / MM / yyyy")

// ══════════════════════════════════════════════════════════════════════════════
//  Root composable
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStockDialog(
    state: InventoryUiState,
    onEvent: (InventoryUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.showForm) return

    val form = state.formState
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        EmeraldDatePickerDialog(
            initialDate = form.expiryDate,
            onDateConfirmed = { date ->
                onEvent(InventoryUiEvent.ExpiryDateChanged(date))
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    Dialog(
        onDismissRequest = { if (!form.isSubmitting) onEvent(InventoryUiEvent.HideForm) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress    = !form.isSubmitting,
            dismissOnClickOutside = !form.isSubmitting
        )
    ) {
        // ── outer scrim gradient ──
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            // Drop shadow ring
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(y = 6.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Crystal600.copy(alpha = 0.18f))
                    .blur(12.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = SurfaceWht,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column {
                    CrystalDialogHeader(
                        title    = form.title,
                        subtitle = form.subtitle,
                        isEditing   = form.isEditing,
                        closeEnabled = !form.isSubmitting,
                        onClose  = { onEvent(InventoryUiEvent.HideForm) }
                    )
                    CrystalDialogBody(
                        form              = form,
                        onEvent           = onEvent,
                        onRequestDatePicker = { showDatePicker = true }
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Header — glass morphism emerald
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CrystalDialogHeader(
    title: String,
    subtitle: String,
    isEditing: Boolean,
    closeEnabled: Boolean,
    onClose: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "header_shimmer")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue = -500f, targetValue = 900f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "shimmer_x"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(HeaderGradient)
    ) {
        // Decorative geometry
        Canvas(modifier = Modifier.matchParentSize()) {
            // Large circle top-right
            drawCircle(
                color = Color.White.copy(alpha = 0.07f),
                radius = size.width * 0.55f,
                center = Offset(size.width * 1.05f, -size.height * 0.2f)
            )
            // Small circle bottom-left
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = size.width * 0.28f,
                center = Offset(-size.width * 0.05f, size.height * 1.1f)
            )
            // Shimmer sweep
            val sw = 160f
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.13f),
                        Color.Transparent
                    ),
                    start = Offset(shimmerX - sw, 0f),
                    end   = Offset(shimmerX + sw, size.height)
                )
            )
        }

        // Glass overlay line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.BottomCenter)
                .background(Crystal300.copy(alpha = 0.35f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isEditing) Icons.Outlined.EditNote else Icons.Outlined.AddBox,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Title block
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.3).sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            letterSpacing = 0.2.sp
                        ),
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Close button
            if (closeEnabled) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClose
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "إغلاق",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Body
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CrystalDialogBody(
    form: InventoryFormState,
    onEvent: (InventoryUiEvent) -> Unit,
    onRequestDatePicker: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 18.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // ── Section: بيانات المنتج ─────────────────────────────────────────
        CrystalSectionCard(
            title = "بيانات المنتج",
            icon  = Icons.Outlined.Inventory2,
            accentColor = Crystal600
        ) {
            // Barcode
            CrystalFieldRow(
                icon  = Icons.Outlined.QrCodeScanner,
                label = "الباركود"
            ) {
                AppTextField(
                    value = form.barcode,
                    onValueChange = { onEvent(InventoryUiEvent.BarcodeChanged(it)) },
                    label       = "الباركود *",
                    placeholder = "أدخل رمز المنتج",
                    enabled     = !form.isEditing && !form.isLoadingProduct,
                    errorText   = form.barcode
                        .takeIf { it.isNotBlank() && !form.isBarcodeValid }
                        ?.let { "الباركود غير صالح (4-64 حرف)" },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
            }

            CrystalFieldRow(
                icon  = Icons.Outlined.TextFields,
                label = "الاسم"
            ) {
                AppTextField(
                    value         = form.productName,
                    onValueChange = { onEvent(InventoryUiEvent.ProductNameChanged(it)) },
                    label         = "اسم المنتج *",
                    placeholder   = "أدخل اسم المنتج",
                    enabled       = !form.isLoadingProduct,
                    errorText     = form.productName
                        .takeIf { it.isNotBlank() && !form.isProductNameValid }
                        ?.let { "اسم المنتج مطلوب" }
                )
            }
        }

        // ── Section: بيانات المخزون ────────────────────────────────────────
        CrystalSectionCard(
            title       = "بيانات المخزون",
            icon        = Icons.Outlined.Scale,
            accentColor = Crystal700
        ) {
            CrystalFieldRow(
                icon  = Icons.Outlined.Numbers,
                label = "الكمية"
            ) {
                AppTextField(
                    value         = form.quantity,
                    onValueChange = { onEvent(InventoryUiEvent.QuantityChanged(it)) },
                    label         = "الكمية *",
                    placeholder   = "0",
                    errorText     = form.quantity
                        .takeIf { it.isNotBlank() && !form.isQuantityValid }
                        ?.let { "الكمية غير صالحة" },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            // Unit selector full-width (no icon row)
            CrystalUnitSelector(
                selectedUnit = form.effectiveUnitOfMeasure,
                onUnitSelected = { onEvent(InventoryUiEvent.UnitOfMeasureChanged(it)) }
            )

            CrystalFieldRow(
                icon  = Icons.Outlined.LocationOn,
                label = "الموقع"
            ) {
                AppTextField(
                    value         = form.location,
                    onValueChange = { onEvent(InventoryUiEvent.LocationChanged(it)) },
                    label         = "الموقع *",
                    placeholder   = "المستودع / الرف",
                    errorText     = form.location
                        .takeIf { it.isNotBlank() && !form.isLocationValid }
                        ?.let { "الموقع مطلوب" }
                )
            }
        }

        // ── Section: تفاصيل إضافية ─────────────────────────────────────────
        CrystalSectionCard(
            title       = "تفاصيل إضافية",
            icon        = Icons.Outlined.Description,
            accentColor = Crystal500,
            optional    = true
        ) {
            // Expiry date
            CrystalExpiryDateField(
                selectedDate = form.expiryDate,
                isValid      = form.isExpiryDateValid,
                onTap        = onRequestDatePicker,
                onClear      = { onEvent(InventoryUiEvent.ExpiryDateChanged(null)) }
            )

            CrystalFieldRow(
                icon  = Icons.Outlined.Description,
                label = "الوصف"
            ) {
                AppTextField(
                    value         = form.description,
                    onValueChange = { onEvent(InventoryUiEvent.DescriptionChanged(it)) },
                    label         = "الوصف",
                    placeholder   = "ملاحظات اختيارية...",
                    singleLine    = false,
                    maxLines      = 3
                )
            }
        }

        // ── Banners ────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = !form.errorMessage.isNullOrBlank(),
            enter = fadeIn(tween(200)) + expandVertically(tween(220)),
            exit  = fadeOut(tween(180)) + shrinkVertically(tween(180))
        ) {
            CrystalBanner(message = form.errorMessage.orEmpty(), isError = true)
        }

        AnimatedVisibility(
            visible = !form.successMessage.isNullOrBlank(),
            enter = fadeIn(tween(200)) + expandVertically(tween(220)),
            exit  = fadeOut(tween(180)) + shrinkVertically(tween(180))
        ) {
            CrystalBanner(message = form.successMessage.orEmpty(), isError = false)
        }

        AnimatedVisibility(
            visible = form.validationErrorMessage != null && !form.isSubmitting,
            enter = fadeIn(tween(200)) + expandVertically(tween(220)),
            exit  = fadeOut(tween(180)) + shrinkVertically(tween(180))
        ) {
            form.validationErrorMessage?.let {
                CrystalBanner(message = it, isError = true)
            }
        }

        // ── Actions ────────────────────────────────────────────────────────
        CrystalActionButtons(
            isEditing   = form.isEditing,
            isSubmitting = form.isSubmitting,
            canSubmit   = form.canSubmit,
            onEvent     = onEvent
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Section Card — glass container with colored accent
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CrystalSectionCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    optional: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Crystal50.copy(alpha = 0.8f), SurfaceWht)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(accentColor.copy(0.30f), Crystal100.copy(0.5f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        // Section title bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(accentColor.copy(0.08f), Color.Transparent)
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Colored left accent bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(accentColor, accentColor.copy(0.5f))
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector    = icon,
                    contentDescription = null,
                    tint           = accentColor,
                    modifier       = Modifier.size(15.dp)
                )
            }

            Text(
                text  = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color      = accentColor
                )
            )

            if (optional) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(accentColor.copy(0.10f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text  = "اختياري",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = accentColor.copy(0.80f),
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }

        // Thin separator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(accentColor.copy(0.20f), Color.Transparent)
                    )
                )
        )

        // Fields
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Field Row
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CrystalFieldRow(
    icon: ImageVector,
    label: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Icon column
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Crystal100),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Crystal700,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Unit of measure — pill chips with animated selection
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CrystalUnitSelector(
    selectedUnit: UnitOfMeasure,
    onUnitSelected: (UnitOfMeasure) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Crystal50)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Scale,
                contentDescription = null,
                tint = Crystal700,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text  = "وحدة القياس",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Crystal800,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        CrystalUnitGroup(
            label    = "وحدات كبيرة",
            units    = UnitOfMeasure.largeUnits,
            selected = selectedUnit,
            onSelect = onUnitSelected
        )
        CrystalUnitGroup(
            label    = "وحدات صغيرة",
            units    = UnitOfMeasure.smallUnits,
            selected = selectedUnit,
            onSelect = onUnitSelected
        )
    }
}

@Composable
private fun CrystalUnitGroup(
    label: String,
    units: List<UnitOfMeasure>,
    selected: UnitOfMeasure,
    onSelect: (UnitOfMeasure) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextMid,
                fontSize = 10.sp
            )
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            userScrollEnabled     = true
        ) {
            items(units) { unit ->
                CrystalUnitChip(
                    unit       = unit,
                    isSelected = unit == selected,
                    onClick    = { onSelect(unit) }
                )
            }
        }
    }
}

@Composable
private fun CrystalUnitChip(
    unit: UnitOfMeasure,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Crystal600 else SurfaceWht,
        animationSpec = tween(200),
        label = "chip_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Crystal800,
        animationSpec = tween(200),
        label = "chip_text"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Crystal700 else Crystal200,
        animationSpec = tween(200),
        label = "chip_border"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chip_scale"
    )

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
            Text(
                text  = unit.displayName,
                style = MaterialTheme.typography.labelSmall.copy(
                    color      = textColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize   = 11.sp
                )
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Expiry Date Field — premium tappable row
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CrystalExpiryDateField(
    selectedDate: LocalDate?,
    isValid: Boolean,
    onTap: () -> Unit,
    onClear: () -> Unit
) {
    val hasDate  = selectedDate != null
    val isInvalid = hasDate && !isValid

    val borderColor = when {
        isInvalid -> ErrorRed
        hasDate   -> Crystal600
        else      -> Crystal100
    }
    val bgColor = when {
        isInvalid -> ErrorRedBg
        hasDate   -> Crystal50
        else      -> SurfaceWht
    }
    val iconTint = when {
        isInvalid -> ErrorRed
        hasDate   -> Crystal700
        else      -> TextLight
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTap
                )
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (hasDate && !isInvalid) Crystal100
                        else if (isInvalid) ErrorRed.copy(0.10f)
                        else Crystal50
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = "تاريخ الانتهاء",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color    = TextLight,
                        fontSize = 10.sp
                    )
                )
                Text(
                    text  = selectedDate?.format(ExpiryDateFormatter) ?: "اضغط لاختيار التاريخ",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color      = if (hasDate) Crystal800 else TextLight,
                        fontWeight = if (hasDate) FontWeight.SemiBold else FontWeight.Normal
                    )
                )
            }

            if (hasDate) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Crystal200.copy(0.6f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClear
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "مسح التاريخ",
                        tint = Crystal700,
                        modifier = Modifier.size(13.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = null,
                    tint = TextLight,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        AnimatedVisibility(visible = isInvalid) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text  = "تاريخ الانتهاء غير صالح",
                    style = MaterialTheme.typography.labelSmall.copy(color = ErrorRed)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Date Picker — emerald themed
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmeraldDatePickerDialog(
    initialDate: LocalDate?,
    onDateConfirmed: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = initialDate
        ?.atStartOfDay(ZoneId.systemDefault())
        ?.toInstant()
        ?.toEpochMilli()

    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = SurfaceWht,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Crystal50)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "اختر تاريخ الانتهاء",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Crystal800,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "إغلاق",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = TextMid,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                DatePicker(
                    state = pickerState,
                    title = null,
                    headline = null,
                    showModeToggle = false,
                    colors = DatePickerDefaults.colors(
                        containerColor = SurfaceWht,
                        titleContentColor = Crystal800,
                        headlineContentColor = Crystal600,
                        weekdayContentColor = Crystal700,
                        subheadContentColor = Crystal700,
                        navigationContentColor = Crystal600,
                        yearContentColor = TextDark,
                        disabledYearContentColor = TextLight,
                        currentYearContentColor = Crystal600,
                        selectedYearContentColor = Color.White,
                        selectedYearContainerColor = Crystal600,
                        dayContentColor = TextDark,
                        disabledDayContentColor = TextLight,
                        selectedDayContentColor = Color.White,
                        disabledSelectedDayContentColor = Color.White,
                        selectedDayContainerColor = Crystal600,
                        disabledSelectedDayContainerColor = Crystal300,
                        todayContentColor = Crystal700,
                        todayDateBorderColor = Crystal500,
                        dayInSelectionRangeContentColor = Crystal700,
                        dayInSelectionRangeContainerColor = Crystal100
                    )
                )

                Divider(color = Crystal100)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "إلغاء",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = TextMid,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    val canConfirm = pickerState.selectedDateMillis != null

                    TextButton(
                        onClick = {
                            pickerState.selectedDateMillis?.let { millis ->
                                onDateConfirmed(
                                    Instant.ofEpochMilli(millis)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                )
                            }
                        },
                        enabled = canConfirm
                    ) {
                        Text(
                            text = "تأكيد",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = if (canConfirm) Crystal600 else TextLight,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Banner — success / error
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CrystalBanner(message: String, isError: Boolean) {
    val accentColor = if (isError) ErrorRed else SuccessGreen
    val bgColor     = if (isError) ErrorRedBg else Crystal50
    val icon        = if (isError) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircleOutline

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.30f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(accentColor.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text     = message,
            style    = MaterialTheme.typography.bodySmall.copy(
                color      = accentColor,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Action Buttons — gradient primary + outlined cancel
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CrystalActionButtons(
    isEditing: Boolean,
    isSubmitting: Boolean,
    canSubmit: Boolean,
    onEvent: (InventoryUiEvent) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Cancel
        Box(
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Crystal50)
                .border(1.5.dp, Crystal200, RoundedCornerShape(14.dp))
                .clickable(
                    enabled = !isSubmitting,
                    onClick = { onEvent(InventoryUiEvent.HideForm) }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = "إلغاء",
                style = MaterialTheme.typography.titleSmall.copy(
                    color      = Crystal700,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        // Submit
        Box(
            modifier = Modifier
                .weight(2f)
                .height(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (canSubmit && !isSubmitting)
                        Brush.linearGradient(
                            listOf(Crystal700, Crystal500),
                            start = Offset(0f, 0f),
                            end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    else
                        Brush.linearGradient(listOf(Crystal200, Crystal100))
                )
                .clickable(
                    enabled = canSubmit && !isSubmitting,
                    onClick = { onEvent(InventoryUiEvent.SubmitForm) }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color    = Color.White,
                    strokeWidth = 2.5.dp
                )
            } else {
                Row(
                    verticalAlignment    = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Rounded.Save else Icons.Rounded.Add,
                        contentDescription = null,
                        tint = if (canSubmit) Color.White else TextLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text  = if (isEditing) "حفظ التعديلات" else "إضافة الصنف",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (canSubmit) Color.White else TextLight
                        )
                    )
                }
            }
        }
    }
}
