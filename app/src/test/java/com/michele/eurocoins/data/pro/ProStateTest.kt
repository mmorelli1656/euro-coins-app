package com.michele.eurocoins.data.pro

import org.junit.Assert.assertEquals
import org.junit.Test

class ProStateTest {
    private val pro = "euro_coins_pro"

    private fun purchase(vararg ids: String, purchased: Boolean = false, pending: Boolean = false) =
        PurchaseSnapshot(ids.toList(), purchased, pending)

    @Test
    fun noPurchasesMeansNotOwned() {
        assertEquals(Ownership.NONE, resolveOwnership(emptyList(), pro))
    }

    @Test
    fun completedPurchaseOfTheProProductIsOwned() {
        assertEquals(Ownership.OWNED, resolveOwnership(listOf(purchase(pro, purchased = true)), pro))
    }

    @Test
    fun otherProductsAreIgnored() {
        assertEquals(Ownership.NONE, resolveOwnership(listOf(purchase("other", purchased = true)), pro))
    }

    @Test
    fun pendingPaymentDoesNotUnlock() {
        assertEquals(Ownership.PENDING, resolveOwnership(listOf(purchase(pro, pending = true)), pro))
    }

    @Test
    fun completedWinsOverPending() {
        val both = listOf(purchase(pro, pending = true), purchase(pro, purchased = true))
        assertEquals(Ownership.OWNED, resolveOwnership(both, pro))
    }

    @Test
    fun failedVerificationKeepsTheStoredValue() {
        assertEquals(true, resolveCachedPro(previous = true, ownership = null))
        assertEquals(false, resolveCachedPro(previous = false, ownership = null))
    }

    @Test
    fun certainAnswerReplacesTheStoredValue() {
        assertEquals(true, resolveCachedPro(previous = false, ownership = Ownership.OWNED))
        // Rimborso: nessun acquisto, Play ha risposto con certezza.
        assertEquals(false, resolveCachedPro(previous = true, ownership = Ownership.NONE))
        assertEquals(false, resolveCachedPro(previous = true, ownership = Ownership.PENDING))
    }
}
