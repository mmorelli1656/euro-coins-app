package com.michele.eurocoins.ui.settings

import com.michele.eurocoins.ui.regular.RegularBrowseMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserSettingsTest {

    @Test
    fun regularMicrostatesInheritTheCommemorativeSettingUntilTouched() {
        // Chi aveva "Hide microstates" acceso prima della divisione per catalogo non vede cambiare niente.
        assertTrue(resolveHideRegularMicrostates(stored = null, commemorative = true))
        assertFalse(resolveHideRegularMicrostates(stored = null, commemorative = false))
    }

    @Test
    fun onceTouchedTheRegularSettingIsIndependent() {
        assertFalse(resolveHideRegularMicrostates(stored = false, commemorative = true))
        assertTrue(resolveHideRegularMicrostates(stored = true, commemorative = false))
    }

    @Test
    fun theRegularDefaultTabFallsBackToCountries() {
        assertEquals(RegularBrowseMode.COUNTRIES, parseRegularBrowseMode(null))
        assertEquals(RegularBrowseMode.COUNTRIES, parseRegularBrowseMode(""))
        assertEquals(RegularBrowseMode.COUNTRIES, parseRegularBrowseMode("YEARS")) // valore di un altro catalogo
        assertEquals(RegularBrowseMode.DENOMINATIONS, parseRegularBrowseMode("DENOMINATIONS"))
        assertEquals(RegularBrowseMode.ALL, parseRegularBrowseMode("ALL"))
    }
}
