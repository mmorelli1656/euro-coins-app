package com.michele.eurocoins.ui.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.michele.eurocoins.R
import com.michele.eurocoins.data.Coin
import com.michele.eurocoins.data.displayCountry
import com.michele.eurocoins.data.flagEmoji

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinListScreen(
    viewModel: CoinListViewModel,
    onCoinClick: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val sections = remember(state.coins, state.groupMode) { groupCoins(state.coins, state.groupMode) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by country or theme…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )

            GroupModeSelector(
                selected = state.groupMode,
                onSelected = viewModel::onGroupModeChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )

            Text(
                text = "${state.coins.size} coins" + if (state.query.isNotBlank()) " found" else "",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            CoinSectionList(sections = sections, groupMode = state.groupMode, onCoinClick = onCoinClick)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupModeSelector(
    selected: GroupMode,
    onSelected: (GroupMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = GroupMode.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = selected == mode,
                onClick = { onSelected(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                label = { Text(mode.label) },
            )
        }
    }
}

private val GroupMode.label: String
    get() = when (this) {
        GroupMode.FLAT -> "List"
        GroupMode.BY_YEAR -> "By year"
        GroupMode.BY_COUNTRY -> "By country"
    }

/** Una sezione della lista: [header] è null per la vista piatta (nessun titolo mostrato). */
private data class CoinSection(val header: String?, val flag: String?, val coins: List<Coin>)

/**
 * Raggruppa mantenendo l'ordinamento già applicato dalla query Room
 * (`ORDER BY anno DESC, paese ASC`, vedi CoinDao.observeAll): per
 * [GroupMode.BY_YEAR] quell'ordine è già quello desiderato (anno
 * decrescente), qui serve solo spezzarlo in sezioni senza un secondo sort.
 * [GroupMode.BY_COUNTRY] invece riordina alfabeticamente le sezioni (non
 * l'ordine di prima apparizione nella lista anno-decrescente), perché per
 * una vista "sfoglia per paese" è quello che ci si aspetta.
 */
private fun groupCoins(coins: List<Coin>, mode: GroupMode): List<CoinSection> = when (mode) {
    GroupMode.FLAT -> listOf(CoinSection(header = null, flag = null, coins = coins))
    GroupMode.BY_YEAR -> coins
        .groupBy { it.anno }
        .toSortedMap(compareByDescending { it })
        .map { (year, list) -> CoinSection(header = year.toString(), flag = null, coins = list) }
    GroupMode.BY_COUNTRY -> coins
        .groupBy { it.displayCountry() }
        .toSortedMap()
        .map { (country, list) -> CoinSection(header = country, flag = list.first().flagEmoji(), coins = list) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CoinSectionList(
    sections: List<CoinSection>,
    groupMode: GroupMode,
    onCoinClick: (Long) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        sections.forEach { section ->
            if (section.header != null) {
                stickyHeader(key = "header_${groupMode}_${section.header}") {
                    SectionHeader(text = section.header, flag = section.flag)
                }
            }
            items(section.coins, key = { it.id }) { coin ->
                CoinRow(coin = coin, onClick = { onCoinClick(coin.id) })
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, flag: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        flag?.let {
            Text(text = it, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 10.dp))
        }
        Text(text = text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun CoinRow(coin: Coin, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoinThumbnail(coin)
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(
                text = "${coin.displayCountry()} · ${coin.anno}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = coin.tema,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CoinThumbnail(coin: Coin) {
    val hasImage = !coin.immaginePlaceholder && coin.urlImmagineFonte != null
    Box(
        modifier = Modifier
            .size(52.dp)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (hasImage) {
            SubcomposeAsyncImage(
                model = coin.urlImmagineFonte,
                contentDescription = coin.tema,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            ) {
                // Distinto dal ramo "else" qui sotto: quello è "la fonte non
                // ha ancora pubblicato l'immagine" (dato), questo è "il link
                // c'era ma il caricamento è fallito ora" (rete/link morto) —
                // vedi scripts/validate_image_links.py nella pipeline dati.
                if (painter.state.value is AsyncImagePainter.State.Error) {
                    Icon(
                        imageVector = Icons.Filled.BrokenImage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp),
                    )
                } else {
                    SubcomposeAsyncImageContent()
                }
            }
        } else {
            Icon(
                imageVector = Icons.Filled.MonetizationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}
