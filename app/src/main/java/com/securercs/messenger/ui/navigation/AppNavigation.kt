package com.securercs.messenger.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.securercs.messenger.data.repository.MessengerRepository
import com.securercs.messenger.ui.screens.chat.ChatScreen
import com.securercs.messenger.ui.screens.contacts.ContactsScreen
import com.securercs.messenger.ui.screens.conversations.ConversationsScreen
import com.securercs.messenger.ui.screens.login.LoginScreen
import com.securercs.messenger.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Conversations : Screen("conversations")
    object Chat : Screen("chat/{conversationId}") {
        fun createRoute(conversationId: String) = "chat/$conversationId"
    }
    object Contacts : Screen("contacts")
    object Settings : Screen("settings")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val connectedServices = MessengerRepository.getConnectedServices()
    val startDestination = if (connectedServices.isEmpty()) Screen.Login.route else Screen.Conversations.route

    var currentRoute by remember { mutableStateOf(startDestination) }

    Column(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.weight(1f)
        ) {
            composable(Screen.Login.route) {
                currentRoute = Screen.Login.route
                LoginScreen(
                    onLoginComplete = {
                        navController.navigate(Screen.Conversations.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Conversations.route) {
                currentRoute = Screen.Conversations.route
                ConversationsScreen(
                    onConversationClick = { conversationId ->
                        navController.navigate(Screen.Chat.createRoute(conversationId))
                    }
                )
            }

            composable(Screen.Chat.route) { backStackEntry ->
                currentRoute = Screen.Chat.route
                val conversationId = backStackEntry.arguments?.getString("conversationId") ?: return@composable
                ChatScreen(
                    conversationId = conversationId,
                    onBackClick = { navController.navigateUp() }
                )
            }

            composable(Screen.Contacts.route) {
                currentRoute = Screen.Contacts.route
                ContactsScreen()
            }

            composable(Screen.Settings.route) {
                currentRoute = Screen.Settings.route
                SettingsScreen()
            }
        }

        // Bottom Navigation Bar (nur für Haupt-Screens, nicht für Login/Chat)
        if (currentRoute != Screen.Login.route && currentRoute != Screen.Chat.route) {
            BottomNavigationBar(
                currentRoute = currentRoute,
                onNavigate = { newRoute ->
                    navController.navigate(newRoute) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Chat, contentDescription = "Chats") },
            label = { Text("Chats") },
            selected = currentRoute == Screen.Conversations.route,
            onClick = { onNavigate(Screen.Conversations.route) }
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Kontakte") },
            label = { Text("Kontakte") },
            selected = currentRoute == Screen.Contacts.route,
            onClick = { onNavigate(Screen.Contacts.route) }
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Einstellungen") },
            label = { Text("Einstellungen") },
            selected = currentRoute == Screen.Settings.route,
            onClick = { onNavigate(Screen.Settings.route) }
        )
    }
}
