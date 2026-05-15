package com.myimdad_por.ui.features.subscription

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.myimdad_por.domain.model.SubscriptionFeature
import com.myimdad_por.domain.model.SubscriptionInfo
import com.myimdad_por.domain.model.SubscriptionPlan
import com.myimdad_por.domain.model.SubscriptionStatus
import com.myimdad_por.ui.components.AppButton
import com.myimdad_por.ui.components.AppButtonSize
import com.myimdad_por.ui.components.AppButtonVariant
import com.myimdad_por.ui.components.AppTopBar
import com.myimdad_por.ui.components.ErrorState
import com.myimdad_por.ui.components.ErrorStateStyle
import com.myimdad_por.ui.components.LoadingIndicator
import com.myimdad_por.ui.components.SubscriptionExpiredDialog
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.AppTypography
import com.myimdad_por.ui.theme.BrandPrimary
import com.myimdad_por.ui.theme.ErrorColor
import com.myimdad_por.ui.theme.SuccessColor
import com.myimdad_por.ui.theme.WarningColor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ── Greens palette ────────────────────────────────────────────────────────────
private val GreenDeep      = Color(0xFF0D4A2A)   // header bg dark stop
private val GreenRich      = Color(0xFF1A7A45)   // header bg light stop / active accent
private val GreenMid       = Color(0xFF25A25A)   // progress ring / badges
private val GreenSoft      = Color(0xFF4DC882)   // icon tint / subtle chip
private val GreenPale      = Color(0xFFD6F5E5)   // chip bg / card tint
private val GreenOnDark    = Color(0xFFE8FFF3)   // text on dark header
private val GreenOnDarkSub = Color(0xFFAEDEC5)   // subtitle on dark header
private val NeutralBg      = Color(0xFFF5FAF7)   // screen background
private val CardBg         = Color(0xFFFFFFFF)
private val DividerColor   = Color(0xFFE0EDE6)
private val TextMain       = Color(0xFF0D2B1A)
private val TextSub        = Color(0xFF4B7260)

// ── Screen entry point ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun SubscriptionScreen (
    onBackClick: () -> Unit,
    onNavigateToRenew: () -> Unit = {},
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle messages via snackbar
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(SubscriptionUiEvent.DismissSuccess)
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(SubscriptionUiEvent.DismissError)
        }
    }

    // Pull-to-refresh
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = { viewModel.onEvent(SubscriptionUiEvent.RefreshSubscription) }
    )

    // Dialogs
    if (uiState.showRenewDialog) {
        ConfirmRenewDialog(
            onConfirm = { viewModel.onEvent(SubscriptionUiEvent.ConfirmRenewSubscription) },
            onDismiss = { viewModel.onEvent(SubscriptionUiEvent.HideRenewDialog) }
        )
    }
    if (uiState.showCancelDialog) {
        ConfirmCancelDialog(
            onConfirm = { viewModel.onEvent(SubscriptionUiEvent.ConfirmCancelSubscription) },
            onDismiss = { viewModel.onEvent(SubscriptionUiEvent.HideCancelDialog) }
        )
    }
    if (uiState.showPlanSelection && uiState.selectedPlan != null) {
        ConfirmPlanDialog(
            plan = uiState.selectedPlan!!,
            onConfirm = { viewModel.onEvent(SubscriptionUiEvent.ConfirmSubscriptionAction) },
            onDismiss = { viewModel.onEvent(SubscriptionUiEvent.HidePlanSelection) }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = "الاشتراك",
                subtitle = uiState.currentPlan?.planDisplayName(),
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { viewModel.onEvent(SubscriptionUiEvent.RefreshSubscription) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث",
                            tint = GreenRich
                        )
                    }
                },
                backgroundColor = CardBg,
                showDivider = true
            )
        },
        containerColor = NeutralBg
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .pullRefresh(pullRefreshState)
        ) {
            AnimatedContent(
                targetState = when {
                    uiState.isLoading -> "loading"
                    uiState.errorMessage != null && uiState.subscriptionInfo == null -> "error"
                    else -> "content"
                },
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                },
                label = "screen_state"
            ) { state ->
                when (state) {
                    "loading" -> LoadingIndicator(
                        title = "جاري تحميل الاشتراك",
                        message = "يرجى الانتظار لحظة…",
                        tint = GreenRich
                    )
                    "error" -> ErrorState(
                        title = "تعذر التحميل",
                        message = uiState.errorMessage ?: "حدث خطأ غير متوقع",
                        style = ErrorStateStyle.Card,
                        retryText = "إعادة المحاولة",
                        onRetry = { viewModel.onEvent(SubscriptionUiEvent.Retry) }
                    )
                    else -> SubscriptionContent(
                        uiState = uiState,
                        innerPadding = innerPadding,
                        onRenewClick = {
                            if (uiState.canRenew) viewModel.onEvent(SubscriptionUiEvent.ShowRenewDialog)
                            else onNavigateToRenew()
                        },
                        onCancelClick = { viewModel.onEvent(SubscriptionUiEvent.ShowCancelDialog) },
                        onPlanSelected = { plan ->
                            viewModel.onEvent(SubscriptionUiEvent.PlanSelected(plan))
                        }
                    )
                }
            }

            PullRefreshIndicator(
                refreshing = uiState.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = CardBg,
                contentColor = GreenRich
            )
        }
    }
}

