package com.aifinancial.mobile.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aifinancial.mobile.ui.capture.CaptureScreen
import com.aifinancial.mobile.ui.pairing.PairingScreen

private object Routes {
    const val PAIRING = "pairing"
    const val CAPTURE = "capture"
}

@Composable
fun AppNavHost(startPaired: Boolean, navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = if (startPaired) Routes.CAPTURE else Routes.PAIRING) {
        composable(Routes.PAIRING) {
            PairingScreen(
                onPaired = {
                    navController.navigate(Routes.CAPTURE) {
                        popUpTo(Routes.PAIRING) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.CAPTURE) {
            CaptureScreen()
        }
    }
}
