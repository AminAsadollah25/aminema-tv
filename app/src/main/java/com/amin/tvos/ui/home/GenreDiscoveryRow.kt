package com.amin.tvos.ui.home

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.amin.tvos.R
import com.amin.tvos.data.DiscoveryCategory
import com.amin.tvos.ui.components.FocusableCard
import com.amin.tvos.ui.components.SectionRow
import com.amin.tvos.ui.theme.TextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.ref.SoftReference

private object GenreAtlasCache {
    @Volatile private var cached: SoftReference<ImageBitmap>? = null
    private val mutex = Mutex()
    fun peek(): ImageBitmap? = cached?.get()
    suspend fun load(resources: android.content.res.Resources): ImageBitmap? = mutex.withLock {
        peek() ?: BitmapFactory.decodeResource(resources, R.drawable.aminema_genre_atlas,
            BitmapFactory.Options().apply { inScaled = false })?.asImageBitmap()?.also {
            cached = SoftReference(it) // Reuse on Back; reclaimable under memory pressure.
        }
    }
}

/** One shared ~6MB atlas, decoded off Main only when this lazy Home row is composed. */
@Composable
fun GenreDiscoveryRow(onSelect: (DiscoveryCategory) -> Unit, onFocused: () -> Unit) {
    val resources = LocalContext.current.resources
    val atlas by produceState<ImageBitmap?>(GenreAtlasCache.peek(), resources) {
        value = withContext(Dispatchers.IO) {
            GenreAtlasCache.load(resources)
        }
    }
    SectionRow(title = "امروز چه حال‌وهوایی داری؟", items = DiscoveryCategory.entries.filter { it != DiscoveryCategory.MARATHON },
        key = { it.name }) { category ->
        FocusableCard(
            modifier = Modifier.width(188.dp).semantics { contentDescription = category.label },
            onClick = { onSelect(category) },
            onInteractionFocusChanged = { if (it) onFocused() }
        ) {
            Column {
                Canvas(Modifier.fillMaxWidth().height(146.dp)) {
                    atlas?.let { image ->
                        // The atlas has baked gutters, not equal edge-to-edge cells.
                        // Crop the actual illustration bounds (1448x1086 reference),
                        // excluding both its embedded rounded frame and generated label.
                        val columns = arrayOf(15 to 362, 374 to 718, 730 to 1075, 1087 to 1434)
                        val rows = arrayOf(14 to 282, 366 to 638, 712 to 986)
                        val column = columns[category.atlasIndex % 4]
                        val row = rows[category.atlasIndex / 4]
                        val left = column.first * image.width / 1448
                        val top = row.first * image.height / 1086
                        val width = (column.second - column.first) * image.width / 1448
                        val height = (row.second - row.first) * image.height / 1086
                        val targetRatio = size.width / size.height
                        val cropWidth = minOf(width, (height * targetRatio).toInt())
                        val cropHeight = minOf(height, (width / targetRatio).toInt())
                        // The generated labels are not UI text: render only the illustration,
                        // then use the app font below for crisp scalable Persian and semantics.
                        drawImage(image,
                            srcOffset = IntOffset(left + (width - cropWidth) / 2,
                                top + (height - cropHeight) / 2),
                            srcSize = IntSize(cropWidth, cropHeight),
                            dstSize = IntSize(size.width.toInt(), size.height.toInt()))
                    }
                }
                Text(category.label, color = TextPrimary, textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
            }
        }
    }
}
