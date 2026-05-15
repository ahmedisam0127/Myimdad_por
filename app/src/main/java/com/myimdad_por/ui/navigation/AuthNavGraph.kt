package com.myimdad_por.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun AuthNavGraph(
    navController: NavHostController,
    startDestination: String = ScreenRoutes.Splash,
    splashScreen: @Composable () -> Unit,
    onboardingScreen: @Composable () -> Unit,
    loginScreen: @Composable () -> Unit,
    registerScreen: @Composable () -> Unit,
    forgotPasswordScreen: @Composable () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(ScreenRoutes.Splash) {
            splashScreen()
        }

        composable(ScreenRoutes.Onboarding) {
            onboardingScreen()
        }

        composable(ScreenRoutes.Login) {
            loginScreen()
        }

        composable(ScreenRoutes.Register) {
            registerScreen()
        }

        composable(ScreenRoutes.ForgotPassword) {
            forgotPasswordScreen()
        }
    }
}