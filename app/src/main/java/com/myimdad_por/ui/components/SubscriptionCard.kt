package com.myimdad_por.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myimdad_por.core.utils.DateTimeUtils
import com.myimdad_por.domain.model.SubscriptionFeature
import com.myimdad_por.domain.model.SubscriptionInfo
import com.myimdad_por.domain.model.SubscriptionPlan
import com.myimdad_por.domain.model.SubscriptionStatus
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.AppShapeTokens
import com.myimdad_por.ui.theme.AppTextStyles
import com.myimdad_por.ui.theme.BrandPrimary
import com.myimdad_por.ui.theme.BrandPrimaryDark
import com.myimdad_por.ui.theme.BrandPrimarySoft
import com.myimdad_por.ui.theme.ErrorColor
import com.myimdad_por.ui.theme.ErrorContainer
import com.myimdad_por.ui.theme.IconOnBrandColor
import com.myimdad_por.ui.theme.InfoColor
import com.myimdad_por.ui.theme.InfoContainer
import com.myimdad_por.ui.theme.SuccessColor
import com.myimdad_por.ui.theme.SuccessContainer
import com.myimdad_por.ui.theme.SurfaceColor
import com.myimdad_por.ui.theme.TextPrimaryColor
import com.myimdad_por.ui.theme.TextSecondaryColor
import com.myimdad_por.ui.theme.WarningColor
import com.myimdad_por.ui.theme.WarningContainer
import java.time.Instant

