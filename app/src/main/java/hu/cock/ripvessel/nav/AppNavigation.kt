package hu.cock.ripvessel.nav

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import hu.cock.ripvessel.channels.ChannelsScreen
import hu.cock.ripvessel.creator.CreatorScreen
import hu.cock.ripvessel.home.HomeScreen
import hu.cock.ripvessel.login.LoginScreen
import hu.cock.ripvessel.profile.ProfileScreen

sealed class BottomNavItem(val route: String, val icon: @Composable () -> Unit, val label: String) {
    object Recents : BottomNavItem("recents", { Icon(Icons.Default.Home, contentDescription = "Recents") }, "Recents")
    object Channels : BottomNavItem("channels", { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Channels") }, "Channels")
    object Profile : BottomNavItem("profile", { Icon(Icons.Default.Person, contentDescription = "Profile") }, "Profile")
}

@Composable
fun AppNavigation(navController: NavHostController, startDestination: String = "login") {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf(BottomNavItem.Recents, BottomNavItem.Channels, BottomNavItem.Profile)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute != "login") {
                NavigationBar {
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = item.icon,
                            label = { Text(item.label) },
                            selected = currentRoute == item.route,
                            onClick = {
                                selectedItem = index
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            composable("login") {
                LoginScreen(onLoginSuccess = { user ->
                    Log.d("LoginScreen", "User logged in: $user")
                    navController.navigate("recents") {
                        popUpTo("login") { inclusive = true }
                    }
                })
            }
            
            composable("recents") {
                HomeScreen(
                    onLogout = {
                        navController.navigate("login") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToCreator = { creatorId, channelId ->
                        navController.navigate("creator/$creatorId/$channelId")
                    }
                )
            }

            composable("channels") {
                ChannelsScreen() { creatorId, channelId ->
                    navController.navigate("creator/$creatorId/$channelId")
                }
            }

            composable("profile") {
                ProfileScreen(
                    onLogout = {
                        navController.navigate("login") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }

            composable("creator/{creatorId}/{channelId}") { backStackEntry ->
                val creatorId = backStackEntry.arguments?.getString("creatorId") ?: ""
                val channelId = backStackEntry.arguments?.getString("channelId") ?: ""

                CreatorScreen(
                    creatorId = creatorId,
                    channelId = channelId
                ) {
                    navController.navigate("recents") {
                        popUpTo("recents") { inclusive = true }
                    }
                }
            }
        }
    }
}
