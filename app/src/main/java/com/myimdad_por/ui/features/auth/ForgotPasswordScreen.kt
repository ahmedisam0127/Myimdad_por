package com.myimdad_por.ui.features.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myimdad_por.core.base.UiState
import com.myimdad_por.ui.components.AppButton
import com.myimdad_por.ui.components.AppButtonVariant
import com.myimdad_por.ui.components.AppPasswordTextField
import com.myimdad_por.ui.components.AppTextField
import com.myimdad_por.ui.components.ErrorState
import com.myimdad_por.ui.components.ErrorStateStyle
import com.myimdad_por.ui.components.LoadingIndicator
import com.myimdad_por.ui.components.LoadingIndicatorSize
import com.myimdad_por.ui.components.LoadingIndicatorVariant
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.AppTypography
import com.myimdad_por.ui.theme.BrandPrimary
import com.myimdad_por.ui.theme.TextPrimaryColor
import com.myimdad_por.ui.theme.TextSecondaryColor

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val step = uiState.step

    BackHandler {
        when (step) {
            ForgotPasswordStep.EnterIdentifier -> onBack()
            ForgotPasswordStep.EnterOtp,
            ForgotPasswordStep.ResetPassword,
            ForgotPasswordStep.Success -> viewModel.onEvent(ForgotPasswordUiEvent.GoBack)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = AppDimens.Layout.screenPadding,
                    vertical = AppDimens.Layout.screenPadding
                ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.large)
        ) {
            ForgotPasswordHeader(
                step = step,
                onBack = {
                    when (step) {
                        ForgotPasswordStep.EnterIdentifier -> onBack()
                        ForgotPasswordStep.EnterOtp,
                        ForgotPasswordStep.ResetPassword,
                        ForgotPasswordStep.Success -> viewModel.onEvent(ForgotPasswordUiEvent.GoBack)
                    }
                }
            )

            ForgotPasswordProgress(step = step)

            when {
                uiState.isLoading -> {
                    LoadingIndicator(
                        variant = LoadingIndicatorVariant.Circular,
                        size = LoadingIndicatorSize.Medium,
                        title = when (step) {
                            ForgotPasswordStep.EnterIdentifier -> "جاري إرسال رمز التحقق"
                            ForgotPasswordStep.EnterOtp -> "جاري التحقق من الرمز"
                            ForgotPasswordStep.ResetPassword -> "جاري حفظ كلمة المرور الجديدة"
                            ForgotPasswordStep.Success -> "تمت العملية"
                        },
                        message = "يرجى الانتظار"
                    )
                }

                uiState.isError && !uiState.errorMessage.isNullOrBlank() -> {
                    ErrorState(
                        message = uiState.errorMessage.orEmpty(),
                        title = "تعذر إكمال العملية",
                        details = when (step) {
                            ForgotPasswordStep.EnterIdentifier ->
                                "تحقق من البريد الإلكتروني أو اسم المستخدم ثم أعد المحاولة."

                            ForgotPasswordStep.EnterOtp ->
                                "تأكد من رمز التحقق ثم حاول مرة أخرى."

                            ForgotPasswordStep.ResetPassword ->
                                "تأكد من كلمة المرور الجديدة وتأكيدها."

                            ForgotPasswordStep.Success -> null
                        },
                        retryText = "إعادة المحاولة",
                        dismissText = "إغلاق",
                        onRetry = { viewModel.onEvent(ForgotPasswordUiEvent.Validate) },
                        onDismiss = { viewModel.onEvent(ForgotPasswordUiEvent.ClearError) },
                        style = ErrorStateStyle.Card
                    )
                }

                step == ForgotPasswordStep.EnterIdentifier -> {
                    IdentifierStep(
                        identifier = uiState.identifier,
                        errorText = uiState.identifierError,
                        canSubmit = uiState.canSubmit,
                        onIdentifierChange = {
                            viewModel.onEvent(ForgotPasswordUiEvent.IdentifierChanged(it))
                        },
                        onSubmit = { viewModel.onEvent(ForgotPasswordUiEvent.SubmitIdentifier) }
                    )
                }

                step == ForgotPasswordStep.EnterOtp -> {
                    OtpStep(
                        otpCode = uiState.otpCode,
                        errorText = uiState.otpCodeError,
                        canSubmit = uiState.canSubmit,
                        onOtpChange = {
                            viewModel.onEvent(ForgotPasswordUiEvent.OtpCodeChanged(it))
                        },
                        onSubmit = { viewModel.onEvent(ForgotPasswordUiEvent.SubmitOtpCode) },
                        onResend = { viewModel.onEvent(ForgotPasswordUiEvent.SubmitIdentifier) }
                    )
                }

                step == ForgotPasswordStep.ResetPassword -> {
                    ResetPasswordStep(
                        newPassword = uiState.newPassword,
                        confirmPassword = uiState.confirmPassword,
                        newPasswordError = uiState.newPasswordError,
                        confirmPasswordError = uiState.confirmPasswordError,
                        canSubmit = uiState.canSubmit,
                        onNewPasswordChange = {
                            viewModel.onEvent(ForgotPasswordUiEvent.NewPasswordChanged(it))
                        },
                        onConfirmPasswordChange = {
                            viewModel.onEvent(ForgotPasswordUiEvent.ConfirmPasswordChanged(it))
                        },
                        onSubmit = { viewModel.onEvent(ForgotPasswordUiEvent.SubmitNewPassword) }
                    )
                }

                step == ForgotPasswordStep.Success -> {
                    SuccessStep(
                        email = uiState.identifier,
                        onContinue = onComplete,
                        onReset = { viewModel.onEvent(ForgotPasswordUiEvent.ResetForm) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ForgotPasswordHeader(
    step: ForgotPasswordStep,
    onBack: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
        ) {
            HeaderBackButton(onClick = onBack)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "استعادة كلمة المرور",
                    style = AppTypography.headlineSmall,
                    color = TextPrimaryColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = when (step) {
                        ForgotPasswordStep.EnterIdentifier ->
                            "أدخل بريدك أو اسم المستخدم لإرسال رمز التحقق"

                        ForgotPasswordStep.EnterOtp ->
                            "أدخل رمز التحقق الذي وصلك"

                        ForgotPasswordStep.ResetPassword ->
                            "أنشئ كلمة مرور جديدة قوية"

                        ForgotPasswordStep.Success ->
                            "تم تغيير كلمة المرور بنجاح"
                    },
                    style = AppTypography.bodyMedium,
                    color = TextSecondaryColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HeaderBackButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Icon(
            imageVector = Icons.Filled.ArrowBack,
            contentDescription = null,
            tint = TextPrimaryColor
        )
    }
}

@Composable
private fun ForgotPasswordProgress(step: ForgotPasswordStep) {
    val progress = when (step) {
        ForgotPasswordStep.EnterIdentifier -> 0.33f
        ForgotPasswordStep.EnterOtp -> 0.66f
        ForgotPasswordStep.ResetPassword -> 1f
        ForgotPasswordStep.Success -> 1f
    }

    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
        ) {
            ProgressChip(
                title = "البيانات",
                active = step == ForgotPasswordStep.EnterIdentifier,
                completed = step != ForgotPasswordStep.EnterIdentifier,
                modifier = Modifier.weight(1f)
            )
            ProgressChip(
                title = "الرمز",
                active = step == ForgotPasswordStep.EnterOtp,
                completed = step == ForgotPasswordStep.ResetPassword ||
                    step == ForgotPasswordStep.Success,
                modifier = Modifier.weight(1f)
            )
            ProgressChip(
                title = "كلمة المرور",
                active = step == ForgotPasswordStep.ResetPassword,
                completed = step == ForgotPasswordStep.Success,
                modifier = Modifier.weight(1f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .matchParentSize()
                    .background(BrandPrimary)
            )
        }
    }
}

@Composable
private fun ProgressChip(
    title: String,
    active: Boolean,
    completed: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        completed -> BrandPrimary
        active -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when {
        completed -> MaterialTheme.colorScheme.onPrimary
        active -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> TextSecondaryColor
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = backgroundColor,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (completed) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = title,
                style = AppTypography.labelMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun IdentifierStep(
    identifier: String,
    errorText: String?,
    canSubmit: Boolean,
    onIdentifierChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.large)) {
        StepCard(
            icon = Icons.Filled.Email,
            title = "الخطوة الأولى",
            subtitle = "اكتب البريد الإلكتروني أو اسم المستخدم المرتبط بالحساب"
        )

        AppTextField(
            value = identifier,
            onValueChange = onIdentifierChange,
            label = "البريد الإلكتروني أو اسم المستخدم",
            placeholder = "example@domain.com",
            errorText = errorText,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = { onSubmit() }
            ),
            leadingIcon = { Icon(imageVector = Icons.Filled.Email, contentDescription = null) },
            contentDescription = "حقل البريد الإلكتروني أو اسم المستخدم"
        )

        AppButton(
            text = "إرسال رمز التحقق",
            onClick = onSubmit,
            enabled = canSubmit,
            fullWidth = true,
            leadingIcon = { Icon(imageVector = Icons.Filled.Shield, contentDescription = null) }
        )
    }
}

@Composable
private fun OtpStep(
    otpCode: String,
    errorText: String?,
    canSubmit: Boolean,
    onOtpChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onResend: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.large)) {
        StepCard(
            icon = Icons.Filled.Numbers,
            title = "الخطوة الثانية",
            subtitle = "أدخل رمز التحقق المرسل إليك"
        )

        AppTextField(
            value = otpCode,
            onValueChange = onOtpChange,
            label = "رمز التحقق",
            placeholder = "••••",
            errorText = errorText,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = { onSubmit() }
            ),
            leadingIcon = { Icon(imageVector = Icons.Filled.Numbers, contentDescription = null) },
            contentDescription = "حقل رمز التحقق"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
        ) {
            AppButton(
                text = "إعادة الإرسال",
                onClick = onResend,
                variant = AppButtonVariant.Outlined,
                modifier = Modifier.weight(1f)
            )
            AppButton(
                text = "التحقق",
                onClick = onSubmit,
                enabled = canSubmit,
                modifier = Modifier.weight(1f),
                leadingIcon = { Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun ResetPasswordStep(
    newPassword: String,
    confirmPassword: String,
    newPasswordError: String?,
    confirmPasswordError: String?,
    canSubmit: Boolean,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.large)) {
        StepCard(
            icon = Icons.Filled.Lock,
            title = "الخطوة الثالثة",
            subtitle = "اختر كلمة مرور جديدة قوية وآمنة"
        )

        AppPasswordTextField(
            value = newPassword,
            onValueChange = onNewPasswordChange,
            label = "كلمة المرور الجديدة",
            placeholder = "••••••••",
            errorText = newPasswordError,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            contentDescription = "حقل كلمة المرور الجديدة"
        )

        AppPasswordTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = "تأكيد كلمة المرور",
            placeholder = "••••••••",
            errorText = confirmPasswordError,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = { onSubmit() }
            ),
            contentDescription = "حقل تأكيد كلمة المرور"
        )

        PasswordTipsCard()

        AppButton(
            text = "حفظ كلمة المرور الجديدة",
            onClick = onSubmit,
            enabled = canSubmit,
            fullWidth = true,
            leadingIcon = { Icon(imageVector = Icons.Filled.Lock, contentDescription = null) }
        )
    }
}

