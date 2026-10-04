package com.michele.eurocoins.data

import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class SeriesPeriodTest {

    private val all: List<RegularIssueSeries> = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<RegularIssueSeriesJson>>(File("src/main/assets/regular_issues.json").readText())
        .map { it.toEntity() }

    private fun period(paese: String, ordine: Int): SeriesPeriod {
        val forCountry = all.filter { it.paese == paese }
        return seriesPeriod(forCountry, forCountry.first { it.ordineCronologico == ordine })
    }

    @Test
    fun franceSeriesAreConsecutiveWindows() {
        assertEquals("2002 – 2021", period("Francia", 1).label)
        assertEquals("2022 – 2023", period("Francia", 2).label)
        assertEquals("2024 – today", period("Francia", 3).label)
    }

    @Test
    fun explicitEndsAndSingleYear() {
        assertEquals("2002 – 2007", period("Belgio", 1).label) // le monete datate 1999 non contano
        assertEquals("2008 – 2013", period("Belgio", 2).label)
        assertEquals("2005", period("Città del Vaticano", 2).label) // Sede Vacante
        assertEquals("2002 – 2005", period("Città del Vaticano", 1).label)
        assertEquals("2002 – 2005", period("Monaco", 1).label) // il 2001 è datato, non in circolazione
        assertEquals("2002 – 2025", period("Lussemburgo", 1).label)
    }

    @Test
    fun spainCutsOpenImagesWhereTheNextSeriesBegins() {
        assertEquals("2010 – 2014", period("Spagna", 2).label)
        assertEquals("2015 – today", period("Spagna", 3).label)
    }

    @Test
    fun seriesWithoutPhotosStartsAfterThePreviousOne() {
        assertEquals("2026 – today", period("Città del Vaticano", 6).label)
    }

    @Test
    fun singleSeriesCountriesStartWhenTheyMint() {
        assertEquals("2002 – today", period("Austria", 1).label)
        assertEquals("2014 – today", period("Andorra", 1).label)
    }

    @Test
    fun chipLabelHasNumberAndStartYear() {
        assertEquals("Series 2 · 2022", seriesChipLabel(2, SeriesPeriod(2022, 2023)))
        assertEquals("Series 6", seriesChipLabel(6, SeriesPeriod(null, null)))
    }
}

class SeriesTextLabelTest {

    private val all: List<RegularIssueSeries> = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<RegularIssueSeriesJson>>(File("src/main/assets/regular_issues.json").readText())
        .map { it.toEntity() }

    private fun label(paese: String, ordine: Int): String {
        val forCountry = all.filter { it.paese == paese }
        return seriesTextLabel(forCountry, forCountry.first { it.ordineCronologico == ordine })
    }

    @Test
    fun sharedTextSaysWhichSeriesItCovers() {
        assertEquals("ABOUT SERIES 1–3", label("Francia", 2))
        assertEquals("ABOUT SERIES 1–3", label("Belgio", 3))
        assertEquals("ABOUT SERIES 1–3", label("Spagna", 1))
        assertEquals("ABOUT SERIES 1–2", label("Paesi Bassi", 2))
        assertEquals("ABOUT SERIES 1–5", label("Città del Vaticano", 4))
        assertEquals("ABOUT SERIES 1–2", label("Monaco", 1))
    }

    @Test
    fun ownTextKeepsTheSingularLabel() {
        assertEquals("ABOUT THIS SERIES", label("Città del Vaticano", 6))
        assertEquals("ABOUT THIS SERIES", label("Monaco", 3))
        assertEquals("ABOUT THIS SERIES", label("Lussemburgo", 2))
        assertEquals("ABOUT THIS SERIES", label("San Marino", 1))
        assertEquals("ABOUT THIS SERIES", label("Austria", 1)) // una sola serie
    }

    @Test
    fun nonConsecutiveSeriesAreListed() {
        val base = all.first { it.paese == "Austria" }
        fun s(ordine: Int, text: String) = base.copy(ordineCronologico = ordine, descrizione = text)
        val list = listOf(s(1, "A"), s(2, "B"), s(3, "A"))
        assertEquals("ABOUT SERIES 1, 3", seriesTextLabel(list, list[2]))
    }
}
