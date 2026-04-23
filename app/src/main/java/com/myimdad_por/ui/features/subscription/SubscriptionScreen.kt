package com.myimdad_por.ui.features.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myimdad_por.domain.model.SubscriptionPlan
import com.myimdad_por.domain.model.SubscriptionStatus
import com.myimdad_por.ui.components.AppButton
import com.myimdad_por.ui.components.AppButtonSize
import com.myimdad_por.ui.components.AppButtonVariant
import com.myimdad_por.ui.components.ErrorState
import com.myimdad_por.ui.components.ErrorStateStyle
import com.myimdad_por.ui.components.LoadingIndicator
import com.myimdad_por.ui.components.LoadingIndicatorSize
import com.myimdad_por.ui.components.LoadingIndicatorVariant
import com.myimdad_por.ui.theme.AppDimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SubscriptionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val isBusy = uiState.isLoading || uiState.isRefreshing || uiState.isSubmitting

    if (uiState.showPlanSelection) {
        PlanSelectionDialog(
            plans = uiState.availablePlans,
            selectedPlan = uiState.selectedPlan,
            onSelectPlan = { plan ->
                viewModel.onEvent(SubscriptionUiEvent.PlanSelected(plan))
            },
            onConfirm = {
                viewModel.onEvent(SubscriptionUiEvent.ConfirmSubscriptionAction)
            },
            onDismiss = {
                viewModel.onEvent(SubscriptionUiEvent.HidePlanSelection)
            }
        )
    }

    if (uiState.showRenewDialog) {
        ConfirmDialog(
            title = "تجديد الاشتراك",
            message = "هل تريد تجديد الاشتراك الآن؟",
            confirmText = "تجديد",
            onConfirm = {
                viewModel.onEvent(SubscriptionUiEvent.ConfirmRenewSubscription)
            },
            onDismiss = {
                viewModel.onEvent(SubscriptionUiEvent.HideRenewDialog)
            }
        )
    }

    if (uiState.showCancelDialog) {
        ConfirmDialog(
            title = "إلغاء الاشتراك",
            message = "سيتم إيقاف الاشتراك الحالي. هل تريد المتابعة؟",
            confirmText = "إلغاء",
            onConfirm = {
                viewModel.onEvent(SubscriptionUiEvent.ConfirmCancelSubscription)
            },
            onDismiss = {
                viewModel.onEvent(SubscriptionUiEvent.HideCancelDialog)
            },
            confirmVariant = AppButtonVariant.Danger
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SubscriptionBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(AppDimens.Layout.screenPadding),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.large)
            ) {
                SubscriptionHeader(
                    onBack = onBack,
                    title = "الاشتراك والباقات",
                    subtitle = "إدارة خطة الاشتراك والقدرات المتاحة لك"
                )

                if (!uiState.errorMessage.isNullOrBlank()) {
                    ErrorState(
                        message = uiState.errorMessage.orEmpty(),
                        title = "تعذر تحميل الاشتراك",
                        details = "تحقق من الاتصال أو أعد المحاولة.",
                        retryText = "إعادة المحاولة",
                        dismissText = "إغلاق",
                        onRetry = { viewModel.onEvent(SubscriptionUiEvent.Retry) },
                        onDismiss = { viewModel.onEvent(SubscriptionUiEvent.DismissError) },
                        style = ErrorStateStyle.Card
                    )
                }

                if (!uiState.successMessage.isNullOrBlank()) {
                    SuccessStateCard(
                        message = uiState.successMessage.orEmpty(),
                        onDismiss = { viewModel.onEvent(SubscriptionUiEvent.DismissSuccess) }
                    )
                }

                if (isBusy) {
                    LoadingIndicator(
                        variant = LoadingIndicatorVariant.Circular,
                        size = LoadingIndicatorSize.Medium,
                        title = if (uiState.isRefreshing) "جاري تحديث الاشتراك" else "جاري التحميل",
                        message = "يرجى الانتظار قليلاً"
                    )
                }

                if (!isBusy) {
                    SubscriptionOverviewCard(uiState = uiState)

                    SubscriptionMetricsCard(uiState = uiState)

                    CurrentPlanCard(
                        plan = uiState.currentPlan,
                        status = uiState.currentStatus,
                        expiryDateMillis = uiState.expiryDateMillis,
                        startDateMillis = uiState.startDateMillis,
                        canOperateOffline = uiState.canOperateOffline
                    )

                    AvailablePlansPreview(
                        plans = uiState.availablePlans,
                        selectedPlan = uiState.selectedPlan,
                        onPlanClick = { plan ->
                            viewModel.onEvent(SubscriptionUiEvent.PlanSelected(plan))
                            viewModel.onEvent(SubscriptionUiEvent.ShowPlanSelection)
                        }
                    )

                    ActionsCard(
                        canRenew = uiState.canRenew,
                        hasSubscription = uiState.hasSubscription,
                        hasSelectedPlan = uiState.hasSelectedPlan,
                        onChoosePlan = { viewModel.onEvent(SubscriptionUiEvent.ShowPlanSelection) },
                        onRenew = { viewModel.onEvent(SubscriptionUiEvent.ShowRenewDialog) },
                        onCancel = { viewModel.onEvent(SubscriptionUiEvent.ShowCancelDialog) },
                        onConfirm = { viewModel.onEvent(SubscriptionUiEvent.ConfirmSubscriptionAction) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SubscriptionBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .padding(top = 20.dp, end = 16.dp)
                .size(120.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .align(Alignment.TopEnd)
        )

        Box(
            modifier = Modifier
                .padding(top = 142.dp, start = 12.dp)
                .size(76.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f))
                .align(Alignment.TopStart)
        )

        Box(
            modifier = Modifier
                .padding(bottom = 100.dp, start = 24.dp)
                .size(94.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f))
                .align(Alignment.BottomStart)
        )
    }
}

