package com.michele.eurocoins.data

import kotlinx.serialization.Serializable

/**
 * Qualità in cui si può possedere una moneta. Una stessa moneta può essere
 * posseduta in più qualità insieme (una riga di collezione per qualità).
 *
 * `@Serializable`: serve a [RegularIssueMintage], letta dall'asset `regular_issues.json` (wire
 * format = nome della costante Kotlin, "STANDARD"/"BU"/"PROOF"). Non tocca il salvataggio Room di
 * `CollectionItem`/`RegularCollectionItem`, che usano il proprio `TypeConverter` a stringa.
 */
@Serializable
enum class CoinQuality(val label: String) {
    STANDARD("Standard"),
    BU("BU"),
    PROOF("Proof"),
}
