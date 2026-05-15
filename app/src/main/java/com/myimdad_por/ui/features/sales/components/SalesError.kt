package com.myimdad_por.ui.features.sales.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.BrandPrimary
import com.myimdad_por.ui.theme.BrandPrimaryDark
import com.myimdad_por.ui.theme.BrandPrimarySoft
import com.myimdad_por.ui.theme.BrandPrimaryTint
import com.myimdad_por.ui.theme.IconOnBrandColor
import com.myimdad_por.ui.theme.TextPrimaryColor
import com.myimdad_por.ui.theme.TextSecondaryColor
import com.myimdad_por.ui.theme.WhiteColor

@Composable
fun SalesError(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    title: String = "حدث خطأ أثناء تحميل البيانات",
    message: String = "تعذر إكمال العملية حالياً، يرجى المحاولة مرة أخرى.",
    actionText: String = "إعادة المحاولة",
    onRetry: (() -> Unit)? = null
) {

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 })
    ) {

        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(AppDimens.Spacing.normal),
            shape = RoundedCornerShape(AppDimens.Radius.extraLarge),
            color = WhiteColor,
            tonalElevation = AppDimens.Elevation.medium,
            shadowElevation = AppDimens.Elevation.high
        ) {

            Column(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                BrandPrimarySoft,
                                WhiteColor
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = BrandPrimary.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(AppDimens.Radius.extraLarge)
                    )
                    .padding(AppDimens.Spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    BrandPrimary.copy(alpha = 0.25f),
                                    BrandPrimaryTint,
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Box(
                        modifier = Modifier
                            .size(62.dp)
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
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = IconOnBrandColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppDimens.Spacing.large))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextPrimaryColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(AppDimens.Spacing.small))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryColor,
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                )

                if (onRetry != null) {

                    Spacer(modifier = Modifier.height(AppDimens.Spacing.extraLarge))

                    TextButton(
                        onClick = onRetry,
                        shape = RoundedCornerShape(AppDimens.Radius.round),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        BrandPrimary,
                                        BrandPrimaryDark
                                    )
                                ),
                                shape = RoundedCornerShape(AppDimens.Radius.round)
                            )
                    ) {

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                tint = WhiteColor,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(
                                modifier = Modifier.padding(
                                    horizontal = AppDimens.Spacing.extraSmall
                                )
                            )

                            Text(
                                text = actionText,
                                color = WhiteColor,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}