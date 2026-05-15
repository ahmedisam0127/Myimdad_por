package com.myimdad_por.ui.features.inventory
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.myimdad_por.ui.features.inventory.components.InventoryDetails
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myimdad_por.core.base.UiState
import com.myimdad_por.domain.model.StockItem
import com.myimdad_por.domain.model.UnitOfMeasure
import com.myimdad_por.domain.repository.SortDirection
import com.myimdad_por.ui.components.LoadingIndicator
import com.myimdad_por.ui.features.inventory.components.AddStockDialog
import com.myimdad_por.ui.features.inventory.contract.InventoryUiEffect
import com.myimdad_por.ui.features.inventory.contract.InventoryUiEvent
import com.myimdad_por.ui.features.inventory.contract.InventoryUiState
import com.myimdad_por.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import kotlin.math.*

// ════════════════════════════════════════════════════════════
//  Design Tokens — Emerald Vault
// ════════════════════════════════════════════════════════════

private val Emerald900 = Color(0xFF064E3B)
private val Emerald800 = Color(0xFF065F46)
private val Emerald700 = Color(0xFF047857)
private val Emerald600 = Color(0xFF059669)
private val Emerald500 = Color(0xFF10B981)
private val Emerald400 = Color(0xFF34D399)
private val Emerald300 = Color(0xFF6EE7B7)
private val Emerald200 = Color(0xFFA7F3D0)
private val Emerald100 = Color(0xFFD1FAE5)
private val Emerald50  = Color(0xFFECFDF5)

private val GlassWhite     = Color(0xFFFFFFFF)
private val GlassSurface   = Color(0xFFF0FDF4)
private val TextOnDark     = Color(0xFFECFDF5)
private val TextMuted      = Color(0xFF6B7280)
private val TextSubtle     = Color(0xFF9CA3AF)
private val CardSurface    = Color(0xFFFFFFFF)
private val WarningAmber   = Color(0xFFF59E0B)
private val DangerRed      = Color(0xFFEF4444)
private val PulseGreen     = Color(0xFF22C55E)

private val VaultGradient = Brush.verticalGradient(
    colorStops = arrayOf(
        0.0f to Emerald900,
        0.35f to Emerald800,
        0.65f to Emerald700,
        1.0f to Emerald600
    )
)

private val CardGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFF0FDF4), Color(0xFFFFFFFF)),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

private val ShimmerGradient = Brush.linearGradient(
    colors = listOf(
        Emerald500.copy(alpha = 0f),
        Emerald400.copy(alpha = 0.6f),
        Emerald500.copy(alpha = 0f)
    )
)

// ════════════════════════════════════════════════════════════
//  Root Screen
// ════════════════════════════════════════════════════════════

