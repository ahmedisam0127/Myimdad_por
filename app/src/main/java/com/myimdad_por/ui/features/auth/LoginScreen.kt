package com.myimdad_por.ui.features.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myimdad_por.core.base.UiState
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
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLoginSuccess: (() -> Unit)? = null,
    viewModel: LoginViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.state) {
        when (val state = uiState.state) {
            is UiState.Success<*> -> onLoginSuccess?.invoke()
            is UiState.Error -> snackbarHostState.showSnackbar(state.message)
            else -> Unit
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LoginBackground()

            LoginContent(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                onNavigateToRegister = onNavigateToRegister,
                onNavigateToForgotPassword = onNavigateToForgotPassword,
                modifier = Modifier.fillMaxSize()
            )

            if (uiState.isLoading) {
                LoadingIndicator(
                    modifier = Modifier.fillMaxSize(),
                    variant = LoadingIndicatorVariant.Circular,
                    size = LoadingIndicatorSize.Medium,
                    title = "جاري تسجيل الدخول",
                    message = "يرجى الانتظار قليلاً"
                )
            }
        }
    }
}

@Composable
private fun LoginBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .padding(top = 24.dp, end = 20.dp)
                .size(120.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .align(Alignment.TopEnd)
        )

        Box(
            modifier = Modifier
                .padding(top = 150.dp, start = 14.dp)
                .size(72.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f))
                .align(Alignment.TopStart)
        )

        Box(
            modifier = Modifier
                .padding(bottom = 90.dp, end = 28.dp)
                .size(96.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f))
                .align(Alignment.BottomEnd)
        )
    }
}

@Composable
private fun LoginContent(
    uiState: LoginUiState,
    onEvent: (LoginUiEvent) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(AppDimens.Layout.screenPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LoginHeader()

            Spacer(modifier = Modifier.height(AppDimens.Spacing.large))

            LoginCard(
                uiState = uiState,
                onEvent = onEvent,
                onNavigateToRegister = onNavigateToRegister,
                onNavigateToForgotPassword = onNavigateToForgotPassword
            )
        }
    }
}

@Composable
private fun LoginHeader() {
    Column(
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
                imageVector = Icons.Filled.Shield,
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
            text = "مرحباً بك مرة أخرى",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "سجّل دخولك لإدارة أعمالك بسرعة وأمان",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.92f)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MiniBadge(text = "آمن", icon = Icons.Filled.Lock)
            MiniBadge(text = "سريع", icon = Icons.Filled.CheckCircle)
            MiniBadge(text = "منظم", icon = Icons.Filled.Person)
        }
    }
}

@Composable
private fun MiniBadge(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LoginCard(
    uiState: LoginUiState,
    onEvent: (LoginUiEvent) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
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
            HeaderSection()

            AppTextField(
                value = uiState.username,
                onValueChange = { onEvent(LoginUiEvent.UsernameChanged(it)) },
                label = "البريد الإلكتروني أو اسم المستخدم",
                placeholder = "example@domain.com",
                errorText = uiState.usernameError,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                contentDescription = "حقل البريد الإلكتروني أو اسم المستخدم"
            )

            AppPasswordTextField(
                value = uiState.password,
                onValueChange = { onEvent(LoginUiEvent.PasswordChanged(it)) },
                label = "كلمة المرور",
                placeholder = "••••••••",
                errorText = uiState.passwordError,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onDone = { onEvent(LoginUiEvent.Submit) }
                ),
                contentDescription = "حقل كلمة المرور"
            )

            RememberMeRow(
                checked = uiState.rememberMe,
                onCheckedChange = { onEvent(LoginUiEvent.ToggleRememberMe) }
            )

            if (!uiState.errorMessage.isNullOrBlank()) {
                ErrorState(
                    message = uiState.errorMessage.orEmpty(),
                    style = ErrorStateStyle.Inline,
                    title = "تعذر تسجيل الدخول"
                )
            }

            AppButton(
                text = if (uiState.isLoading) "جاري تسجيل الدخول" else "تسجيل الدخول",
                onClick = { onEvent(LoginUiEvent.Submit) },
                enabled = uiState.canSubmit,
                loading = uiState.isLoading,
                fullWidth = true,
                size = AppButtonSize.Large,
                variant = AppButtonVariant.Primary
            )

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

            AppButton(
                text = "إنشاء حساب جديد",
                onClick = onNavigateToRegister,
                fullWidth = true,
                variant = AppButtonVariant.Outlined,
                size = AppButtonSize.Medium
            )

            AppButton(
                text = "نسيت كلمة المرور؟",
                onClick = onNavigateToForgotPassword,
                fullWidth = true,
                variant = AppButtonVariant.Text,
                size = AppButtonSize.Medium
            )
        }
    }
}

@Composable
private fun HeaderSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "تسجيل الدخول",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "ادخل بياناتك للمتابعة إلى لوحة التحكم",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RememberMeRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "تذكرني",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}