package com.amin.tvos.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.amin.tvos.ui.theme.CinemaRed
import com.amin.tvos.ui.theme.TextSecondary

/**
 * TV-friendly error screen: No Internet / Service unavailable / Login expired.
 * Big text, big retry button, DPAD-focusable.
 */
@Composable
fun ErrorScreen(
    title: String,
    message: String,
    retryLabel: String = "Retry",
    onRetry: () -> Unit,
    onHome: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.CloudOff,
            contentDescription = null,
            tint = CinemaRed,
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        FocusableCard(onClick = onRetry) {
            Text(
                "▶  $retryLabel",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 14.dp)
            )
        }
        if (onHome != null) {
            Spacer(Modifier.height(14.dp))
            FocusableCard(onClick = onHome) {
                Text(
                    "Back to Home",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                )
            }
        }
    }
}
