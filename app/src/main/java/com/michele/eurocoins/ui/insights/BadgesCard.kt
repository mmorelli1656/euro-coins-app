package com.michele.eurocoins.ui.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Factory
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.michele.eurocoins.data.Badge
import com.michele.eurocoins.data.BadgeId
import com.michele.eurocoins.data.BadgeTarget
import com.michele.eurocoins.data.Progress
import com.michele.eurocoins.data.StatsCard

/** Quante medaglie per riga della griglia. */
private const val BADGES_PER_ROW = 4

private val MedalSize = 52.dp

/** Quanto può crescere il testo delle etichette sotto le medaglie (la cella ha una larghezza fissa): oltre, le parole si spezzano. */
private const val MAX_LABEL_FONT_SCALE = 1.15f

/** Nome, spiegazione e simbolo (icona Material o disegno [BadgeArt]) di una medaglia: i testi sono nostri, i numeri arrivano dal dato. */
private class BadgeCopy(val name: String, val how: String, val icon: ImageVector? = null, val art: BadgeArt? = null)

private fun BadgeId.copy(): BadgeCopy = when (this) {
    // Le monete si impilano: una moneta, una colonna, due, tre (2026-11-01, "poca differenza tra 250 e 500"). Niente numeri nei simboli.
    BadgeId.FIRST_COIN -> BadgeCopy("First coin", "Add your first coin to the collection.", art = BadgeArt.COIN)
    BadgeId.COINS_100 -> BadgeCopy("100 coins", "Own 100 commemorative coins.", art = BadgeArt.STACK_100)
    BadgeId.COINS_250 -> BadgeCopy("250 coins", "Own 250 commemorative coins.", art = BadgeArt.STACK_250)
    BadgeId.COINS_500 -> BadgeCopy("500 coins", "Own 500 commemorative coins.", art = BadgeArt.STACK_500)
    BadgeId.FIRST_BU -> BadgeCopy("First BU", "Own a coin in BU finish.", Icons.Outlined.Diamond)
    BadgeId.FIRST_PROOF -> BadgeCopy("First Proof", "Own a coin in Proof finish.", Icons.Outlined.AutoAwesome)
    BadgeId.JOINT_ISSUE -> BadgeCopy("Common issue", "Complete one of the five common issues, with the coin of every country.", art = BadgeArt.EU_STARS)
    BadgeId.FIRST_COUNTRY -> BadgeCopy("First country", "Complete every commemorative of one country.", Icons.Outlined.Flag)
    BadgeId.FIVE_COUNTRIES -> BadgeCopy("5 countries", "Complete five countries in Commemorative.", Icons.Outlined.Map)
    BadgeId.FIRST_SERIES -> BadgeCopy("First series", "Complete all eight values of one Regular series.", Icons.Outlined.Extension)
    BadgeId.TEN_SERIES -> BadgeCopy("10 series", "Complete ten Regular series.", Icons.Outlined.EmojiEvents)
    BadgeId.FIVE_MINTS -> BadgeCopy("Five mints", "Own one coin from all five German mints: A, D, F, G and J.", Icons.Outlined.Factory)
    BadgeId.BIG_SPENDER -> BadgeCopy("Big spender", "Spend at least €100 on your collection. Your wallet noticed.", Icons.Outlined.Payments)
    BadgeId.TRIPLE_THREAT -> BadgeCopy("Triple threat", "Own the same coin in Standard, BU and Proof. Why choose?", Icons.Outlined.Layers)
    BadgeId.POCKET_CHANGE -> BadgeCopy(
        "Pocket change",
        "Add a coin you got for its face value or less, like a 2 euro from your change. Price it at €2.00 or less.",
        Icons.Outlined.AccountBalanceWallet,
    )
    // ΕΥΡΩ = "euro" in greco (lettere greche maiuscole), e il tempio è la Grecia: la serie EFS è greca.
    BadgeId.LETTER_HUNTER -> BadgeCopy(
        "ΕΥΡΩ hunter",
        "Find all eight Greek 2002 coins with a letter in the star (E, F or S): the whole EFS series.",
        Icons.Outlined.AccountBalance,
    )
}

