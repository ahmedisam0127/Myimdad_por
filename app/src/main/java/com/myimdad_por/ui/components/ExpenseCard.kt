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
import com.myimdad_por.core.utils.CurrencyFormatter
import com.myimdad_por.core.utils.DateTimeUtils
import com.myimdad_por.domain.model.Expense
import com.myimdad_por.domain.model.ExpenseCategory
import com.myimdad_por.domain.model.ExpenseStatus
import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.AppShapeTokens
import com.myimdad_por.ui.theme.AppTextStyles
import com.myimdad_por.ui.theme.ErrorColor
import com.myimdad_por.ui.theme.ErrorContainer
import com.myimdad_por.ui.theme.ExpenseColor
import com.myimdad_por.ui.theme.ExpenseContainer
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

@Composable
fun ExpenseCard(
    expense: Expense,
    modifier: Modifier = Modifier,
    onCardClick: (() -> Unit)? = null,
    primaryActionText: String = "التفاصيل",
    secondaryActionText: String? = when (expense.status) {
        ExpenseStatus.PENDING -> "اعتماد"
        ExpenseStatus.APPROVED -> "سداد"
        ExpenseStatus.PAID -> "إيصال"
        ExpenseStatus.CANCELLED -> null
    },
    onPrimaryActionClick: () -> Unit = {},
    onSecondaryActionClick: (() -> Unit)? = null,
    showNote: Boolean = true
) {
    val statusUi = expense.toStatusUi()
    val amountText = CurrencyFormatter.formatSDG(expense.amount)
    val paidText = CurrencyFormatter.formatSDG(expense.paidAmount)
    val remainingText = CurrencyFormatter.formatSDG(expense.remainingAmount)
    val dateText = DateTimeUtils.formatDateTime(expense.expenseDate, pattern = "dd/MM/yyyy HH:mm")

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
        Column(modifier = Modifier.fillMaxWidth()) {
            ExpenseHeader(
                expense = expense,
                statusUi = statusUi
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.Layout.screenPadding),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
            ) {
                AmountSection(
                    amountText = amountText,
                    statusUi = statusUi
                )

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                MetricRow(
                    title = "المتبقي",
                    value = remainingText
                )
                MetricRow(
                    title = "المدفوع",
                    value = paidText
                )
                MetricRow(
                    title = "طريقة الدفع",
                    value = expense.paymentMethod.toDisplayName()
                )
                MetricRow(
                    title = "التاريخ",
                    value = dateText
                )

                if (!expense.supplierName.isNullOrBlank()) {
                    MetricRow(
                        title = "المورد",
                        value = expense.supplierName.orEmpty()
                    )
                }

                if (!expense.referenceNumber.isNullOrBlank()) {
                    MetricRow(
                        title = "المرجع",
                        value = expense.referenceNumber.orEmpty()
                    )
                }

                if (!expense.employeeId.isNullOrBlank()) {
                    MetricRow(
                        title = "الموظف",
                        value = expense.employeeId.orEmpty()
                    )
                }

                if (showNote && !expense.note.isNullOrBlank()) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        text = "ملاحظة",
                        style = AppTextStyles.ArabicBody,
                        color = TextSecondaryColor
                    )
                    Text(
                        text = expense.note.orEmpty(),
                        style = AppTextStyles.ArabicBody,
                        color = TextPrimaryColor,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
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
                        variant = AppButtonVariant.Primary
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
private fun ExpenseHeader(
    expense: Expense,
    statusUi: ExpenseStatusUi
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(ExpenseColor, Color(0xFFB42318))
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
                        text = expense.title,
                        style = AppTextStyles.ArabicTitle,
                        color = IconOnBrandColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = expense.expenseNumber,
                        style = AppTextStyles.NumberBody,
                        color = IconOnBrandColor.copy(alpha = 0.92f),
                        maxLines = 1,
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
                    text = expense.category.toDisplayName(),
                    containerColor = ExpenseContainer,
                    contentColor = ExpenseColor
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
private fun AmountSection(
    amountText: String,
    statusUi: ExpenseStatusUi
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)) {
        Text(
            text = "إجمالي المصروف",
            style = AppTextStyles.ArabicBody,
            color = TextSecondaryColor
        )
        Text(
            text = amountText,
            style = AppTextStyles.NumberDisplay,
            color = statusUi.amountColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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

private data class ExpenseStatusUi(
    val label: String,
    val shortLabel: String,
    val containerColor: Color,
    val contentColor: Color,
    val tagContainerColor: Color,
    val tagContentColor: Color,
    val amountColor: Color
)

private fun Expense.toStatusUi(): ExpenseStatusUi {
    return when (status) {
        ExpenseStatus.PENDING -> ExpenseStatusUi(
            label = "قيد الانتظار",
            shortLabel = "Pending",
            containerColor = WarningContainer,
            contentColor = WarningColor,
            tagContainerColor = WarningContainer,
            tagContentColor = WarningColor,
            amountColor = WarningColor
        )

        ExpenseStatus.APPROVED -> ExpenseStatusUi(
            label = "معتمد",
            shortLabel = "Approved",
            containerColor = InfoContainer,
            contentColor = InfoColor,
            tagContainerColor = InfoContainer,
            tagContentColor = InfoColor,
            amountColor = InfoColor
        )

        ExpenseStatus.PAID -> ExpenseStatusUi(
            label = "مسدد",
            shortLabel = "Paid",
            containerColor = SuccessContainer,
            contentColor = SuccessColor,
            tagContainerColor = SuccessContainer,
            tagContentColor = SuccessColor,
            amountColor = SuccessColor
        )

        ExpenseStatus.CANCELLED -> ExpenseStatusUi(
            label = "ملغي",
            shortLabel = "Canceled",
            containerColor = ErrorContainer,
            contentColor = ErrorColor,
            tagContainerColor = ErrorContainer,
            tagContentColor = ErrorColor,
            amountColor = ErrorColor
        )
    }
}

private fun ExpenseCategory.toDisplayName(): String {
    return when (this) {
        ExpenseCategory.RENT -> "إيجار"
        ExpenseCategory.SALARY -> "رواتب"
        ExpenseCategory.UTILITIES -> "خدمات"
        ExpenseCategory.TRANSPORT -> "نقل"
        ExpenseCategory.MAINTENANCE -> "صيانة"
        ExpenseCategory.PURCHASE -> "مشتريات"
        ExpenseCategory.TAX -> "ضرائب"
        ExpenseCategory.MARKETING -> "تسويق"
        ExpenseCategory.OFFICE -> "مصاريف مكتبية"
        ExpenseCategory.OTHER -> "أخرى"
    }
}

private fun PaymentMethod.toDisplayName(): String {
    return when (this) {
        PaymentMethod.CASH -> "نقداً"
        PaymentMethod.BANK_TRANSFER -> "تحويل بنكي"
        
        else -> toString().replace('_', ' ')
    }
}