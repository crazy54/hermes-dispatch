package com.nousresearch.hermes.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nousresearch.hermes.ui.screens.ChatScreen
import com.nousresearch.hermes.ui.screens.ChannelManagerScreen
import com.nousresearch.hermes.ui.screens.ConnectScreen
import com.nousresearch.hermes.ui.screens.CronTaskScreen
import com.nousresearch.hermes.ui.screens.PairingScreen
import com.nousresearch.hermes.ui.screens.SessionListScreen
import com.nousresearch.hermes.viewmodel.ConnectionViewModel

object Routes {
    const val CONNECT  = "connect"
    const val PAIRING  = "pairing"
    const val SESSIONS = "sessions"
    const val CRON     = "cron"
    const val CHANNELS = "channels"
    const val CHAT = "chat/{sessionId}?profile={profile}"
    fun chat(sessionId: String = "new", profile: String = "default") =
        "chat/$sessionId?profile=$profile"
}

@Composable
fun HermesApp() {
    val navController = rememberNavController()
    val connectionVm: ConnectionViewModel = hiltViewModel()
    val connectionState by connectionVm.state.collectAsState()

    val startDestination = if (connectionState.isConnected) Routes.SESSIONS else Routes.CONNECT

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.CONNECT) {
            ConnectScreen(
                onConnected = { navController.navigate(Routes.SESSIONS) {
                    popUpTo(Routes.CONNECT) { inclusive = true }
                }},
                onOpenPairing = { navController.navigate(Routes.PAIRING) },
            )
        }
        composable(Routes.PAIRING) {
            PairingScreen(
                onPaired = {
                    navController.navigate(Routes.SESSIONS) {
                        popUpTo(Routes.CONNECT) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Routes.SESSIONS) {
            val sessionVm: com.nousresearch.hermes.viewmodel.SessionViewModel = hiltViewModel()
            SessionListScreen(
                onSessionSelected = { sessionId ->
                    navController.navigate(Routes.chat(sessionId))
                },
                onNewChat = {
                    val profile = sessionVm.state.value.selectedProfile
                    navController.navigate(Routes.chat("new", profile))
                },
                onDisconnect = {
                    connectionVm.disconnect()
                    navController.navigate(Routes.CONNECT) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onOpenCron = { navController.navigate(Routes.CRON) },
                onOpenChannels = { navController.navigate(Routes.CHANNELS) },
                sessionViewModel = sessionVm,
            )
        }
        composable(Routes.CRON) {
            CronTaskScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.CHANNELS) {
            ChannelManagerScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            Routes.CHAT,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType },
                navArgument("profile") { type = NavType.StringType; defaultValue = "default" },
            ),
        ) { backStack ->
            val sessionId = backStack.arguments?.getString("sessionId") ?: "new"
            val profile   = backStack.arguments?.getString("profile") ?: "default"
            ChatScreen(
                sessionId      = sessionId,
                initialProfile = profile,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