/**
 * La card Badges (2026-11-01, mockup approvato): sedici medaglie in quattro righe per tema (monete commemorative, set commemorativi,
 * Regular, finiture e soldi), di cui quattro "just for fun" a quadrato morbido (il cerchio dice "soglia", il quadrato "battuta"), in UNA griglia sola: il filetto "Just for fun" è stato tolto su richiesta
 * del proprietario, la forma basta a distinguerle e le quattro chiudono l'ultima riga. Le conquistate sono piene, le altre spente con il
 * progresso sotto; il tocco apre sotto la griglia il perché e, se non conquistata, la barra e un tasto verso ciò che manca.
 * Derivata dalla collezione, mai salvata. Da Free si sfoca con le altre card ([StatCard]).
 */
@Composable
internal fun BadgesCard(badges: List<Badge>, onGo: (BadgeTarget) -> Unit) {
    if (badges.isEmpty()) return
    // L'ultima medaglia toccata resta in memoria anche a dettaglio chiuso: serve a farlo richiudere con la sua animazione.
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    var open by rememberSaveable { mutableStateOf(false) }
    val selected = badges.firstOrNull { it.id.name == selectedName }
    StatCard(StatsCard.BADGES.title, StatsCard.BADGES.icon) {
        Hint("${badges.count { it.earned }} of ${badges.size} earned. Tap one for details.", Modifier.padding(top = 2.dp))
        BadgeGrid(
            badges = badges,
            selected = selected,
            open = open,
            onGo = onGo,
            modifier = Modifier.padding(top = 14.dp),
        ) { tapped ->
            if (open && selected?.id == tapped.id) {
                open = false
            } else {
                selectedName = tapped.id.name
                open = true
            }
        }
    }
}

/**
 * La griglia, quattro per riga. Il dettaglio di una medaglia si apre SUBITO SOTTO la sua riga (prima stava sotto tutta la griglia,
 * quattro righe più in basso e spesso fuori schermo), con la stessa apertura dall'alto della History ([Reveal]).
 */
@Composable
private fun BadgeGrid(
    badges: List<Badge>,
    selected: Badge?,
    open: Boolean,
    onGo: (BadgeTarget) -> Unit,
    modifier: Modifier = Modifier,
    onTap: (Badge) -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        badges.chunked(BADGES_PER_ROW).forEach { row ->
            // Riga e dettaglio sono UN elemento: con lo spazio tra gli elementi un dettaglio chiuso (altezza zero) lascerebbe 10 dp in più.
            Column {
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEach { badge ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                            BadgeCell(badge, open && badge.id == selected?.id, reserveProgress = row.any { !it.earned }, onTap = onTap)
                        }
                    }
                    // Una riga corta lascia lo spazio delle medaglie che mancano, così le colonne restano allineate.
                    repeat(BADGES_PER_ROW - row.size) { Box(modifier = Modifier.weight(1f)) }
                }
                if (selected != null) {
                    Reveal(visible = open && row.any { it.id == selected.id }) { BadgeDetail(selected, onGo) }
                }
            }
        }
    }
}

