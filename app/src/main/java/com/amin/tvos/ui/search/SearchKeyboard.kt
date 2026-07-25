package com.amin.tvos.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amin.tvos.ui.components.FocusableCard
import com.amin.tvos.ui.theme.CinemaRed

/**
 * On-screen keyboard for TV search.
 *
 * Deliberately not a Compose text field: a native field would summon the Android TV system
 * IME, which is exactly the input path this project avoids. Every key is a normal focusable
 * card, so the remote's DPAD and a USB mouse both work with no IME involved.
 */
@Composable
fun SearchKeyboard(
    persian: Boolean,
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onToggleLanguage: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = if (persian) PERSIAN_ROWS else LATIN_ROWS
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DIGITS.forEach { key -> KeyCap(key) { onKey(key) } }
        }
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key -> KeyCap(key) { onKey(key) } }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WideKey(if (persian) "ENG" else "فارسی", onToggleLanguage)
            WideKey("فاصله", { onKey(" ") })
            FocusableCard(shape = RoundedCornerShape(10.dp), onClick = onBackspace) {
                Icon(
                    Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp)
                )
            }
            WideKey("پاک", onClear)
            FocusableCard(shape = RoundedCornerShape(10.dp), onClick = onSubmit) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = CinemaRed)
                    Spacer(Modifier.width(8.dp))
                    Text("جستجو", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun KeyCap(label: String, onClick: () -> Unit) {
    FocusableCard(shape = RoundedCornerShape(10.dp), onClick = onClick) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun WideKey(label: String, onClick: () -> Unit) {
    FocusableCard(shape = RoundedCornerShape(10.dp), onClick = onClick) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp)
        )
    }
}

private val DIGITS = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")

private val LATIN_ROWS = listOf(
    listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
    listOf("a", "s", "d", "f", "g", "h", "j", "k", "l", "-"),
    listOf("z", "x", "c", "v", "b", "n", "m", ":", "'", ".")
)

private val PERSIAN_ROWS = listOf(
    listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح"),
    listOf("ج", "چ", "ش", "س", "ی", "ب", "ل", "ا", "ت", "ن"),
    listOf("م", "ک", "گ", "پ", "ظ", "ط", "ز", "ر", "ذ", "د"),
    listOf("و", "ژ", "آ", "أ", "ء", "ئ", "ة", "ؤ", "،", "؟")
)
