package hu.cock.ripvessel.nav

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import hu.cock.ripvessel.home.ListScreen
import hu.cock.ripvessel.login.LoginScreen

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
            ListScreen()
        }
    }
}