// ── Main scrollable content ───────────────────────────────────────────────────
@Composable
private fun SubscriptionContent(
    uiState: SubscriptionUiState,
    innerPadding: PaddingValues,
    onRenewClick: () -> Unit,
    onCancelClick: () -> Unit,
    onPlanSelected: (SubscriptionPlan) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(
            bottom = innerPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── Hero header card
        item {
            SubscriptionHeroCard(
                subscriptionInfo = uiState.subscriptionInfo,
                currentStatus = uiState.currentStatus,
                onRenewClick = onRenewClick
            )
        }

        // ── Quick stats row
        uiState.subscriptionInfo?.let { info ->
            item {
                QuickStatsRow(info = info)
            }
        }

        // ── Expiry banner for near-expiry / expired
        uiState.subscriptionInfo?.let { info ->
            val daysLeft = ((info.expiryDateMillis - System.currentTimeMillis()) /
                    (24L * 60 * 60 * 1000)).toInt()
            if (daysLeft in 0..14 || info.isExpired || info.isInGracePeriod) {
                item {
                    ExpiryAlertBanner(
                        daysLeft = daysLeft,
                        isExpired = info.isExpired,
                        isGrace = info.isInGracePeriod,
                        onRenewClick = onRenewClick
                    )
                }
            }
        }

        // ── Plan upgrade picker
        if (uiState.availablePlans.isNotEmpty()) {
            item {
                SectionLabel(text = "الخطط المتاحة")
            }
            item {
                PlanPickerRow(
                    plans = uiState.availablePlans,
                    currentPlan = uiState.currentPlan,
                    selectedPlan = uiState.selectedPlan,
                    onPlanSelected = onPlanSelected
                )
            }
        }

        // ── Features grid
        uiState.subscriptionInfo?.let { info ->
            if (info.featuresEnabled.isNotEmpty()) {
                item { SectionLabel(text = "المزايا المفعّلة") }
                item {
                    FeaturesGrid(features = info.featuresEnabled)
                }
            }
        }

        // ── Subscription details
        uiState.subscriptionInfo?.let { info ->
            item { SectionLabel(text = "تفاصيل الاشتراك") }
            item {
                SubscriptionDetailsCard(info = info)
            }
        }

        // ── Danger zone: cancel
        if (uiState.isSubscriptionActive) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                DangerZoneCard(onCancelClick = onCancelClick)
            }
        }
    }
}

