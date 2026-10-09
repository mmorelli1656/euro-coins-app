package com.michele.eurocoins.data

import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Due monete diverse non devono mostrare la stessa foto. Capitò con Monaco 2025: la pagina BCE
 * usa un solo `Monaco.jpg` (il Marquisat des Baux) anche per il Comté de Carladès, e l'app
 * mostrava la stessa moneta due volte. Il Carladès è stato lasciato senza foto (segnaposto) nel
 * `coins.json`: rifacendo l'export dalla pipeline il difetto torna, e questo test lo dice.
 *
 * Controllo fatto sull'URL: un controllo sul contenuto delle foto (hash) sull'intero dataset del
 * 2026-10-09 non ha trovato altri duplicati tra le commemorative.
 */
class CoinImageDuplicatesTest {

    private val coins: List<Coin> = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<CoinJson>>(File("src/main/assets/coins.json").readText())
        .map { it.toEntity() }

    @Test
    fun noTwoCoinsShareTheSamePhoto() {
        val shared = coins
            .filter { !it.urlImmagineFonte.isNullOrBlank() }
            .groupBy { it.urlImmagineFonte }
            .filterValues { it.size > 1 }
            .map { (url, group) -> "$url -> " + group.joinToString { "${it.anno} ${it.paese} ${it.tema}" } }
        assertTrue("Foto condivise tra monete diverse:\n" + shared.joinToString("\n"), shared.isEmpty())
    }

    @Test
    fun monaco2025HasOnePhotoAndOnePlaceholder() {
        val monaco = coins.filter { it.paese == "Monaco" && it.anno == 2025 }
        assertEquals(2, monaco.size)
        val carlades = monaco.single { it.tema.contains("Carladès") }
        assertTrue(carlades.urlImmagineFonte == null)
        assertTrue(carlades.immaginePlaceholder)
        assertTrue(monaco.single { it.tema.contains("Baux") }.urlImmagineFonte != null)
    }
}
