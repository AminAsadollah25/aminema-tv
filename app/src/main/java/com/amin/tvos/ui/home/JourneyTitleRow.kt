package com.amin.tvos.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.amin.tvos.data.ContentMetadataPolicy
import com.amin.tvos.data.model.CanonicalMedia
import com.amin.tvos.data.model.TitleMetadata
import com.amin.tvos.ui.components.FocusableCard
import com.amin.tvos.ui.theme.*

internal fun journeyArtworkSources(media:CanonicalMedia?, metadata:Map<String,TitleMetadata>):List<JourneyArtworkSource> =
    media?.let { canonical ->
        (listOf(canonical.representative)+canonical.variants.map { it.item }).distinctBy { it.contentUrl }
            .flatMap { item ->
                val detail=metadata[ContentMetadataPolicy.canonicalContentUrl(item.contentUrl)]?.detailPosterUrl.orEmpty()
                listOf(detail,item.posterUrl).filter { it.isNotBlank() }.distinct()
                    .map { JourneyArtworkSource(it,item.contentUrl) }
            }.distinctBy { it.image }.take(4)
    }.orEmpty()

/** Consistent portrait, generous copy, fixed image footprint and visible offline design. */
@Composable
internal fun JourneyTitleRow(
    title:String, subtitle:String, identity:String, group:String,
    media:CanonicalMedia?, metadata:Map<String,TitleMetadata>, onClick:()->Unit
) {
    val colors=remember(identity,group) { journeyColors(identity,group) }
    val sources=remember(media,metadata) { journeyArtworkSources(media,metadata) }
    FocusableCard(onClick=onClick) {
        Row(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(colors.base,SurfaceElevated)))
            .padding(18.dp), verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(22.dp)) {
            JourneyArtwork(title,identity,group,Modifier.width(88.dp).height(132.dp),sources,portrait=true)
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(title,color=TextPrimary,style=MaterialTheme.typography.titleLarge,maxLines=2,overflow=TextOverflow.Ellipsis)
                Text(subtitle,color=TextPrimary.copy(alpha=.80f),style=MaterialTheme.typography.bodyMedium,
                    maxLines=2,overflow=TextOverflow.Ellipsis)
                Text(if(media==null) "تطبیق قطعی هنوز پیدا نشده" else
                    "${media.variants.map { it.providerId }.distinct().size} منبع · اطلاعات و پخش",
                    color=TextPrimary.copy(alpha=.84f),style=MaterialTheme.typography.labelLarge)
            }
            Text(if(media==null) "بررسی عنوان" else "جزئیات  ›",color=TextPrimary,
                style=MaterialTheme.typography.labelLarge)
        }
    }
}
