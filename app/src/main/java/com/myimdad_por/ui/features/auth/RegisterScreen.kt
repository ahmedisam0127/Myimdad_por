package com.myimdad_por.ui.features.auth

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
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Work
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myimdad_por.core.base.UiState
import com.myimdad_por.domain.model.Role
import com.myimdad_por.ui.components.AppButton
import com.myimdad_por.ui.components.AppButtonSize
import com.myimdad_por.ui.components.AppButtonVariant
import com.myimdad_por.ui.components.AppPasswordTextField
import com.myimdad_por.ui.components.AppTextField
import com.myimdad_por.ui.components.ErrorState
import com.myimdad_por.ui.components.ErrorStateStyle
import com.myimdad_por.ui.components.LoadingIndicator
import com.myimdad_por.ui.components.LoadingIndicatorSize
import com.myimdad_por.ui.components.LoadingIndicatorVariant
import com.myimdad_por.ui.theme.AppDimens

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    onRegisterSuccess: (() -> Unit)? = null,
    viewModel: RegisterViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.registrationState) {
        when (val state = uiState.registrationState) {
            is UiState.Success<*> -> onRegisterSuccess?.invoke()
            else -> Unit
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            RegisterBackground()

            RegisterContent(
                uiState = uiState,
                onFullNameChanged = viewModel::onFullNameChanged,
                onUsernameChanged = viewModel::onUsernameChanged,
                onEmailChanged = viewModel::onEmailChanged,
                onPhoneChanged = viewModel::onPhoneNumberChanged,
                onPasswordChanged = viewModel::onPasswordChanged,
                onConfirmPasswordChanged = viewModel::onConfirmPasswordChanged,
                onRoleSelected = viewModel::onRoleSelected,
                onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
                onToggleConfirmPasswordVisibility = viewModel::toggleConfirmPasswordVisibility,
                onSubmit = viewModel::submit,
                onClearError = viewModel::clearError,
                onNavigateToLogin = onNavigateToLogin,
                onNavigateBack = onNavigateBack,
                modifier = Modifier.fillMaxSize()
            )

            if (uiState.isSubmitting) {
                LoadingIndicator(
                    modifier = Modifier.fillMaxSize(),
                    variant = LoadingIndicatorVariant.Circular,
                    size = LoadingIndicatorSize.Medium,
                    title = "جاري إنشاء الحساب",
                    message = "يرجى الانتظار قليلاً"
                )
            }
        }
    }
}

@Composable
private fun RegisterBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .padding(top = 20.dp, end = 16.dp)
                .size(118.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .align(Alignment.TopEnd)
        )

        Box(
            modifier = Modifier
                .padding(top = 140.dp, start = 12.dp)
                .size(74.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f))
                .align(Alignment.TopStart)
        )

        Box(
            modifier = Modifier
                .padding(bottom = 88.dp, start = 24.dp)
                .size(94.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f))
                .align(Alignment.BottomStart)
        )
    }
}

