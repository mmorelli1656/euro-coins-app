package com.michele.eurocoins.ui.components

import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.michele.eurocoins.data.Coin
import com.michele.eurocoins.data.CoinQuality
import com.michele.eurocoins.data.CollectionItem
import com.michele.eurocoins.data.displayCountry
import com.michele.eurocoins.data.stableKey
import com.michele.eurocoins.ui.theme.PurpleFieldDark
import com.michele.eurocoins.ui.theme.PurpleFieldFocusDark
import com.michele.eurocoins.ui.theme.PurpleFieldFocusLight
import com.michele.eurocoins.ui.theme.PurpleFieldLight

/** Sottotitolo di ogni finitura nel pannello (testo nostro, in inglese). */
private val CoinQuality.descriptor: String
    get() = when (this) {
        CoinQuality.STANDARD -> "Circulation"
        CoinQuality.BU -> "Brilliant Uncirculated"
        CoinQuality.PROOF -> "Mirror finish"
    }

private val CardHeight = 64.dp

/**
 * Pannello per registrare una moneta: una card per qualità (Standard / BU /
 * Proof) con spunta a sinistra e prezzo pagato (facoltativo) a destra.
 *
 * **Altezza stabile**: il campo prezzo è SEMPRE presente; senza spunta è
 * attenuato e non editabile, con la spunta si attiva. Prima compariva solo se
 * spuntata e il pannello cambiava altezza a ogni tocco.
 *
 * Lavora su una BOZZA locale e scrive solo con "Save": chiudere il pannello
 * senza salvare non cambia nulla, quindi un tocco sbagliato non cancella un
 * prezzo già inserito. Alla prima apertura (moneta non ancora posseduta)
 * "Standard" è già spuntata, perché è il caso di quasi tutte le monete: un
 * tocco sulla casella + Save.
 *
 * Usato sia dall'elenco (casella accanto a ogni moneta) sia dal dettaglio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionSheet(
    coin: Coin,
    currentItems: List<CollectionItem>,
    onSave: (Map<CoinQuality, Int?>) -> Unit,
    onDismiss: () -> Unit,
) {
    val checked = remember(coin.stableKey) {
        mutableStateMapOf<CoinQuality, Boolean>().apply {
            CoinQuality.entries.forEach { quality ->
                this[quality] = currentItems.any { it.quality == quality } ||
                    (currentItems.isEmpty() && quality == CoinQuality.STANDARD)
            }
        }
    }
    val prices = remember(coin.stableKey) {
        mutableStateMapOf<CoinQuality, String>().apply {
            CoinQuality.entries.forEach { quality ->
                this[quality] = formatPrice(currentItems.firstOrNull { it.quality == quality }?.priceCents)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Text(
                text = "MY COLLECTION",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = coin.tema,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = "${coin.displayCountry()} · ${coin.anno}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CoinQuality.entries.forEach { quality ->
                    FinishCard(
                        quality = quality,
                        checked = checked[quality] == true,
                        price = prices[quality].orEmpty(),
                        onCheckedChange = { checked[quality] = it },
                        onPriceChange = { prices[quality] = it },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    onClick = {
                        onSave(
                            CoinQuality.entries
                                .filter { checked[it] == true }
                                .associateWith { parsePriceCents(prices[it].orEmpty()) },
                        )
                    },
                ) { Text("Save") }
            }
        }
    }
}

/**
 * Riga a altezza fissa ([CardHeight]): cambia solo il colore di fondo (lilla
 * parte sinistra (checkbox + etichette) è `toggleable` DOPO un `clip`, così il ripple segue gli angoli
 * arrotondati; il resto della card, prezzo compreso, non spunta né toglie nulla.
 * ha un proprio click), il campo a destra ha il suo.
 */
