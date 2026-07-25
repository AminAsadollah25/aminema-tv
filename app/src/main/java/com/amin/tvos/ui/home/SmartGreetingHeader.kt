package com.amin.tvos.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amin.tvos.ui.components.FocusableCard
import com.amin.tvos.ui.theme.CinemaRed
import com.amin.tvos.ui.theme.TextSecondary

/**
 * The Home greeting: what time it is, what day it is, and one thing to do about it.
 *
 * The action chip is the whole point of the row — the greeting suggests something and the
 * user can act on it in one click — so it is only rendered when the underlying data can
 * actually serve it. The shuffle button asks for another take on the same moment.
 */
@Composable
fun SmartGreetingHeader(
    greeting: SmartGreeting,
    onAction: (GreetingAction) -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(horizontal = 48.dp)
    ) {
        AnimatedContent(
            targetState = greeting,
            transitionSpec = {
                fadeIn(tween(320)) togetherWith fadeOut(tween(200))
            },
            label = "greeting"
        ) { current ->
            Column {
                Text(current.headline, style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.width(6.dp))
                Text(
                    current.subline,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (greeting.action != GreetingAction.NONE && greeting.actionLabel.isNotBlank()) {
                FocusableCard(
                    shape = RoundedCornerShape(50),
                    onClick = { onAction(greeting.action) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = CinemaRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            greeting.actionLabel,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
            FocusableCard(shape = RoundedCornerShape(50), onClick = onShuffle) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "پیشنهاد دیگر",
                    modifier = Modifier.padding(12.dp).size(20.dp)
                )
            }
        }
    }
}
