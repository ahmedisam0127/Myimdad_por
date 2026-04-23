package com.myimdad_por.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.AppShapeTokens
import com.myimdad_por.ui.theme.AppTypography
import com.myimdad_por.ui.theme.ErrorColor
import com.myimdad_por.ui.theme.TextPrimaryColor
import com.myimdad_por.ui.theme.TextSecondaryColor

enum class AppTextFieldVariant {
    Filled,
    Outlined
}

enum class AppTextFieldSize(
    val minHeight: Dp,
    val verticalPadding: Dp,
    val textStyle: TextStyle
) {
    Small(
        minHeight = 48.dp,
        verticalPadding = 10.dp,
        textStyle = AppTypography.bodyMedium
    ),
    Medium(
        minHeight = 56.dp,
        verticalPadding = 12.dp,
        textStyle = AppTypography.bodyLarge
    ),
    Large(
        minHeight = 64.dp,
        verticalPadding = 14.dp,
        textStyle = AppTypography.bodyLarge
    )
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    leadingIcon: (@Composable (() -> Unit))? = null,
    trailingIcon: (@Composable (() -> Unit))? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordVisibilityChange: ((Boolean) -> Unit)? = null,
    variant: AppTextFieldVariant = AppTextFieldVariant.Outlined,
    size: AppTextFieldSize = AppTextFieldSize.Medium,
    contentDescription: String? = null
) {
    val isError = !errorText.isNullOrBlank()
    val visualTransformation = when {
        isPassword && !passwordVisible -> PasswordVisualTransformation()
        else -> VisualTransformation.None
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = size.minHeight)
                .semantics {
                    if (!contentDescription.isNullOrBlank()) {
                        this.contentDescription = contentDescription
                    }
                    if (isError && !errorText.isNullOrBlank()) {
                        error(errorText)
                    }
                },
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            textStyle = size.textStyle.copy(color = TextPrimaryColor),
            label = label?.let { text ->
                {
                    Text(
                        text = text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            placeholder = placeholder?.let { text ->
                {
                    Text(
                        text = text,
                        style = AppTypography.bodyMedium,
                        color = TextSecondaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            leadingIcon = leadingIcon,
            trailingIcon = {
                when {
                    isPassword -> {
                        val clickAction = onPasswordVisibilityChange
                        TextButton(
                            onClick = {
                                if (clickAction != null) {
                                    clickAction(!passwordVisible)
                                }
                            }
                        ) {
                            Text(
                                text = if (passwordVisible) "إخفاء" else "إظهار",
                                style = AppTypography.labelLarge
                            )
                        }
                    }

                    trailingIcon != null -> trailingIcon()

                    else -> Unit
                }
            },
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = AppShapeTokens.textField,
            colors = when (variant) {
                AppTextFieldVariant.Filled -> OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    errorBorderColor = ErrorColor,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = TextSecondaryColor,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = TextPrimaryColor,
                    unfocusedTextColor = TextPrimaryColor,
                    disabledTextColor = TextPrimaryColor.copy(alpha = 0.5f),
                    errorTextColor = ErrorColor,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f)
                )

                AppTextFieldVariant.Outlined -> OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    errorBorderColor = ErrorColor,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = TextSecondaryColor,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = TextPrimaryColor,
                    unfocusedTextColor = TextPrimaryColor,
                    disabledTextColor = TextPrimaryColor.copy(alpha = 0.5f),
                    errorTextColor = ErrorColor,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            },
            isError = isError
        )

        FieldSupportText(
            helperText = helperText,
            errorText = errorText
        )
    }
}

@Composable
fun AppPasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "كلمة المرور",
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    variant: AppTextFieldVariant = AppTextFieldVariant.Outlined,
    size: AppTextFieldSize = AppTextFieldSize.Medium,
    contentDescription: String? = null
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    AppTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        helperText = helperText,
        errorText = errorText,
        enabled = enabled,
        readOnly = readOnly,
        isPassword = true,
        passwordVisible = passwordVisible,
        onPasswordVisibilityChange = { passwordVisible = it },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        variant = variant,
        size = size,
        contentDescription = contentDescription
    )
}

@Composable
private fun FieldSupportText(
    helperText: String?,
    errorText: String?
) {
    val message = when {
        !errorText.isNullOrBlank() -> errorText
        !helperText.isNullOrBlank() -> helperText
        else -> null
    } ?: return

    val color = if (!errorText.isNullOrBlank()) ErrorColor else TextSecondaryColor

    Text(
        text = message,
        style = AppTypography.bodySmall,
        color = color,
        modifier = Modifier.padding(
            top = AppDimens.Spacing.extraSmall,
            start = AppDimens.Spacing.small,
            end = AppDimens.Spacing.small
        ),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}