package com.michele.eurocoins.data.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.michele.eurocoins.BuildConfig
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

data class GoogleAccount(val email: String, val displayName: String?)

/** Esito di una richiesta di accesso a Drive: subito un token, oppure serve prima il consenso dell'utente. */
sealed interface DriveAuthorization {
    data class Granted(val accessToken: String) : DriveAuthorization
    data class NeedsConsent(val intentSender: IntentSender) : DriveAuthorization
}

/**
 * Login con account Google (Credential Manager) e autorizzazione allo scope
 * `drive.appdata`, cioè alla cartella privata e nascosta dell'app su Drive:
 * l'app non vede gli altri file dell'utente e l'utente non vede il backup tra
 * i suoi file.
 *
 * Identità e autorizzazione sono due passaggi distinti in Google Identity: il
 * primo dice CHI è l'utente, il secondo ottiene il token per parlare con Drive.
 */
class GoogleAccountManager(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** False finché `google.webClientId` non è impostato in local.properties. */
    val isConfigured: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    fun currentAccount(): GoogleAccount? {
        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        return GoogleAccount(email, prefs.getString(KEY_NAME, null))
    }

    /**
     * Mostra il selettore account di Google. Restituisce null se l'utente
     * annulla; per ogni altro errore lancia [BackupException].
     */
    suspend fun signIn(activity: Activity): GoogleAccount? {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = try {
            CredentialManager.create(activity).getCredential(activity, request).credential
        } catch (_: GetCredentialCancellationException) {
            return null
        } catch (e: GetCredentialException) {
            throw BackupException("Google sign-in failed: ${e.message ?: e.type}", e)
        }
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw BackupException("Google sign-in returned an unexpected credential.")
        }
        val google = GoogleIdTokenCredential.createFrom(credential.data)
        prefs.edit().putString(KEY_EMAIL, google.id).putString(KEY_NAME, google.displayName).apply()
        return GoogleAccount(google.id, google.displayName)
    }

    suspend fun signOut(context: Context) {
        prefs.edit().clear().apply()
        // Best effort: dimentica la scelta dell'account sul dispositivo.
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Exception) {
        }
    }

    /** Chiede a Google un token per Drive appdata. Può richiedere il consenso dell'utente la prima volta. */
    suspend fun authorizeDrive(activity: Activity): DriveAuthorization {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
            .build()
        val result = suspendCancellableCoroutine { cont ->
            Identity.getAuthorizationClient(activity).authorize(request)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(BackupException("Google Drive authorization failed: ${it.message}", it)) }
        }
        if (result.hasResolution()) {
            val pending = result.pendingIntent ?: throw BackupException("Google Drive authorization needs consent but gave no way to ask.")
            return DriveAuthorization.NeedsConsent(pending.intentSender)
        }
        val token = result.accessToken ?: throw BackupException("Google Drive did not return an access token.")
        return DriveAuthorization.Granted(token)
    }

    /** Legge il token dall'esito della schermata di consenso lanciata con [DriveAuthorization.NeedsConsent]. */
    fun tokenFromConsent(context: Context, data: Intent?): String {
        try {
            return Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data).accessToken
                ?: throw BackupException("Google Drive did not return an access token.")
        } catch (e: com.google.android.gms.common.api.ApiException) {
            throw BackupException("Google Drive access was not granted.", e)
        }
    }

    companion object {
        private const val PREFS_NAME = "google_account"
        private const val KEY_EMAIL = "email"
        private const val KEY_NAME = "display_name"
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    }
}
