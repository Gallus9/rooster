package com.example.rooster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.rooster.ui.theme.RoosterTheme
import com.parse.ParseUser

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoosterTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = if (isUserLoggedIn()) "main" else "auth") {
                    composable("auth") { authScreen(navController) }
                    composable("main") { MainContent(onLogout = { recreate() }) }
                    composable("community") { CommunityFeedScreen() }
                    composable("fowl") { FowlScreen() }
                    composable("marketplace") { MarketplaceScreen() }
                    composable("profile") { ProfileScreen(onLogout = { recreate() }) }
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
    var selectedScreen by remember { mutableStateOf(0) }
    Scaffold(
        bottomBar = { BottomNavigationBar(navController = rememberNavController(), onLogout = onLogout) },
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            val navController2 = rememberNavController()
            NavHost(navController = navController2, startDestination = "community") {
                composable("community") { CommunityFeedScreen() }
                composable("fowl") { FowlScreen() }
                composable("marketplace") { MarketplaceScreen() }
                composable("profile") { ProfileScreen(onLogout = onLogout) }
                composable("transferVerification/{orderId}") { backStackEntry ->
                    val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                    TransferVerificationScreen(orderId = orderId, onVerified = { navController2.popBackStack() })
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    navController: NavController,
    onLogout: () -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Community") },
            label = { Text("Community") },
            selected = true,
            onClick = {
                navController.navigate("community")
            },
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Pets, contentDescription = "Fowl") },
            label = { Text("Fowl") },
            selected = false,
            onClick = {
                navController.navigate("fowl")
            },
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Store, contentDescription = "Marketplace") },
            label = { Text("Marketplace") },
            selected = false,
            onClick = {
                navController.navigate("marketplace")
            },
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            selected = false,
            onClick = {
                navController.navigate("profile")
            },
        )
    }
}

@Composable
fun Greeting(
    name: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Hello $name!",
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RoosterTheme {
        Greeting("Android")
    }
}
