package com.example.rooster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.rooster.ui.theme.RoosterTheme
import com.parse.ParseUser
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoosterTheme {
                RoosterEnthusiastApp()
            }
        }
    }
}

@Composable
fun RoosterEnthusiastApp() {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    var userRole by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isCheckingAuth by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        try {
            val currentUser = ParseUser.getCurrentUser()
            if (currentUser == null) {
                isCheckingAuth = false
                // User not logged in, will show auth screen via the null userRole check later
            } else {
                userRole = currentUser.getString("role") ?: "general" // Default to general if no role
                isCheckingAuth = false
            }
        } catch (e: Exception) {
            error = e.message
            isCheckingAuth = false
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Error checking authentication: ${e.message}")
            }
        }
    }

    if (isCheckingAuth) {
        // Show loading screen while checking authentication
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading Rooster App...")
        }
        return
    }

    // Determine start destination outside NavHost for clarity
    val startDestination = when (userRole) {
        "farmer" -> "home"
        "highLevel" -> "home"
        "general" -> "market"
        else -> "auth" // Default to auth if role is null (not logged in) or unknown
    }

    Scaffold(
        bottomBar = {
            // Only show bottom nav if a user role is determined and it's not the auth screen
            if (userRole != null && startDestination != "auth") {
                BottomNavigationBar(navController, userRole!!)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("auth") { authScreen(navController) } // Ensure authScreen is defined elsewhere
            
            composable("home") {
                // This screen should now only be reached if userRole is "farmer" or "highLevel"
                // as per the startDestination logic.
                when (userRole) {
                    "farmer" -> FarmerHomeScreen() // Ensure FarmerHomeScreen is defined
                    "highLevel" -> HighLevelHomeScreen() // Ensure HighLevelHomeScreen is defined
                    else -> {
                        // This case should ideally not be reached if startDestination is correct.
                        // Fallback or error screen, or navigate to auth if role somehow becomes null.
                        LaunchedEffect(Unit) {
                            navController.navigate("auth") {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                            }
                        }
                    }
                }
            }
            composable("market") { MarketplaceScreen() } // Ensure MarketplaceScreen is defined
            composable("explore") { ExploreScreen() } // Ensure ExploreScreen is defined
            composable("create") { CommunityFeedScreen() } // Ensure CommunityFeedScreen is defined
            composable("cart") { CartScreen() } // Ensure CartScreen is defined
            composable("community") { CommunityScreen() } // Ensure CommunityScreen is defined
            composable("dashboard") { DashboardScreen() } // Ensure DashboardScreen is defined
            composable("transfers") { TransfersScreen() } // Ensure TransfersScreen is defined
            composable("profile") {
                ProfileScreen(onLogout = {
                    ParseUser.logOut()
                    userRole = null // Clear role on logout
                    navController.navigate("auth") {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    }
                })
            }
            composable("transferVerification/{orderId}") { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                TransferVerificationScreen(
                    orderId = orderId,
                    onVerified = { navController.popBackStack() },
                ) // Ensure TransferVerificationScreen is defined
            }
            // Removed the problematic "main" composable route
        }
    }

    error?.let {
        LaunchedEffect(it) {
            snackbarHostState.showSnackbar("Error: $it")
        }
    }
}

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    role: String,
) {
    val items =
        when (role) {
            "general" ->
                listOf(
                    BottomNavItem("market", "Market", Icons.Filled.Store),
                    BottomNavItem("explore", "Explore", Icons.Filled.Search),
                    BottomNavItem("create", "Create", Icons.Filled.Create),
                    BottomNavItem("cart", "Cart", Icons.Filled.ShoppingCart),
                    BottomNavItem("profile", "Profile", Icons.Filled.Person),
                )
            "farmer" ->
                listOf(
                    BottomNavItem("home", "Home", Icons.Filled.Home),
                    BottomNavItem("market", "Market", Icons.Filled.Store),
                    BottomNavItem("create", "Create", Icons.Filled.Create),
                    BottomNavItem("community", "Community", Icons.Filled.Group),
                    BottomNavItem("profile", "Profile", Icons.Filled.Person),
                )
            "highLevel" ->
                listOf(
                    BottomNavItem("home", "Home", Icons.Filled.Home),
                    BottomNavItem("explore", "Explore", Icons.Filled.Search),
                    BottomNavItem("create", "Create", Icons.Filled.Create),
                    BottomNavItem("dashboard", "Dashboard", Icons.Filled.Dashboard),
                    BottomNavItem("transfers", "Transfers", Icons.Filled.SwapHoriz),
                )
            else -> emptyList()
        }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                },
            )
        }
    }
}

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun HighLevelHomeScreen() {
    Text("High-Level Home Screen - Placeholder (Rank Board, Flock Board, Alerts)")
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