@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToProductDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.screenState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    var selectedItemForDetails by remember { mutableStateOf<StockItem?>(null) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is InventoryUiEffect.ShowMessage ->
                    snackbarHostState.showSnackbar(effect.message)
                is InventoryUiEffect.ShowError ->
                    snackbarHostState.showSnackbar(effect.message)
                is InventoryUiEffect.NavigateToProductDetails ->
                    onNavigateToProductDetails(effect.barcode)
                else -> Unit
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // ── background canvas ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color(0xFFF8FFFE))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Emerald200.copy(alpha = 0.25f), Color.Transparent),
                    radius = size.width * 0.7f,
                    center = Offset(size.width * 0.9f, -size.height * 0.05f)
                ),
                radius = size.width * 0.7f,
                center = Offset(size.width * 0.9f, -size.height * 0.05f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Emerald300.copy(alpha = 0.12f), Color.Transparent),
                    radius = size.width * 0.5f,
                    center = Offset(-size.width * 0.1f, size.height * 0.75f)
                ),
                radius = size.width * 0.5f,
                center = Offset(-size.width * 0.1f, size.height * 0.75f)
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    EmeraldSnackbar(data)
                }
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = !state.isMultiSelectMode,
                    enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    EmeraldFAB(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onEvent(InventoryUiEvent.ShowForm)
                        }
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // ── Vault Header ──
                VaultHeader(
                    state = state,
                    onBackClick = onNavigateBack,
                    onEvent = viewModel::onEvent
                )

                // ── Stats Strip ──
                AnimatedVisibility(
                    visible = state.stockItems.isNotEmpty(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    StatsStrip(state = state)
                }

                // ── Search & Filter ──
                SearchFilterBar(
                    state = state,
                    onEvent = viewModel::onEvent
                )

                // ── Multi-select Action Bar ──
                AnimatedVisibility(
                    visible = state.isMultiSelectMode,
                    enter = expandVertically(spring()) + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    MultiSelectActionBar(
                        count = state.selectedItemsCount,
                        onDelete = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onEvent(InventoryUiEvent.DeleteSelected)
                        },
                        onCancel = { viewModel.onEvent(InventoryUiEvent.StopMultiSelect) }
                    )
                }

                // ── Content ──
                Box(modifier = Modifier.weight(1f)) {
                    InventoryContent(
                        state = state,
                        onEvent = viewModel::onEvent,
                        onItemClick = { item ->
                            if (state.isMultiSelectMode) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.onEvent(InventoryUiEvent.ToggleSelection(item.productBarcode))
                            } else {
                                selectedItemForDetails = item
                            }
                        },
                        onItemLongClick = { item ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onEvent(InventoryUiEvent.ToggleSelection(item.productBarcode))
                        }
                    )
                }
            }
        }

        // ── Add/Edit Dialog ──
        AddStockDialog(
            state = state,
            onEvent = viewModel::onEvent
        )

        // ── Item Details Bottom Sheet ──
        selectedItemForDetails?.let { item ->
            ModalBottomSheet(
                onDismissRequest = { selectedItemForDetails = null },
                sheetState = bottomSheetState,
                containerColor = Color.White
            ) {
                InventoryDetails(
                    stockItem = item,
                    onEdit = {
                        selectedItemForDetails = null
                        viewModel.onEvent(InventoryUiEvent.StockItemSelected(item))
                    },
                    onDelete = {
                        selectedItemForDetails = null
                        viewModel.onEvent(InventoryUiEvent.DeleteStockItem(item.productBarcode))
                    },
                    onClose = { selectedItemForDetails = null }
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  Vault Header
// ════════════════════════════════════════════════════════════

@Composable
private fun VaultHeader(
    state: InventoryUiState,
    onBackClick: () -> Unit,
    onEvent: (InventoryUiEvent) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "header_pulse")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing)),
        label = "shimmer"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(VaultGradient)
            .statusBarsPadding()
    ) {
        // ── decorative circles ──
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = size.width * 0.55f,
                center = Offset(size.width * 1.1f, size.height * 0.3f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.04f),
                radius = size.width * 0.35f,
                center = Offset(-size.width * 0.1f, size.height * 0.8f)
            )
            // shimmer line
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.15f),
                        Color.Transparent
                    )
                ),
                start = Offset(shimmerX - 150f, 0f),
                end = Offset(shimmerX + 150f, size.height),
                strokeWidth = 60f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // ── top row ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable(onClick = onBackClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "رجوع",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "المخزون",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "إدارة وتتبع المخزون",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }

                // Refresh button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { onEvent(InventoryUiEvent.Refresh) },
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "تحديث",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  Stats Strip
// ════════════════════════════════════════════════════════════

@Composable
private fun StatsStrip(state: InventoryUiState) {
    val today = remember { LocalDate.now() }

    val expiredCount = remember(state.stockItems) {
        state.stockItems.count { it.isExpired(today) }
    }
    val lowStockCount = remember(state.stockItems) {
        state.stockItems.count { it.quantity < 5 && !it.isOutOfStock }
    }
    val outOfStockCount = remember(state.stockItems) {
        state.stockItems.count { it.isOutOfStock }
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Emerald900)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(end = 4.dp)
    ) {
        item {
            MiniStatChip(
                label = "إجمالي الأصناف",
                value = "${state.totalItemsCount}",
                icon = Icons.Outlined.Inventory2,
                tint = Emerald300
            )
        }
        item {
            MiniStatChip(
                label = "إجمالي الكمية",
                value = state.totalQuantity.toPlainString(),
                icon = Icons.Outlined.Scale,
                tint = Emerald300
            )
        }
        if (outOfStockCount > 0) {
            item {
                MiniStatChip(
                    label = "نفد المخزون",
                    value = "$outOfStockCount",
                    icon = Icons.Outlined.RemoveShoppingCart,
                    tint = DangerRed
                )
            }
        }
        if (lowStockCount > 0) {
            item {
                MiniStatChip(
                    label = "مخزون منخفض",
                    value = "$lowStockCount",
                    icon = Icons.Outlined.WarningAmber,
                    tint = WarningAmber
                )
            }
        }
        if (expiredCount > 0) {
            item {
                MiniStatChip(
                    label = "منتهي الصلاحية",
                    value = "$expiredCount",
                    icon = Icons.Outlined.EventBusy,
                    tint = DangerRed
                )
            }
        }
    }
}

