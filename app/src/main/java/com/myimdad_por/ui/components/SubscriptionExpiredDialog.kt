package com.myimdad_por.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

// استيراد المكونات والثيمات الخاصة بك بشكل صريح
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.AppShapeTokens
import com.myimdad_por.ui.theme.AppTypography
import com.myimdad_por.ui.theme.WarningColor

@Composable
fun SubscriptionExpiredDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    onRenewClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "انتهى الاشتراك",
    message: String = "لقد انتهت مدة الاشتراك الحالية. يرجى التجديد لمتابعة استخدام الميزات.",
    renewText: String = "تجديد الآن",
    dismissText: String = "لاحقًا",
    loading: Boolean = false
) {
    if (!visible) return

    // استخدمنا AlertDialog لأنها أكثر استقراراً في AndroidIDE
    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        ),
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = AppDimens.Component.dialogMaxWidth),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = AppShapeTokens.dialog,
        // العنوان مع الأيقونة والخط الملون
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // الشريط الملون العلوي للتمييز
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(WarningColor, AppShapeTokens.buttonPill)
                )
                
                Spacer(modifier = Modifier.height(AppDimens.Spacing.medium))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = WarningColor.copy(alpha = 0.12f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = WarningColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(AppDimens.Spacing.small))
                    Text(
                        text = title,
                        style = AppTypography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        // نص الرسالة
        text = {
            Text(
                text = message,
                style = AppTypography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        // الأزرار
        confirmButton = {
            AppButton(
                text = renewText,
                onClick = onRenewClick,
                variant = AppButtonVariant.Primary,
                size = AppButtonSize.Medium,
                loading = loading,
                fullWidth = true
            )
        },
        dismissButton = {
            AppButton(
                text = dismissText,
                onClick = onDismissRequest,
                variant = AppButtonVariant.Text,
                size = AppButtonSize.Medium,
                enabled = !loading,
                fullWidth = true
            )
        }
    )
}
