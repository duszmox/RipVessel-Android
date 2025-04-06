package hu.cock.ripvessel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import hu.cock.ripvessel.home.ListScreen
import hu.cock.ripvessel.login.LoginScreen
import hu.cock.ripvessel.nav.AppNavigation
import hu.cock.ripvessel.ui.theme.RIPVesselTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RIPVesselTheme {
                val navController = rememberNavController()
                AppNavigation(navController)
            }
        }
    }
}