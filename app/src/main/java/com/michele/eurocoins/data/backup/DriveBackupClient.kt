package com.michele.eurocoins.data.backup

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Un file di backup su Drive: [modifiedTime] è ISO-8601 (es. `2026-09-20T10:15:30.123Z`). */
data class DriveBackupInfo(val id: String, val modifiedTime: String?)

/**
 * Client minimale per la Drive REST API v3, limitato alla cartella privata
 * `appDataFolder` dell'app. Volutamente senza la libreria client di Google
 * (pesante): servono solo cerca / crea / aggiorna / scarica.
 *
 * Due file: il backup attuale ([FILE_NAME]) e la **versione precedente** ([PREVIOUS_FILE_NAME]).
 * Il salvataggio automatico non deve mai poter distruggere un backup buono (vedi [upload]).
 */
class DriveBackupClient {

    /** Il backup attuale, o null se non ne è mai stato fatto uno. */
    suspend fun find(token: String): DriveBackupInfo? = find(token, FILE_NAME)

    /** La versione precedente del backup, o null se non esiste. */
    suspend fun findPrevious(token: String): DriveBackupInfo? = find(token, PREVIOUS_FILE_NAME)

    private suspend fun find(token: String, name: String): DriveBackupInfo? = withContext(Dispatchers.IO) {
        val query = URLEncoder.encode("name = '$name' and trashed = false", "UTF-8")
        val url = "$API/files?spaces=appDataFolder&q=$query&fields=files(id,modifiedTime)&orderBy=modifiedTime%20desc"
        val files = Json.parseToJsonElement(call("GET", url, token).decodeToString())
            .jsonObject["files"]?.jsonArray.orEmpty()
        val first = files.firstOrNull()?.jsonObject ?: return@withContext null
        DriveBackupInfo(
            id = first.string("id") ?: return@withContext null,
            modifiedTime = first.string("modifiedTime"),
        )
    }

    /**
     * Scrive [content] come backup attuale (lo crea al primo salvataggio). Prima, se il backup
     * attuale esiste ed è vecchio abbastanza ([shouldRotatePrevious]), lo copia in "versione
     * precedente": così salvataggi ravvicinati non cancellano l'ultima copia buona (un reset seguito
     * da pochi acquisti non porta subito via la collezione di prima), e chi se ne accorge ha un giorno
     * per ripristinarla.
     */
    suspend fun upload(token: String, content: String): Unit = withContext(Dispatchers.IO) {
        val existing = find(token)
        if (existing != null) {
            val previous = find(token, PREVIOUS_FILE_NAME)
            if (shouldRotatePrevious(parseInstant(existing.modifiedTime), previous != null, Instant.now())) {
                write(token, PREVIOUS_FILE_NAME, download(token, existing.id), previous)
            }
        }
        write(token, FILE_NAME, content, existing)
    }

    private suspend fun write(token: String, name: String, content: String, existing: DriveBackupInfo?): Unit =
        withContext(Dispatchers.IO) {
            if (existing != null) {
                call(
                    "PATCH", "$UPLOAD/files/${existing.id}?uploadType=media", token,
                    body = content.toByteArray(), contentType = "application/json; charset=UTF-8",
                )
            } else {
                val metadata = """{"name":"$name","parents":["appDataFolder"]}"""
                val body = buildString {
                    append("--$BOUNDARY\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$metadata\r\n")
                    append("--$BOUNDARY\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$content\r\n")
                    append("--$BOUNDARY--")
                }
                call(
                    "POST", "$UPLOAD/files?uploadType=multipart", token,
                    body = body.toByteArray(), contentType = "multipart/related; boundary=$BOUNDARY",
                )
            }
        }

    suspend fun download(token: String, fileId: String): String = withContext(Dispatchers.IO) {
        call("GET", "$API/files/$fileId?alt=media", token).decodeToString()
    }

    private fun call(method: String, url: String, token: String, body: ByteArray? = null, contentType: String? = null): ByteArray {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST".takeIf { method == "PATCH" } ?: method
            // HttpURLConnection non supporta PATCH: si passa da POST con l'override standard di Google.
            if (method == "PATCH") setRequestProperty("X-HTTP-Method-Override", "PATCH")
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
        }
        try {
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", contentType)
                connection.outputStream.use { it.write(body) }
            }
            val code = connection.responseCode
            if (code in 200..299) return connection.inputStream.use { it.readBytes() }
            // Il dettaglio grezzo della risposta non va all'utente: messaggi d'uso, niente JSON/HTTP.
            throw BackupException(
                when (code) {
                    401 -> "Google Drive session expired. Try again."
                    403 -> "Google Drive refused access. Check the permissions granted to this app."
                    else -> "Google Drive is unavailable right now. Please try again later."
                },
            )
        } catch (e: IOException) {
            throw BackupException("Network unavailable. Please check your connection.", e)
        } finally {
            connection.disconnect()
        }
    }

    private fun JsonObject.string(key: String): String? = get(key)?.jsonPrimitive?.content

    companion object {
        const val FILE_NAME = "euro-coins-collection.json"
        const val PREVIOUS_FILE_NAME = "euro-coins-collection-previous.json"
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
        private const val BOUNDARY = "euro-coins-backup-boundary"
        private const val TIMEOUT_MS = 20_000
    }
}

/** Il backup attuale diventa "versione precedente" solo se ha almeno questa età: un giorno per accorgersi di un errore. */
internal val PREVIOUS_MIN_AGE: Duration = Duration.ofHours(24)

/**
 * Se il backup attuale va copiato in "versione precedente" prima di essere sovrascritto: sempre se
 * la precedente non esiste o la data dell'attuale non è nota; altrimenti solo se l'attuale ha
 * almeno [minAge]. Senza la soglia, due salvataggi di fila (anche automatici) farebbero scivolare
 * via subito la copia buona.
 */
internal fun shouldRotatePrevious(
    currentModified: Instant?,
    previousExists: Boolean,
    now: Instant,
    minAge: Duration = PREVIOUS_MIN_AGE,
): Boolean = !previousExists || currentModified == null || Duration.between(currentModified, now) >= minAge

internal fun parseInstant(iso: String?): Instant? = iso?.let { runCatching { Instant.parse(it) }.getOrNull() }
