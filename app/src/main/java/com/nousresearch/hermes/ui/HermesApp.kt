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
import com.nousresearch.hermes.ui.screens.SessionListScreen
import com.nousresearch.hermes.viewmodel.ConnectionViewModel

object Routes {
    const val CONNECT = "connect"
    const val SESSIONS = "sessions"
    const val CRON = "cron"
    const val CHANNELS = "channels"
    const val CHAT = "chat/{sessionId}"
    fun chat(sessionId: String = "new") = "chat/$sessionId"
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
                }}
            )
        }
        composable(Routes.SESSIONS) {
            SessionListScreen(
                onSessionSelected = { sessionId ->
                    navController.navigate(Routes.chat(sessionId))
                },
                onNewChat = {
                    navController.navigate(Routes.chat("new"))
                },
                onDisconnect = {
                    connectionVm.disconnect()
                    navController.navigate(Routes.CONNECT) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onOpenCron = { navController.navigate(Routes.CRON) },
                onOpenChannels = { navController.navigate(Routes.CHANNELS) },
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
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
        ) { backStack ->
            val sessionId = backStack.arguments?.getString("sessionId") ?: "new"
            ChatScreen(
                sessionId = sessionId,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
