package com.aifinancial.mobile.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aifinancial.mobile.ui.capture.CaptureScreen
import com.aifinancial.mobile.ui.overview.OverviewScreen
import com.aifinancial.mobile.ui.pairing.PairingScreen
import com.aifinancial.mobile.ui.session.SessionViewModel

private object Routes {
    const val PAIRING = "pairing"
    const val CAPTURE = "capture"
    const val OVERVIEW = "overview"
}

// Only these two routes get the bottom nav bar — pairing is a full-screen,
// one-time flow with nothing yet to switch between.
private val bottomNavRoutes = listOf(Routes.CAPTURE, Routes.OVERVIEW)

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

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val showBottomNav = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentBackStackEntry?.destination?.hierarchy?.any { it.route == Routes.CAPTURE } == true,
                        onClick = {
                            navController.navigate(Routes.CAPTURE) { popUpTo(Routes.CAPTURE) { inclusive = true } }
                        },
                        // Text, not androidx.compose.material.icons.Icons: that
                        // artifact isn't a dependency of this module, and
                        // isn't worth adding just for two nav-bar glyphs.
                        icon = { Text("📷") },
                        label = { Text("Capture") },
                    )
                    NavigationBarItem(
                        selected = currentBackStackEntry?.destination?.hierarchy?.any { it.route == Routes.OVERVIEW } == true,
                        onClick = {
                            navController.navigate(Routes.OVERVIEW) { popUpTo(Routes.CAPTURE) }
                        },
                        icon = { Text("📋") },
                        label = { Text("Overview") },
                    )
                }
            }
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = if (startPaired) Routes.CAPTURE else Routes.PAIRING,
            modifier = Modifier.padding(contentPadding),
        ) {
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
            composable(Routes.OVERVIEW) {
                OverviewScreen()
            }
        }
    }
}
