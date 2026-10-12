package com.amin.tvos.data

import com.amin.tvos.data.model.PersonRef
import java.text.Normalizer

/** Candidates must already belong to the title's verified Wikidata cast/director relation. */
data class TrustedPortrait(val qid: String, val names: Set<String>, val imageUrl: String)

object PersonPortraitPolicy {
    private fun name(value: String) = Normalizer.normalize(value, Normalizer.Form.NFKC)
        .lowercase().replace('ي', 'ی').replace('ك', 'ک').replace('\u200c', ' ')
        .replace(Regex("\\s+"), " ").trim()
    fun fill(people: List<PersonRef>, candidates: List<TrustedPortrait>): List<PersonRef> = people.map { person ->
        if (person.imageUrl.isNotBlank()) person else {
            val matches = candidates.filter { candidate ->
                person.providerId == candidate.qid || candidate.names.any { name(it) == name(person.name) }
            }
            val candidate = matches.singleOrNull()
            if (candidate == null) person else person.copy(imageUrl=PersonImagePolicy.safeUrl(candidate.imageUrl))
        }
    }
}