@Composable
private fun MiniStatChip(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.65f)
        )
    }
}

// ════════════════════════════════════════════════════════════
//  Search & Filter Bar
// ════════════════════════════════════════════════════════════

@Composable
private fun SearchFilterBar(
    state: InventoryUiState,
    onEvent: (InventoryUiEvent) -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Emerald50)
                    .border(
                        width = 1.5.dp,
                        color = if (state.hasSearchQuery) Emerald500 else Emerald100,
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = if (state.hasSearchQuery) Emerald600 else TextSubtle,
                        modifier = Modifier.size(18.dp)
                    )
                    BasicTextField(
                        value = state.searchQuery,
                        onValueChange = { onEvent(InventoryUiEvent.SearchQueryChanged(it)) },
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = Emerald900
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        decorationBox = { inner ->
                            if (state.searchQuery.isEmpty()) {
                                Text(
                                    text = "ابحث عن منتج أو باركود...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSubtle
                                )
                            }
                            inner()
                        }
                    )
                    if (state.hasSearchQuery) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "مسح البحث",
                            tint = TextSubtle,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onEvent(InventoryUiEvent.SearchQueryChanged("")) }
                        )
                    }
                }
            }

            // Sort Toggle
            SortButton(
                direction = state.sortDirection,
                onClick = {
                    val next = if (state.sortDirection == SortDirection.ASCENDING)
                        SortDirection.DESCENDING else SortDirection.ASCENDING
                    onEvent(InventoryUiEvent.SortDirectionChanged(next))
                }
            )
        }

        // Unit Filter Chips
        UnitFilterRow(
            selectedUnit = state.selectedUnitFilter,
            onUnitSelected = { onEvent(InventoryUiEvent.UnitFilterChanged(it)) }
        )
    }

    // Divider
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Emerald400, Emerald600, Emerald400)
                )
            )
    )
}

