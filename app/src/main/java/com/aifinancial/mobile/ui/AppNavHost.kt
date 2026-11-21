package com.aifinancial.mobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aifinancial.mobile.ui.capture.CaptureScreen
import com.aifinancial.mobile.ui.pairing.PairingScreen
import com.aifinancial.mobile.ui.session.SessionViewModel

private object Routes {
    const val PAIRING = "pairing"
    const val CAPTURE = "capture"
}

@Composable
fun AppNavHost(
    startPaired: Boolean,
    navController: NavHostController = rememberNavController(),
    sessionViewModel: SessionViewModel = hiltViewModel(),
) {
    // Stage 2's "a failed refresh ... routes back to pairing"
    // (specs/architecture/overview.md): the only place in the UI layer that
    // reacts to session expiry, regardless of which screen is open when it
    // happens.
    LaunchedEffect(sessionViewModel) {
        sessionViewModel.expiryEvents.collect {
            navController.navigate(Routes.PAIRING) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

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
