package com.michele.eurocoins.data.pro

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.michele.eurocoins.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

/**
 * Acquisto del Pro con Google Play Billing: un prodotto in-app non consumabile ([BuildConfig.PRO_PRODUCT_ID],
 * da creare con lo stesso ID in Play Console) che toglie la pubblicità. Il Play Store è l'unica fonte di
 * verità: qui si tiene solo una copia locale del risultato ([KEY_IS_PRO]) perché un utente Pro senza rete
 * non deve rivedere il banner, e la si riallinea a ogni avvio e a ogni ritorno nell'app ([refresh]).
 *
 * **Nessun server**: la verifica lato server delle ricevute non c'è. Per un acquisto di pochi euro che
 * sblocca solo l'assenza di un banner è un rischio accettato (chi lo aggira si toglie un banner da solo);
 * se il Pro sbloccasse funzioni a pagamento con costi per noi andrebbe rivisto.
 *
 * Non provato end-to-end: serve un'app in Play Console con il prodotto attivo e un tester di licenza.
 */
class ProBilling(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val connectMutex = Mutex()

    private var productDetails: ProductDetails? = null

    private val _state = MutableStateFlow(ProState(isPro = storedPro()))

    val state: StateFlow<ProState> = _state.asStateFlow()

    private val client: BillingClient = BillingClient.newBuilder(appContext)
        .setListener { result, purchases -> onPurchasesUpdated(result, purchases) }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    /** Debug: una chiave a mano nelle preferenze (`debug_pro`) simula il Pro, per provare la UI senza Play Console. */
    private fun storedPro(): Boolean =
        (BuildConfig.DEBUG && prefs.getBoolean(KEY_DEBUG_PRO, false)) || prefs.getBoolean(KEY_IS_PRO, false)

    /** Riallinea prezzo e acquisti con Play: all'avvio, al ritorno nell'app e dal pulsante di ripristino. */
    fun refresh() {
        scope.launch { sync(userInitiated = false) }
    }

    /** "Restore purchase": stessa verifica, ma dice all'utente com'è andata. */
    fun restore() {
        scope.launch { sync(userInitiated = true) }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    /** Apre la schermata di acquisto di Play. Senza prodotto caricato (offline, app non in Play) spiega e basta. */
    fun purchase(activity: Activity) {
        scope.launch {
            _state.update { it.copy(busy = true, message = null) }
            if (!connect()) return@launch fail(UNAVAILABLE)
            val details = productDetails ?: loadProductDetails() ?: return@launch fail(UNAVAILABLE)
            val params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()),
                )
                .build()
            val result = client.launchBillingFlow(activity, params)
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> _state.update { it.copy(busy = false) }
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> sync(userInitiated = true)
                else -> fail(UNAVAILABLE)
            }
        }
    }

    private suspend fun sync(userInitiated: Boolean) {
        if (userInitiated) _state.update { it.copy(busy = true, message = null) }
        if (!connect()) {
            if (userInitiated) fail(UNAVAILABLE) else _state.update { it.copy(busy = false) }
            return
        }
        if (productDetails == null) loadProductDetails()
        val result = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build())
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            // Verifica fallita: non è un "no", il valore salvato resta com'era.
            if (userInitiated) fail(UNAVAILABLE) else _state.update { it.copy(busy = false) }
            return
        }
        val ownership = applyPurchases(result.purchasesList)
        if (userInitiated) {
            val message = when (ownership) {
                Ownership.OWNED -> "Purchase restored. Thank you!"
                Ownership.PENDING -> PENDING_MESSAGE
                Ownership.NONE -> "No purchase found on this Google account."
            }
            _state.update { it.copy(busy = false, message = message) }
        } else {
            _state.update { it.copy(busy = false) }
        }
    }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> scope.launch {
                val ownership = applyPurchases(purchases.orEmpty())
                _state.update {
                    it.copy(
                        busy = false,
                        message = when (ownership) {
                            Ownership.OWNED -> "Thank you for your support!"
                            Ownership.PENDING -> PENDING_MESSAGE
                            Ownership.NONE -> null
                        },
                    )
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> _state.update { it.copy(busy = false) }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> refresh()
            else -> _state.update { it.copy(busy = false, message = UNAVAILABLE) }
        }
    }

    /** Salva l'esito, conferma (acknowledge) gli acquisti completati — senza, Play li rimborsa dopo 3 giorni. */
    private suspend fun applyPurchases(purchases: List<Purchase>): Ownership {
        val productId = BuildConfig.PRO_PRODUCT_ID
        val snapshots = purchases.map {
            PurchaseSnapshot(
                productIds = it.products,
                purchased = it.purchaseState == Purchase.PurchaseState.PURCHASED,
                pending = it.purchaseState == Purchase.PurchaseState.PENDING,
            )
        }
        val ownership = resolveOwnership(snapshots, productId)
        purchases
            .filter { productId in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
            .forEach { client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(it.purchaseToken).build()) }
        val pro = resolveCachedPro(previous = prefs.getBoolean(KEY_IS_PRO, false), ownership = ownership)
        prefs.edit().putBoolean(KEY_IS_PRO, pro).apply()
        _state.update { it.copy(isPro = pro || (BuildConfig.DEBUG && prefs.getBoolean(KEY_DEBUG_PRO, false))) }
        return ownership
    }

    private suspend fun loadProductDetails(): ProductDetails? {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BuildConfig.PRO_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        val result = client.queryProductDetails(params)
        val details = result.productDetailsList?.firstOrNull()
            ?.takeIf { result.billingResult.responseCode == BillingClient.BillingResponseCode.OK }
        productDetails = details
        _state.update { it.copy(price = details?.oneTimePurchaseOfferDetails?.formattedPrice) }
        return details
    }

    private fun fail(message: String) {
        _state.update { it.copy(busy = false, message = message) }
    }

    /** Connessione al servizio Play, una alla volta; true se pronta. */
    private suspend fun connect(): Boolean = connectMutex.withLock {
        if (client.isReady) return true
        suspendCancellableCoroutine { continuation ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (continuation.isActive) continuation.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                }

                override fun onBillingServiceDisconnected() {
                    if (continuation.isActive) continuation.resume(false)
                }
            })
        }
    }

    private companion object {
        const val PREFS_NAME = "pro"
        const val KEY_IS_PRO = "is_pro"
        const val KEY_DEBUG_PRO = "debug_pro"
        const val UNAVAILABLE = "Google Play is unavailable. Try again later."
        const val PENDING_MESSAGE = "Payment pending. Pro unlocks when it completes."
    }
}
