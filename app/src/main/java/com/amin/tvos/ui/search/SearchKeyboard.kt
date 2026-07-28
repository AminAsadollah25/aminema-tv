package com.amin.tvos.ui.search

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.amin.tvos.ui.theme.CinemaRed
import com.amin.tvos.ui.theme.SurfaceDark
import com.amin.tvos.ui.theme.SurfaceElevated
import com.amin.tvos.ui.theme.TextPrimary
import com.amin.tvos.ui.theme.TextSecondary

/**
 * Aminema's native TV search deck.
 *
 * It deliberately avoids a Compose text field so Android TV's system IME never covers the
 * screen. Every key remains a real mouse and DPAD target, but the deck now reads as one
 * coherent input surface instead of a loose collection of differently sized cards.
 */
@Composable
fun SearchKeyboard(
    query: String,
    persian: Boolean,
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onToggleLanguage: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = if (persian) PERSIAN_QWERTY_ROWS else LATIN_QWERTY_ROWS
    val canSearch = query.trim().length >= 2

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SurfaceDark.copy(alpha = 0.98f),
        contentColor = TextPrimary,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
        shadowElevation = 18.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SearchDisplay(query = query, persian = persian)

            // Keep the familiar staggered QWERTY geometry. Re-wrapping the Persian alphabet
            // into equal ten-key rows looks tidy but destroys physical-keyboard muscle memory.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    EqualKeyRow(DIGITS, onKey)
                    rows.forEach { row -> QwertyKeyRow(row, onKey) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DeckKey(
                            label = if (persian) "English" else "فارسی",
                            modifier = Modifier.weight(1.35f),
                            tone = KeyTone.SECONDARY,
                            onClick = onToggleLanguage
                        )
                        if (persian) {
                            DeckKey(
                                label = "ژ",
                                modifier = Modifier.weight(0.64f),
                                onClick = { onKey("ژ") }
                            )
                            DeckKey(
                                label = "آ",
                                modifier = Modifier.weight(0.64f),
                                onClick = { onKey("آ") }
                            )
                        }
                        DeckKey(
                            label = "فاصله",
                            modifier = Modifier.weight(if (persian) 2.15f else 3.15f),
                            tone = KeyTone.SPACE,
                            onClick = { onKey(" ") }
                        )
                        DeckKey(
                            label = "",
                            modifier = Modifier.weight(1f),
                            contentDescription = "پاک کردن حرف آخر",
                            tone = KeyTone.SECONDARY,
                            onClick = onBackspace
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = null,
                                modifier = Modifier.size(25.dp)
                            )
                        }
                        DeckKey(
                            label = "پاک‌کردن",
                            modifier = Modifier.weight(1.35f),
                            enabled = query.isNotEmpty(),
                            tone = KeyTone.SECONDARY,
                            onClick = onClear
                        )
                        DeckKey(
                            label = "جستجو",
                            modifier = Modifier.weight(1.75f),
                            enabled = canSearch,
                            contentDescription = "شروع جستجو",
                            tone = KeyTone.PRIMARY,
                            onClick = onSubmit
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(23.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "جستجو",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchDisplay(query: String, persian: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        color = Color.Black.copy(alpha = 0.32f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (query.isBlank()) {
                Color.White.copy(alpha = 0.08f)
            } else {
                CinemaRed.copy(alpha = 0.62f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(CinemaRed.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = CinemaRed,
                    modifier = Modifier.size(23.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = query.ifBlank { "نام فیلم یا سریال را بنویسید…" },
                modifier = Modifier.weight(1f),
                color = if (query.isBlank()) TextSecondary else TextPrimary,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (query.isNotBlank()) {
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "${query.length}/60",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.width(16.dp))
            Surface(
                color = Color.White.copy(alpha = 0.07f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Text(
                    text = if (persian) "فا" else "EN",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun EqualKeyRow(keys: List<String>, onKey: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        keys.forEach { key ->
            DeckKey(
                label = key,
                modifier = Modifier.weight(1f),
                onClick = { onKey(key) }
            )
        }
    }
}

@Composable
private fun QwertyKeyRow(row: QwertyRow, onKey: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (row.edgeInset > 0f) {
            Spacer(Modifier.weight(row.edgeInset))
        }
        row.keys.forEach { key ->
            DeckKey(
                label = key,
                modifier = Modifier.weight(1f),
                onClick = { onKey(key) }
            )
        }
        if (row.edgeInset > 0f) {
            Spacer(Modifier.weight(row.edgeInset))
        }
    }
}

private enum class KeyTone { NORMAL, SECONDARY, SPACE, PRIMARY }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeckKey(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = label,
    tone: KeyTone = KeyTone.NORMAL,
    onClick: () -> Unit,
    content: (@Composable () -> Unit)? = null
) {
    var dpadFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val highlighted = enabled && (dpadFocused || hovered)

    val restingColor = when (tone) {
        KeyTone.NORMAL -> SurfaceElevated
        KeyTone.SECONDARY -> Color(0xFF292933)
        KeyTone.SPACE -> Color(0xFF24242E)
        KeyTone.PRIMARY -> CinemaRed
    }
    val focusedColor = when (tone) {
        KeyTone.PRIMARY -> Color(0xFFFF2630)
        else -> Color(0xFF383844)
    }
    val background by animateColorAsState(
        targetValue = if (highlighted) focusedColor else restingColor,
        animationSpec = tween(120),
        label = "searchKeyColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (highlighted) 1.045f else 1f,
        animationSpec = tween(120),
        label = "searchKeyScale"
    )

    Surface(
        modifier = modifier
            .height(39.dp)
            .alpha(if (enabled) 1f else 0.34f)
            .scale(scale)
            .onFocusChanged { dpadFocused = it.isFocused || it.hasFocus }
            .hoverable(interactionSource)
            .combinedClickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        color = background,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(11.dp),
        border = BorderStroke(
            if (highlighted) 2.dp else 1.dp,
            if (highlighted) Color.White.copy(alpha = 0.88f)
            else Color.White.copy(alpha = 0.055f)
        ),
        shadowElevation = if (highlighted) 9.dp else 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (content != null) {
                content()
            } else {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (tone == KeyTone.PRIMARY) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private val DIGITS = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")

private data class QwertyRow(
    val keys: List<String>,
    val edgeInset: Float = 0f
)

private val LATIN_QWERTY_ROWS = listOf(
    QwertyRow(listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")),
    QwertyRow(listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"), edgeInset = 0.5f),
    QwertyRow(listOf("z", "x", "c", "v", "b", "n", "m"), edgeInset = 1.5f)
)

private val PERSIAN_QWERTY_ROWS = listOf(
    QwertyRow(listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج", "چ")),
    QwertyRow(listOf("ش", "س", "ی", "ب", "ل", "ا", "ت", "ن", "م", "ک", "گ"), edgeInset = 0.5f),
    QwertyRow(listOf("ظ", "ط", "ز", "ر", "ذ", "د", "پ", "و", ".", "؟"), edgeInset = 1f)
)
