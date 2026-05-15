package com.myimdad_por.ui.features.sales.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myimdad_por.domain.model.Customer
import com.myimdad_por.ui.components.AppTextField
import com.myimdad_por.ui.components.AppTextFieldSize
import com.myimdad_por.ui.components.AppTextFieldVariant
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.AppShapeTokens
import com.myimdad_por.ui.theme.BrandPrimary
import com.myimdad_por.ui.theme.BrandPrimarySoft
import com.myimdad_por.ui.theme.BrandPrimaryTint
import com.myimdad_por.ui.theme.BorderColor
import com.myimdad_por.ui.theme.SuccessColor
import com.myimdad_por.ui.theme.TextPrimaryColor
import com.myimdad_por.ui.theme.TextSecondaryColor
import com.myimdad_por.ui.theme.WhiteColor
import java.math.BigDecimal
import java.text.DecimalFormat

@Composable
fun CustomerSection(
    customerQuery: String,
    onCustomerQueryChange: (String) -> Unit,
    searchResults: List<Customer>,
    selectedCustomer: Customer?,
    onCustomerSelected: (Customer) -> Unit,
    onRemoveCustomer: () -> Unit,
    onAddNewCustomerClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapeTokens.card,
        color = WhiteColor,
        contentColor = TextPrimaryColor,
        border = BorderStroke(1.dp, BrandPrimaryTint),
        tonalElevation = AppDimens.Elevation.low,
        shadowElevation = AppDimens.Elevation.low
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Spacing.large),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.large)
        ) {
            CustomerSectionHeader(
                selectedCustomer = selectedCustomer,
                onAddNewCustomerClick = onAddNewCustomerClick
            )

            if (selectedCustomer != null) {
                SelectedCustomerCard(
                    customer = selectedCustomer,
                    onRemoveCustomer = onRemoveCustomer
                )
            } else {
                CustomerSearchField(
                    value = customerQuery,
                    onValueChange = onCustomerQueryChange,
                    enabled = enabled,
                    loading = loading
                )

                when {
                    loading -> {
                        SearchLoadingState()
                    }

                    searchResults.isNotEmpty() -> {
                        CustomerResultsList(
                            customers = searchResults,
                            onCustomerSelected = onCustomerSelected
                        )
                    }

                    customerQuery.isNotBlank() -> {
                        EmptyCustomerState()
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerSectionHeader(
    selectedCustomer: Customer?,
    onAddNewCustomerClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(BrandPrimary)
                )
                Text(
                    text = "العميل",
                    color = TextPrimaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = when (selectedCustomer) {
                    null -> "ابحث عن عميل أو أضف عميلًا جديدًا"
                    else -> "تم اختيار العميل بنجاح"
                },
                color = TextSecondaryColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        OutlinedButton(
            onClick = onAddNewCustomerClick,
            shape = AppShapeTokens.buttonPill,
            border = BorderStroke(1.dp, BrandPrimary)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = BrandPrimary
            )
            Spacer(modifier = Modifier.width(AppDimens.Spacing.extraSmall))
            Text(
                text = "عميل جديد",
                color = BrandPrimary
            )
        }
    }
}

@Composable
private fun CustomerSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    loading: Boolean
) {
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        readOnly = false,
        label = "بحث العميل",
        placeholder = "اكتب الاسم، الهاتف، الكود، أو الاسم التجاري",
        helperText = if (loading) "جارٍ البحث..." else "ابدأ بالكتابة للوصول السريع للعميل",
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = BrandPrimary
            )
        },
        variant = AppTextFieldVariant.Outlined,
        size = AppTextFieldSize.Large,
        contentDescription = "حقل بحث العملاء"
    )
}

@Composable
private fun SearchLoadingState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapeTokens.filledCard,
        color = BrandPrimarySoft,
        border = BorderStroke(1.dp, BrandPrimaryTint)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Spacing.large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(BrandPrimary)
            )
            Text(
                text = "جارٍ البحث عن العملاء...",
                color = TextSecondaryColor
            )
        }
    }
}

@Composable
private fun CustomerResultsList(
    customers: List<Customer>,
    onCustomerSelected: (Customer) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
    ) {
        Text(
            text = "نتائج البحث",
            color = TextSecondaryColor
        )

        LazyColumn(
            modifier = Modifier.heightIn(max = 320.dp),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
        ) {
            items(
                items = customers,
                key = { it.id }
            ) { customer ->
                CustomerResultItem(
                    customer = customer,
                    onClick = { onCustomerSelected(customer) }
                )
            }
        }
    }
}