@Composable
private fun FinishCard(
    quality: CoinQuality,
    checked: Boolean,
    price: String,
    onCheckedChange: (Boolean) -> Unit,
    onPriceChange: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    val backgroundColor by animateColorAsState(
        targetValue = if (checked) colors.secondaryContainer else colors.surface,
        animationSpec = tween(durationMillis = 150),
        label = "cardBackgroundColor",
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) colors.secondary.copy(alpha = 0.35f) else Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "cardBorderColor",
    )
    // Un solo ripple, sull'intera card (angoli da 16 dp), innescato dal tocco sulla parte sinistra:
    // un ripple ritagliato sulla sola area sinistra lasciava un sottorettangolo visibile.
    val interaction = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(CardHeight)
            .clip(shape)
            .background(backgroundColor)
            .border(1.5.dp, borderColor, shape)
            .indication(interaction, ripple())
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxHeight()
                .toggleable(
                    value = checked,
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Checkbox,
                    onValueChange = onCheckedChange,
                )
                .padding(end = 8.dp),
        ) {
            Checkbox(checked = checked, onCheckedChange = null)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = quality.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                Text(
                    text = quality.descriptor,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        PriceField(
            value = price,
            onValueChange = { onPriceChange(sanitizePrice(it)) },
            enabled = checked,
            description = "Price paid for ${quality.label} (€)",
        )
    }
}

/**
 * Campo prezzo: "€" + cifre come un'unica riga di testo, dentro una pillola
 * che cresce con il contenuto (nessuna larghezza fissa: "15000.00" non si
 * tronca e le etichette a sinistra prendono il resto).
 *
 * - Non spuntato: testo piatto al 38%, nessun contenitore.
 * - Spuntato: pillola con bordo viola da 1.5 dp e fondo chiaro traslucido; in
 *   focus bordo viola scuro e fondo pieno. Il bordo è SEMPRE da 1.5 dp
 *   (trasparente quando non serve) così niente si sposta al cambio di stato.
 * - "€" sta nel `decorationBox` con lo stesso [TextStyle] delle cifre, quindi
 *   condivide altezza di riga e linea di base. Il segnaposto "0.00" resta
 *   solo se il campo è vuoto e, insieme alla misura del testo, garantisce la larghezza
 *   minima di 4 caratteri.
 */
@Composable
private fun PriceField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    description: String,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val fieldColor = if (dark) PurpleFieldDark else PurpleFieldLight
    val focusColor = if (dark) PurpleFieldFocusDark else PurpleFieldFocusLight
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    val idleFill = if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.55f)
    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Transparent
            focused -> focusColor
            else -> fieldColor
        },
        animationSpec = tween(durationMillis = 150),
        label = "priceBorder",
    )
    val fillColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Transparent
            focused -> colors.surface
            else -> idleFill
        },
        animationSpec = tween(durationMillis = 150),
        label = "priceFill",
    )
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        color = colors.onSurface,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.End,
    )
    // Larghezza = quella del testo (minimo "0.00"): un BasicTextField a riga singola
    // altrimenti si allarga a tutto lo spazio disponibile e la pillola diventa enorme.
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textWidth = with(density) {
        val sample = if (value.length > PriceSample.length) value else PriceSample
        measurer.measure(sample, textStyle).size.width.toDp() + 2.dp
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        interactionSource = interactionSource,
        textStyle = textStyle,
        cursorBrush = SolidColor(focusColor),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.38f)
            .clip(shape)
            .background(fillColor)
            .border(1.5.dp, borderColor, shape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .semantics { contentDescription = description },
        decorationBox = { inner ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "€",
                    style = textStyle.copy(color = if (enabled) focusColor else colors.onSurface),
                )
                Box(modifier = Modifier.width(textWidth), contentAlignment = Alignment.CenterEnd) {
                    if (value.isEmpty()) {
                        Text(text = "0.00", style = textStyle, color = colors.onSurfaceVariant)
                    }
                    inner()
                }
            }
        },
    )
}

/** Campione che fissa la larghezza della pillola: il massimo consentito, 9999.99. */
private const val PriceSample = "0000.00"

/**
 * Limita il prezzo a 9999.99: solo cifre e un separatore (`.` o `,`), al massimo
 * 4 cifre intere e 2 decimali. Così la pillola ha sempre la stessa larghezza.
 */
private fun sanitizePrice(input: String): String {
    val separator = input.indexOfFirst { it == '.' || it == ',' }
    val integer = input.substring(0, if (separator >= 0) separator else input.length)
        .filter(Char::isDigit).take(4)
    if (separator < 0) return integer
    val decimals = input.substring(separator + 1).filter(Char::isDigit).take(2)
    return integer + input[separator] + decimals
}