@Composable
private fun SubscriptionHeader(
    onBack: () -> Unit,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(22.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.WorkspacePremium,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp)
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.92f)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusChip(text = "احترافي", icon = Icons.Filled.Star)
            StatusChip(text = "آمن", icon = Icons.Filled.Shield)
            StatusChip(text = "متابع", icon = Icons.Filled.Info)
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SubscriptionOverviewCard(uiState: SubscriptionUiState) {
    Surface(
        shape = RoundedCornerShape(AppDimens.Radius.large),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Layout.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
        ) {
            Text(
                text = "نظرة عامة",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = if (uiState.hasSubscription) {
                    "اشتراكك الحالي جاهز للإدارة والمتابعة من هنا."
                } else {
                    "لا يوجد اشتراك نشط حاليًا، ويمكنك اختيار خطة مناسبة."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallInfoBadge(
                    label = "الحالة",
                    value = subscriptionStatusLabel(uiState.currentStatus)
                )
                SmallInfoBadge(
                    label = "الخطة",
                    value = uiState.currentPlan?.name ?: "غير محددة"
                )
            }
        }
    }
}

@Composable
private fun SubscriptionMetricsCard(uiState: SubscriptionUiState) {
    Surface(
        shape = RoundedCornerShape(AppDimens.Radius.large),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Layout.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
        ) {
            Text(
                text = "القدرات الحالية",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )

            MetricRow(label = "الحد الأقصى للمستخدمين", value = uiState.maxUsers.toString())
            MetricRow(
                label = "الفواتير الشهرية",
                value = uiState.maxInvoicesPerMonth?.toString() ?: "غير محدود"
            )
            MetricRow(
                label = "الوضع غير المتصل",
                value = if (uiState.canOperateOffline) "متاح" else "غير متاح"
            )
            MetricRow(
                label = "الحماية من الإيقاف",
                value = if (uiState.isInGracePeriod) "فترة سماح" else "عادي"
            )
        }
    }
}

@Composable
private fun CurrentPlanCard(
    plan: SubscriptionPlan?,
    status: SubscriptionStatus?,
    expiryDateMillis: Long?,
    startDateMillis: Long?,
    canOperateOffline: Boolean
) {
    Surface(
        shape = RoundedCornerShape(AppDimens.Radius.large),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Layout.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "الخطة الحالية",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = plan?.name ?: "لا توجد خطة محددة",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))

            MetricRow(label = "الحالة", value = subscriptionStatusLabel(status))
            MetricRow(
                label = "تاريخ البداية",
                value = formatMillis(startDateMillis)
            )
            MetricRow(
                label = "تاريخ الانتهاء",
                value = formatMillis(expiryDateMillis)
            )
            MetricRow(
                label = "الوضع غير المتصل",
                value = if (canOperateOffline) "يمكن العمل بدون اتصال" else "يتطلب اتصالًا"
            )
        }
    }
}

