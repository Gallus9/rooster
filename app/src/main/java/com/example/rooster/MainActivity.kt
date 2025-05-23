package com.example.rooster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.rooster.ui.theme.RoosterTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.parse.ParseUser
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoosterTheme {
                if (isUserLoggedIn()) {
                    MainContent(onLogout = { recreate() })
                } else {
                    AuthScreen(onAuthSuccess = { recreate() })
                }
            }
        }
    }
}

fun isUserLoggedIn(): Boolean {
    return ParseUser.getCurrentUser() != null && ParseUser.getCurrentUser().isAuthenticated
}

@Composable
fun MainContent(onLogout: () -> Unit) {
    val navController = rememberNavController()
    var selectedScreen by remember { mutableStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Community") },
                    label = { Text("Community") },
                    selected = selectedScreen == 0,
                    onClick = { selectedScreen = 0; navController.navigate("community") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Pets, contentDescription = "Fowl") },
                    label = { Text("Fowl") },
                    selected = selectedScreen == 1,
                    onClick = { selectedScreen = 1; navController.navigate("fowl") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Store, contentDescription = "Marketplace") },
                    label = { Text("Marketplace") },
                    selected = selectedScreen == 2,
                    onClick = { selectedScreen = 2; navController.navigate("marketplace") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = selectedScreen == 3,
                    onClick = { selectedScreen = 3; navController.navigate("profile") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(navController = navController, startDestination = "community") {
                composable("community") { CommunityFeedScreen() }
                composable("fowl") { FowlScreen() }
                composable("marketplace") { MarketplaceScreen(navController) }
                composable("profile") { ProfileScreen(onLogout = onLogout) }
                composable("transferVerification/{orderId}") { backStackEntry ->
                    val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                    TransferVerificationScreen(orderId = orderId, onVerified = { navController.popBackStack() })
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RoosterTheme {
        Greeting("Android")
    }
}

