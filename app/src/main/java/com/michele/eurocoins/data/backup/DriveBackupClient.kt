package com.michele.eurocoins.data.backup

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Il file di backup su Drive: [modifiedTime] è ISO-8601 (es. `2026-09-20T10:15:30.123Z`). */
data class DriveBackupInfo(val id: String, val modifiedTime: String?)

/**
 * Client minimale per la Drive REST API v3, limitato alla cartella privata
 * `appDataFolder` dell'app. Volutamente senza la libreria client di Google
 * (pesante): servono solo cerca / crea / aggiorna / scarica di UN file.
 */
class DriveBackupClient {

    /** Il backup esistente, o null se non ne è mai stato fatto uno. */
    suspend fun find(token: String): DriveBackupInfo? = withContext(Dispatchers.IO) {
        val query = URLEncoder.encode("name = '$FILE_NAME' and trashed = false", "UTF-8")
        val url = "$API/files?spaces=appDataFolder&q=$query&fields=files(id,modifiedTime)&orderBy=modifiedTime%20desc"
        val files = Json.parseToJsonElement(call("GET", url, token).decodeToString())
            .jsonObject["files"]?.jsonArray.orEmpty()
        val first = files.firstOrNull()?.jsonObject ?: return@withContext null
        DriveBackupInfo(
            id = first.string("id") ?: return@withContext null,
            modifiedTime = first.string("modifiedTime"),
        )
    }

    /** Scrive [content] sul backup esistente, o lo crea al primo salvataggio. */
    suspend fun upload(token: String, content: String): Unit = withContext(Dispatchers.IO) {
        val existing = find(token)
        if (existing != null) {
            call(
                "PATCH", "$UPLOAD/files/${existing.id}?uploadType=media", token,
                body = content.toByteArray(), contentType = "application/json; charset=UTF-8",
            )
        } else {
            val metadata = """{"name":"$FILE_NAME","parents":["appDataFolder"]}"""
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
            val detail = connection.errorStream?.use { it.readBytes().decodeToString() }.orEmpty().take(300)
            throw BackupException(
                when (code) {
                    401 -> "Google Drive session expired. Try again."
                    403 -> "Google Drive refused access ($detail)"
                    else -> "Google Drive error $code: $detail"
                },
            )
        } catch (e: IOException) {
            throw BackupException("Network error: ${e.message}", e)
        } finally {
            connection.disconnect()
        }
    }

    private fun JsonObject.string(key: String): String? = get(key)?.jsonPrimitive?.content

    companion object {
        const val FILE_NAME = "euro-coins-collection.json"
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
        private const val BOUNDARY = "euro-coins-backup-boundary"
        private const val TIMEOUT_MS = 20_000
    }
}