@Composable
private fun RegisterContent(
    uiState: RegisterUiState,
    onFullNameChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onRoleSelected: (Role) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
    onClearError: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showEmployeeComingSoon by remember { mutableStateOf(false) }

    if (showEmployeeComingSoon) {
        AlertDialog(
            onDismissRequest = { showEmployeeComingSoon = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Work,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "هذه الميزة قريبا",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "إنشاء حساب موظف سيكون متاحًا في إصدار قريب من التطبيق.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showEmployeeComingSoon = false }) {
                    Text(text = "حسنًا")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(AppDimens.Layout.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
    ) {
        RegisterHeader(
            onBack = onNavigateBack
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimens.Radius.large),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.Layout.screenPadding),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
            ) {
                SectionBadge(
                    icon = Icons.Filled.Shield,
                    title = "إنشاء حساب جديد",
                    subtitle = "املأ البيانات التالية للبدء باستخدام النظام"
                )

                if (!uiState.errorMessage.isNullOrBlank()) {
                    ErrorState(
                        message = uiState.errorMessage.orEmpty(),
                        title = "تعذر إنشاء الحساب",
                        style = ErrorStateStyle.Inline,
                        onDismiss = onClearError
                    )
                }

                AppTextField(
                    value = uiState.fullName,
                    onValueChange = onFullNameChanged,
                    label = "الاسم الكامل",
                    placeholder = "اكتب الاسم الكامل",
                    errorText = if (uiState.fullName.isBlank()) null else null,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    contentDescription = "حقل الاسم الكامل"
                )

                AppTextField(
                    value = uiState.username,
                    onValueChange = onUsernameChanged,
                    label = "اسم المستخدم / اسم المتجر",
                    placeholder = "اكتب اسم المستخدم",
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    contentDescription = "حقل اسم المستخدم"
                )

                AppTextField(
                    value = uiState.email,
                    onValueChange = onEmailChanged,
                    label = "البريد الإلكتروني",
                    placeholder = "example@domain.com",
                    errorText = if (uiState.email.isBlank() || uiState.isEmailValid) null else "البريد الإلكتروني غير صالح",
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    contentDescription = "حقل البريد الإلكتروني"
                )

                AppTextField(
                    value = uiState.phoneNumber,
                    onValueChange = onPhoneChanged,
                    label = "رقم الهاتف",
                    placeholder = "09xxxxxxxx",
                    errorText = if (uiState.phoneNumber.isBlank() || uiState.isPhoneValid) null else "رقم الهاتف غير صالح",
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    contentDescription = "حقل رقم الهاتف"
                )

                RoleSection(
                    selectedRole = uiState.selectedRole,
                    onManagerSelected = { onRoleSelected(Role.MANAGER) },
                    onEmployeeClicked = {
                        showEmployeeComingSoon = true
                    }
                )

                AppPasswordTextField(
                    value = uiState.password,
                    onValueChange = onPasswordChanged,
                    label = "كلمة المرور",
                    placeholder = "••••••••",
                    errorText = if (uiState.password.isBlank() || uiState.isPasswordValid) null else "كلمة المرور غير صالحة",
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    contentDescription = "حقل كلمة المرور"
                )

                AppPasswordTextField(
                    value = uiState.confirmPassword,
                    onValueChange = onConfirmPasswordChanged,
                    label = "تأكيد كلمة المرور",
                    placeholder = "••••••••",
                    errorText = if (uiState.confirmPassword.isBlank() || uiState.isConfirmPasswordValid) null else "كلمة المرور وتأكيدها غير متطابقين",
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { onSubmit() }
                    ),
                    contentDescription = "حقل تأكيد كلمة المرور"
                )

                AppButton(
                    text = if (uiState.isSubmitting) "جاري الإنشاء" else "إنشاء الحساب",
                    onClick = onSubmit,
                    enabled = uiState.isFormValid && !uiState.isSubmitting,
                    loading = uiState.isSubmitting,
                    fullWidth = true,
                    size = AppButtonSize.Large,
                    variant = AppButtonVariant.Primary
                )

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                AppButton(
                    text = "العودة إلى تسجيل الدخول",
                    onClick = onNavigateToLogin,
                    fullWidth = true,
                    size = AppButtonSize.Medium,
                    variant = AppButtonVariant.Outlined
                )
            }
        }
    }
}

@Composable
private fun RegisterHeader(
    onBack: (() -> Unit)?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
    ) {
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
                imageVector = Icons.Filled.BusinessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp)
            )
        }

        Text(
            text = "MyImdad",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "ابدأ الآن في إنشاء حسابك",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "اختر نوع الحساب وأدخل البيانات المطلوبة",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.92f)
        )

        if (onBack != null) {
            Spacer(modifier = Modifier.height(AppDimens.Spacing.small))
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
        }
    }
}

@Composable
private fun SectionBadge(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RoleSection(
    selectedRole: Role?,
    onManagerSelected: () -> Unit,
    onEmployeeClicked: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)) {
        Text(
            text = "نوع الحساب",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
        ) {
            RoleCard(
                title = "حساب مدير",
                subtitle = "إدارة كاملة للنظام",
                icon = Icons.Filled.Shield,
                selected = selectedRole == Role.MANAGER,
                modifier = Modifier.weight(1f),
                onClick = onManagerSelected
            )

            RoleCard(
                title = "حساب موظف",
                subtitle = "قريبًا",
                icon = Icons.Filled.Work,
                selected = selectedRole == Role.EMPLOYEE,
                modifier = Modifier.weight(1f),
                onClick = onEmployeeClicked,
                comingSoon = true
            )
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    comingSoon: Boolean = false,
    onClick: () -> Unit
) {
    val background = when {
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        comingSoon -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    val borderColor = when {
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        comingSoon -> MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(AppDimens.Radius.large))
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(AppDimens.Radius.large)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(AppDimens.Radius.large),
        color = background,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.70f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (comingSoon) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}