@Composable
private fun CustomerResultItem(
    customer: Customer,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = AppShapeTokens.card,
        color = BrandPrimarySoft,
        contentColor = TextPrimaryColor,
        border = BorderStroke(1.dp, BrandPrimaryTint),
        tonalElevation = AppDimens.Elevation.low,
        shadowElevation = AppDimens.Elevation.low
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            CustomerAvatar(
                name = customer.displayName,
                modifier = Modifier.size(48.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
                ) {
                    Text(
                        text = customer.displayName,
                        color = TextPrimaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (customer.isCreditCustomer) {
                        StatusBadge(
                            text = "آجل",
                            tint = SuccessColor
                        )
                    }
                }

                Text(
                    text = customer.identifier,
                    color = TextSecondaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                customer.phoneNumber?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        color = TextSecondaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = "اختيار",
                color = BrandPrimary
            )
        }
    }
}

@Composable
private fun SelectedCustomerCard(
    customer: Customer,
    onRemoveCustomer: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapeTokens.card,
        color = WhiteColor,
        contentColor = TextPrimaryColor,
        border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.16f)),
        tonalElevation = AppDimens.Elevation.medium,
        shadowElevation = AppDimens.Elevation.medium
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandPrimarySoft)
                    .padding(AppDimens.Spacing.large),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
                ) {
                    CustomerAvatar(
                        name = customer.displayName,
                        modifier = Modifier.size(56.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
                        ) {
                            Text(
                                text = customer.displayName,
                                color = TextPrimaryColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            StatusBadge(
                                text = if (customer.hasDebt) "مديون" else "منظم",
                                tint = if (customer.hasDebt) SuccessColor else BrandPrimary
                            )
                        }

                        Text(
                            text = customer.identifier,
                            color = TextSecondaryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    TextButton(onClick = onRemoveCustomer) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = BrandPrimary
                        )
                        Spacer(modifier = Modifier.width(AppDimens.Spacing.extraSmall))
                        Text(
                            text = "إزالة",
                            color = BrandPrimary
                        )
                    }
                }

                Text(
                    text = "تفاصيل العميل الأساسية",
                    color = TextSecondaryColor
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.Spacing.large),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
            ) {
                InfoRow(
                    icon = Icons.Default.Phone,
                    label = "الهاتف",
                    value = customer.phoneNumber,
                    accent = BrandPrimary
                )
                InfoRow(
                    icon = Icons.Default.Email,
                    label = "البريد",
                    value = customer.email,
                    accent = BrandPrimary
                )
                InfoRow(
                    icon = Icons.Default.LocationOn,
                    label = "المدينة",
                    value = customer.city,
                    accent = BrandPrimary
                )
                InfoRow(
                    icon = Icons.Default.CreditCard,
                    label = "الحد الائتماني",
                    value = formatMoney(customer.creditLimit),
                    accent = SuccessColor
                )
                InfoRow(
                    icon = Icons.Default.CreditCard,
                    label = "المتبقي",
                    value = formatMoney(customer.availableCredit),
                    accent = BrandPrimary
                )

                customer.address?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = "العنوان: $it",
                        color = TextSecondaryColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerAvatar(
    name: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(BrandPrimary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initialsOf(name),
            color = WhiteColor,
            maxLines = 1
        )
    }
}

@Composable
private fun StatusBadge(
    text: String,
    tint: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = tint.copy(alpha = 0.12f),
        contentColor = tint,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.22f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = AppDimens.Spacing.small, vertical = 4.dp),
            color = tint,
            maxLines = 1
        )
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String?,
    accent: Color
) {
    if (value.isNullOrBlank()) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapeTokens.filledCard,
        color = WhiteColor,
        contentColor = TextPrimaryColor,
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = label,
                    color = TextSecondaryColor
                )
                Text(
                    text = value,
                    color = TextPrimaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyCustomerState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapeTokens.card,
        color = BrandPrimaryTint,
        contentColor = TextPrimaryColor,
        border = BorderStroke(1.dp, BrandPrimaryTint)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Spacing.large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(BrandPrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = BrandPrimary
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "لا توجد نتائج مطابقة",
                    color = TextPrimaryColor
                )
                Text(
                    text = "جرّب اسمًا آخر، أو أضف عميلًا جديدًا مباشرة.",
                    color = TextSecondaryColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun initialsOf(name: String): String {
    val parts = name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }

    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> (parts.first().first().toString() + parts.last().first().toString()).uppercase()
    }
}

private fun formatMoney(value: BigDecimal): String {
    return DecimalFormat("#,##0.##").format(value)
}