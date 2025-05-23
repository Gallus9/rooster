package com.example.rooster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.parse.ParseUser

@Composable
fun authScreen(navController: NavController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Farmer") }
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
        Text(text = if (isLogin) "Login" else "Register", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(autoCorrect = false),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = role == "Farmer",
                onClick = { role = "Farmer" },
            )
            Text("Farmer")
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(
                selected = role == "Consumer",
                onClick = { role = "Consumer" },
            )
            Text("Consumer")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }
        Button(
            onClick = {
                loading = true
                errorMessage = ""
                if (isLogin) {
                    ParseUser.logInInBackground(username, password) { user, e ->
                        loading = false
                        if (user != null) {
                            val userRole = user.getString("role") ?: "Unknown"
                            AnalyticsTracker.trackLogin(userRole)
                            navController.navigate("community") { popUpTo(0) }
                        } else {
                            errorMessage = e?.localizedMessage ?: "Login failed."
                        }
                    }
                } else {
                    val user = ParseUser()
                    user.username = username
                    user.setPassword(password)
                    user.put("role", role)
                    user.signUpInBackground { e ->
                        loading = false
                        if (e == null) {
                            AnalyticsTracker.trackLogin(role)
                            navController.navigate("community") { popUpTo(0) }
                        } else {
                            errorMessage = e.localizedMessage ?: "Registration failed."
                        }
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (isLogin) "Login" else "Register")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { isLogin = !isLogin }) {
            Text(if (isLogin) "Don't have an account? Register" else "Already have an account? Login")
        }
    }
}