@Composable
private fun SuccessStep(
    email: String,
    onContinue: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(AppDimens.Radius.large),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(AppDimens.Layout.screenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(BrandPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    text = "تم تحديث كلمة المرور بنجاح",
                    style = AppTypography.titleLarge,
                    color = TextPrimaryColor,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (email.isNotBlank()) {
                        "تمت العملية للحساب: $email"
                    } else {
                        "تمت العملية بنجاح"
                    },
                    style = AppTypography.bodyMedium,
                    color = TextSecondaryColor,
                    textAlign = TextAlign.Center
                )
            }
        }

        AppButton(
            text = "متابعة",
            onClick = onContinue,
            fullWidth = true,
            leadingIcon = { Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null) }
        )

        AppButton(
            text = "إعادة ضبط النموذج",
            onClick = onReset,
            variant = AppButtonVariant.Outlined,
            fullWidth = true
        )
    }
}

@Composable
private fun StepCard(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(AppDimens.Radius.large),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
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
                    .size(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(BrandPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BrandPrimary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTypography.titleMedium,
                    color = TextPrimaryColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    style = AppTypography.bodyMedium,
                    color = TextSecondaryColor,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PasswordTipsCard() {
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
                text = "نصائح لكلمة مرور قوية",
                style = AppTypography.titleSmall,
                color = TextPrimaryColor,
                fontWeight = FontWeight.SemiBold
            )

            TipRow(text = "استخدم 8 أحرف على الأقل")
            TipRow(text = "ادمج بين الأحرف الكبيرة والصغيرة")
            TipRow(text = "أضف أرقامًا أو رموزًا لزيادة الأمان")
        }
    }
}

@Composable
private fun TipRow(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(6.dp)
                .clip(RoundedCornerShape(50))
                .background(BrandPrimary)
        )

        Text(
            text = text,
            style = AppTypography.bodyMedium,
            color = TextSecondaryColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}