@Composable
private fun AvailablePlansPreview(
    plans: List<SubscriptionPlan>,
    selectedPlan: SubscriptionPlan?,
    onPlanClick: (SubscriptionPlan) -> Unit
) {
    if (plans.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)) {
        Text(
            text = "الباقات المتاحة",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )

        plans.forEach { plan ->
            Surface(
                shape = RoundedCornerShape(AppDimens.Radius.large),
                color = if (selectedPlan == plan) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                },
                tonalElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppDimens.Radius.large))
                    .border(
                        width = 1.dp,
                        color = if (selectedPlan == plan) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
                        },
                        shape = RoundedCornerShape(AppDimens.Radius.large)
                    )
                    .clickable { onPlanClick(plan) }
            ) {
                Row(
                    modifier = Modifier.padding(AppDimens.Layout.screenPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = planDisplayName(plan),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "اضغط لاختيار هذه الخطة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (selectedPlan == plan) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionsCard(
    canRenew: Boolean,
    hasSubscription: Boolean,
    hasSelectedPlan: Boolean,
    onChoosePlan: () -> Unit,
    onRenew: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(AppDimens.Radius.large),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Layout.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            Text(
                text = "الإجراءات",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )

            AppButton(
                text = if (hasSubscription) "تغيير الخطة" else "اختيار خطة",
                onClick = onChoosePlan,
                fullWidth = true,
                variant = AppButtonVariant.Primary,
                size = AppButtonSize.Medium
            )

            AppButton(
                text = "تأكيد الخطة المختارة",
                onClick = onConfirm,
                fullWidth = true,
                enabled = hasSelectedPlan,
                variant = AppButtonVariant.Secondary,
                size = AppButtonSize.Medium
            )

            if (canRenew) {
                AppButton(
                    text = "تجديد الاشتراك",
                    onClick = onRenew,
                    fullWidth = true,
                    variant = AppButtonVariant.Outlined,
                    size = AppButtonSize.Medium,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Update,
                            contentDescription = null
                        )
                    }
                )
            }

            if (hasSubscription) {
                AppButton(
                    text = "إلغاء الاشتراك",
                    onClick = onCancel,
                    fullWidth = true,
                    variant = AppButtonVariant.Danger,
                    size = AppButtonSize.Medium
                )
            }
        }
    }
}

@Composable
private fun SmallInfoBadge(
    label: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SuccessStateCard(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(AppDimens.Radius.large),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(AppDimens.Layout.screenPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "تم التنفيذ بنجاح",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(onClick = onDismiss) {
                Text(text = "إغلاق")
            }
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmVariant: AppButtonVariant = AppButtonVariant.Primary
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(text = message)
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "إلغاء")
            }
        }
    )
}

@Composable
private fun PlanSelectionDialog(
    plans: List<SubscriptionPlan>,
    selectedPlan: SubscriptionPlan?,
    onSelectPlan: (SubscriptionPlan) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "اختر الخطة المناسبة",
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
            ) {
                Text(
                    text = "اضغط على الخطة التي تريد اعتمادها ثم أكّد الاختيار.",
                    style = MaterialTheme.typography.bodyMedium
                )

                plans.forEach { plan ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selectedPlan == plan) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPlan(plan) }
                    ) {
                        Text(
                            text = planDisplayName(plan),
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedPlan == plan) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "تأكيد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "إغلاق")
            }
        }
    )
}

private fun subscriptionStatusLabel(status: SubscriptionStatus?): String {
    return when (status?.name) {
        "ACTIVE" -> "نشطة"
        "EXPIRED" -> "منتهية"
        "GRACE_PERIOD" -> "فترة سماح"
        "CANCELED" -> "ملغاة"
        "BLOCKED" -> "موقوفة"
        null -> "غير محددة"
        else -> status.name
            .lowercase()
            .replace('_', ' ')
            .replaceFirstChar { ch -> ch.titlecase(Locale.getDefault()) }
    }
}

private fun planDisplayName(plan: SubscriptionPlan): String {
    return plan.name
        .lowercase()
        .replace('_', ' ')
        .replaceFirstChar { ch -> ch.titlecase(Locale.getDefault()) }
}

private fun formatMillis(millis: Long?): String {
    if (millis == null || millis <= 0L) return "غير محدد"
    return runCatching {
        SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(millis))
    }.getOrDefault("غير محدد")
}