// ── Hero header ───────────────────────────────────────────────────────────────
@Composable
private fun SubscriptionHeroCard(
    subscriptionInfo: SubscriptionInfo?,
    currentStatus: SubscriptionStatus?,
    onRenewClick: () -> Unit
) {
    val progressFraction = remember(subscriptionInfo) {
        if (subscriptionInfo == null) return@remember 0f
        val total = (subscriptionInfo.expiryDateMillis - subscriptionInfo.startDateMillis)
            .coerceAtLeast(1L).toFloat()
        val elapsed = (System.currentTimeMillis() - subscriptionInfo.startDateMillis)
            .coerceIn(0L, total.toLong()).toFloat()
        1f - (elapsed / total)
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 1200, easing = EaseOutCubic),
        label = "progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(GreenDeep, GreenRich),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        // Decorative circle glow top-right
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.TopEnd)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            GreenMid.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Plan label + status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = subscriptionInfo?.plan?.planDisplayName() ?: "لا يوجد اشتراك",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = GreenOnDark
                    )
                    Text(
                        text = subscriptionInfo?.plan?.planSubtitle() ?: "ابدأ باختيار خطة مناسبة",
                        style = MaterialTheme.typography.bodySmall,
                        color = GreenOnDarkSub,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusChip(status = currentStatus)
            }

            // Progress ring + days counter
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ProgressRing(
                    fraction = animatedProgress,
                    label = daysLeftLabel(subscriptionInfo?.expiryDateMillis)
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProgressLegendItem(
                        color = GreenSoft,
                        label = "المدة المتبقية"
                    )
                    ProgressLegendItem(
                        color = GreenOnDarkSub,
                        label = "المدة المنقضية"
                    )
                    subscriptionInfo?.let { info ->
                        Text(
                            text = "ID: ${info.subscriptionId.take(12)}…",
                            style = MaterialTheme.typography.labelSmall,
                            color = GreenOnDarkSub.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Renew CTA (shown when not active or near expiry)
            val daysLeft = subscriptionInfo?.let {
                ((it.expiryDateMillis - System.currentTimeMillis()) /
                        (24L * 60 * 60 * 1000)).toInt()
            } ?: -1

            if (subscriptionInfo == null || daysLeft <= 14 ||
                subscriptionInfo.isExpired || subscriptionInfo.isInGracePeriod
            ) {
                AppButton(
                    text = if (subscriptionInfo?.isExpired == true || subscriptionInfo == null)
                        "تجديد الاشتراك الآن" else "تجديد مبكر",
                    onClick = onRenewClick,
                    variant = AppButtonVariant.Primary,
                    size = AppButtonSize.Large,
                    fullWidth = true
                )
            }
        }
    }
}

@Composable
private fun ProgressRing(
    fraction: Float,
    label: String,
    modifier: Modifier = Modifier
) {
    val ringSize = 80.dp
    Box(
        modifier = modifier
            .size(ringSize)
            .drawBehind {
                val strokeWidth = 8.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)
                // Track
                drawCircle(
                    color = GreenOnDarkSub.copy(alpha = 0.25f),
                    radius = radius,
                    style = Stroke(width = strokeWidth)
                )
                // Progress arc
                drawArc(
                    color = GreenSoft,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(fraction * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = GreenOnDark
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = GreenOnDarkSub,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProgressLegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = GreenOnDarkSub
        )
    }
}

@Composable
private fun StatusChip(status: SubscriptionStatus?) {
    val (bg, fg, label) = when (status) {
        SubscriptionStatus.ACTIVE     -> Triple(GreenPale, GreenDeep, "نشط ✓")
        SubscriptionStatus.EXPIRED    -> Triple(Color(0xFFFFE5E5), Color(0xFFB00020), "منتهي")
        SubscriptionStatus.GRACE_PERIOD -> Triple(Color(0xFFFFF3CD), Color(0xFF7A4F00), "سماح")
        SubscriptionStatus.SUSPENDED  -> Triple(Color(0xFFFFE5E5), Color(0xFFB00020), "موقوف")
        SubscriptionStatus.CANCELED   -> Triple(Color(0xFFF0F0F0), Color(0xFF555555), "ملغي")
        SubscriptionStatus.PENDING    -> Triple(Color(0xFFE3F0FF), Color(0xFF1A4A8A), "قيد الانتظار")
        null -> Triple(Color(0xFFF0F0F0), Color(0xFF555555), "غير معروف")
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = fg
        )
    }
}

