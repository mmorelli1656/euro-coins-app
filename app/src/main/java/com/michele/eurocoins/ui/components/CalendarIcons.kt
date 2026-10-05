package com.michele.eurocoins.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Icone calendario "con più" (data da aggiungere) e "con spunta" (data impostata) del pulsante della data
 * nelle card di finitura. Disegno a tratto da 2 px su 24, angoli e terminazioni arrotondati: sono le
 * icone "calendar-plus" e "calendar-check" di Tabler Icons (MIT, © Paweł Kuna, tabler.io/icons), scelte
 * dall'utente su mockup. Il set di Material non ha un calendario con il più. Si colorano con `tint` come
 * ogni `Icon`: il nero qui sotto è solo il colore della maschera.
 */
internal object CalendarIcons {
    /** Calendario con "+": nessuna data ancora. */
    val Plus: ImageVector by lazy {
        stroked(
            "CalendarPlus",
            "M12.5 21h-6.5a2 2 0 0 1 -2 -2v-12a2 2 0 0 1 2 -2h12a2 2 0 0 1 2 2v5 " +
                "M16 3v4 M8 3v4 M4 11h16 M16 19h6 M19 16v6",
        )
    }

    /** Calendario con spunta: la data c'è. */
    val Check: ImageVector by lazy {
        stroked(
            "CalendarCheck",
            "M11.5 21h-5.5a2 2 0 0 1 -2 -2v-12a2 2 0 0 1 2 -2h12a2 2 0 0 1 2 2v6 " +
                "M16 3v4 M8 3v4 M4 11h16 M15 19l2 2l4 -4",
        )
    }

    private fun stroked(name: String, pathData: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = PathParser().parsePathString(pathData).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ).build()
}
