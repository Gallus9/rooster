package com.example.rooster

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ErrorBoundary(
    onRetry: (() -> Unit)? = null,
    fallbackTitle: String = "Something went wrong",
    fallbackMessage: String = "We're working to fix this issue. Please try again.",
    content: @Composable () -> Unit,
) {
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    if (hasError) {
        ErrorScreen(
            title = fallbackTitle,
            message = if (errorMessage.isNotEmpty()) errorMessage else fallbackMessage,
            onRetry = {
                hasError = false
                errorMessage = ""
                onRetry?.invoke()
            },
        )
    } else {
        // Note: Compose doesn't support try-catch around composable calls
        // Error handling should be done within individual composables
        content()
    }
}

@Composable
fun ErrorScreen(
    title: String = "Oops! Something went wrong",
    message: String = "We're working to fix this issue. Please try again.",
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = "Error",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (onRetry != null) {
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(0.6f),
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Try Again")
            }
        }
    }
}

// Network error specific screen
@Composable
fun NetworkErrorScreen(
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    ErrorScreen(
        title = "📶 Network Connection Issue",
        message = "Please check your internet connection and try again. Make sure you have a stable network connection.",
        onRetry = onRetry,
        modifier = modifier,
    )
}

// Parse/Backend error specific screen
@Composable
fun BackendErrorScreen(
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    ErrorScreen(
        title = "🔄 Server Connection Issue",
        message = "We're having trouble connecting to our servers. Please try again in a moment.",
        onRetry = onRetry,
        modifier = modifier,
    )
}
