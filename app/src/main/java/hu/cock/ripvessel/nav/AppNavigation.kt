package hu.cock.ripvessel.nav

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import hu.cock.ripvessel.home.HomeScreen
import hu.cock.ripvessel.login.LoginScreen
import hu.cock.ripvessel.creator.CreatorScreen

@Composable
fun AppNavigation(navController: NavHostController, startDestination: String = "login") {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(onLoginSuccess = { user ->
                Log.d("LoginScreen", "User logged in: $user")
                navController.navigate("list")
            })
        }
        composable("list") {
            HomeScreen(
                onLogout = {
                    // Navigate back to the login screen when user logs out
                    navController.navigate("login") {
                        // Clear the back stack to prevent going back to the home screen
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToCreator = { creatorId, channelId ->
                    navController.navigate("creator/$creatorId/$channelId")
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
                navController.navigate("list") {
                    popUpTo("list") { inclusive = true }
                }
            }
        }
    }
}