// ── Quick stats ───────────────────────────────────────────────────────────────
@Composable
private fun QuickStatsRow(info: SubscriptionInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatCell(
            icon = Icons.Outlined.Group,
            value = "${info.maxUsers}",
            label = "مستخدمون",
            tint = GreenRich
        )
        StatDivider()
        StatCell(
            icon = Icons.Outlined.Receipt,
            value = info.maxInvoicesPerMonth?.toString() ?: "∞",
            label = "فاتورة/شهر",
            tint = GreenRich
        )
        StatDivider()
        StatCell(
            icon = if (info.canOperateOffline) Icons.Outlined.WifiOff else Icons.Outlined.Wifi,
            value = if (info.canOperateOffline) "مسموح" else "متصل",
            label = "وضع أوفلاين",
            tint = if (info.canOperateOffline) GreenRich else TextSub
        )
        StatDivider()
        StatCell(
            icon = Icons.Outlined.AccessTime,
            value = "${info.offlineGracePeriodDays}",
            label = "يوم سماح",
            tint = GreenRich
        )
    }
    Divider(color = DividerColor)
}

@Composable
private fun StatCell(
    icon: ImageVector,
    value: String,
    label: String,
    tint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextMain
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSub
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .height(36.dp)
            .width(1.dp)
            .background(DividerColor)
    )
}

// ── Expiry banner ─────────────────────────────────────────────────────────────
@Composable
private fun ExpiryAlertBanner(
    daysLeft: Int,
    isExpired: Boolean,
    isGrace: Boolean,
    onRenewClick: () -> Unit
) {
    val (bg, accent, icon, message) = when {
        isExpired && !isGrace -> Quad(
            Color(0xFFFFE9E9), Color(0xFFB00020),
            "⛔", "انتهت صلاحية اشتراكك. بعض الميزات معطّلة الآن."
        )
        isGrace -> Quad(
            Color(0xFFFFF7E0), Color(0xFF8A6000),
            "⚠️", "أنت في فترة السماح. جدّد الاشتراك قبل انقضائها."
        )
        daysLeft <= 3 -> Quad(
            Color(0xFFFFEEDD), Color(0xFF8A3800),
            "🔔", "متبقٍ $daysLeft يوم فقط. جدّد الآن لتجنّب الانقطاع."
        )
        else -> Quad(
            Color(0xFFF0FBF5), GreenRich,
            "📅", "متبقٍ $daysLeft يومًا على انتهاء الاشتراك."
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = icon, fontSize = 20.sp)
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = accent,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onRenewClick) {
            Text(
                text = "تجديد",
                color = accent,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
    Divider(color = DividerColor)
}

// ── Plan picker ───────────────────────────────────────────────────────────────
@Composable
private fun PlanPickerRow(
    plans: List<SubscriptionPlan>,
    currentPlan: SubscriptionPlan?,
    selectedPlan: SubscriptionPlan?,
    onPlanSelected: (SubscriptionPlan) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(plans) { plan ->
            PlanCard(
                plan = plan,
                isCurrent = plan == currentPlan,
                isSelected = plan == selectedPlan,
                onSelect = { onPlanSelected(plan) }
            )
        }
    }
}

@Composable
private fun PlanCard(
    plan: SubscriptionPlan,
    isCurrent: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = when {
        isCurrent  -> GreenRich
        isSelected -> GreenMid
        else       -> DividerColor
    }
    val bgColor = when {
        isCurrent  -> GreenPale
        isSelected -> Color(0xFFEAFFF3)
        else       -> CardBg
    }
    Surface(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isCurrent || isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onSelect),
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = plan.planEmoji(),
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = plan.planShortName(),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (isCurrent) GreenDeep else TextMain,
                textAlign = TextAlign.Center
            )
            if (isCurrent) {
                Surface(
                    color = GreenRich,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "الحالية",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            } else if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = GreenMid,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ── Features grid ─────────────────────────────────────────────────────────────
@Composable
private fun FeaturesGrid(features: Set<SubscriptionFeature>) {
    val sorted = remember(features) { features.sortedBy { it.featureDisplayName() } }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sorted.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pair.forEach { feature ->
                    FeatureChip(
                        text = feature.featureDisplayName(),
                        modifier = Modifier.weight(1f)
                    )
                }
                // fill empty slot
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun FeatureChip(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(GreenPale)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = GreenRich,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = GreenDeep,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── Subscription details card ─────────────────────────────────────────────────
@Composable
private fun SubscriptionDetailsCard(info: SubscriptionInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailRow(
                icon = Icons.Outlined.CalendarToday,
                label = "تاريخ البدء",
                value = formatMillis(info.startDateMillis)
            )
            Divider(color = DividerColor)
            DetailRow(
                icon = Icons.Outlined.AccessTime,
                label = "تاريخ الانتهاء",
                value = formatMillis(info.expiryDateMillis)
            )
            if (info.renewedAtMillis != null) {
                Divider(color = DividerColor)
                DetailRow(
                    icon = Icons.Default.Refresh,
                    label = "آخر تجديد",
                    value = formatMillis(info.renewedAtMillis)
                )
            }
            if (info.lastSyncedAtMillis != null) {
                Divider(color = DividerColor)
                DetailRow(
                    icon = Icons.Outlined.Wifi,
                    label = "آخر مزامنة",
                    value = formatMillis(info.lastSyncedAtMillis)
                )
            }
            if (!info.notes.isNullOrBlank()) {
                Divider(color = DividerColor)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "ملاحظات",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSub
                    )
                    Text(
                        text = info.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMain,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = GreenRich, modifier = Modifier.size(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSub,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = TextMain
        )
    }
}

// ── Danger zone ───────────────────────────────────────────────────────────────
@Composable
private fun DangerZoneCard(onCancelClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCancelClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFFFE0E0), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color(0xFFB00020),
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "إلغاء الاشتراك",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFB00020)
                )
                Text(
                    text = "سيتم إيقاف جميع الميزات عند الإلغاء",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8A2020)
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFB00020)
            )
        }
    }
}

