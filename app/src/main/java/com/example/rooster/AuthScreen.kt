package com.example.rooster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.parse.ParseUser

@Composable
fun authScreen(navController: NavController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("farmer") } // Fixed: Use lowercase consistent with MainActivity
    var isLogin by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "🐓 Rooster App",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isLogin) "Welcome Back!" else "Join Our Community",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it.trim() },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    autoCorrect = false,
                ),
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
        )

        if (!isLogin) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Select Your Role:", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = role == "farmer",
                        onClick = { role = "farmer" },
                        enabled = !loading,
                    )
                    Text("🌾 Farmer - Manage fowl and participate in markets")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = role == "general",
                        onClick = { role = "general" },
                        enabled = !loading,
                    )
                    Text("🛒 Consumer - Buy fowl and explore markets")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = role == "highLevel",
                        onClick = { role = "highLevel" },
                        enabled = !loading,
                    )
                    Text("📊 Manager - Oversee operations and analytics")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (errorMessage.isNotEmpty()) {
            Text(
                text = "⚠️ $errorMessage",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = {
                // Clear error and validate input
                errorMessage = ""

                if (username.isBlank()) {
                    errorMessage = "Please enter a username"
                    return@Button
                }

                if (password.length < 4) {
                    errorMessage = "Password must be at least 4 characters"
                    return@Button
                }

                loading = true

                if (isLogin) {
                    // Login logic
                    ParseUser.logInInBackground(username, password) { user, e ->
                        loading = false
                        if (user != null) {
                            try {
                                val userRole = user.getString("role") ?: "general"
                                AnalyticsTracker.trackLogin(userRole)

                                // Navigate based on role to prevent crashes
                                navController.navigate("main") {
                                    popUpTo(0) { inclusive = true }
                                }
                            } catch (ex: Exception) {
                                errorMessage =
                                    "Login successful but navigation failed: ${ex.message}"
                            }
                        } else {
                            errorMessage =
                                when {
                                    e?.code == 101 -> "Invalid username or password"
                                    e?.message?.contains("network", ignoreCase = true) == true ->
                                        "Network error. Please check your connection."

                                    else -> e?.localizedMessage ?: "Login failed. Please try again."
                                }
                        }
                    }
                } else {
                    // Registration logic
                    val user = ParseUser()
                    user.username = username
                    user.setPassword(password)
                    user.put("role", role)

                    user.signUpInBackground { e ->
                        loading = false
                        if (e == null) {
                            try {
                                AnalyticsTracker.trackLogin(role)
                                navController.navigate("main") {
                                    popUpTo(0) { inclusive = true }
                                }
                            } catch (ex: Exception) {
                                errorMessage =
                                    "Registration successful but navigation failed: ${ex.message}"
                            }
                        } else {
                            errorMessage =
                                when {
                                    e.code == 202 -> "Username already taken. Please choose another."
                                    e.message?.contains("network", ignoreCase = true) == true ->
                                        "Network error. Please check your connection."

                                    else ->
                                        e.localizedMessage
                                            ?: "Registration failed. Please try again."
                                }
                        }
                    }
                }
            },
            enabled = !loading && username.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 8.dp),
                    strokeWidth = 2.dp,
                )
            }
            Text(if (isLogin) "Sign In" else "Create Account")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = {
                if (!loading) {
                    isLogin = !isLogin
                    errorMessage = ""
                }
            },
            enabled = !loading,
        ) {
            Text(
                if (isLogin) {
                    "Don't have an account? Sign Up"
                } else {
                    "Already have an account? Sign In"
                },
            )
        }

        if (loading) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isLogin) "Signing you in..." else "Creating your account...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
