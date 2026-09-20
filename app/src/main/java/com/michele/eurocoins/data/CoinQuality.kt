package com.michele.eurocoins.data

/**
 * Qualità in cui si può possedere una moneta. Una stessa moneta può essere
 * posseduta in più qualità insieme (una riga di collezione per qualità).
 */
enum class CoinQuality(val label: String) {
    STANDARD("Standard"),
    BU("BU"),
    PROOF("Proof"),
}