@Composable
fun SubscriptionCard(
    subscription: SubscriptionInfo,
    modifier: Modifier = Modifier,
    onCardClick: (() -> Unit)? = null,
    primaryActionText: String = if (subscription.isActive) "إدارة الاشتراك" else "تجديد الاشتراك",
    secondaryActionText: String? = "عرض التفاصيل",
    onPrimaryActionClick: () -> Unit = {},
    onSecondaryActionClick: (() -> Unit)? = null,
    showFeatures: Boolean = true,
    showDates: Boolean = true,
    showNotes: Boolean = true,
    showMetadataCount: Boolean = true
) {
    val planUi = subscription.plan.toPlanUiModel()
    val statusUi = subscription.toStatusUiModel()
    val remainingText = formatRemainingTime(subscription.expiryDateMillis)
    val startDateText = formatDateTimeMillis(subscription.startDateMillis)
    val expiryDateText = formatDateTimeMillis(subscription.expiryDateMillis)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onCardClick != null) {
                    Modifier.clickable(onClick = onCardClick)
                } else {
                    Modifier
                }
            ),
        shape = AppShapeTokens.card,
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.Elevation.high)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            SubscriptionHeader(
                title = planUi.title,
                subtitle = planUi.subtitle,
                badgeText = planUi.badge,
                statusUi = statusUi
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.Layout.screenPadding),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
            ) {
                MetricRow(title = "الحالة", value = statusUi.description)
                MetricRow(title = "المستخدمون", value = "${subscription.maxUsers} مستخدم")
                MetricRow(
                    title = "الفواتير الشهرية",
                    value = subscription.maxInvoicesPerMonth?.let { "$it فاتورة" } ?: "غير محدود"
                )
                MetricRow(
                    title = "التشغيل دون اتصال",
                    value = if (subscription.canOperateOffline) "مسموح" else "غير مسموح"
                )

                if (showMetadataCount) {
                    MetricRow(
                        title = "البيانات الإضافية",
                        value = "${subscription.metadata.size} عنصر"
                    )
                }

                if (showDates) {
                    Divider(
                        modifier = Modifier.padding(vertical = AppDimens.Spacing.extraSmall),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    MetricRow(title = "تاريخ البدء", value = startDateText)
                    MetricRow(title = "تاريخ الانتهاء", value = expiryDateText)
                    MetricRow(title = "المتبقي", value = remainingText)
                }

                if (showFeatures && subscription.featuresEnabled.isNotEmpty()) {
                    Divider(
                        modifier = Modifier.padding(top = AppDimens.Spacing.extraSmall),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Text(
                        text = "المزايا المفعلة",
                        style = AppTextStyles.ArabicBody,
                        color = TextSecondaryColor
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
                    ) {
                        subscription.featuresEnabled
                            .sortedBy { it.displayName() }
                            .forEach { feature ->
                                FeatureRow(text = feature.displayName())
                            }
                    }
                }

                if (showNotes && !subscription.notes.isNullOrBlank()) {
                    Divider(
                        modifier = Modifier.padding(top = AppDimens.Spacing.extraSmall),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Text(
                        text = "ملاحظات",
                        style = AppTextStyles.ArabicBody,
                        color = TextSecondaryColor
                    )

                    Text(
                        text = subscription.notes.orEmpty(),
                        style = AppTextStyles.ArabicBody,
                        color = TextPrimaryColor,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (subscription.renewedAtMillis != null || subscription.lastSyncedAtMillis != null) {
                    Divider(
                        modifier = Modifier.padding(top = AppDimens.Spacing.extraSmall),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    subscription.renewedAtMillis?.let {
                        MetricRow(
                            title = "آخر تجديد",
                            value = formatDateTimeMillis(it)
                        )
                    }

                    subscription.lastSyncedAtMillis?.let {
                        MetricRow(
                            title = "آخر مزامنة",
                            value = formatDateTimeMillis(it)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppButton(
                        text = primaryActionText,
                        onClick = onPrimaryActionClick,
                        modifier = Modifier.weight(1f),
                        variant = if (subscription.isActive) AppButtonVariant.Primary else AppButtonVariant.Info
                    )

                    if (secondaryActionText != null && onSecondaryActionClick != null) {
                        AppButton(
                            text = secondaryActionText,
                            onClick = onSecondaryActionClick,
                            modifier = Modifier.weight(1f),
                            variant = AppButtonVariant.Outlined
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionHeader(
    title: String,
    subtitle: String,
    badgeText: String,
    statusUi: StatusUiModel
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BrandPrimary, BrandPrimaryDark)
                )
            )
            .padding(AppDimens.Layout.screenPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
                ) {
                    Text(
                        text = title,
                        style = AppTextStyles.ArabicTitle,
                        color = IconOnBrandColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = AppTextStyles.ArabicBody,
                        color = IconOnBrandColor.copy(alpha = 0.92f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                StatusBadge(
                    text = statusUi.label,
                    containerColor = statusUi.containerColor,
                    contentColor = statusUi.contentColor
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TagPill(
                    text = badgeText,
                    containerColor = BrandPrimarySoft,
                    contentColor = BrandPrimaryDark
                )
                TagPill(
                    text = statusUi.shortLabel,
                    containerColor = statusUi.tagContainerColor,
                    contentColor = statusUi.tagContentColor
                )
            }
        }
    }
}

@Composable
private fun MetricRow(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = title,
            style = AppTextStyles.ArabicBody,
            color = TextSecondaryColor,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(AppDimens.Spacing.medium))
        Text(
            text = value,
            style = AppTextStyles.ArabicBody,
            color = TextPrimaryColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.2f)
        )
    }
}

@Composable
private fun FeatureRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.Radius.medium))
            .border(
                width = AppDimens.Component.borderThickness,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(AppDimens.Radius.medium)
            )
            .padding(
                horizontal = AppDimens.Spacing.medium,
                vertical = AppDimens.Spacing.small
            ),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(BrandPrimary)
        )
        Text(
            text = text,
            style = AppTextStyles.ArabicBody,
            color = TextPrimaryColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TagPill(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        color = containerColor,
        shape = AppShapeTokens.chip,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = AppDimens.Spacing.large,
                vertical = AppDimens.Spacing.small
            ),
            style = AppTextStyles.ArabicBody,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusBadge(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        color = containerColor,
        shape = AppShapeTokens.chip,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = AppDimens.Spacing.large,
                vertical = AppDimens.Spacing.small
            ),
            style = AppTextStyles.ArabicBody,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private data class PlanUiModel(
    val title: String,
    val subtitle: String,
    val badge: String
)

private data class StatusUiModel(
    val label: String,
    val shortLabel: String,
    val description: String,
    val containerColor: Color,
    val contentColor: Color,
    val tagContainerColor: Color,
    val tagContentColor: Color
)

private fun SubscriptionPlan.toPlanUiModel(): PlanUiModel {
    return when (this) {
        SubscriptionPlan.STARTER -> PlanUiModel(
            title = "خطة البداية",
            subtitle = "حل مناسب للانطلاق وتجربة المزايا الأساسية",
            badge = "Starter"
        )

        SubscriptionPlan.PRO -> PlanUiModel(
            title = "الخطة الاحترافية",
            subtitle = "أفضل توازن بين القوة والمرونة للأعمال المتنامية",
            badge = "Pro"
        )

        SubscriptionPlan.BUSINESS -> PlanUiModel(
            title = "خطة الأعمال",
            subtitle = "مناسبة للفروع والفرق التي تحتاج إدارة أوسع",
            badge = "Business"
        )

        SubscriptionPlan.ENTERPRISE -> PlanUiModel(
            title = "الخطة المؤسسية",
            subtitle = "للمنشآت الكبيرة مع أعلى مستويات التحكم",
            badge = "Enterprise"
        )

        SubscriptionPlan.CUSTOM -> PlanUiModel(
            title = "خطة مخصصة",
            subtitle = "إعدادات مخصصة وفق احتياج العميل",
            badge = "Custom"
        )
    }
}

private fun SubscriptionInfo.toStatusUiModel(): StatusUiModel {
    return when {
        status == SubscriptionStatus.ACTIVE && !isExpired -> StatusUiModel(
            label = "نشط",
            shortLabel = "Active",
            description = "الاشتراك يعمل بشكل طبيعي",
            containerColor = SuccessContainer,
            contentColor = SuccessColor,
            tagContainerColor = BrandPrimarySoft,
            tagContentColor = BrandPrimaryDark
        )

        isInGracePeriod -> StatusUiModel(
            label = "فترة سماح",
            shortLabel = "Grace",
            description = "انتهى الاشتراك لكن ما زالت فترة السماح سارية",
            containerColor = WarningContainer,
            contentColor = WarningColor,
            tagContainerColor = WarningContainer,
            tagContentColor = WarningColor
        )

        status == SubscriptionStatus.SUSPENDED -> StatusUiModel(
            label = "موقوف",
            shortLabel = "Hold",
            description = "تم إيقاف الاشتراك مؤقتًا",
            containerColor = ErrorContainer,
            contentColor = ErrorColor,
            tagContainerColor = ErrorContainer,
            tagContentColor = ErrorColor
        )

        status == SubscriptionStatus.CANCELED -> StatusUiModel(
            label = "ملغي",
            shortLabel = "Canceled",
            description = "تم إلغاء الاشتراك",
            containerColor = ErrorContainer,
            contentColor = ErrorColor,
            tagContainerColor = ErrorContainer,
            tagContentColor = ErrorColor
        )

        status == SubscriptionStatus.EXPIRED || isExpired -> StatusUiModel(
            label = "منتهي",
            shortLabel = "Expired",
            description = "انتهت صلاحية الاشتراك",
            containerColor = ErrorContainer,
            contentColor = ErrorColor,
            tagContainerColor = ErrorContainer,
            tagContentColor = ErrorColor
        )

        else -> StatusUiModel(
            label = "قيد الانتظار",
            shortLabel = "Pending",
            description = "الاشتراك غير مفعّل بعد",
            containerColor = InfoContainer,
            contentColor = InfoColor,
            tagContainerColor = InfoContainer,
            tagContentColor = InfoColor
        )
    }
}

private fun formatRemainingTime(expiryDateMillis: Long): String {
    val now = System.currentTimeMillis()
    val diff = expiryDateMillis - now
    val dayMillis = 24L * 60L * 60L * 1000L
    val days = (diff / dayMillis).toInt()

    return when {
        diff < 0L -> "منتهي"
        days == 0 -> "اليوم"
        days == 1 -> "يوم واحد"
        days in 2..30 -> "$days أيام"
        days > 30 -> "${days / 30} شهر تقريبًا"
        else -> "قريبًا"
    }
}

private fun formatDateTimeMillis(millis: Long): String {
    val dateTime = DateTimeUtils.instantToLocalDateTime(Instant.ofEpochMilli(millis))
    return DateTimeUtils.formatDateTime(dateTime, pattern = "dd/MM/yyyy HH:mm")
}

private fun SubscriptionFeature.displayName(): String {
    return when (this) {
        SubscriptionFeature.ADVANCED_REPORTS -> "تقارير متقدمة"
        SubscriptionFeature.MULTI_BRANCH -> "إدارة فروع متعددة"
        SubscriptionFeature.API_ACCESS -> "وصول API"
        SubscriptionFeature.PAYMENT_GATEWAY -> "بوابة دفع"
        SubscriptionFeature.TAX_INVOICE -> "فاتورة ضريبية"
        SubscriptionFeature.LEGAL_INVOICE -> "فاتورة قانونية"
        SubscriptionFeature.OFFLINE_MODE -> "وضع دون اتصال"
        SubscriptionFeature.AUDIT_LOGS -> "سجل تدقيق"
        SubscriptionFeature.EXPORT_EXCEL -> "تصدير Excel"
        SubscriptionFeature.EXPORT_PDF -> "تصدير PDF"
        SubscriptionFeature.ROLE_BASED_ACCESS -> "صلاحيات حسب الدور"
        SubscriptionFeature.BACKUP_SYNC -> "نسخ احتياطي ومزامنة"
    }
}