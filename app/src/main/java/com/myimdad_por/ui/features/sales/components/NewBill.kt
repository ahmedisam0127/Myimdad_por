package com.myimdad_por.ui.features.sales.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PersonAddAlt1
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myimdad_por.core.utils.CurrencyFormatter
import com.myimdad_por.ui.components.AppButton
import com.myimdad_por.ui.components.AppButtonSize
import com.myimdad_por.ui.components.AppButtonVariant
import com.myimdad_por.ui.features.sales.SalesConstants
import com.myimdad_por.ui.features.sales.models.BillUiModel
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.AppShapeTokens
import com.myimdad_por.ui.theme.BorderColor
import com.myimdad_por.ui.theme.BrandPrimary
import com.myimdad_por.ui.theme.BrandPrimaryDark
import com.myimdad_por.ui.theme.BrandPrimarySoft
import com.myimdad_por.ui.theme.BrandPrimaryTint
import com.myimdad_por.ui.theme.IconOnBrandColor
import com.myimdad_por.ui.theme.SuccessColor
import com.myimdad_por.ui.theme.SurfaceColor
import com.myimdad_por.ui.theme.TextPrimaryColor
import com.myimdad_por.ui.theme.TextSecondaryColor
import java.math.BigDecimal

@Composable
fun NewBill(
    modifier: Modifier = Modifier,
    bill: BillUiModel = BillUiModel.empty(),
    enabled: Boolean = true,
    loading: Boolean = false,
    onAddCustomer: () -> Unit = {},
    onCheckout: () -> Unit = {},
    onAddProducts: () -> Unit = {}
) {

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapeTokens.card,
        color = SurfaceColor,
        tonalElevation = AppDimens.Elevation.medium,
        shadowElevation = AppDimens.Elevation.high
    ) {

        Column(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            BrandPrimarySoft,
                            SurfaceColor
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = BrandPrimary.copy(alpha = 0.14f),
                    shape = AppShapeTokens.card
                )
                .padding(AppDimens.Spacing.large)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    BrandPrimary,
                                    BrandPrimaryDark
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Rounded.AddShoppingCart,
                        contentDescription = null,
                        tint = IconOnBrandColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(AppDimens.Spacing.normal))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "فاتورة جديدة",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (bill.hasCustomer) {
                            "العميل: ${bill.customerName}"
                        } else {
                            "ابدأ بإضافة منتجات لإنشاء فاتورة احترافية"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = AppShapeTokens.chip,
                    color = BrandPrimaryTint
                ) {

                    Text(
                        text = "${bill.itemCount} عنصر",
                        modifier = Modifier.padding(
                            horizontal = AppDimens.Spacing.medium,
                            vertical = AppDimens.Spacing.small
                        ),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = BrandPrimaryDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.Spacing.large))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapeTokens.filledCard,
                color = Color.White.copy(alpha = 0.95f)
            ) {

                Column(
                    modifier = Modifier.padding(AppDimens.Spacing.large)
                ) {

                    BillPriceRow(
                        title = SalesConstants.Ui.LABEL_SUBTOTAL,
                        value = bill.formattedSubtotalAmount.ifBlank {
                            CurrencyFormatter.formatSDG(BigDecimal.ZERO)
                        }
                    )

                    Spacer(modifier = Modifier.height(AppDimens.Spacing.medium))

                    BillPriceRow(
                        title = SalesConstants.Ui.LABEL_DISCOUNT,
                        value = bill.formattedDiscountAmount.ifBlank {
                            CurrencyFormatter.formatSDG(BigDecimal.ZERO)
                        }
                    )

                    Spacer(modifier = Modifier.height(AppDimens.Spacing.medium))

                    BillPriceRow(
                        title = SalesConstants.Ui.LABEL_TAX,
                        value = bill.formattedTaxAmount.ifBlank {
                            CurrencyFormatter.formatSDG(BigDecimal.ZERO)
                        }
                    )

                    Spacer(modifier = Modifier.height(AppDimens.Spacing.large))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(BorderColor)
                    )

                    Spacer(modifier = Modifier.height(AppDimens.Spacing.large))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Column {

                            Text(
                                text = SalesConstants.Ui.LABEL_TOTAL,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = TextPrimaryColor
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "إجمالي الفاتورة الحالية",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryColor
                            )
                        }

                        Text(
                            text = bill.formattedTotalAmount.ifBlank {
                                CurrencyFormatter.formatSDG(BigDecimal.ZERO)
                            },
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = SuccessColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.Spacing.extraLarge))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    AppDimens.Spacing.medium
                )
            ) {

                AppButton(
                    text = "إضافة عميل",
                    onClick = onAddCustomer,
                    modifier = Modifier.weight(1f),
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Medium,
                    enabled = enabled,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.PersonAddAlt1,
                            contentDescription = null
                        )
                    }
                )

                AppButton(
                    text = "إضافة منتجات",
                    onClick = onAddProducts,
                    modifier = Modifier.weight(1f),
                    variant = AppButtonVariant.Outlined,
                    size = AppButtonSize.Medium,
                    enabled = enabled,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.AddShoppingCart,
                            contentDescription = null
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.Spacing.medium))

            AppButton(
                text = SalesConstants.Ui.ACTION_PAY,
                onClick = onCheckout,
                modifier = Modifier.fillMaxWidth(),
                variant = AppButtonVariant.Primary,
                size = AppButtonSize.Large,
                enabled = enabled,
                loading = loading,
                fullWidth = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Payments,
                        contentDescription = null
                    )
                }
            )
        }
    }
}

@Composable
private fun BillPriceRow(
    title: String,
    value: String
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondaryColor
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = TextPrimaryColor
        )
    }
}