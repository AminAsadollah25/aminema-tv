package com.amin.tvos.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.amin.tvos.data.*
import com.amin.tvos.ui.components.FocusableCard
import com.amin.tvos.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun MarathonAtlasGrid(
    families:List<MarathonFamily>,state:LazyGridState,groupId:String,lastOpened:String,query:String,onQuery:(String)->Unit,
    referenceCounts:Map<String,Int>,records:List<JourneyLookupRecord>,error:String,
    onGroup:(String)->Unit,onBack:()->Unit,onSelectReference:(MarathonFamily)->Unit,onDiscover:(String)->Unit
) {
    val initialFocus=remember {FocusRequester()}
    var keyboard by remember { mutableStateOf(false) }
    var persian by remember { mutableStateOf(true) }
    val visible=remember(families,groupId,query) { families.filter {
        (groupId.isBlank() || groupId in it.groups) && MarathonAtlas.matchesFilter(it,query) } }
    val representatives by produceState<Map<String,com.amin.tvos.data.model.CatalogItem>>(emptyMap(),records) {
        value=withContext(Dispatchers.Default) {
            records.groupBy { it.key.removePrefix("atlas:").substringBefore(':') }.mapNotNull { (id,rows) ->
                rows.flatMap { it.items }.firstOrNull { it.posterUrl.isNotBlank() }?.let { id to it }
            }.toMap()
        }
    }
    val focusId=lastOpened.takeIf { id -> visible.any { it.id==id } }.orEmpty()
    LaunchedEffect(visible.isNotEmpty()) { if(visible.isNotEmpty()) { withFrameNanos {};runCatching {initialFocus.requestFocus()} } }
    BackHandler(onBack=onBack)
    if(keyboard)androidx.compose.ui.window.Dialog(onDismissRequest={keyboard=false},
        properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)) {
        androidx.compose.material3.Surface(color=Ink,modifier=Modifier.widthIn(max=900.dp).fillMaxWidth()) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                FocusableCard(onClick={keyboard=false}) { Text("بستن و دیدن نتیجه‌ها",Modifier.padding(14.dp),color=TextPrimary) }
                com.amin.tvos.ui.search.SearchKeyboard(query,persian,
                    onKey={if(query.length<80)onQuery(query+it)},onBackspace={onQuery(query.dropLast(1))},
                    onClear={onQuery("")},onToggleLanguage={persian=!persian},onSubmit={keyboard=false})
            }
        }
    }
    LazyVerticalGrid(GridCells.Fixed(3),state=state,modifier=Modifier.fillMaxSize().background(Ink),
        contentPadding=PaddingValues(48.dp,24.dp),horizontalArrangement=Arrangement.spacedBy(20.dp),
        verticalArrangement=Arrangement.spacedBy(20.dp)) {
        item(key="header",span={GridItemSpan(maxLineSpan)}) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(24.dp)) {
                FocusableCard(if(focusId.isBlank())Modifier.focusRequester(initialFocus) else Modifier,onClick=onBack) {
                    Text("بازگشت به خانه",Modifier.padding(18.dp),color=TextPrimary)
                }
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text("ماراتون سینمایی",color=TextPrimary,style=MaterialTheme.typography.headlineMedium)
                    Text("${families.size} خانوادهٔ پژوهشی · ${MarathonAtlas.groups.size} سبک · یک مجموعه، یک انتخاب",
                        color=TextSecondary,style=MaterialTheme.typography.bodyMedium)
                }
                FocusableCard(Modifier.widthIn(max=180.dp),onClick={keyboard=true}) {
                    Text(query.ifBlank {"جستجوی مجموعه"},Modifier.padding(16.dp),color=TextPrimary,
                        maxLines=1,overflow=TextOverflow.Ellipsis)
                }
                if(query.isNotBlank())FocusableCard(onClick={onQuery("")}) { Text("پاک‌کردن",Modifier.padding(16.dp),color=TextPrimary) }
            }
        }
        item(key="groups",span={GridItemSpan(maxLineSpan)}) {
            LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(6.dp)) {
                items(listOf(MarathonGroup("","همهٔ مجموعه‌ها",0f))+MarathonAtlas.groups,key={it.id}) { group ->
                    FocusableCard(onClick={onGroup(group.id)}) {
                        Text((if(group.id==groupId)"✓ " else "")+group.title,
                            Modifier.background(if(group.id==groupId)CinemaRed.copy(alpha=.28f) else SurfaceElevated)
                                .padding(horizontal=18.dp,vertical=14.dp),color=TextPrimary,
                            style=MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        item(key="scope",span={GridItemSpan(maxLineSpan)}) {
            Text(if(error.isNotBlank())error else
                "${visible.size} خانواده · «مرجع انتشار» = فهرست فعلی ممیزی‌شده · «کاوش» = نتایج مرتبط سایت‌ها، نه آرشیو کامل",
                color=TextSecondary,style=MaterialTheme.typography.bodyMedium)
        }
        if(families.isEmpty())item(key="loading",span={GridItemSpan(maxLineSpan)}) {
            JourneyArtwork("اطلس سینما","atlas","scifi",Modifier.fillMaxWidth().height(150.dp))
        }
        if(families.isNotEmpty() && visible.isEmpty())item(key="no-filter-result",span={GridItemSpan(maxLineSpan)}) {
            Text("برای این فیلتر مجموعه‌ای پیدا نشد؛ عبارت را کوتاه‌تر کن یا گروه «همه» را انتخاب کن.",color=TextPrimary)
        }
        items(count=visible.size,key={visible[it].id},contentType={"family"}) { index ->
            val family=visible[index]
            val group=family.groups.first()
            val colors=remember(family.id) {journeyColors(family.id,group)}
            val poster=representatives[family.id]
            val artwork=remember(poster) {poster?.let {listOf(JourneyArtworkSource(it.posterUrl,it.contentUrl))}.orEmpty()}
            FocusableCard(if(focusId==family.id)Modifier.focusRequester(initialFocus) else Modifier,
                onClick={if(family.referenceId.isNotBlank())onSelectReference(family) else onDiscover(family.id)}) {
                Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(colors.base,SurfaceElevated)))) {
                    Box {
                        JourneyArtwork(family.title,family.id,group,Modifier.fillMaxWidth().aspectRatio(1.8f),
                            sources=artwork,localResource=MarathonArtwork.forCollection(family.referenceId))
                        Text(if(family.referenceId.isNotBlank())"مرجع انتشار" else "کاوش مجموعه",
                            Modifier.align(Alignment.TopStart).padding(12.dp).background(Ink.copy(alpha=.86f))
                                .padding(horizontal=10.dp,vertical=6.dp),color=TextPrimary,style=MaterialTheme.typography.labelSmall)
                    }
                    Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.width(36.dp).height(3.dp).background(colors.accent))
                        Text(family.title,color=TextPrimary,style=MaterialTheme.typography.titleLarge,
                            minLines=2,maxLines=2,overflow=TextOverflow.Ellipsis)
                        Text(if(family.referenceId.isNotBlank())"${referenceCounts[family.referenceId]?:0} اثر/فصل در فهرست فعلی" else
                            MarathonAtlas.groups.first {it.id==group}.title,
                            color=TextPrimary.copy(alpha=.82f),style=MaterialTheme.typography.bodyMedium,
                            maxLines=1,overflow=TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
