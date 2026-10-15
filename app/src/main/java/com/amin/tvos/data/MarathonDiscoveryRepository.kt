package com.amin.tvos.data

import android.content.Context
import android.util.AtomicFile
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** On-demand discovery only; isolated from the verified Journey lookup and Home caches. */
class MarathonDiscoveryRepository(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "marathon_discovery.json"))
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private var loaded = false
    private val _records = MutableStateFlow<List<JourneyLookupRecord>>(emptyList())
    val records = _records.asStateFlow()
    suspend fun load() = withContext(Dispatchers.IO) { mutex.withLock {
        if (!loaded) {
            if (file.baseFile.exists() || File(file.baseFile.path+".bak").exists()) {
                _records.value = json.decodeFromString<List<JourneyLookupRecord>>(
                    file.openRead().use { it.reader().readText() }).take(MAX_RECORDS)
            }
            loaded = true
        }
    } }
    suspend fun save(record: JourneyLookupRecord) = withContext(Dispatchers.IO) { mutex.withLock {
        check(loaded)
        val previous = _records.value.firstOrNull { it.key == record.key }
        val safe = record.copy(items=(record.items+previous?.items.orEmpty()).distinctBy { it.contentUrl }.take(48))
        val updated = (listOf(safe)+_records.value.filterNot { it.key==record.key }).take(MAX_RECORDS)
        val stream=file.startWrite()
        try { stream.write(json.encodeToString(updated).toByteArray(Charsets.UTF_8))
            file.finishWrite(stream); _records.value=updated
        } catch(error:Exception) { file.failWrite(stream); throw error }
    } }
    companion object {
        const val MAX_RECORDS = 60
        fun prefix(familyId:String)="atlas:$familyId:"
        fun key(familyId:String,provider:String,query:String)=prefix(familyId)+provider+":"+CanonicalText.normalizeTitle(query)
    }
}