// ── Section label ─────────────────────────────────────────────────────────────
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = TextSub,
        modifier = Modifier
            .fillMaxWidth()
            .background(NeutralBg)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

// ── Dialogs ───────────────────────────────────────────────────────────────────
@Composable
private fun ConfirmRenewDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    SubscriptionActionDialog(
        title = "تأكيد تجديد الاشتراك",
        message = "هل تريد المتابعة لتجديد اشتراكك وإعادة تفعيل جميع المزايا؟",
        confirmText = "تجديد الآن",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        accentColor = GreenRich
    )
}

@Composable
private fun ConfirmCancelDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    SubscriptionActionDialog(
        title = "إلغاء الاشتراك",
        message = "هل أنت متأكد أنك تريد إلغاء الاشتراك؟ ستفقد الوصول إلى الميزات عند انتهاء الفترة الحالية.",
        confirmText = "نعم، إلغاء",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        accentColor = Color(0xFFB00020)
    )
}

@Composable
private fun ConfirmPlanDialog(
    plan: SubscriptionPlan,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    SubscriptionActionDialog(
        title = "تغيير الخطة",
        message = "هل تريد التحويل إلى خطة ${plan.planDisplayName()}؟ سيتم تطبيق التغيير فوراً.",
        confirmText = "تأكيد التغيير",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        accentColor = GreenRich
    )
}

