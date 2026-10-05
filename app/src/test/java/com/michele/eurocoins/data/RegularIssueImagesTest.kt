package com.michele.eurocoins.data

import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RegularIssueImagesTest {

    private val all: List<RegularIssueSeries> = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<RegularIssueSeriesJson>>(File("src/main/assets/regular_issues.json").readText())
        .map { it.toEntity() }

    private val luxembourg2 get() = all.first { it.paese == "Lussemburgo" && it.ordineCronologico == 2 }.withUsableImages()

    @Test
    fun luxembourgNewSeriesOneAndTwoEuroUsePlaceholder() {
        for (taglio in listOf("1 euro", "2 euro")) {
            val image = luxembourg2.immagini.first { it.taglio == taglio }
            assertNull(taglio, image.urlImmagineFonte)
            // niente fonte/licenza/credito dell'immagine: i crediti non accreditano una foto che non c'e'
            assertEquals("", image.fonteDati)
            assertEquals("", image.licenzaImmagine)
            assertNull(image.attribuzioneImmagineRaw)
        }
    }

    @Test
    fun theCentCoinsOfTheSameSeriesKeepTheirPhotos() {
        val cents = luxembourg2.immagini.filter { it.taglio.endsWith("cent") }
        assertEquals(6, cents.size)
        assertTrue(cents.all { it.urlImmagineFonte != null && it.fonteDati == "bcl" })
    }

    @Test
    fun onlyThoseTwoImagesChangeInTheWholeCatalog() {
        val before = all.flatMap { it.immagini }
        val after = all.map { it.withUsableImages() }.flatMap { it.immagini }
        assertEquals(before.size, after.size)
        assertEquals(2, before.indices.count { before[it] != after[it] })
        assertEquals(0, after.count { it.urlImmagineFonte?.endsWith("nouvelles-faces/1-2-euro.png") == true })
    }

    @Test
    fun seriesWithoutUnusableImagesAreReturnedAsTheSameInstance() {
        val italy = all.first { it.paese == "Italia" }
        assertSame(italy, italy.withUsableImages())
        assertNotNull(luxembourg2)
    }

    @Test
    fun textAndMintagesOfTheCoinAreUntouched() {
        val raw = all.first { it.paese == "Lussemburgo" && it.ordineCronologico == 2 }.immagini.first { it.taglio == "2 euro" }
        val clean = luxembourg2.immagini.first { it.taglio == "2 euro" }
        assertEquals(raw.descrizione, clean.descrizione)
        assertEquals(raw.tirature, clean.tirature)
        assertEquals(raw.taglio, clean.taglio)
    }
}
