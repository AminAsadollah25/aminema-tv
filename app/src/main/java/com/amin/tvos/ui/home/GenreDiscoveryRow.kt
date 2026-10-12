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
    SectionRow(title = "امروز چه حال‌وهوایی داری؟", items = DiscoveryCategory.entries,
        key = { it.name }) { category ->
        FocusableCard(
            modifier = Modifier.width(188.dp).semantics { contentDescription = category.label },
            onClick = { onSelect(category) },
            onInteractionFocusChanged = { if (it) onFocused() }
        ) {
            Column {
                Canvas(Modifier.fillMaxWidth().height(146.dp)) {
                    atlas?.let { image ->
                        val tileWidth = image.width / 4
                        val tileHeight = image.height / 3
                        // The generated labels are not UI text: render only the illustration,
                        // then use the app font below for crisp scalable Persian and semantics.
                        drawImage(image,
                            srcOffset = IntOffset((category.atlasIndex % 4) * tileWidth,
                                (category.atlasIndex / 4) * tileHeight),
                            srcSize = IntSize(tileWidth, (tileHeight * 0.78f).toInt()),
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
