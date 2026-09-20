package com.michele.eurocoins.data

/** Quante monete di un gruppo (anno, paese, catalogo) sono possedute. */
data class Progress(val owned: Int, val total: Int) {
    val fraction: Float get() = if (total == 0) 0f else owned.toFloat() / total
}

/** Una moneta conta come posseduta se l'utente ne ha almeno una qualità. */
fun List<Coin>.progress(ownedKeys: Set<String>): Progress =
    Progress(owned = count { it.stableKey in ownedKeys }, total = size)
