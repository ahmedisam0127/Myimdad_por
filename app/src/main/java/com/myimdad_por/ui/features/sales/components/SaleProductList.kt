package com.myimdad_por.ui.features.sales.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myimdad_por.ui.features.sales.SalesConstants
import com.myimdad_por.ui.features.sales.SalesUiEvent
import com.myimdad_por.ui.features.sales.SalesUiState
import com.myimdad_por.ui.features.sales.models.ProductUiModel
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.AppShapeTokens
import com.myimdad_por.ui.theme.BackgroundColor
import com.myimdad_por.ui.theme.BorderColor
import com.myimdad_por.ui.theme.BrandPrimary
import com.myimdad_por.ui.theme.BrandPrimaryDark
import com.myimdad_por.ui.theme.BrandPrimarySoft
import com.myimdad_por.ui.theme.BrandPrimaryTint
import com.myimdad_por.ui.theme.ErrorColor
import com.myimdad_por.ui.theme.SuccessColor
import com.myimdad_por.ui.theme.SurfaceColor
import com.myimdad_por.ui.theme.TextPrimaryColor
import com.myimdad_por.ui.theme.TextSecondaryColor
import com.myimdad_por.ui.theme.WhiteColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SaleProductList(
    state: SalesUiState,
    modifier: Modifier = Modifier,
    onEvent: (SalesUiEvent) -> Unit,
    onProductClick: ((ProductUiModel) -> Unit)? = null
) {
    val products = if (state.searchQuery.isBlank()) {
        state.products
    } else {
        state.filteredProducts
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        SaleProductListHeader(
            query = state.searchQuery,
            totalProducts = products.size,
            onSearchChange = {
                onEvent(SalesUiEvent.SearchProducts(it))
            },
            onClearSearch = {
                onEvent(SalesUiEvent.ClearSearch)
            }
        )

        AnimatedContent(
            targetState = Triple(
                state.isLoadingProducts,
                state.hasError,
                products.isEmpty()
            ),
            label = "products_content_animation"
        ) { target ->

            when {
                target.first -> {
                    SalesLoading(
                        modifier = Modifier.padding(AppDimens.Spacing.normal)
                    )
                }

                target.second -> {
                    SaleProductsError(
                        message = state.errorMessage
                            ?: SalesConstants.Error.NETWORK_ERROR,
                        onRetry = {
                            onEvent(SalesUiEvent.Retry)
                        }
                    )
                }

                target.third -> {
                    EmptySales(
                        modifier = Modifier.padding(AppDimens.Spacing.normal),
                        title = if (state.searchQuery.isBlank()) {
                            SalesConstants.Ui.NO_PRODUCTS_MESSAGE
                        } else {
                            SalesConstants.Ui.NO_RESULTS_MESSAGE
                        },
                        message = if (state.searchQuery.isBlank()) {
                            "لا توجد منتجات متاحة حالياً داخل النظام"
                        } else {
                            "لم يتم العثور على نتائج مطابقة لعبارة البحث"
                        },
                        primaryActionText = "تحديث المنتجات",
                        secondaryActionText = "مسح البحث",
                        onPrimaryActionClick = {
                            onEvent(SalesUiEvent.RefreshData)
                        },
                        onSecondaryActionClick = {
                            onEvent(SalesUiEvent.ClearSearch)
                        }
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = AppDimens.Spacing.normal,
                            end = AppDimens.Spacing.normal,
                            top = AppDimens.Spacing.small,
                            bottom = 120.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
                    ) {
                        items(
                            items = products,
                            key = { it.id }
                        ) { product ->
                            SaleProductCard(
                                product = product,
                                onClick = {
                                    onProductClick?.invoke(product)
                                },
                                onAddToCart = {
                                    onEvent(
                                        SalesUiEvent.AddProductToCart(product)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaleProductListHeader(
    query: String,
    totalProducts: Int,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BrandPrimary,
                        BrandPrimaryDark
                    )
                )
            )
            .padding(AppDimens.Spacing.large)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "المنتجات",
                    color = WhiteColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "إدارة وعرض المنتجات بطريقة احترافية",
                    color = WhiteColor.copy(alpha = 0.82f),
                    fontSize = 13.sp
                )
            }

            Surface(
                shape = CircleShape,
                color = WhiteColor.copy(alpha = 0.14f)
            ) {
                Box(
                    modifier = Modifier.padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Inventory2,
                        contentDescription = null,
                        tint = WhiteColor,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.Spacing.large))

        OutlinedTextField(
            value = query,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = AppShapeTokens.textField,
            placeholder = {
                Text(
                    text = SalesConstants.Ui.SEARCH_HINT,
                    color = TextSecondaryColor
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = BrandPrimary
                )
            },
            trailingIcon = {
                AnimatedVisibility(visible = query.isNotBlank()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = ErrorColor
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = WhiteColor,
                unfocusedContainerColor = WhiteColor,
                focusedBorderColor = WhiteColor,
                unfocusedBorderColor = WhiteColor.copy(alpha = 0.65f),
                cursorColor = BrandPrimary,
                focusedTextColor = TextPrimaryColor,
                unfocusedTextColor = TextPrimaryColor
            )
        )

        Spacer(modifier = Modifier.height(AppDimens.Spacing.medium))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = AppShapeTokens.badge,
                color = WhiteColor.copy(alpha = 0.14f)
            ) {
                Text(
                    text = "$totalProducts منتج",
                    modifier = Modifier.padding(
                        horizontal = AppDimens.Spacing.medium,
                        vertical = AppDimens.Spacing.small
                    ),
                    color = WhiteColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Text(
                text = "جاهز للبيع",
                color = WhiteColor.copy(alpha = 0.90f),
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SaleProductCard(
    product: ProductUiModel,
    onClick: () -> Unit,
    onAddToCart: () -> Unit
) {
    val available = product.isActive && product.isAvailableForSale

    val borderColor by animateColorAsState(
        targetValue = if (available) {
            BrandPrimaryTint
        } else {
            BorderColor
        },
        animationSpec = tween(350),
        label = "product_border_animation"
    )

    val elevation by animateDpAsState(
        targetValue = if (available) {
            AppDimens.Elevation.high
        } else {
            AppDimens.Elevation.low
        },
        label = "product_card_elevation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapeTokens.card)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        shape = AppShapeTokens.card,
        colors = CardDefaults.cardColors(
            containerColor = SurfaceColor
        ),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                BrandPrimary,
                                BrandPrimaryDark
                            )
                        )
                    )
                    .padding(AppDimens.Spacing.large)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.displayName,
                            color = WhiteColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = product.formattedPrice,
                            color = WhiteColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = WhiteColor.copy(alpha = 0.14f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Inventory2,
                            contentDescription = null,
                            tint = WhiteColor,
                            modifier = Modifier
                                .padding(14.dp)
                                .size(28.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.Spacing.large),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
            ) {
                ProductInfoRow(
                    icon = Icons.Rounded.Inventory2,
                    title = "الباركود",
                    value = product.barcode,
                    tint = BrandPrimaryDark
                )

                if (product.supportsUnitHierarchy) {
                    ProductInfoRow(
                        icon = Icons.Rounded.LocalOffer,
                        title = "الوحدات",
                        value = product.conversionLabel,
                        tint = SuccessColor
                    )
                }

                AnimatedVisibility(
                    visible = product.shortDescription.isNotBlank()
                ) {
                    Text(
                        text = product.shortDescription,
                        color = TextSecondaryColor,
                        fontSize = 13.sp,
                        lineHeight = 22.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Divider(color = BorderColor.copy(alpha = 0.6f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Crossfade(
                        targetState = available,
                        label = "availability_state"
                    ) { isAvailable ->
                        if (isAvailable) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessColor,
                                    modifier = Modifier.size(18.dp)
                                )

                                Text(
                                    text = "متوفر للبيع",
                                    color = SuccessColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            Text(
                                text = "غير متاح حالياً",
                                color = ErrorColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.clickable(
                            enabled = available,
                            onClick = onAddToCart
                        ),
                        shape = AppShapeTokens.buttonPill,
                        color = if (available) {
                            BrandPrimary
                        } else {
                            BorderColor
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = AppDimens.Spacing.medium,
                                vertical = AppDimens.Spacing.small
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddShoppingCart,
                                contentDescription = null,
                                tint = WhiteColor,
                                modifier = Modifier.size(18.dp)
                            )

                            Text(
                                text = "إضافة",
                                color = WhiteColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    tint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = tint.copy(alpha = 0.12f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .padding(10.dp)
                    .size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextSecondaryColor,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = value,
                color = TextPrimaryColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SaleProductsError(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppDimens.Spacing.xLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.large)
    ) {
        Surface(
            shape = CircleShape,
            color = ErrorColor.copy(alpha = 0.12f)
        ) {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = ErrorColor,
                modifier = Modifier
                    .padding(24.dp)
                    .size(44.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
        ) {
            Text(
                text = "حدث خطأ أثناء تحميل المنتجات",
                color = TextPrimaryColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = message,
                color = TextSecondaryColor,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }

        Surface(
            modifier = Modifier.clickable(onClick = onRetry),
            shape = AppShapeTokens.buttonPill,
            color = BrandPrimary
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = AppDimens.Spacing.large,
                    vertical = AppDimens.Spacing.medium
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = WhiteColor,
                    strokeWidth = 2.dp
                )

                Text(
                    text = "إعادة المحاولة",
                    color = WhiteColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
