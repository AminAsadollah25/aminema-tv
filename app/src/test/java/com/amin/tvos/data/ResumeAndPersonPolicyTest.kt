package com.amin.tvos.data

import com.amin.tvos.data.model.PlaybackSession
import com.amin.tvos.data.model.PersonRef
import com.amin.tvos.data.model.TitleMetadata
import com.amin.tvos.data.model.mergePrefer
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test

class ResumeAndPersonPolicyTest {
    @Test fun nameOnlyRefreshPreservesTheVerifiedPortrait() {
        val previous = TitleMetadata("page", cast=listOf(PersonRef("Actor", imageUrl="https://cdn.example.test/a.jpg")))
        val refreshed = previous.mergePrefer(TitleMetadata("page", cast=listOf(PersonRef("Actor"))))
        assertEquals(previous.cast, refreshed.cast)
        val different = previous.mergePrefer(TitleMetadata("page", cast=listOf(PersonRef("Other Actor"))))
        assertEquals("", different.cast.single().imageUrl)
    }
    @Test fun publicPortraitNeverMatchesAnUnrelatedOrAmbiguousPerson() {
        val actor = PersonRef("Actor")
        val related = TrustedPortrait("Q1", setOf("Actor"), "https://cdn.example.test/actor.jpg")
        assertEquals(related.imageUrl, PersonPortraitPolicy.fill(listOf(actor), listOf(related)).single().imageUrl)
        assertEquals(actor, PersonPortraitPolicy.fill(listOf(actor), listOf(related.copy(names=setOf("Another")))).single())
        assertEquals(actor, PersonPortraitPolicy.fill(listOf(actor), listOf(related, related.copy(qid="Q2"))).single())
        val provider = actor.copy(imageUrl="https://provider.example.test/portrait.jpg")
        assertEquals(provider, PersonPortraitPolicy.fill(listOf(provider), listOf(related)).single())
    }
    @Test fun semanticSelectionAcceptsOnlyKnownTokens() {
        assertEquals("0", EpisodeResumePolicy.season("#parsiflix-s0-epnum-4"))
        assertEquals("2", EpisodeResumePolicy.season("#filmrooz-s2-box0-epnum-8"))
        assertEquals("", EpisodeResumePolicy.safeAction("https://example.test/media"))
        assertEquals("", EpisodeResumePolicy.safeAction("#parsiflix-sx-epnum-4"))
    }
    @Test fun nextEpisodeDoesNotInheritThePreviousTimeline() {
        assertFalse(EpisodeResumePolicy.sameEpisode("#parsiflix-s0-epnum-4", "#parsiflix-s0-epnum-5", "page", "page"))
        assertTrue(EpisodeResumePolicy.sameEpisode("#parsiflix-s0-epnum-5", "#parsiflix-s0-epnum-5", "page", "page"))
        assertFalse(EpisodeResumePolicy.sameEpisode("", "", "episode4", "episode5"))
    }
    @Test fun oldSessionStillDecodesWithoutEpisodeFields() {
        val old = """{"id":"x","title":"t","serviceId":"parsiflix","serviceName":"s","contentUrl":"page","playbackUrl":"page","lastPlayed":1}"""
        assertEquals("", Json.decodeFromString<PlaybackSession>(old).episodeAction)
    }
    @Test fun selectedEpisodeRoundTrips() {
        val value = PlaybackSession("x", "t", serviceId="parsiflix", serviceName="s", contentUrl="page", playbackUrl="page", lastPlayed=1, episodeAction="#parsiflix-s0-epnum-4", episodeSeason="0")
        assertEquals(value, Json.decodeFromString<PlaybackSession>(Json.encodeToString(value)))
    }
    @Test fun portraitIsIndependentOfHtmlProfile() {
        val old = Json.decodeFromString<PersonRef>("""{"name":"Actor","profileUrl":"https://example.test/person/1"}""")
        assertEquals("", old.imageUrl)
        val withImage = old.copy(imageUrl="https://images.example.test/portrait.jpg")
        assertEquals(withImage, Json.decodeFromString<PersonRef>(Json.encodeToString(withImage)))
    }
    @Test fun invalidImageSchemesAndCredentialsAreRejected() {
        assertEquals("", PersonImagePolicy.safeUrl("javascript:alert(1)"))
        assertEquals("", PersonImagePolicy.safeUrl("file:///private/photo.png"))
        assertEquals("", PersonImagePolicy.safeUrl("https://user:secret@example.test/photo.jpg"))
        assertEquals("https://cdn.example.test/actor.jpg", PersonImagePolicy.safeUrl("https://cdn.example.test/actor.jpg"))
    }
}
