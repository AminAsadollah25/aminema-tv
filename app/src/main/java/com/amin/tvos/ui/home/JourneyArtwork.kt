package com.amin.tvos.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.amin.tvos.data.MarathonAtlas
import com.amin.tvos.ui.components.authenticatedPosterModel
import kotlin.math.cos
import kotlin.math.sin

data class JourneyArtworkSource(val image: String, val page: String)
internal data class JourneyColors(val base: Color, val accent: Color)
internal fun journeyColors(id: String, group: String): JourneyColors {
    val knownHue = mapOf("middleearth" to 43f, "starwars" to 220f, "dc" to 350f,
        "potter" to 210f, "bond" to 215f, "mission" to 25f, "wick" to 190f,
        "diehard" to 20f, "bourne" to 200f, "fast" to 270f, "rocky" to 42f, "madmax" to 28f)
    val hash=id.hashCode().toUInt().let {it xor (it shr 16)}.toLong()
    val hue = knownHue[id] ?: ((MarathonAtlas.groups.firstOrNull { it.id == group }?.hue ?: 215f) +
        (hash % 65521).toFloat()/65520f*40f-20f+360f) % 360f
    return JourneyColors(Color.hsv(hue, .55f, .20f), Color.hsv(hue, .68f, .70f))
}

/** A composed graphic exists from the first frame through loading/error/offline states.
 * No request on focus, no blur, no remote fallback and no placeholder black rectangle.
 * Each alternate image keeps its own normal page for the existing same-host-cookie rule. */
@Composable
fun JourneyArtwork(
    title: String, identity: String, group: String, modifier: Modifier = Modifier,
    sources: List<JourneyArtworkSource> = emptyList(), localResource: Int? = null,
    portrait: Boolean = false
) {
    val colors = remember(identity, group) { journeyColors(identity, group) }
    val candidates = remember(sources) { sources.filter { it.image.isNotBlank() }.distinctBy { it.image }.take(4) }
    var candidate by remember(candidates, localResource) { mutableIntStateOf(0) }
    var loaded by remember(candidates, localResource, candidate) { mutableStateOf(false) }
    val context = LocalContext.current
    val width = if (portrait) 240 else 720
    val height = if (portrait) 360 else 400
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(
        listOf(colors.accent.copy(alpha = .84f), colors.base, colors.base)))) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val center = Offset(w * .77f, h * .40f)
            val radius = minOf(w, h) * .43f
            when (group) {
                "scifi", "anime", "games" -> {
                    drawCircle(colors.accent.copy(alpha = .40f), radius, center)
                    drawCircle(Color.White.copy(alpha = .24f), radius * 1.13f, center, style = Stroke(2.dp.toPx()))
                    repeat(13) { n ->
                        val x = ((n * 73 + identity.hashCode().and(255)) % 331) / 331f * w
                        val y = ((n * 37 + 11) % 193) / 193f * h
                        drawCircle(Color.White.copy(alpha = .36f), if (n % 3 == 0) 2.dp.toPx() else 1.dp.toPx(), Offset(x, y))
                    }
                }
                "fantasy", "adventure", "martial" -> {
                    drawCircle(colors.accent.copy(alpha = .42f), radius * .75f, center)
                    val p = Path().apply { moveTo(0f,h); lineTo(w*.40f,h*.24f); lineTo(w*.67f,h*.78f)
                        lineTo(w*.86f,h*.40f); lineTo(w,h*.72f); lineTo(w,h); close() }
                    drawPath(p, colors.base.copy(alpha = .68f))
                }
                "horror" -> {
                    drawCircle(colors.accent.copy(alpha = .45f), radius, center)
                    drawCircle(colors.base.copy(alpha = .75f), radius*.86f, center + Offset(radius*.40f,-radius*.23f))
                    repeat(7) { n -> drawLine(Color.White.copy(alpha=.10f),Offset(w*(n/7f),h),
                        Offset(w*(n/7f)+w*.12f,h*.52f), 2.dp.toPx()) }
                }
                "action", "comic", "sport" -> repeat(7) { n ->
                    drawLine(colors.accent.copy(alpha = .13f + n*.025f),
                        Offset(w*(n*.21f)-w*.2f,0f),Offset(w*(n*.21f)-w*.5f,h), 18.dp.toPx())
                }
                else -> {
                    drawCircle(colors.accent.copy(alpha = .32f), radius, center)
                    drawCircle(Color.White.copy(alpha = .16f), radius*.90f, center, style = Stroke(2.dp.toPx()))
                    repeat(5) { n -> val a = n * Math.PI * 2 / 5
                        drawCircle(colors.base.copy(alpha = .72f),radius*.22f,
                            center + Offset((cos(a)*radius*.54).toFloat(),(sin(a)*radius*.54).toFloat())) }
                }
            }
        }
        Column(Modifier.align(Alignment.CenterStart).padding(if (portrait) 10.dp else 22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val initials = remember(title) { title.split(Regex("\\s+")).filter { it.isNotBlank() }
                .take(2).joinToString("") { it.take(1) }.uppercase() }
            Text(initials, color=Color.White.copy(alpha=.82f), fontSize=if(portrait) 34.sp else 48.sp,
                fontWeight=FontWeight.Bold, maxLines=1)
            if (!portrait) Text("AMINEMA · COLLECTION",color=Color.White.copy(alpha=.66f),
                style=MaterialTheme.typography.labelSmall)
        }
        val source = candidates.getOrNull(candidate)
        val model = if (localResource != null) remember(localResource) {
            ImageRequest.Builder(context).data(localResource).size(width,height).crossfade(false).build()
        } else source?.let { authenticatedPosterModel(it.image,it.page,width,height) }
        if (model != null && (localResource != null || source != null)) AsyncImage(
            model=model, contentDescription=null, modifier=Modifier.fillMaxSize().alpha(if(loaded)1f else 0f),
            contentScale=if(portrait) ContentScale.Fit else ContentScale.Crop,
            onSuccess={ loaded=true },
            onError={ error ->
                if(com.amin.tvos.BuildConfig.DEBUG) android.util.Log.d("JourneyArtwork",
                    "image-error:"+ when(val reason=error.result.throwable) {
                        is coil.network.HttpException -> "HTTP${reason.response.code}"
                        else -> reason.javaClass.simpleName
                    })
                if(localResource==null && candidate<candidates.lastIndex)candidate+=1
            })
    }
}