@Composable
private fun SortButton(
    direction: SortDirection,
    onClick: () -> Unit
) {
    val isAsc = direction == SortDirection.ASCENDING
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isAsc) Emerald100 else Emerald600)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isAsc) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
            contentDescription = "ترتيب",
            tint = if (isAsc) Emerald700 else Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun UnitFilterRow(
    selectedUnit: UnitOfMeasure?,
    onUnitSelected: (UnitOfMeasure?) -> Unit
) {
    val popularUnits = remember {
        listOf(null) + UnitOfMeasure.entries.take(6)
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(end = 4.dp)
    ) {
        items(popularUnits) { unit ->
            val isSelected = unit == selectedUnit
            val label = unit?.displayName ?: "الكل"

            FilterChip(
                selected = isSelected,
                onClick = { onUnitSelected(unit) },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Emerald600,
                    selectedLabelColor = Color.White,
                    containerColor = Emerald50,
                    labelColor = Emerald800
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    selectedBorderColor = Color.Transparent,
                    borderColor = Emerald200,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 0.dp
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(30.dp)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  Multi-Select Action Bar
// ════════════════════════════════════════════════════════════

@Composable
private fun MultiSelectActionBar(
    count: Int,
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Emerald900)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Emerald600),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                color = Color.White
            )
        }

        Text(
            text = if (count == 1) "صنف محدد" else "أصناف محددة",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )

        TextButton(
            onClick = onDelete,
            colors = ButtonDefaults.textButtonColors(contentColor = DangerRed)
        ) {
            Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("حذف", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
        }

        TextButton(
            onClick = onCancel,
            colors = ButtonDefaults.textButtonColors(contentColor = Emerald300)
        ) {
            Text("إلغاء", style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ════════════════════════════════════════════════════════════
//  Content Area
// ════════════════════════════════════════════════════════════

@Composable
private fun InventoryContent(
    state: InventoryUiState,
    onEvent: (InventoryUiEvent) -> Unit,
    onItemClick: (StockItem) -> Unit,
    onItemLongClick: (StockItem) -> Unit
) {
    when {
        state.stockItemsState is UiState.Loading && state.stockItems.isEmpty() -> {
            InventoryLoadingState()
        }

        state.stockItemsState is UiState.Error && state.stockItems.isEmpty() -> {
            InventoryErrorState(
                message = (state.stockItemsState as UiState.Error).message,
                onRetry = { onEvent(InventoryUiEvent.Retry) }
            )
        }

        state.canShowEmptyState -> {
            InventoryEmptyState(onAdd = { onEvent(InventoryUiEvent.ShowForm) })
        }

        state.canShowNoSearchResultsState -> {
            NoSearchResults(query = state.searchQuery)
        }

        else -> {
            InventoryList(
                items = state.filteredStockItems,
                selectedBarcodes = state.selectedBarcodes,
                isMultiSelect = state.isMultiSelectMode,
                onItemClick = onItemClick,
                onItemLongClick = onItemLongClick,
                onDeleteItem = { barcode -> onEvent(InventoryUiEvent.DeleteStockItem(barcode)) },
                onEditItem = { stockItem ->
                    onEvent(InventoryUiEvent.StockItemSelected(stockItem))
                }
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  Inventory List
// ════════════════════════════════════════════════════════════

@Composable
private fun InventoryList(
    items: List<StockItem>,
    selectedBarcodes: Set<String>,
    isMultiSelect: Boolean,
    onItemClick: (StockItem) -> Unit,
    onItemLongClick: (StockItem) -> Unit,
    onDeleteItem: (String) -> Unit,
    onEditItem: (StockItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = 14.dp, bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Results count header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Emerald500)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${items.size} صنف",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Emerald900
                )
            }
        }

        itemsIndexed(
            items = items,
            key = { _, item -> item.productBarcode }
        ) { index, item ->
            val isSelected = item.productBarcode in selectedBarcodes
            val animDelay = (index * 40).coerceAtMost(400)

            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(animDelay.toLong())
                visible = true
            }

            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                ) + fadeIn(tween(animDelay + 200))
            ) {
                StockItemCard(
                    item = item,
                    isSelected = isSelected,
                    isMultiSelectMode = isMultiSelect,
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) },
                    onEdit = { onEditItem(item) },
                    onDelete = { onDeleteItem(item.productBarcode) }
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  Stock Item Card
// ════════════════════════════════════════════════════════════

@Composable
private fun StockItemCard(
    item: StockItem,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val today = remember { LocalDate.now() }
    val isExpired = remember(item) { item.isExpired(today) }
    val expiresWarn = remember(item) { item.expiresWithin(7, today) }

    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> Emerald500
            isExpired -> DangerRed.copy(alpha = 0.5f)
            expiresWarn -> WarningAmber.copy(alpha = 0.5f)
            item.isOutOfStock -> DangerRed.copy(alpha = 0.3f)
            else -> Color.Transparent
        },
        animationSpec = tween(250),
        label = "card_border"
    )

    val cardElevation by animateDpAsState(
        targetValue = if (isSelected) 6.dp else 2.dp,
        animationSpec = spring(),
        label = "card_elevation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .border(
                width = if (isSelected || isExpired || expiresWarn) 1.5.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isSelected) Brush.linearGradient(
                        listOf(Emerald50, Color.White)
                    ) else Brush.linearGradient(
                        listOf(Color.White, Color.White)
                    )
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Selection / Avatar ──
            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isMultiSelectMode) {
                    SelectionCircle(isSelected = isSelected)
                } else {
                    ProductAvatar(
                        name = item.effectiveName,
                        isExpired = isExpired,
                        isOutOfStock = item.isOutOfStock
                    )
                }
            }

            // ── Info ──
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = item.effectiveName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Emerald900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = TextSubtle
                    )
                    Text(
                        text = item.normalizedBarcode,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = TextSubtle,
                        maxLines = 1
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Location pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = Emerald600
                        )
                        Text(
                            text = item.normalizedLocation,
                            style = MaterialTheme.typography.labelSmall,
                            color = Emerald700,
                            maxLines = 1
                        )
                    }

                    // Expiry warning
                    if (isExpired || expiresWarn) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isExpired) DangerRed.copy(0.1f) else WarningAmber.copy(0.1f)
                                )
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = if (isExpired) DangerRed else WarningAmber
                            )
                            Text(
                                text = if (isExpired) "منتهي" else "قارب الانتهاء",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = if (isExpired) DangerRed else WarningAmber,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ── Right Side ──
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Quantity badge
                QuantityBadge(
                    quantity = item.quantity,
                    unit = item.unitOfMeasure,
                    isOutOfStock = item.isOutOfStock
                )

                // Action buttons
                if (!isMultiSelectMode) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        MiniActionButton(
                            icon = Icons.Rounded.EditNote,
                            tint = Emerald600,
                            bgColor = Emerald50,
                            onClick = onEdit
                        )
                        MiniActionButton(
                            icon = Icons.Rounded.DeleteOutline,
                            tint = DangerRed,
                            bgColor = DangerRed.copy(alpha = 0.08f),
                            onClick = onDelete
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionCircle(isSelected: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "selection_scale"
    )
    Box(
        modifier = Modifier
            .size(36.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(if (isSelected) Emerald500 else Emerald100)
            .border(2.dp, if (isSelected) Emerald600 else Emerald300, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ProductAvatar(
    name: String,
    isExpired: Boolean,
    isOutOfStock: Boolean
) {
    val initial = name.take(1).uppercase()
    val gradientColors = when {
        isExpired -> listOf(DangerRed.copy(0.7f), DangerRed)
        isOutOfStock -> listOf(Color.Gray.copy(0.5f), Color.Gray.copy(0.7f))
        else -> listOf(Emerald500, Emerald700)
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black
            ),
            color = Color.White
        )
    }
}

@Composable
private fun QuantityBadge(
    quantity: Double,
    unit: UnitOfMeasure,
    isOutOfStock: Boolean
) {
    val displayQty = if (quantity == kotlin.math.floor(quantity) && !quantity.isInfinite())
        quantity.toLong().toString() else "%.1f".format(quantity)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isOutOfStock -> DangerRed.copy(0.12f)
                    quantity < 5 -> WarningAmber.copy(0.12f)
                    else -> Emerald100
                }
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = displayQty,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                ),
                color = when {
                    isOutOfStock -> DangerRed
                    quantity < 5 -> WarningAmber
                    else -> Emerald700
                }
            )
            Text(
                text = unit.symbol,
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    isOutOfStock -> DangerRed.copy(0.7f)
                    quantity < 5 -> WarningAmber.copy(0.7f)
                    else -> Emerald600
                }
            )
        }
    }
}