@Composable
private fun SubscriptionActionDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
        containerColor = CardBg,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextMain
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSub
            )
        },
        confirmButton = {
            AppButton(
                text = confirmText,
                onClick = onConfirm,
                variant = AppButtonVariant.Primary,
                size = AppButtonSize.Medium,
                fullWidth = false
            )
        },
        dismissButton = {
            AppButton(
                text = "إلغاء",
                onClick = onDismiss,
                variant = AppButtonVariant.Text,
                size = AppButtonSize.Medium,
                fullWidth = false
            )
        }
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────
private fun formatMillis(millis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}

private fun daysLeftLabel(expiryMillis: Long?): String {
    if (expiryMillis == null) return "—"
    val days = ((expiryMillis - System.currentTimeMillis()) / (24L * 60 * 60 * 1000)).toInt()
    return when {
        days < 0  -> "منتهي"
        days == 0 -> "اليوم"
        days == 1 -> "يوم"
        else      -> "$days يوم"
    }
}

private fun SubscriptionPlan.planDisplayName() = when (this) {
    SubscriptionPlan.STARTER    -> "خطة البداية"
    SubscriptionPlan.PRO        -> "الخطة الاحترافية"
    SubscriptionPlan.BUSINESS   -> "خطة الأعمال"
    SubscriptionPlan.ENTERPRISE -> "الخطة المؤسسية"
    SubscriptionPlan.CUSTOM     -> "خطة مخصصة"
}

private fun SubscriptionPlan.planSubtitle() = when (this) {
    SubscriptionPlan.STARTER    -> "انطلق مع الأساسيات"
    SubscriptionPlan.PRO        -> "قوة ومرونة للأعمال المتنامية"
    SubscriptionPlan.BUSINESS   -> "إدارة متعددة الفروع"
    SubscriptionPlan.ENTERPRISE -> "أعلى مستويات التحكم"
    SubscriptionPlan.CUSTOM     -> "حسب احتياجك تماماً"
}

private fun SubscriptionPlan.planShortName() = when (this) {
    SubscriptionPlan.STARTER    -> "Starter"
    SubscriptionPlan.PRO        -> "Pro"
    SubscriptionPlan.BUSINESS   -> "Business"
    SubscriptionPlan.ENTERPRISE -> "Enterprise"
    SubscriptionPlan.CUSTOM     -> "Custom"
}

private fun SubscriptionPlan.planEmoji() = when (this) {
    SubscriptionPlan.STARTER    -> "🌱"
    SubscriptionPlan.PRO        -> "⚡"
    SubscriptionPlan.BUSINESS   -> "🏢"
    SubscriptionPlan.ENTERPRISE -> "🏆"
    SubscriptionPlan.CUSTOM     -> "🎯"
}

private fun SubscriptionFeature.featureDisplayName() = when (this) {
    SubscriptionFeature.ADVANCED_REPORTS  -> "تقارير متقدمة"
    SubscriptionFeature.MULTI_BRANCH      -> "فروع متعددة"
    SubscriptionFeature.API_ACCESS        -> "وصول API"
    SubscriptionFeature.PAYMENT_GATEWAY   -> "بوابة دفع"
    SubscriptionFeature.TAX_INVOICE       -> "فاتورة ضريبية"
    SubscriptionFeature.LEGAL_INVOICE     -> "فاتورة قانونية"
    SubscriptionFeature.OFFLINE_MODE      -> "وضع أوفلاين"
    SubscriptionFeature.AUDIT_LOGS        -> "سجل التدقيق"
    SubscriptionFeature.EXPORT_EXCEL      -> "تصدير Excel"
    SubscriptionFeature.EXPORT_PDF        -> "تصدير PDF"
    SubscriptionFeature.ROLE_BASED_ACCESS -> "صلاحيات الأدوار"
    SubscriptionFeature.BACKUP_SYNC       -> "النسخ والمزامنة"
}

// Tiny utility to destructure 4-element tuples
private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
private operator fun <A, B, C, D> Quad<A, B, C, D>.component1() = a
private operator fun <A, B, C, D> Quad<A, B, C, D>.component2() = b
private operator fun <A, B, C, D> Quad<A, B, C, D>.component3() = c
private operator fun <A, B, C, D> Quad<A, B, C, D>.component4() = d