@Composable
private fun BadgeCell(badge: Badge, selected: Boolean, reserveProgress: Boolean, onTap: (Badge) -> Unit) {
    val copy = badge.id.copy()
    val shape: Shape = if (badge.id.isFun) RoundedCornerShape(14.dp) else CircleShape
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else Color.Transparent)
            .clickable { onTap(badge) }
            .semantics {
                role = Role.Button
                contentDescription = "${copy.name}, ${if (badge.earned) "earned" else "not yet, ${cellProgress(badge)}"}"
            }
            .padding(horizontal = 2.dp, vertical = 4.dp),
    ) {
        Medal(badge, copy, shape)
        // La cella ha una larghezza fissa (un quarto della card): con il font al 130% "countries" non ci stava e si spezzava a metà parola
        // ("countrie" / "s"). Qui il testo cresce al massimo del 115%, come la pillola "COMMON ISSUE" in Browse; il resto della pagina segue il font.
        val capped = LocalDensity.current.let { Density(it.density, fontScale = it.fontScale.coerceAtMost(MAX_LABEL_FONT_SCALE)) }
        CompositionLocalProvider(LocalDensity provides capped) {
        Text(
            text = copy.name,
            style = MaterialTheme.typography.bodySmall,
            color = if (badge.earned) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            // Larghezza massima: "Big spender" e "Triple threat" riempivano la cella e si toccavano; così vanno a capo con un po' d'aria.
            modifier = Modifier.padding(top = 6.dp).widthIn(max = 68.dp),
        )
        // La riga del progresso c'è anche per le conquistate, ma solo se nella riga c'è almeno una medaglia da fare: una riga
        // tutta conquistata non lascia uno spazio vuoto sotto le etichette.
        if (!badge.earned || reserveProgress) {
            Text(
                text = if (badge.earned) " " else cellProgress(badge),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
        }
        }
    }
}

/** "37 / 40"; per i soldi la percentuale (64% di €100), perché "€64.00 / €100.00" non sta sotto una medaglia. */
private fun cellProgress(b: Badge): String =
    if (b.id == BadgeId.BIG_SPENDER) "${b.progress * 100 / b.target}%" else "${b.progress} / ${b.target}"

@Composable
private fun Medal(badge: Badge, copy: BadgeCopy, shape: Shape) {
    val primary = MaterialTheme.colorScheme.primary
    val dash = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val radius = if (badge.id.isFun) 14.dp else MedalSize / 2
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(MedalSize)
            .then(
                if (badge.earned) {
                    Modifier.clip(shape).background(primary)
                } else {
                    // Bordo tratteggiato = "ancora da conquistare", senza colore di stato: il pieno è l'unico segnale forte.
                    Modifier.drawBehind {
                        val stroke = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                        )
                        val inset = stroke.width / 2
                        drawRoundRect(
                            color = dash,
                            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                            size = Size(size.width - stroke.width, size.height - stroke.width),
                            cornerRadius = CornerRadius(radius.toPx()),
                            style = stroke,
                        )
                    }
                },
            ),
    ) {
        val tint = if (badge.earned) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        if (copy.art != null) {
            Canvas(modifier = Modifier.size(30.dp)) { drawBadgeArt(copy.art, tint) }
        } else if (copy.icon != null) {
            Icon(imageVector = copy.icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
        }
    }
}

/** Il perché di una medaglia: conquistata con la sua riga, altrimenti barra, progresso e il tasto verso ciò che manca. */
@Composable
private fun BadgeDetail(badge: Badge, onGo: (BadgeTarget) -> Unit) {
    val copy = badge.id.copy()
    Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(copy.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Hint(if (badge.earned) "Earned" else "Not yet")
        }
        Hint(copy.how, Modifier.padding(top = 4.dp))
        if (badge.earned) {
            if (badge.note != null) {
                Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text(badge.note, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 6.dp))
                }
            }
        } else {
            StatBar(Progress(badge.progress, badge.target).fraction, Modifier.padding(top = 10.dp))
            val line = listOfNotNull(
                if (badge.id == BadgeId.BIG_SPENDER) null else cellProgress(badge),
                badge.note,
            ).joinToString(" · ")
            if (line.isNotEmpty()) Hint(line, Modifier.padding(top = 6.dp))
            if (badge.go != null && badge.goLabel != null) {
                OutlinedButton(
                    onClick = { onGo(badge.go) },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) { Text(badge.goLabel, maxLines = 1) }
            }
        }
    }
}