@Composable
private fun MiniActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    bgColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}

// ════════════════════════════════════════════════════════════
//  Empty / Error / Loading States
// ════════════════════════════════════════════════════════════

@Composable
private fun InventoryLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(6) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "shimmer_$index")
            val shimmerAlpha by infiniteTransition.animateFloat(
                initialValue = 0.4f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(900, delayMillis = index * 120, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse
                ),
                label = "alpha_$index"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Emerald100.copy(alpha = shimmerAlpha))
            )
        }
    }
}

@Composable
private fun InventoryErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated error icon
        val infiniteTransition = rememberInfiniteTransition(label = "error_pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.95f, targetValue = 1.05f,
            animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
            label = "pulse_scale"
        )

        Box(
            modifier = Modifier
                .size(90.dp)
                .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(DangerRed.copy(0.15f), DangerRed.copy(0.05f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = DangerRed,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "حدث خطأ",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = Emerald900
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "إعادة المحاولة",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun InventoryEmptyState(onAdd: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_float")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "float_y"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer { translationY = floatY }
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.linearGradient(listOf(Emerald100, Emerald200))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Inventory2,
                contentDescription = null,
                tint = Emerald600,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "المخزون فارغ",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
            color = Emerald900
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "أضف أول صنف إلى مخزونك الآن\nوابدأ رحلة إدارة احترافية",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(52.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "إضافة صنف",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun NoSearchResults(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(Emerald50),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.SearchOff,
                contentDescription = null,
                tint = Emerald400,
                modifier = Modifier.size(46.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "لا توجد نتائج",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = Emerald900
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "لم نجد أي صنف يطابق\n\"$query\"",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

// ════════════════════════════════════════════════════════════
//  FAB
// ════════════════════════════════════════════════════════════

@Composable
private fun EmeraldFAB(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "fab_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "glow"
    )

    Box(contentAlignment = Alignment.Center) {
        // Glow ring
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Emerald400.copy(alpha = glowAlpha * 0.35f))
        )
        // FAB
        FloatingActionButton(
            onClick = onClick,
            containerColor = Emerald600,
            contentColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.size(58.dp),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 12.dp
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "إضافة صنف",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
//  Custom Snackbar
// ════════════════════════════════════════════════════════════

@Composable
private fun EmeraldSnackbar(data: SnackbarData) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Emerald900)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(PulseGreen)
            )
            Text(
                text = data.visuals.message,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            data.visuals.actionLabel?.let { label ->
                TextButton(onClick = { data.performAction() }) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Emerald400
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
//  BasicTextField alias (no import conflict)
// ════════════════════════════════════════════════════════════

@Composable
private fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = androidx.compose.ui.text.TextStyle.Default,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() }
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = textStyle,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        decorationBox = decorationBox
    )
}
