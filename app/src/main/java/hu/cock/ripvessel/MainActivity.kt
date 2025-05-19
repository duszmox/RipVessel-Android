package hu.cock.ripvessel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import hu.cock.ripvessel.nav.AppNavigation
import hu.cock.ripvessel.ui.theme.RIPVesselTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sessionManager = SessionManager(applicationContext)
        val user = sessionManager.getUser()
        val startDestination = if (user != null) "recents" else "login"
        setContent {
            RIPVesselTheme {
                val navController = rememberNavController()
                AppNavigation(navController, startDestination = startDestination)
            }
        }
    }
}