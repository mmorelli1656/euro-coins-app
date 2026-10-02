# euro-coins-app

## Scopo del progetto

App Android per sfogliare il catalogo delle monete da 2€ commemorative
dell'Eurozona (2004-oggi) e, da settembre 2026, anche le serie divisionali
(1 cent - 2 euro) per paese — "Regular Issues" nella Home. Consuma i
dataset prodotti dalla pipeline dati in un repo separato —
**[euro-coins-data-pipeline](../euro-coins-data-pipeline)** — e non fa
scraping né validazione dati di persona: quella responsabilità resta
interamente nella pipeline.

## Come i dati arrivano nell'app

1. La pipeline produce `data/processed/coins_with_mintages.jsonl` (i record
   BCE/Wikipedia/EUR-Lex-Cellar di `ecb_coins.jsonl` — schema pydantic
   `MonetaCommemorativa`, vedi `src/models.py` in quel repo — arricchiti con
   le tirature per finitura da Numista, `tiratura_numista_*`). È il file da
   usare, non `ecb_coins.jsonl` da solo: è un suo superset, stessi 584
   record più questi campi.
2. Quel JSONL viene esportato come array JSON unico in
   `app/src/main/assets/coins.json` (vedi comando sotto). **Non è generato
   automaticamente da uno script di questo repo** — va rieseguito a mano
   quando il dataset della pipeline cambia:

   ```bash
   python -c "
   import json
   records = [json.loads(l) for l in open(r'..\euro-coins-data-pipeline\data\processed\coins_with_mintages.jsonl', encoding='utf-8')]
   json.dump(records, open('app/src/main/assets/coins.json', 'w', encoding='utf-8'), ensure_ascii=False, separators=(',', ':'))
   "
   ```

   Se `python` non è disponibile sulla macchina (capitato: solo lo stub
   Microsoft Store su `PATH`), lo stesso risultato in PowerShell:

   ```powershell
   $records = Get-Content "..\euro-coins-data-pipeline\data\processed\coins_with_mintages.jsonl" -Encoding UTF8 | ForEach-Object { $_ | ConvertFrom-Json }
   $json = $records | ConvertTo-Json -Compress -Depth 5
   [System.IO.File]::WriteAllText("app\src\main\assets\coins.json", $json, [System.Text.UTF8Encoding]::new($false))
   ```

   Differenza innocua rispetto all'output Python: `ConvertTo-Json` esegue
   l'escape di alcuni caratteri come l'apice (diventa la sequenza `'`)
   che Python lascerebbe letterale — JSON valido in entrambi i casi,
   kotlinx.serialization li legge identici.

3. A ogni avvio `CoinRepository.ensureSeeded()` confronta l'hash SHA-256
   dell'asset con quello salvato (SharedPreferences `dataset`): se è il primo
   avvio o l'asset è cambiato, ripopola il database Room locale (`coins.db`)
   in una transazione (`CoinDao.replaceAll`), altrimenti non fa nulla. Senza
   questo, un telefono che aveva già seminato una versione precedente
   mostrava per sempre i dati vecchi. **Gli id delle monete vengono
   rigenerati a ogni ripopolamento**: i futuri dati utente (posseduta,
   qualità, prezzo) NON vanno agganciati a `Coin.id` ma a una chiave naturale
   stabile, in una tabella separata.
4. **Le immagini non sono bundlate nell'APK**: vengono caricate on-demand
   con Coil direttamente dagli URL originali della fonte
   (`Coin.urlImmagineFonte`, salvato così com'era in `data/processed/`),
   con cache su disco automatica di Coil dopo il primo caricamento. Scelta
   deliberata (vedi conversazione che ha avviato questo repo): APK leggero
   a scapito di richiedere rete la prima volta che si vede un'immagine.
5. **Stesso schema per le serie divisionali**: la pipeline produce
   `data/processed/ec_national_sides.jsonl` (schema pydantic
   `SezioneSerieDivisionale`/`ImmagineTaglio`, vedi `src/models.py` in quel
   repo — una riga per "serie" nazionale delle monete 1c-2€, non per
   moneta), esportato allo stesso modo in
   `app/src/main/assets/regular_issues.json`:

   ```powershell
   $records = Get-Content "..\euro-coins-data-pipeline\data\processed\ec_national_sides.jsonl" -Encoding UTF8 | ForEach-Object { $_ | ConvertFrom-Json }
   $json = $records | ConvertTo-Json -Compress -Depth 8
   [System.IO.File]::WriteAllText("app\src\main\assets\regular_issues.json", $json, [System.Text.UTF8Encoding]::new($false))
   ```

   `-Depth 8` e non 5 come per `coins.json`: qui i record hanno un campo
   annidato (`immagini`, fino a 8 oggetti per serie), che `coins.json` non
   ha. Stesso meccanismo di seeding (`RegularIssueRepository.ensureSeeded()`,
   hash SHA-256 dell'asset, SharedPreferences separate `regular_issues_dataset`)
   e stessa scelta sulle immagini (hotlink, non bundlate).

## Stack tecnico

- Kotlin, Jetpack Compose (Material 3) — nessuna View/XML per la UI
- Room per la persistenza locale
- Coil 3 (`coil3.compose.AsyncImage`) per il caricamento immagini on-demand
- kotlinx.serialization per il parsing di `coins.json`
- Navigation Compose per la navigazione tra schermate
- Haze (`dev.chrisbanes.haze`) per il blur reale della barra flottante
- Credential Manager + Play Services Auth per il login Google del backup; Drive
  via REST diretto (nessuna libreria client Google)
- Coil con `coil-network-okhttp` (Coil 3 non include il client di rete)
- minSdk 26, target/compileSdk 37, AGP 9.4.0, Gradle 9.6.0 — stessi valori
  usati in BtAuto, per coerenza tra i progetti Android di questa macchina

Nessuna dependency injection framework (niente Hilt): singleton manuali in
`EuroCoinsApplication` + `viewModelFactory { initializer { ... } }` per i
ViewModel. Sufficiente per la dimensione attuale dell'app; da rivalutare se
cresce.

## Struttura del progetto

```
app/src/main/java/com/michele/eurocoins/
├── MainActivity.kt
├── EuroCoinsApplication.kt   # singleton di repository/database
├── data/
│   ├── Coin.kt               # @Entity Room
│   ├── CoinJson.kt           # forma di assets/coins.json + mapping a Coin
│   ├── CoinDao.kt
│   ├── CoinDatabase.kt
│   ├── CoinRepository.kt     # seeding da asset + esposizione Flow
│   ├── CountryNames.kt       # Coin.displayCountry() — nome paese in UI
│   ├── CoinTitle.kt          # Coin.displayTema() — titolo in UI (ordinali "550Th" → "550th")
│   ├── CoinCredits.kt        # Coin.displayMint()/displayEngraver()/displayDesigner()
│   ├── CountryFlags.kt       # Coin.flagEmoji() — bandiera da codice ISO
│   ├── CollectionProgress.kt # Progress (x / y possedute)
│   ├── CoinKey.kt            # Coin.stableKey — chiave stabile per la collezione
│   ├── CoinQuality.kt        # Standard / BU / Proof
│   ├── CollectionItem.kt     # @Entity: moneta posseduta in una qualità
│   ├── CollectionDao.kt
│   ├── Microstates.kt        # MICROSTATE_PAESI + Coin.isMicrostate (filtro "Hide microstates")
│   ├── RegularIssue.kt       # @Entity RegularIssueSeries + RegularIssueImage (TypeConverter JSON)
│   ├── RegularIssueJson.kt   # forma di assets/regular_issues.json + mapping a RegularIssueSeries
│   ├── RegularIssueDao.kt
│   ├── RegularIssueRepository.kt   # seeding da asset + esposizione Flow, dataset separato da CoinRepository
│   ├── RegularIssueCountryNames.kt # RegularIssueSeries.displayCountry() — mappa esaustiva (no zeccaRaw inglese qui)
│   ├── RegularIssueKey.kt    # RegularIssueSeries.stableKey — chiave stabile per la collezione (paese + ordineCronologico)
│   ├── RegularCollectionItem.kt # @Entity: taglio posseduto in un'annata+qualità + RegularCollectionEntry (bozza pannello)
│   ├── RegularCollectionDao.kt
│   ├── RegularMintageSummary.kt # summarizeMintages()/groupMintagesByYear() — logica pura, testata
│   └── backup/               # BackupFile, GoogleAccountManager, DriveBackupClient, BackupService
└── ui/
    ├── theme/                # palette "verdigris/bronzo" coerente col
    │                         # report di riconciliazione della pipeline dati
    ├── components/           # CollectionProgressBar, CollectionSheet (qualità + prezzo),
    │                         # RegularCollectionSheet (qualità + annate multiple per le Regular Issues),
    │                         # PriceFormat, FloatingSearchBar (vetro/Haze), FilterSheet
    ├── home/                 # ingresso: due tile (commemorative / regular issues)
    ├── browse/               # commemorative: Years / Countries / All
    ├── list/                 # elenco filtrato (CoinFilter), CoinListOptions, ricerca
    ├── regular/               # Regular Issues: griglia paesi + serie del paese + collezione per taglio
    │                         # + RegularDenominationDetailScreen (dettaglio taglio, building
    │                         # block riusati da ui/detail/)
    ├── settings/             # SettingsScreen unificata, SettingsViewModel, UserSettings (prefs `settings`)
    ├── backup/               # BackupSection (sezione account/backup di Settings), BackupViewModel
    ├── detail/                # dettaglio moneta, licenza/attribuzione immagine — DetailCard/
    │                         # SectionLabel/OwnedBadge/FooterLine/ValueLabel/DetailsSection
    │                         # esportati, riusati dal dettaglio taglio di Regular Issues
    └── navigation/           # home -> browse -> lista filtrata -> dettaglio; home -> regular issues -> dettaglio taglio; home -> backup
```

Navigazione: `HomeScreen` (start) → `BrowseScreen` (selettore Years /
Countries / All) → `CoinListScreen` filtrato per anno o paese (`CoinFilter`)
→ `CoinDetailScreen`. Years e Countries sono griglie di card; "All" è
l'elenco completo. **Emissioni comuni** (le 5 monete coniate congiuntamente
da tutti i paesi dell'Eurozona — 2007 Trattato di Roma, 2009 EMU, 2012 dieci
anni di euro, 2015 bandiera UE, 2022 Erasmus, `Coin.emissioneComune`): nella
griglia Years, l'anno con un'emissione comune ha **due card** invece di una
— quella normale (solo le monete del singolo paese, `CoinFilter.Year(year)`)
e una seconda identica con la pillola "COMMON ISSUE" (`CommonIssueBadge` in
`BrowseScreen.kt`) ancorata nell'angolo in alto a destra fuori dal flusso del
testo (`CoinFilter.Year(year, commonOnly = true)`) — in riga con la cifra
dell'anno cambiava l'altezza della card e rompeva l'allineamento con le
altre, scartato dopo un mockup. Nell'elenco (sia "All" sia per paese), le
monete di un'emissione comune hanno una piccola icona a globo accanto a
"Paese · Anno", tinta come quel testo (`primary`)
per leggersi come parte dell'etichetta invece che un accento nuovo. **Barra
flottante in basso** (`FloatingSearchBar`: pillola con ricerca + pulsante FILTER, sfondo vetro con blur reale via libreria Haze, `hazeSource` sulla lista/griglia sottostante; sotto Android 12 resta il solo fondo semitrasparente) in ogni scheda di Browse e in ogni lista filtrata; ogni scheda ha query e filtri propri. Il pannello FILTER (`FilterSheet`) contiene anche l'ordinamento (Years: dal più recente / dal 2004; Countries: A → Z / Z → A; liste: per anno o paese) più filtri Collection (All/Incomplete/Complete sulle griglie, All/Owned/Missing + qualità sulle liste); il pallino sul pulsante segnala un filtro attivo. Liste e griglie lasciano `floatingBarClearance()` di padding in fondo. La griglia (e ora anche l'elenco) torna in cima a ogni cambio d'ordine: lo stato di scorrimento si ricrea con `key(...)` nella stessa composizione, NON con un `LaunchedEffect`, che arrivava un fotogramma dopo e faceva vedere l'ordine nuovo scorso a metà (scritte che sembravano sovrapporsi). **Elenco monete**: card ad **altezza FISSA 72 dp** (`.height(72.dp)`: non varia con la lunghezza del titolo; miniatura 52 dp (era 46 dp in area 52: rimessa a 52 su richiesta), MA il cerchio lilla del segnaposto è 50 dp (`PlaceholderSize`): le foto BCE hanno un margine bianco attorno alla moneta, e a pari riquadro il lilla pieno sembrava più grande (misurato sullo screenshot: 169 px contro 162-165, dopo 163), sottotitolo 13 sp Bold in `primary`, titolo `bodyMedium` SemiBold max 2 righe con ellissi, il testo intero sta nel dettaglio; scelta dopo mockup A+Y, scartate 88 dp a 3 righe e sottotitolo in pillola lilla); il tocco sulla miniatura apre il dettaglio come il resto della riga. Titolo e paese passano da `displayTema()`/`displayCountry()`; testi con 12 dp a destra (prima della casella). Senza foto o foto che non si carica (anche offline con cache svuotata): la stessa icona `€`; MENTRE la foto arriva solo il cerchio lilla, senza icona (il `€` a ogni riapertura sembrava un riscaricamento; vedi § Decisioni di prodotto, "Stato di `SubcomposeAsyncImage`"). L'icona è `Icons.Filled.EuroSymbol` (il glifo pieno: l'outline sottile `Outlined.Euro` "sembrava strano"; scartate anche la 2€ disegnata e una moneta con € dentro) su cerchio lilla, non più SOTTO la foto ma solo dove serve: niente icone diverse per "non pubblicata" e "non caricata". `CoinImageDialog` (foto grande, "Close"/"Details") esiste ancora ma è scollegato: per riattivarlo decommentare il blocco `zoomed` in `CoinListContent` e passare `onImageClick` a `CoinRow`. `PrefetchThumbnails` accoda in Coil le foto delle 24 monete oltre l'ultima visibile, così sono già nella cache su disco quando la riga arriva (le foto pesano ~130 KB l'una da BCE, vedi Decisioni di prodotto). Regular Issues (§ omonima più sotto) riusa questo stesso trattamento di caricamento/fallback per le immagini dei tagli, non questo elenco: ha una sua schermata. Dettagli della barra non ovvi: **testo e cursore vivono nella barra** (`TextFieldValue` locale, `query` vale solo come valore iniziale): il valore che tornava da un StateFlow del ViewModel arrivava con qualche fotogramma di ritardo e un `BasicTextField(String)` che riceve un valore vecchio riporta indietro testo e cursore (cursore dopo la terza lettera, caratteri persi, blocco in Years); tutta la metà sinistra (lente, margini, altezza intera) è cliccabile e porta il focus al campo (`FocusRequester` + `keyboard.show()`), perché il `BasicTextField` è alto quanto una riga di testo e toccare sopra, sotto o sulla lente non apriva la tastiera; alta 72 dp e larga quasi tutto lo schermo (margini 8 dp) per coprire per intero la riga sottostante; fondo molto opaco (0.84 scuro, 0.78 chiaro; era 0.94/0.88, ridotto a vista) perché con testo chiaro su fondo scuro il solo blur lascia il testo leggibile; sta in un Box esterno a schermo intero che assorbe i tocchi ("zona morta", `BarDeadZone` sopra + margine sotto) per non aprire monete vicine per errore (blocca anche il trascinamento iniziato lì). **Tastiera**: `MainActivity` ha `windowSoftInputMode="adjustNothing"` e la barra si solleva con `WindowInsets.ime`/`navigationBars` via `offset`, senza `imePadding()` e senza molle: con il ridimensionamento della finestra attivo l'altezza della tastiera veniva contata due volte, e una molla sopra l'animazione di sistema partiva in ritardo.
Il paese si passa in rotta come `Coin.paese` (valore stabile, non il nome
mostrato) con `Uri.encode`, perché "Città del Vaticano" e "Paesi Bassi"
hanno spazi/accenti.

### Home

Due schede di **pari peso** (14 dp tra le due; altezza: vedi sotto, con scroll di riserva),
stessa struttura (`CardContent`): fascia di 4 monete a
bordo scheda (fascia FISSA da 153 dp (era 160: -10% di aria sopra/sotto le monete). ALTEZZA delle schede: `heightIn(min = (spazio - 14 dp) / 2)` con tetto `CardMaxHeight` = 320 dp — dividono lo spazio disponibile, sugli schermi alti l'avanzo resta libero in fondo (dove andrà il banner, nel `bottomBar` dello Scaffold, uguale per base e Pro: nessun ramo Pro), e mai sotto il contenuto; la colonna ha `verticalScroll` di riserva per schermi bassi o banner. L'avanzo dentro la scheda va tra i testi (`SpaceEvenly`), non alla fascia. Storia: la fascia che prendeva tutto l'avanzo (~185 dp) lasciava le monete "perse in un deserto" (sono limitate dalla larghezza), schede compatte da 128 dp lasciavano la pagina vuota e la variante "superficie unica" con card `wrapContent` e monete su fondo pulito è stata provata e bocciata; 160 dp è il compromesso; cerchi sovrapposti di 14 dp, i due centrali più grandi), titolo,
**riga unica di dati** (`StatsLine`, 15 sp, titolo 26 sp, numeri in grassetto: "**584** coins · **24** countries · **2004–2025**") e
footer. La riga unica ha sostituito una griglia di tre statistiche a 22 sp (le colonne non
stavano centrate e costavano ~50 dp che ora vanno alla fascia; in cambio i numeri pesano
meno: se servisse più peso, tornare alla griglia — commit 48df1ca). Scelta dopo mockup (X fascia in alto, scartata Y foto a sinistra + griglia
2×2; prima ancora A numero grande, B medaglione disegnato, C tre statistiche): le
foto stanno in una fascia orizzontale perché il mosaico 2×2/3×3 e le schede basse
davano problemi di proporzioni/spazio vuoto — non riproporli. Foto = **cerchi
ritagliati** dalle foto BCE, scelte da `pickShowcase` (a rotazione giornaliera, vedi § Impostazioni "Rotate home coins"), zoom 1.05 (taglia l'anello bianco; qualche moneta può
risultare poco centrata, accettato). Nessun contatore globale. **Commemorative**
(attiva, cliccabile per intero, angoli 22 dp, fondo `primary`): riga dati con gli anni come
**intervallo** e barra "x / y collected" con onda — numeri dal database. **Regular Issues**
(attiva dal dataset `ec_national_sides`, § omonima più sotto; stesso trattamento pieno di
Commemorative, non più tratteggiata/"Coming soon"): fascia di 4 **foto reali** di tagli diversi,
di paesi diversi, con la STESSA rotazione giornaliera di Commemorative e la stessa impostazione
("Rotate home coins" — vedi sotto), moneta disegnata (`RegularCoin`/`RegularCoins`, non più
`FallbackCoins`: tematicamente sono proprio le monete circolanti) come ripiego se la foto manca o
non carica; riga dati "**N** coins · **M** countries · **P** series" (in quest'ordine: "coins" qui
sono i disegni di taglio noti, non le 4 monete della fascia — coerente con "584 coins" di
Commemorative, il totale del catalogo; terzo valore "series", non un intervallo di anni: nessuno
è affidabile per le serie, vedi § dataset), e sotto la STESSA barra "liquido" di Commemorative,
ora reale come quella di Commemorative: conta i tagli distinti posseduti in almeno un'annata
su N (`HomeViewModel.regularIssueOwnedCount`, vedi § Regular Issues per il dettaglio). La home è anche dove parte il
seeding di ENTRAMBI i database (`HomeViewModel` chiama `repository.ensureSeeded()` e
`regularIssueRepository.ensureSeeded()`; il Mutex in ciascun repository evita il doppio
inserimento se più ViewModel lo chiamano). In alto
a destra un'icona ingranaggio apre le Impostazioni (unico accesso: la pillola del tema e l'icona profilo non ci sono più).

## Collezione utente

Ogni moneta ha una **casella** nell'elenco (vuota / piena con spunta). Un
tocco apre un pannello dal basso (`CollectionSheet`) con le tre qualità —
**Standard, BU, Proof**, anche più di una insieme — e, per ciascuna
spuntata, il prezzo pagato (facoltativo, in euro, salvato in centesimi).
Lo stesso pannello si apre dal dettaglio ("Add to collection" / "Edit collection"
nella card COLLECTION), così c'è un solo modo di registrare.

- Il pannello lavora su una **bozza** e scrive solo con "Save": chiuderlo
  senza salvare non cambia nulla (un tocco sbagliato non cancella un
  prezzo). Alla prima apertura di una moneta non posseduta "Standard" è già
  spuntata.
- Una moneta conta come "posseduta" (casella piena, barre "x / y collected"
  di home/anni/paesi) se ha almeno una qualità.
- Il salvataggio sostituisce in blocco le qualità della moneta
  (`CollectionDao.replaceForCoin`, transazione) e conserva `addedAt` delle
  voci già esistenti.
- **Tastiera nel pannello**: il contenuto usa
  `windowInsetsPadding(navigationBars.union(ime))` e non `navigationBarsPadding()` +
  `imePadding()` insieme, che contavano due volte la barra di navigazione (già inclusa
  nell'altezza della tastiera). Non verificato con la tastiera aperta sul telefono.
- **Layout del pannello** (`CollectionSheet.kt`): titolo a max 2 righe,
  sottotitolo "Paese · Anno", fondo grigio-verde del tema. Una **card da 64 dp
  per finitura** (Standard "Circulation", BU "Brilliant Uncirculated", Proof
  "Mirror finish"): checkbox + etichette a sinistra, prezzo a destra. Il
  campo prezzo è SEMPRE presente (non spuntata: testo piatto al 38%, non
  editabile), così il pannello non cambia altezza a ogni tocco. Dettagli non
  ovvi, tutti scelti dopo mockup e prove su telefono:
  - **Card spuntata: solo bordo, mai fondo pieno** (`FinishCard`): prima il fondo
    diventava `secondaryContainer` (lilla) a tutta card; con la spunta verde già
    presente il lilla pieno era ridondante ("pesante" secondo l'utente) e competeva
    con il verde come segnale di stato. Ora il fondo resta sempre `colors.surface`
    (bianco/nero della card) e solo il bordo cambia: 2.5 dp pieno (non più 1.5 dp al
    40% di opacità) quando spuntata, trasparente quando no — su fondo bianco un
    bordo sottile e sbiadito si vedeva poco. Stesso trattamento sulla pillola
    "COLLECTION" del dettaglio (§ Dettaglio moneta, sotto), bordo lì 2 dp.
  - **Campo prezzo = pillola a dimensioni FISSE** (altezza 40 dp, larghezza
    misurata su "0000.00" con `rememberTextMeasurer`), bordo viola
    (`PurpleField*` in `Color.kt`, contrasto ~3.4:1 sul lilla, varianti chiare
    per il tema scuro, verificato su telefono) e "€" viola scuro nello stesso
    `decorationBox`, con lo stesso stile delle cifre. Larghezza legata al testo
    scartata: cambiava a ogni cifra e disallineava le card; l'altezza va
    fissata perché il segnaposto "0.00" fa più altezza del campo con una cifra
    e la pillola si assottigliava appena si scriveva. Un `BasicTextField` a
    riga singola si allarga a tutto lo spazio libero: la larghezza va sempre
    imposta dall'esterno.
  - **Tetto 9999.99** (`sanitizePrice`: solo cifre e un separatore, max 4
    intere e 2 decimali); `,` e `.` accettati ma nel box compare sempre `.`. Un
    valore già salvato oltre il tetto non entra nella pillola e non viene
    modificato finché non si riscrive.
  - **Tocco**: l'intera card è `toggleable` (dopo il `clip` a 16 dp, un solo
    ripple); il campo prezzo attivo prende i propri tocchi (focus + tastiera
    numerica), quello disattivato lascia passare il tocco alla card e la
    spunta. Nessun focus automatico alla spunta (aprirebbe la tastiera a ogni
    tocco). Scartato il solo lato sinistro: un ripple ritagliato su quell'area
    lasciava un sottorettangolo visibile. Effetto collaterale: per TalkBack la
    card è un unico elemento e il campo non è separato.

- **Tabella separata `collection_items`** (`CollectionItem`), chiave
  primaria (`coinKey`, `quality`): sono dati dell'utente, non del catalogo, e
  non vengono mai toccati dal ripopolamento di `coins`.
- **Chiave della moneta = `Coin.stableKey`** (`CoinKey.kt`: fonte + anno +
  paese + tema normalizzato), NON `Coin.id`, che viene rigenerato a ogni
  aggiornamento del dataset. Univoca sulle 584 monete. Punto debole noto: se
  la pipeline correggesse il testo di `tema`, la chiave cambierebbe e le voci
  resterebbero orfane; per questo ogni voce conserva anche anno/paese/tema di
  quando è stata salvata. Soluzione definitiva: un id stabile emesso dalla
  pipeline dati.
- **Migrazioni Room esplicite** (DB versione 7, `CoinDatabase.kt`), mai
  `fallbackToDestructiveMigration`: distruggerebbe anche la collezione
  dell'utente. `MIGRATION_1_2` (tabella `collection_items`), `MIGRATION_2_3`
  (`coins.emissioneComune`), `MIGRATION_3_4` (`coins.tiraturaNumista{Standard,Bu,Proof}`,
  colonne nullable, niente `DEFAULT`), `MIGRATION_4_5`
  (`coins.incisoreRetroRaw`/`disegnatoreRetroRaw`, stesso pattern nullable),
  `MIGRATION_5_6` (`CREATE TABLE regular_issue_series`, § Regular Issues —
  tabella nuova, non un `ALTER` su `coins`), `MIGRATION_6_7`
  (`CREATE TABLE regular_collection_items`, § Regular Issues — collezione
  utente sulle monete circolanti, stesso motivo). Ogni migrazione aggiunta va accodata,
  mai riscritta sopra una già rilasciata (anche in sviluppo: una volta
  installata su un telefono di prova, quel numero di versione è "usato"). Il
  valore delle nuove colonne conta poco: `ensureSeeded()` ripopola comunque
  `coins` da zero appena l'hash dell'asset cambia, la migrazione serve solo a
  far coincidere lo schema SQLite con l'entity Kotlin nel frattempo. Il SQL
  di ogni migrazione deve coincidere con quello generato da Room
  (`build/generated/ksp/.../CoinDatabase_Impl.kt`).
- Non ancora fatto: note libere, data di acquisto, valuta diversa
  dall'euro, export CSV.

### Impostazioni

Schermata unica (`SettingsScreen`), sezioni: Account and backup (con la card Go Pro
sotto, senza titolo proprio), Catalog and display, Appearance, Danger zone.
Ordine interno delle card, uguale in ogni sezione: prima gli interruttori (`SwitchRow`), poi i selettori a segmenti (`SegmentedChoice`), separati da un filetto. Titoli con "and", non "&" (coerenza con "Account and backup").

**Palette: tre livelli visibili** — neutro (sfondi, testi, titoli), lilla (selezioni) e
rosso (distruttivo), con il verdigris riservato alle azioni (avatar, "Back up now",
"Restore", "Sign out", switch acceso). Scelta fatta su mockup (l'alternativa, tutto
l'interattivo in lilla, avrebbe tolto peso a "Back up now", perché un lilla pallido non
regge un pulsante pieno):
- **Titoli di sezione** in `onSurface` Bold, NON verdigris (sembravano azioni); anche
  "Danger zone" è neutro, il rosso sta solo sulla card di reset (bordo, icona, titolo e
  sottotitolo).
- **`SegmentedButton`** (Default tab, Theme): selezionato `secondaryContainer` (lilla),
  inattivo trasparente, bordo `onSurfaceVariant` al 40% — l'`outline` del tema scuro
  (`34351F`) è quasi uguale alla card e il bordo spariva —, `icon = {}` (la spunta di
  default spostava l'etichetta e sbilanciava le larghezze) e `weight(1f)` su ogni segmento.
- **Switch** "Hide microstates": colori espliciti anche da spento (bordo e pallino
  `onSurfaceVariant`, traccia `background`) per lo stesso motivo; spunta nel pallino solo
  da acceso.


- **Hide microstates** (Andorra, Monaco, San Marino, Città del Vaticano,
  `MICROSTATE_PAESI`): il filtro sta in `CoinRepository.coins`/`paesi`
  (`combine` con `UserSettings.hideMicrostates`), quindi elenchi, griglie,
  ricerca e home lo rispettano tutti e i totali "x / y collected" escludono
  i microstati nascosti. Le monete già possedute restano nella collezione e
  nel backup.
- **Default tab**: scheda di Commemorative che si apre per prima
  (`UserSettings.defaultTab`, letto alla creazione del `BrowseViewModel`).
  Scartato il riordino completo dei segmenti: i segmenti restano Years /
  Countries / All.
- **Rotate home coins** (sezione Appearance, interruttore, **acceso di default**;
  `UserSettings.rotateHomeCoins`): le 4 monete della fascia della Home cambiano ogni giorno.
  Solo on/off, senza scegliere la frequenza (scelta dell'utente; scartati "a ogni apertura",
  che scarica 4 foto nuove a ogni avvio, e "settimanale"). `pickShowcase` (`HomeShowcase.kt`)
  sceglie con seme = `LocalDate.toEpochDay()`: DETERMINISTICO (stesso giorno = stesse monete,
  niente da salvare, foto in cache di Coil), una moneta per paese, solo con foto, rispetta Hide
  microstates perché parte da `repository.coins`. Spento: set fisso (i primi 4 paesi). La Home
  precarica in Coil le foto del set di DOMANI (`nextShowcase`): domani è già pronta, anche
  offline se oggi l'app è stata aperta online. **Foto che non si carica: catena per moneta** (`ShowcaseCoin`): foto di oggi → foto dello stesso slot dell'ultimo set mostrato per intero (`UserSettings.lastShowcase`, salvato quando le 4 sono arrivate; sta già nella cache su disco di Coil, quindi regge anche offline) → moneta DISEGNATA (2€ bimetallica, `FallbackCoins`) se non c'è altro. NON ci sono foto BCE nell'APK come set predefinito: licenza "uso editoriale", da chiarire prima di pubblicare (§ Backlog). **NESSUNA animazione di entrata** (scelta dell'utente: la dissolvenza per moneta di Coil dava comparse scaglionate; una dissolvenza coordinata dopo aver atteso tutte le foto risultava "lenta"; le monete devono esserci all'apertura, come prima della rotazione). Per esserci al primo fotogramma la Home non aspetta il database: `UserSettings` salva gli URL del set di OGGI e di DOMANI (`saveShowcaseUrls`, chiavi `showcase_day_<giorno>`) e `HomeViewModel` li usa subito all'avvio (ripiego: l'ultimo set mostrato); quando arriva il database il set calcolato coincide e non si nota nulla. Le foto sono già nella cache su disco perché la Home le precarica il giorno prima. (Prima non gestito: foto che non si carica il
  giorno stesso.) **Vale anche per la fascia di Regular Issues** (stessa impostazione, stesso
  `ShowcaseCoin`): `pickShowcase` è stato scomposto in un nucleo generico `pickByCountry`
  (paese → elemento, stessa logica "una scelta per paese, deterministica sul giorno") più due
  funzioni sottili in cima, `pickShowcase` (commemorative, invariata) e
  `pickRegularIssueShowcaseUrls` (appiattisce `RegularIssueSeries.immagini` di tutte le serie in
  coppie paese/URL, perché lì non esiste un'entità "moneta" singola come `Coin`). **Regular Issues
  ha ORA la stessa catena di ripiego/precaricamento di Commemorative** (allineata su richiesta
  esplicita, "devono seguire la stessa logica per coerenza e uniformità dell'app" — prima era
  un'estensione minima con solo "foto di oggi → moneta disegnata", `onLoaded` un no-op che non
  registrava mai l'ultimo set mostrato): `HomeViewModel` calcola anche `regularIssueNextShowcase`
  (precaricato in Coil da un secondo `LaunchedEffect` in `HomeScreen`, accanto a quello di
  Commemorative) e `regularShown`/`regularIssueLastShowcaseUrls` tracciano e salvano l'ultimo set
  mostrato per intero, stesso meccanismo di `shown`/`lastShowcaseUrls`. **Le funzioni equivalenti
  in `UserSettings.kt` lavorano su `List<String?>`, non `List<String>` come quelle Commemorative**:
  a differenza delle commemorative (`pickShowcase` restituisce sempre 4 monete dense, mai meno),
  una fascia di taglio di `pickRegularIssueShowcaseUrls` può restituire `null` (nessuna serie ha
  una foto per quella fascia) — filtrare i null prima di salvare sposterebbe gli URL nelle
  posizioni sbagliate rispetto al confronto posizionale di `ShowcaseCoin` (slot per slot), quindi
  `regularIssueLastShowcase`/`regularIssueShowcaseUrlsFor`/`saveRegularIssueShowcaseUrls` non
  filtrano le righe vuote come fanno gli equivalenti Commemorative, le preservano come `null`.
  **Prefisso delle chiavi "giorno" deliberatamente diverso**: `regular_showcase_day_` e non
  `showcase_day_regular_`, per non essere un sottoinsieme del prefisso Commemorative
  `showcase_day_` né viceversa — altrimenti il filtro a prefisso che pulisce le chiavi dei giorni
  passati in uno dei due `saveShowcaseUrls` avrebbe cancellato anche le chiavi dell'altro. Test:
  `HomeShowcaseTest` (solo `pickShowcase`, invariato dal refactoring — nessun test aggiunto per
  `pickRegularIssueShowcaseUrls`, anche dopo questo allineamento).
- **Reset collection**: dialog di conferma con il numero di monete; svuota
  solo `collection_items` (`CoinRepository.resetCollection`). Il backup su
  Drive non viene toccato: un nuovo backup dopo il reset lo sovrascrive.

### Backup su Google Drive

Sezione "Account and backup" della schermata Impostazioni (ingranaggio in
alto a destra nella home; la vecchia schermata Backup è stata assorbita): login con Google (Credential Manager) e backup/ripristino della
collezione su Drive.

- **UI** (`BackupSection`): senza accesso una card d'invito + "Sign in with
  Google"; con l'accesso l'email, una card di stato in evidenza ("Collection
  saved" + data dell'ultimo backup, o "Not backed up yet", con barra di
  avanzamento durante le operazioni) con "Last backup: <data>", "Back up now"
  (2/3 della riga, pieno) e "Restore" (1/3, a contorno: è quello che
  sovrascrive). "Sign out" è un TextButton nel colore primario a destra
  dell'email, sulla stessa riga. Lo stato non dice "up to date": confrontare backup e collezione locale non è
  implementato, quindi non lo si afferma.
- **Banner "Go Pro"** (rimozione pubblicità; card neutra come le altre, sotto quella del
  backup, senza titolo di sezione; l'accento è solo la corona nel viola `PurpleField*` su
  un cerchio `secondaryContainer`, non più il bronzo): oggi solo
  segnaposto, il tocco apre un avviso "coming soon". Non esistono ancora
  Play Billing, AdMob né consenso GDPR (UMP); l'app non è pubblica. Quando
  ci saranno: acquisto dal banner, banner nascosto per gli utenti Pro.
- **Sovrascrittura del backup**: "Back up now" controlla prima se su Drive
  esiste già un backup (`BackupViewModel.execute`, flag `confirmed`); se sì
  apre "Overwrite existing backup?" con la data e il numero di monete locali
  che lo sostituirebbero, e carica solo con "Overwrite". Motivo: su un telefono
  nuovo la collezione locale è vuota e cancellerebbe il backup. Il controllo
  sta nel ViewModel perché serve il token Drive, e il consenso potrebbe non
  esserci ancora.
- **Errori e stato di caricamento**: mai testo tecnico all'utente (niente
  `e.message`, DNS o JSON di Drive): `DriveBackupClient`/`GoogleAccountManager`/
  `BackupViewModel` producono messaggi d'uso ("Network unavailable. Please check your
  connection."). L'esito è un banner `InlineNotice` DENTRO la card dell'account (errore
  in rosso, successo in verdigris), non testo libero sotto le card: appariva e spariva
  causando layout shift. Durante il primo controllo su Drive lo `StatusBox` mostra
  spinner + "Checking backup status…" invece di "Not backed up yet". "Back up now" e
  "Restore" disabilitati hanno colori/bordo espliciti per restare leggibili.
- **Restore disabilitato solo se Drive è stato interrogato** e non ha un
  backup (`BackupUiState.backupChecked`). Senza consenso Drive (telefono
  nuovo, appena fatto l'accesso) la data resta null anche se il backup esiste:
  disabilitarlo lì bloccherebbe il caso d'uso principale del ripristino.
  Senza file su Drive, Restore mostra "No backup found on this Google account.".

- **Formato**: un unico JSON versionato (`BackupFile`, `schemaVersion`) con
  le voci di `collection_items`, agganciate a `coinKey` (= `stableKey`) —
  mai il catalogo. JSON e non CSV perché deve poter crescere (note, data di
  acquisto) senza rompere i backup vecchi.
- **Storage**: cartella `appDataFolder` di Drive (scope `drive.appdata`):
  privata e nascosta, l'app non vede altri file dell'utente. Un solo file
  (`euro-coins-collection.json`), ogni backup lo sovrascrive. Chiamate REST
  dirette in `DriveBackupClient` (nessuna libreria client Google).
- **Ripristino = sostituzione** della collezione locale
  (`CollectionDao.replaceAll`), con dialog di conferma.
- **Login** in due passaggi distinti: identità (`signIn`) e autorizzazione
  Drive (`authorizeDrive`, con schermata di consenso la prima volta).
- **Configurazione richiesta** (non versionata): `google.webClientId=...`
  in `local.properties` (client OAuth "Web application"), più un client
  OAuth "Android" nella stessa Google Cloud Console con package
  `com.michele.eurocoins` e lo SHA-1 della chiave di firma. Senza client ID
  la schermata lo segnala e il login resta disabilitato.
- Codice in `data/backup/` (`BackupFile`, `GoogleAccountManager`,
  `DriveBackupClient`, `BackupService`) e `ui/backup/`.

## Regular Issues

Catalogo delle serie divisionali (1 cent - 2 euro, il disegno nazionale di
ogni taglio) per paese, arrivato dopo le commemorative (dataset
`ec_national_sides` della pipeline, § "Come i dati arrivano nell'app").
Prima versione deliberatamente **minima**: solo consultazione, riusando
dove possibile quanto già costruito per le commemorative, non un secondo
catalogo completo.

- **Dataset diverso da `coins.json`**: una riga per **serie** (il "blocco"
  nazionale di 8 tagli con uno stesso disegno), non per moneta — alcuni
  paesi ne hanno più di una (cambio di ritratto/stemma: Belgio 3, Monaco 3,
  Città del Vaticano 6). `paese`/`zeccaEmittente` sono lo stesso valore
  stabile già usato da `Coin.paese`: `CountryFlags.kt`
  (`flagEmojiForCountry`, generalizzata da `Coin.flagEmoji()` per servire
  entrambi i dataset) e `Microstates.kt` si applicano diretti. `zeccaRaw`
  invece qui è lo slug minuscolo della pagina sorgente EC (es. "andorra"),
  non un nome inglese come nelle commemorative: **non è riusabile** da
  `Coin.displayCountry()`, che userebbe quello slug come ripiego. Per
  questo `RegularIssueCountryNames.kt` ha una mappa **esaustiva** (25
  paesi, incluso `Bulgaria`, appena entrata nell'euro) invece di un
  override parziale come `CANONICAL_COUNTRY_NAMES`.
- **`RegularIssueSeries`** (Room, tabella `regular_issue_series`, DB
  versione 6, `MIGRATION_5_6`): `immagini` (fino a 8 `RegularIssueImage`,
  uno per taglio) e `anniCitati` sono liste, serializzate a stringa JSON
  con un `TypeConverter` (`RegularIssueConverters`) — niente tabella
  separata, sono sempre lette/scritte insieme alla serie. `anniCitati` non
  è affidabile come "anno di inizio serie" (verificato caso per caso solo
  nella pipeline per Belgio; per gli altri paesi resta una lista grezza):
  non è usata per ordinare o mostrare intervalli di anni in questa UI.
  **Lezione imparata nel giro che ha aggiunto `RegularIssueImage.fonteDati`
  (sotto)**: un campo NUOVO su una classe salvata come blob JSON in Room
  (qui, non su una tabella con colonne SQL vere) deve avere un default,
  non solo essere nullable — kotlinx.serialization lancia
  `MissingFieldException` e l'app va in crash all'avvio leggendo righe già
  salvate col JSON vecchio, PRIMA che `ensureSeeded()` faccia in tempo a
  ripopolarle col nuovo asset (la corsa tra "prima lettura" e "reseed" si
  perde quasi sempre). Vale per qualunque campo futuro su `RegularIssueImage`
  o `anniCitati`: qui le migrazioni Room esplicite (sopra) non proteggono,
  perché la colonna SQL resta `TEXT` e non cambia — il contratto è tutto
  nella classe Kotlin.
- **Immagini quasi tutte dalla BCE, non dalla fonte EC**: la fonte EC
  serviva miniature disomogenee (62-400 px a seconda del file, 100×100 per
  4 tagli su 8 — verificato confrontando le 8 monete italiane). Un secondo
  arricchimento (`enrich_divisional_images_from_ecb.py` nella pipeline)
  sostituisce quelle URL con le pagine BCE per taglio (`coins/2euro`,
  `coins/1euro`, ...), uniformi a 540×540: **295 immagini su 303 sono ora
  `ecb`, le 8 del Lussemburgo restano `bcl`** (il Granduca Guglielmo non è
  ancora sulla BCE). Per questo `RegularIssueImage` ha un proprio
  `fonteDati` (default `""`, vedi lezione sopra), distinto da quello della
  serie: testo (`RegularIssueSeries.fonteDati`, sempre dalla fonte EC) e
  immagini di una stessa sezione possono avere provenienza diversa. Il
  passaggio ha aggiunto anche due tagli mai pubblicati dalla fonte EC
  (Andorra 1 euro, Monaco 50 cent 2025) — da qui "coins" nella scheda Home
  è salito da 292 a 303 (sotto). **`RegularIssueImageTrim`
  (`ui/regular/RegularIssueImageTrim.kt`, `coil3.transform.Transformation`)
  resta comunque**, applicata sia alla fascia Home sia al dettaglio del
  paese: anche con la BCE il margine attorno alla moneta non è sempre a
  filo (misurato fino al 12% per lato su alcuni file EC ancora in uso, un
  caso BCE noto a 220×223 invece di 540×540 per Monaco 50 cent — vedi
  NOTES.md della pipeline). Senza il ritaglio, `ContentScale.Crop` riempie
  comunque il riquadro ma CON quel margine incluso: la moneta risultava più
  piccola del cerchio, con un alone del colore di sfondo intorno e
  dimensioni non uniformi tra le 4 posizioni della fascia — bug reale,
  trovato isolandolo con uno sfondo rosso temporaneo sulla sola foto (non
  sul contenitore) per vedere se il gap veniva dal layout o dal contenuto.
- **`RegularIssueRepository`**, non un'estensione di `CoinRepository`:
  dataset e ciclo di vita separati (stesso pattern hash SHA-256 + mutex di
  `CoinRepository.ensureSeeded()`, asset `regular_issues.json`,
  SharedPreferences proprie `regular_issues_dataset`). Seeding avviato da
  `HomeViewModel` insieme a quello delle commemorative.
- **Navigazione**: `HomeScreen` → `RegularIssuesScreen` (griglia paesi,
  bandiera + nome + "N series", riuso visivo di `BrowseCard`/`CardGrid` di
  `BrowseScreen.kt` duplicato localmente, non condiviso — stesso approccio
  di `CommemorativeCard`/`RegularIssuesCard` in `HomeScreen.kt`) →
  `RegularIssueCountryScreen` (route `regular-issues/{paese}`, `Uri.encode`
  come per le commemorative). **Nessuna ricerca/filtro/ordinamento**: 25
  paesi entrano in una griglia senza doverli cercare.
- **`RegularIssueCountryScreen`**: con più di una serie per il paese (Belgio
  3, Monaco 3, Città del Vaticano 6...) un **selettore a chip** in cima
  (`FilterChip`, riuso dello stesso componente di `FilterSheet`) sceglie
  quale mostrare — con una sola serie il selettore non compare. Sotto,
  intestazione "Series N" + `intestazioneRaw` se presente (es. "Series 1 ·
  2002") e `descrizione` **per intero** (nessun troncamento/"Show more" in
  questa prima versione — a differenza di HISTORICAL NOTES nel dettaglio
  commemorative, ma stesso testo giustificato con sillabazione,
  `TextAlign.Justify` + `LineBreak.Paragraph` + `Hyphens.Auto`, per lo
  stesso motivo: senza, il bordo destro era irregolare). **Il numero "Series
  N" del chip e dell'intestazione è la posizione (1-based) della serie
  nella lista del paese** (`seriesHeading(series, number)`, `number` passato
  dal chiamante), NON `RegularIssueSeries.numeroSerieIpotesi`: quel campo
  raggruppa varianti minori sotto lo stesso numero (Belgio: 2002 e 2008 sono
  entrambe "1" nel dataset, un ritocco minore non classificato come nuova
  serie) e con più di 2 serie per paese può ripetersi — bug reale, trovato
  sul telefono (due chip del Belgio dicevano entrambi "Series 1", anche
  con l'anno già aggiunto per disambiguare). Lo stesso `number` è passato a
  `RegularCollectionSheet` per il sottotitolo del pannello, così resta
  coerente.
  Sotto la descrizione, **una card per taglio** (non più una riga
  orizzontale scorrevole dentro un'unica card di serie — cambiato su
  richiesta, riusa la struttura di `CoinRow` in `CoinListScreen.kt`: card ad
  altezza fissa 72 dp, miniatura 52 dp/segnaposto lilla 50 dp, testo,
  casella a destra), **ordinate per valore** (`RegularIssueCountryViewModel`,
  elenco canonico 1 cent → 2 euro): `RegularIssueSeries.immagini` non è
  garantita in quest'ordine — i tagli aggiunti dal secondo arricchimento BCE
  (es. Andorra 1 euro, mai pubblicato dalla fonte EC, vedi sotto) restano in
  coda alla lista originale invece di essere inseriti al posto giusto — bug
  reale, trovato sul telefono (1 euro dell'Andorra in fondo alla lista). Un
  taglio non riconosciuto finisce in fondo invece di far fallire
  l'ordinamento. Stesso trattamento di caricamento/fallback delle
  commemorative: `SubcomposeAsyncImage` + `painter.state.collectAsState()`
  (MAI `.value`, vedi § Decisioni di prodotto) + icona
  `Icons.Filled.EuroSymbol` su cerchio lilla se l'URL manca o il
  caricamento fallisce. Quando `immagini` è vuota (Città del Vaticano,
  serie 2026 non ancora fotografata dalla fonte) la lista di card non viene
  mostrata affatto — non è un caso di "immagine che non carica", è
  l'assenza della lista stessa, verificato in emulatore/telefono.
- **`RegularDenominationDetailScreen`** (route `regular-issues/{paese}/{ordine}/{taglio}`,
  `ordineCronologico` e non `seriesKey` diretto — eviterebbe il separatore `|` in un argomento di
  rotta): il tocco sulla riga di un taglio (non più solo la casella) apre un dettaglio a schermo
  intero, stessa struttura di `CoinDetailScreen` — Hero con foto grande (è lo "zoom": nessun
  dialog di ingrandimento separato, come nel dettaglio Commemorative, che non ce l'ha nemmeno lui,
  vedi § Dettaglio moneta più sotto), MINTAGES+DETAILS, COLLECTION, ABOUT THIS SERIES (la
  `descrizione` della serie, ripetuta qui perché la schermata è raggiungibile direttamente),
  crediti. **DETAILS (Mint/Engraver/Designer) resta SEMPRE vuota** (`NO_VALUE`, "—"): il dataset
  Regular Issues non ha zecca fisica/incisore/disegnatore per taglio, solo `zeccaEmittente` a
  livello di serie (= il paese, non una zecca fisica). La sezione c'è comunque, non omessa —
  stessa posizione e card unica del dettaglio Commemorative, per coerenza strutturale e per essere
  già pronta se la pipeline aggiungesse questi dati un giorno; un primo giro l'aveva omessa,
  corretto su richiesta esplicita. **MINTAGES invece è dinamica** (§ omonima più sotto): oggi
  vuota per lo stesso motivo (nessuna serie ha ancora tirature nel dataset), ma pronta a
  riceverle. `RegularDenominationDetailViewModel` rilegge reattivamente da `RegularIssueRepository.series`/
  `collectionItems` (come `RegularIssueCountryViewModel`), non un fetch singolo come
  `CoinDetailViewModel.getById`: qui non serve un id stabile per riga, il taglio è già una chiave
  dentro la serie. Building block riusati da `CoinDetailScreen.kt` (esportati, non più `private`):
  `DetailCard`, `SectionLabel`, `OwnedBadge`, `FooterLine`, `sansTitleMedium`, `ValueLabel`,
  `NO_VALUE`; `DetailsSection` non prende più un `Coin` ma tre stringhe già pronte (mint, engraver,
  designer), per essere chiamabile anche con tre `NO_VALUE` fissi. Nuova
  `RegularIssueImage.displayImageLicense()` in `ImageLicenseNames.kt` (stessa mappa
  italiano→inglese di `Coin.displayImageLicense()`). I crediti distinguono fonte del TESTO
  (`RegularIssueSeries.fonteDati`, sempre EC) da fonte dell'IMMAGINE (`RegularIssueImage.fonteDati`,
  quasi sempre BCE) — due voci, non una sola "Data source" come nelle commemorative, perché qui
  possono differire (vedi sopra).
- **MINTAGES: una tiratura per (anno, qualità), non una per qualità.** A differenza delle
  commemorative, dove un anno = una tiratura per qualità, qui una serie copre più anni (spesso
  decenni) e ogni qualità può avere una tiratura diversa per anno — `RegularIssueImage.tirature`
  (`RegularIssueMintage`: `anno`+`quality`+`tiratura`, `data/RegularIssue.kt`), lista vuota di
  default per lo stesso motivo di `fonteDati` (un campo nuovo su un blob JSON Room deve avere un
  default, non solo essere nullable, o kotlinx.serialization crasha leggendo righe vecchie prima
  che `ensureSeeded()` faccia in tempo a ripopolarle — vedi § Regular Issues, lezione su
  `RegularIssueImage.fonteDati`). `CoinQuality` è ora `@Serializable` (wire format = nome della
  costante Kotlin, "STANDARD"/"BU"/"PROOF") solo per questo: non tocca il salvataggio Room di
  `CollectionItem`/`RegularCollectionItem`, che restano sul proprio `TypeConverter`.
  - **Card compatta invariata nella forma** (3 colonne Standard/BU/Proof, stessa posizione): ogni
    colonna mostra la **somma** delle tirature di quella qualità (`summarizeMintages` in
    `data/RegularMintageSummary.kt`, con test in `RegularMintageSummaryTest.kt`), con una
    didascalia piccola sotto — **"all years" con più annate, l'anno stesso con una sola** (mai
    "all years" quando non c'è nulla da sommare, per non far leggere l'anno singolo come
    un'aggregazione). Senza questa distinzione una somma tra più anni si legge come la tiratura di
    un anno solo, un ordine di grandezza fuorviante per chi guarda la card — punto sollevato
    dall'utente come consulenza UI/UX (vedi § Convenzioni di lavoro, "Pareri su UI/UX").
  - **"View by year" compare solo se ci sono almeno due anni distinti in totale** (su qualunque
    qualità): con un solo anno la somma coincide già col dato di quell'anno, un pulsante per
    aprire una tabella da una riga sola sarebbe solo attrito.
  - **`RegularMintageHistorySheet`: bottom sheet di sola lettura**, non un pannello che si espande
    dentro la pagina (scartato dopo un giro di mockup): con uno sheet l'unica superficie che
    scorre mentre è aperto è lui stesso, niente ambiguità tra lo scroll della pagina sotto e un
    riquadro con altezza fissa dentro — un problema reale di scroll annidato su Android, non solo
    estetico.
  - **Una riga PER ANNO, tre colonne Standard/BU/Proof affiancate** (`groupMintagesByYear` in
    `data/RegularMintageSummary.kt`, ordine crescente) — NON tre elenchi separati impilati per
    qualità (prima proposta, scartata dall'utente): con Standard su 20 anni e BU/Proof su pochi,
    impilare per qualità avrebbe voluto dire scorrere tutti gli anni di Standard prima di arrivare
    a BU — con le righe per anno il totale è al massimo quanti sono gli anni distinti, non la
    somma dei conteggi per qualità. Include solo gli anni con almeno un dato: una serie uscita nel
    2009 e finita nel 2015 non genera righe fuori da quell'intervallo, per costruzione (non serve
    un campo "anno inizio/fine serie" — `anniCitati` tra l'altro non è affidabile per quello, vedi
    sopra).
- **Scheda Home**: "**N** coins · **M** countries · **P** series" (`RegularIssuesStatsLine` in
  `HomeScreen.kt`, non il generico `StatsLine` di Commemorative: lì il terzo valore è un
  intervallo di anni senza etichetta, qui serve la parola "series"). "Coins" = numero di
  `RegularIssueImage` su tutte le serie (303 oggi), non le 4 monete della fascia — è il numero
  più vicino a "quante monete diverse contiene il catalogo" che questo dataset permetta di
  contare. Fascia con foto reali (BCE, sopra) e rotazione giornaliera come Commemorative (§
  Impostazioni "Rotate home coins"). **Ordine dei 4 tagli**: `1-5 cent · 1€ · 2€ · 10-50 cent`
  (`DENOMINATION_TIERS` in `HomeShowcase.kt`), non crescente — le due bimetalliche (1€/2€, "di
  pregio") stanno nelle due posizioni centrali più grandi di `BandRatios`, i centesimi monometallici
  (rame/oro nordico, colori caldi) fanno da cornice ai lati; scelta dell'utente dopo aver notato che
  l'ordine crescente sprecava la 2€ nel bordo piccolo. **Set fisso** (rotazione spenta,
  `CURATED_DEFAULTS`): non "il primo trovato in ordine alfabetico" (dava sempre Andorra) ma 4 paesi
  scelti a mano — Finlandia, Germania, Grecia, Paesi Bassi — con foto BCE verificate nitide a piena
  risoluzione; Italia (Uomo Vitruviano) e Spagna (Cervantes) scartate perché le foto BCE stesse sono
  leggermente sfocate, non un problema di ridimensionamento dell'app. Sotto, la stessa barra "liquido" di Commemorative
  (`CollectionProgressBar` con `animation`, animazione separata nel `HomeViewModel` — `lastShownRegularOwned`,
  stesso pattern di `lastShownOwned` per Commemorative), **ora reale**: conta `regularIssueOwnedCount` (§
  Collezione su Regular Issues) su `regularIssueCoinsCount`.
- **Fuori scope di questa prima versione** (vedi anche § Backlog):
  ricerca/filtro/ordinamento nella griglia Countries; gestione di `possibileIncongruenza` in UI
  (oggi sempre `false` nel dataset). La catena di ripiego/precaricamento della fascia Home è stata
  allineata a Commemorative (vedi sopra, § Impostazioni "Rotate home coins").

### Collezione su Regular Issues

A differenza delle commemorative, dove `Coin.stableKey` (fonte + anno + paese + tema) identifica
la moneta esatta perché l'anno è nel dataset, qui il dataset descrive solo il **disegno** di un
taglio per una serie — non esiste un anno: lo stesso disegno viene coniato per anni, spesso
decenni. "Moneta posseduta" qui è quindi (serie, taglio, **anno inserito dall'utente**[, qualità]),
e un utente può avere più annate dello stesso taglio (es. Belgio serie 2, 1 euro, sia 2018 sia
2020) — non è un riuso di `CollectionItem`/`CollectionSheet`, serve un modello diverso.

- **`RegularIssueSeries.stableKey`** (`RegularIssueKey.kt`): `paese` + `ordineCronologico`, stesso
  motivo di `Coin.stableKey` (`RegularIssueSeries.id` si rigenera a ogni reseed). `ordineCronologico`
  e non `numeroSerieIpotesi` perché quest'ultimo NON è univoco per paese (vedi sopra, Belgio).
- **Tabella separata `regular_collection_items`** (`RegularCollectionItem`, chiave primaria
  composita `seriesKey`+`taglio`+`anno`+`quality`, DB versione 7, `MIGRATION_6_7`): permette più
  annate E più qualità sullo stesso taglio, mai duplicati sull'identico (anno, qualità). `paese` è
  una copia di quando la voce è stata salvata, stesso scopo "riconoscere un orfano" di
  `CollectionItem.anno/paese/tema`. `RegularIssueRepository.saveCollection(seriesKey, taglio,
  paese, entries)` sostituisce in blocco le voci **di quel taglio** (`RegularCollectionDao
  .replaceForDenomination`, transazione), conserva `addedAt` delle voci esistenti — stesso pattern
  di `CoinRepository.saveCollection`/`CollectionDao.replaceForCoin`.
- **Un taglio conta come "posseduto"** (casella piena nella card, `HomeViewModel
  .regularIssueOwnedCount`) se ha almeno un'annata in una qualsiasi qualità — **conta i tagli
  distinti, non le annate**: coerente con `regularIssueCoinsCount` ("N coins" in Home), che è già
  un conteggio di disegni di taglio, non di monete fisiche. Un utente con 2 annate dello stesso
  taglio fa avanzare la barra di 1, non di 2.
- **Punto di ingresso**: una casella a destra di ogni card taglio in `RegularIssueCountryScreen`
  (`CollectionBox`, duplicata da `CoinListScreen.kt` — stesso approccio di `BrowseCard`, non
  condivisa), stessa spunta verdigris di Commemorative. Sopra il titolo del taglio, una riga di
  stato riusa la posizione di "Paese · Anno" di `CoinRow`: "Not owned" (neutro) o "N years owned"
  (`primary` Bold) — dà un'informazione utile invece di ripetere il paese, già nella barra in alto.
  Anche la card COLLECTION di `RegularDenominationDetailScreen` (§ omonima più sopra) apre lo
  stesso pannello con "Add to collection"/"Edit collection": due punti di ingresso allo stesso
  pannello, come nelle commemorative.
- **`RegularCollectionSheet`** (`ui/components/RegularCollectionSheet.kt`, non `CollectionSheet`
  riusato tale e quale — la forma dei dati è diversa, ma stesso linguaggio visivo, `PriceField` e
  `sanitizePrice` esportati da `CollectionSheet.kt` e riusati qui): tre card Standard/BU/Proof come
  Commemorative, ma spuntare una qualità non mostra un solo campo prezzo — mostra una **lista di
  annate** (`YearEntry`, identità stabile con un id incrementale per le chiavi di Compose): riga
  anno (pillola 4 cifre, `sanitizeYear` — solo cifre, nessun separatore) + pillola prezzo (stessa
  di Commemorative) + rimuovi, e "+ Add year" sotto l'ultima. Qualità non spuntata = card compatta,
  nessuna annata visibile (dati non persi, solo nascosti finché non si tocca di nuovo la spunta —
  come il prezzo di `CollectionSheet`). Alla prima apertura (nessuna voce esistente) Standard è
  già spuntata con una riga vuota, come nelle commemorative. **Le righe con anno vuoto o
  incompleto (meno di 4 cifre) vengono ignorate al salvataggio**, invece di bloccare "Save" con un
  errore — un modo leggero di scartare bozze non finite di scrivere, scelto per non introdurre
  validazione bloccante in un pannello che finora non ne aveva mai avuta. **Card spuntata: solo
  bordo (2.5 dp pieno), mai fondo lilla pieno** — stesso trattamento di `FinishCard` in
  `CollectionSheet.kt`. Era rimasta indietro rispetto a quel cambiamento (fondo
  `secondaryContainer` e bordo 1.5 dp al 40%, lo stile vecchio): trovato e allineato dall'utente
  dopo il giro di fix su MINTAGES — i building block condivisi (`PriceField`/`sanitizePrice`)
  erano stati riesportati, ma lo stile della card non era mai stato risincronizzato quando è
  cambiato nell'originale.
- **Non ancora fatto** (vedi anche § Backlog): nessuna deduplicazione visibile se l'utente scrive
  lo stesso anno due volte nella stessa qualità (l'ultima riga sovrascrive silenziosamente
  l'altra al salvataggio, per via della chiave primaria); prezzo non testato per anno diverso
  dello stesso taglio/qualità in scenari con più di 2-3 annate (il pannello può diventare alto,
  non ancora verificato uno scroll interno oltre `windowInsetsPadding`).

## Lingua

L'app (chrome UI e contenuto mostrato) è in inglese, hardcoded direttamente
nel codice Compose — nessuna infrastruttura di localizzazione Android
(`values-it/`, `strings.xml` multipli) perché non serve ancora: c'è una sola
lingua da servire.

- Testo scritto da noi (label, titoli, placeholder, messaggi) → inglese
  hardcoded in Kotlin.
- Testo che viene dal dataset (`tema`, `noteStoriche`) → già inglese perché
  la fonte BCE scrive in inglese; mostrato verbatim, nessuna traduzione.
- **Nome del paese mostrato in UI → `Coin.displayCountry()`
  ([CountryNames.kt](app/src/main/java/com/michele/eurocoins/data/CountryNames.kt)),
  non `paese`/`zeccaEmittente` direttamente.** Sotto, la fonte è `zeccaRaw`:
  il testo originale così come letto dalla fonte BCE (in inglese:
  `"Belgium"`, `"Croatia"`...), salvato dalla pipeline prima di qualsiasi
  mapping sull'enum `ZeccaEmittente` (che invece usa nomi italiani, es.
  `"Città del Vaticano"`). Non è una traduzione fatta da noi.
  `zeccaRaw` però **non è consistente su tutti i record dello stesso
  paese**: verificato sull'intero dataset che 2 dei 24 paesi hanno pagine
  BCE che scrivono il nome in modo diverso — `"Vatican"` (20 monete) vs
  `"Vatican City"` (14), `"Netherlands"` (3) vs `"The Netherlands"` (1); la
  forma canonica scelta è `"Vatican City"` e `"Netherlands"`.
  `displayCountry()` normalizza questi 2 casi più Irlanda (`"EIRE"` e
  `"IRELAND"` → `"Ireland"`), chiave sul `paese` stabile. **Maiuscolo**: 31
  monete su 584 hanno `zeccaRaw` tutto maiuscolo (`"SPAIN"`, `"ITALY"`...): se
  il testo è interamente maiuscolo `displayCountry()` lo porta alle sole
  iniziali maiuscole; per tutti gli altri fallback a `zeccaRaw`. `paese`/
  `zeccaEmittente` restano nell'entity Room e nella ricerca (come
  fallback, insieme a `zeccaRaw`) per compatibilità con lo schema
  pydantic della pipeline, ma non vengono più renderizzati direttamente.
- **Licenza dell'immagine → `Coin.displayImageLicense()`
  ([ImageLicenseNames.kt](app/src/main/java/com/michele/eurocoins/data/ImageLicenseNames.kt))**,
  non `licenzaImmagine` direttamente. La pipeline salva un valore **italiano**
  (enum `LicenzaImmagine`) e, a differenza del nome paese, non conserva un
  originale inglese: la mappa è quindi una traduzione nostra (oggi solo 2 valori:
  580 monete "Copyright zecca emittente (uso editoriale)" → "Copyright of the
  issuing mint (editorial use)", 4 "Sconosciuta - da verificare" → "Unknown, to
  be verified"). Prima non la facevamo per il rischio di disallineamento con
  l'enum; il ripiego sul testo originale lo copre (un valore nuovo compare in
  italiano, si nota e si aggiunge una riga). Soluzione definitiva: la pipeline
  emette anche il testo inglese.
- **Se in futuro serve l'italiano come lingua dei contenuti**, arriverà come
  dato aggiuntivo dalla pipeline (una nuova fonte/campo), non come
  traduzione automatica del dataset esistente — vedi il commento su
  `Coin.kt` (`tema`/`noteStoriche`). Fino ad allora non introdurre
  assunzioni tipo "un solo blob di testo per lingua" che renderebbero quel
  giorno più doloroso, ma non costruire infrastruttura i18n prima che serva
  davvero.

## Cose da sapere sul dataset (non ovvie dal codice)

- **584 monete**, non 499: include le 85 delle 5 emissioni congiunte
  dell'Eurozona (`Coin.emissioneComune = true` — 2007 Trattato di Roma, 2009
  EMU, 2012 dieci anni di euro, 2015 bandiera UE, 2022 Erasmus), aggiunte
  dalla pipeline via Wikipedia/EUR-Lex-Cellar/BCE dopo che l'app le ha
  richieste (la BCE le pubblica raggruppate sotto un'unica voce "Euro area
  countries" invece che per paese, storicamente escluse per questo). Anche
  **San Marino 2012** ("Ten years of the euro"), il gap storico documentato
  qui fino a poco fa, è stato risolto allo stesso modo (confermato dal campo
  "Issuing date" del carosello immagini BCE). Dettagli e fonti in `NOTES.md`
  nella pipeline dati.
- **La tiratura per 7 paesi** (Italia, Slovacchia, Slovenia, Grecia,
  Germania, Lituania, Lussemburgo) è quasi certamente un contingente
  autorizzato, non la tiratura reale della singola moneta — l'app lo
  segnala in UI nel dettaglio moneta (`PAESI_TIRATURA_SOSPETTA` in
  `CoinDetailScreen.kt`), ma **solo quando la riga "Standard" mostra
  davvero quel numero BCE** (`Coin.tiratura`), non quando Numista ha un
  proprio valore indipendente per la finitura standard — il pattern
  "contingente" è stato osservato sul dato BCE, non ha senso applicarlo a
  un numero che viene da un'altra fonte. Se estendi quella logica altrove
  tienilo a mente. Dettaglio completo in `NOTES.md` nella pipeline dati.
- **Tirature per finitura da Numista** (`tiraturaNumistaStandard/Bu/Proof`,
  fonte indipendente da `tiratura` BCE, mai fusa con essa — coperte 420-453
  monete su 584 a seconda della finitura). La riga "Standard" del dettaglio
  cade sul numero BCE solo se Numista non ha **nessun** dato per quella
  moneta (né standard, né BU, né proof): se Numista distingue BU/proof ma
  non ha una riga standard, di solito è perché la moneta non ne ha una
  (l'intera tiratura è divisa tra BU e proof) — cadere comunque sul numero
  BCE in quel caso mostrerebbe quella somma come una terza tiratura
  inventata (bug reale, trovato e corretto durante lo sviluppo di questa
  funzione). Il dataset della pipeline ha anche un quarto bucket Numista,
  `tiratura_numista_altro` (tirature con un commento non classificabile
  come standard/BU/proof, es. lotti in rotoli o coincard — a volte è la
  maggioranza della tiratura reale), **non mappato in `CoinJson`/`Coin`**:
  scelta deliberata per ora, non è una quarta "qualità" pari alle altre tre
  e mostrarlo richiede una decisione di UI a sé — vedi § Backlog.
- **Zecca fisica, incisore e disegnatore** (`zeccaFisicaRaw`/`incisoreRetroRaw`/
  `disegnatoreRetroRaw`, fonte Numista `GET /types/{id}`, mai dalla BCE — le sue pagine
  non hanno mai campi strutturati `Designer`/`Engraver`/`Mint`): copertura 96% zecca
  fisica, 59% incisore, 31% disegnatore, 86% almeno uno dei due sul disegno commemorativo.
  **Incisore e disegnatore sono ruoli distinti, non un ripiego l'uno dell'altro**: 25
  monete su 584 hanno entrambi valorizzati con persone diverse (es. Lettonia 2014-2017:
  incisore sempre "Jānis Strupulis", disegnatore diverso ogni anno) — mostrarne solo uno
  perderebbe l'informazione sull'altro. Il lato comune europeo (`incisoreFronteRaw`/
  `disegnatoreFronteRaw`, quasi sempre "Luc Luycx") non è mappato in `CoinJson`/`Coin`:
  non distingue le monete tra loro. Dettaglio completo (inversione terminologica
  obverse/reverse di Numista rispetto alla convenzione IPZS di questo progetto, caso
  Malta 2022) in `NOTES.md` nella pipeline dati.
- 4 monete hanno `immaginePlaceholder = true` (BCE non ha ancora
  pubblicato l'immagine reale): l'app lo gestisce mostrando un'icona al
  posto dell'immagine, non un errore.
- **Distinto da quanto sopra**: un `urlImmagineFonte` presente ma che
  fallisce il caricamento a runtime (link scaduto, rete assente) usa la
  STESSA icona `€` (elenco e dettaglio: prima il dettaglio mostrava un
  `MonetizationOn` a dollaro e l'elenco un vuoto offline). La distinzione sta
  solo nel testo del dettaglio: "Image not yet published by the source"
  (dato) contro "Couldn't load this image" (rete/link); mentre carica, o se
  il caricamento resta in sospeso senza rete, solo l'icona senza testo. Misure: 26 dp nell'elenco,
  22,5% della larghezza della card nel dettaglio (era 25%, ridotta del 10% su richiesta) — vedi
  `AsyncImagePainter.State` in `CoinListScreen.kt`/`CoinDetailScreen.kt`. La pipeline dati ha uno
  script (`scripts/validate_image_links.py`) che controlla periodicamente
  se qualcuno dei 495 URL delle monete originali è morto, per distinguere
  "capita raramente in rete" da "gap permanente nei dati" prima di
  documentarlo in NOTES.md — non ancora esteso alle 85 emissioni comuni
  (immagini BCE aggiunte più di recente).
- **`note_storiche` non contiene più la frase "Issue date"/"Data di
  emissione"**: la pipeline la aggiungeva in coda alla descrizione, è stata
  tolta del tutto. Il mese di emissione esiste come `issuing_date_raw` durante
  lo scraping ma non è salvato in nessun campo: se serve (es. ordinare dentro
  un anno) va aggiunto come campo dedicato nella pipeline, non reinserito nel
  testo libero.
- **Tutti i 495 URL immagine erano raggiungibili** (controllo con
  `scripts/validate_image_links.py`, settembre 2026): i "buchi" visibili nell'app
  sono le 4 monete Vaticano non ancora pubblicate, non link morti.
- Il numero di monete per paese/anno mostrato nelle card è quello del
  database, mai un valore fisso nel codice.

## Convenzioni di lavoro

- **Lingua**: commenti/KDoc, `CLAUDE.md` e messaggi di commit in italiano; testi
  mostrati dall'app in inglese (vedi § Lingua).
- **Commit**: messaggi lunghi che spiegano il *perché* e le alternative scartate,
  con il trailer `Co-Authored-By`. Progetto personale: si pubblica direttamente
  su `main`.
- **Pareri su UI/UX**: quando si chiede un parere su una scelta grafica o di
  interazione (forme, colori, gerarchie, layout), dare sempre una raccomandazione
  tecnica esplicita e motivata, come un consulente UI/UX — non limitarsi a
  elencare pro e contro in modo neutro. Riferirsi quando pertinente alla
  grammatica visiva già stabilita in questo file (es. § Raggi dei bordi a tre
  livelli: pillola = controllo azionabile o etichetta/badge, rettangolo con
  angoli morbidi = contenuto/card). Il parere precede il mockup, non lo
  sostituisce: prima l'opinione motivata, poi comunque il confronto visivo
  prima di toccare il codice (vedi i mockup sparsi nel file, es. § Barra "x / y
  collected" della home, "Nota sul processo").
- **Più sessioni Claude lavorano su questo repo insieme** (una per funzione:
  collezione, ricerca/filtri, backup). Regole per non sovrascriversi, imparate
  a caro prezzo (`CollectionDao` toccato da due chat contemporaneamente):
  - lavorare in un `git worktree` su un ramo dedicato se altre chat hanno lavoro
    non committato nella cartella principale;
  - committare **solo i propri file**, mai `git add -A` in un albero condiviso
    (porta dentro il lavoro a metà degli altri); per un file toccato da due chat
    si prepara la propria versione e la si mette in stage a parte
    (`git hash-object -w` + `git update-index --cacheinfo`);
  - non pubblicare commit non propri senza chiedere;
  - se l'albero contiene lavoro altrui, verificare che il proprio commit compili
    da solo costruendo una copia pulita di `HEAD` (worktree) prima del push;
  - file di contesto come questo si modificano con un commit breve e mirato
    (`git commit CLAUDE.md`), perché tutte le chat lo aggiornano.

## Test

Unit test JVM in `app/src/test` (`./gradlew.bat --offline :app:testDebugUnitTest`).
`MicrostatesTest` legge il `coins.json` vero e controlla che i nomi in
`MICROSTATE_PAESI` esistano (24 paesi -> 20 nascondendoli) e che `stableKey` sia
unica. `RegularMintageSummaryTest` copre `summarizeMintages()`/`groupMintagesByYear()`
con dati sintetici (nessuna serie ha ancora tirature vere nel dataset — vedi § MINTAGES
in § Regular Issues): somma vs. anno singolo, qualità che non si mescolano, ordine
crescente, anni con più qualità che si fondono in una riga sola. Non ci sono test di UI
né di backup (serve un account Google reale). Il lint non gira offline
(`lint-gradle` non è in cache): serve la rete.

## Verifica su emulatore e telefono

Non descritta nei file di build, utile per non rifare gli stessi giri:

- **Analisi di una registrazione schermo** (`adb shell screenrecord`, 60 s max, schermo sbloccato): senza ffmpeg né python si serve la cartella con `jwebserver.exe` del JBR (`-d <cartella> -p 8765`) e una pagina con `<video>` + `requestVideoFrameCallback` che disegna i fotogrammi su un canvas, poi si legge lo screenshot nel browser integrato. Il seek non funziona (il server non supporta Range): si riproduce il video, e a riquadro nascosto si mette in pausa da solo (rilanciare `play()`).
- **Emulatore**: AVD `euro_coins_test` (Pixel 6, Android 15 / API 35,
  `google_apis` x86_64), creato con i `cmdline-tools` installati in
  `%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest`. Avvio senza finestra:
  `emulator -avd euro_coins_test -no-window -no-audio -gpu swiftshader_indirect -no-snapshot`;
  attendere `sys.boot_completed`.
- **L'emulatore è rumoroso, non l'app**: l'immagine `google_apis` carica in
  background l'intera suite Google e produce dialoghi "X isn't responding"
  (Pixel Launcher, System UI, a volte anche Euro Coins) che bloccano i tocchi.
  Verificato con la traccia ANR (`/data/anr`): il thread principale era fermo
  nel `Looper`, senza codice dell'app in esecuzione. Con `adb root` +
  `adb shell settings put global hide_error_dialogs 1` non compaiono più. Sul
  telefono reale l'app è fluida: il lag visto all'inizio era dell'emulatore.
- Dopo l'installazione il primo avvio è lento (fino a un paio di minuti): la home
  mostra "0 coins" finché il database non risponde. Non è un errore.
- **Coordinate dei tocchi**: gli screenshot letti dagli strumenti possono essere
  ridotti (900×2000 invece di 1080×2400): moltiplicare per 1.2 prima di
  `adb shell input tap`.
- **adb**: se compare "server version (32) doesn't match this client (41)",
  `adb kill-server` + `adb start-server` e ritentare l'install in ciclo.
- **Screenshot dall'emulatore**: `adb exec-out screencap -p > file.png` da Git
  Bash corrompe il PNG (CRLF). Usare `adb shell screencap -p /sdcard/s.png` +
  `adb pull` (con `MSYS_NO_PATHCONV=1`, altrimenti Git Bash riscrive
  `/sdcard`). Gli strumenti rifiutano immagini oltre 2000 px: con
  `adb shell wm size 720x1600` e `wm density 280` lo schermo è leggibile e le
  coordinate dei tocchi sono quelle dello screenshot (ripristinare con
  `wm size reset` / `wm density reset`).
- **Emulatore con finestra**: se è già acceso in `-no-window`, `emu kill` può non
  bastare; terminare `qemu-system-x86_64-headless.exe` con `taskkill //F` e
  riavviare `emulator -avd euro_coins_test -gpu swiftshader_indirect`. L'emulatore
  mostra "3G": è lento sulla rete, le foto arrivano piano.
- **Più dispositivi**: con telefono ed emulatore collegati usare
  `adb -s <serial> install -r` (telefono: `487e0cf2`). Se `adb devices` non
  vede il telefono, `adb kill-server` + `adb start-server`. Dopo un build
  controllare che l'APK sia stato rigenerato (data del file): alcune volte
  `grep BUILD` non stampava nulla e si installava l'APK vecchio.
- **Cache immagini di Coil**: `cache/coil3_disk_cache` nel dato dell'app
  (`adb shell "run-as com.michele.eurocoins ls cache/coil3_disk_cache | wc -l"`,
  2 file per immagine + il journal): per verificare il precaricamento svuotarla
  con l'app ferma e riaprire un elenco senza scorrere.
- **Migrazioni e ripopolamento**: si verificano installando la nuova build *sopra*
  una vecchia con il database già popolato (`adb install -r`), non su dati
  vuoti — è lo scenario reale del telefono. I dati dell'utente sopravvivono
  all'aggiornamento; controllare sempre che le monete restino tante quante
  nel dataset corrente (584 a oggi), non il doppio.
- **Foto offline e ripieghi della Home** (verificato dall'utente sul telefono): modalità aereo +
  Impostazioni Android → App → Euro Coins → Archiviazione → **Svuota cache** (NON "Cancella dati":
  azzera anche la collezione). Senza cache né rete le monete della Home diventano le 2€ disegnate e
  l'elenco/dettaglio mostrano l'icona `€`. Per provare il ripiego "ultimo set mostrato": aperta
  l'app online, modalità aereo e data del telefono avanti di 2 giorni (il set di domani è già
  precaricato), poi riattivare la data automatica. Da `adb` (con lo schermo sbloccato: da script
  non si sblocca): `cache/coil3_disk_cache` si svuota con `run-as`, vedi punto sulla cache di Coil.
- **Telefono**: debug USB attivo. Con lo schermo bloccato lo screenshot è nero: non
  sbloccarlo da script.
- **Build da Git Bash**: `JAVA_HOME` sul JBR di Android Studio
  (`C:\Program Files\Android\Android Studio\jbr`) e
  `./gradlew.bat --offline :app:assembleDebug`. Senza `JAVA_HOME` lo stub Oracle
  su `PATH` fa fallire `gradlew.bat`.

## Decisioni di prodotto già prese (con il perché)

- **Registrare una moneta** = una casella accanto a ogni moneta + pannello dal
  basso con Standard/BU/Proof e prezzo. Scartati: chip nel dettaglio (troppo
  nascosto) e tre caselle S/B/P in ogni riga (bersagli minuscoli, e mostrerebbe
  BU/Proof anche dove non esistono).
- **Barre di avanzamento** sulle card: parte del progetto dall'inizio, ora
  collegate alla collezione vera.
- **Catalogo per anno/paese** = griglie di card con selettore Years / Countries /
  All, non una lista con etichette di sezione (bocciata: "restava sempre una
  lista").
- **Emissioni comuni: card separata in Years, non un'etichetta sulla stessa
  riga dell'anno.** Primo tentativo (etichetta "COMMON ISSUE" accanto alla
  cifra dell'anno, o riga a parte sopra il conteggio) scartato dopo un
  mockup: cambiava l'altezza della card e rompeva l'allineamento con le
  altre nella griglia a 2 colonne. Pillola ancorata nell'angolo in alto a
  destra, fuori dal flusso del testo (`Box` con `Alignment.TopEnd`): stessa
  altezza della card standard in ogni caso. Nell'elenco (dove lo spazio
  verticale non è un vincolo) un'icona inline basta, tinta come il testo
  della riga invece di un colore nuovo.
- **Immagini in hotlink** dalla fonte BCE con cache locale, mai ospitate
  (licenza "copyright zecca emittente, uso editoriale"): con attribuzione sempre
  visibile e link alla fonte. Le foto BCE sono JPEG quadrati (i campioni
  controllati: 270×270, ~100-260 KB), con sfondo **bianco puro** (255,255,255)
  e senza trasparenza. Conseguenze: nell'elenco pesano molto per un cerchio da
  52 dp (da qui il precaricamento); WebP non serve finché non le serviamo noi.
- **Foto della moneta**: nell'ingrandimento è un quadrato bianco con angoli
  arrotondati (16 dp); nel dettaglio è in una **card bianca pura fissa**
  (`Color.White`, angoli 24 dp, bordo 1 dp, senza secondo riquadro dentro): lo
  sfondo delle foto è bianco puro, quindi la moneta sembra appoggiata sulla card.
  Bianco FISSO e testi di fallback scuri fissi anche nel tema scuro (il bianco
  crema di una card mostrerebbe il bordo della foto). **Ritaglio a cerchio scartato**: le monete non sono centrate
  né grandi uguale nelle foto e risultavano "storte", anche con zoom fisso o con
  un riquadro calcolato sui pixel non bianchi (provato su telefono). Non
  riprovarlo senza un dato migliore dalla pipeline (es. centro/raggio della
  moneta).
- **Ingrandimento senza rotella di caricamento**: con `SubcomposeAsyncImage`
  la prima apertura non tornava mai a Success e la rotella girava per sempre
  sopra la foto già visibile. Causa poi chiarita (vedi sotto): `painter.state` è uno `StateFlow`. Solo icona di errore.
- **Stato di `SubcomposeAsyncImage`: `painter.state.collectAsState()`, MAI
  `painter.state.value`.** In Coil 3 `state` è un `StateFlow`: leggerlo con `.value`
  dentro il contenuto non sottoscrive nulla, quindi se la foto arriva DOPO il primo
  disegno (lettura dal disco dopo un avvio a freddo, cache in memoria vuota) la UI
  resta sul segnaposto finché qualcos'altro la ridisegna. Sintomi visti: cerchi
  vuoti o `€` grande nel dettaglio per secondi anche con la foto già in cache (sembrava
  un riscaricamento, ma il log Coil mostrava tutto da DISK in ~100 ms, zero rete),
  e la rotella infinita dell'ingrandimento. Applicato a `CoinListScreen`,
  `CoinDetailScreen`, `CoinImageDialog`. Come si è trovato: `EventListener`/`DebugLogger`
  temporanei di Coil + registrazione schermo con adb; se torna un dubbio simile,
  prima guardare se la richiesta è `success` nel log.
- **Transizioni di navigazione: fade-through** (`EuroCoinsNavHost.kt`): uscita 120 ms
  lineare, entrata 420 ms dopo una pausa di 90 ms con `cubic-bezier(0.05, 0.7, 0.1, 1)`
  (la frenata finale è ciò che dà la sensazione "burrosa"). Storia: il default di
  Navigation Compose (700 ms) sembrava un ritardo dopo il tocco -> 280/120 ms -> troppo
  veloce e "a scatti" -> fade-through; l'entrata è stata allungata da 340 a 420 ms su
  richiesta e si può portare a ~500, oltre sembra lenta. Confrontate con un mockup
  animato tre varianti: la terza (fade-through + scorrimento laterale di 24 dp) non si
  distingueva dalla seconda e costa di più (il blur Haze di Browse verrebbe ricalcolato a
  ogni fotogramma), scartata. **Il `Surface` di `MainActivity` usa `background`, non
  `surface`**: nella pausa tra le due schermate si vede il fondo, e col bianco del tema
  chiaro comparirebbe un lampo bianco.
- **Browse ed elenchi si aprono già pieni.** `CoinRepository.coins` e `collectionItems` sono
  `SharedFlow` caldi (`shareIn` `Eagerly`, replay 1) e `BrowseViewModel` / `CoinListViewModel`
  calcolano lo stato iniziale subito (`coinsNow`/`collectionNow`, sul thread principale:
  ~584 monete, pochi ms). Prima lo stato iniziale era vuoto e i dati arrivavano qualche
  fotogramma dopo (griglia vuota o "0 coins" e poi le card di colpo, e un primo tentativo di
  fade dei contenuti girava a scatti sopra il lavoro di composizione). Il ripiego "non
  caricato" (`BrowseUiState.loaded`, `CoinListUiState.loading`) con fade di 220 ms resta
  per quando Room non ha ancora risposto. Lo stato iniziale di `BrowseViewModel` deve
  usare la scheda scelta in Impostazioni: con `BrowseUiState()` predefinito si vedeva
  Years per un istante e poi Countries.
- **Tema: segmented button System / Light / Dark nelle Impostazioni** (Appearance), System predefinito = segue il telefono. `ThemePreference` salva la
  scelta in SharedPreferences (`theme`); `MainActivity` riapplica
  `enableEdgeToEdge` a ogni cambio (altrimenti le icone delle barre di sistema
  seguono il tema del telefono e spariscono con un tema forzato). Scartati:
  pillola nella barra della home (sostituita quando le impostazioni sono state unificate), pillola a due stati (non si tornerebbe a
  "segui il telefono"). L'ordine è System, Light, Dark; "System" e non "Auto" perché è la dicitura di Android e "Auto" farebbe pensare a un cambio a orari.
- **Palette del tema chiaro: grigio-verde, non crema.** Il beige/crema faceva
  sembrare tutto piatto e con poco contrasto tra card e fondo (rapporto ~1.13).
  Ora fondo `BEC8BB` (era `D0D7CE`, scurito per staccare le card), card `FFFFFF` (bianco puro, come le foto BCE), outline
  `8A968A` (era `A9B3A8`), inchiostro `1F2620` (rapporto fondo/card ~1.7). L'outline è anche
  traccia delle barre di avanzamento. Un **lilla** (`LilacLight`, `E2D9F3`) resta
  come piccolo accento su segmento attivo del selettore, chip selezionati e cerchi
  delle monete senza foto (`secondaryContainer`). Tema scuro (riallineato dopo un giro sul telefono: card e fondo erano quasi lo stesso nero, rapporto ~1.07): fondo `0F110B`, card `20231A` (~1.3), outline `4A4F3A` (~2:1 sulle card), bordo 1 dp; la tile Commemorative della Home è verde profondo `2F4A38` (`TileDark`) con testo crema, non più il salvia chiaro `primary` (troppo luminoso), mentre FILTER, spunte, barre e badge restano salvia. La card con la foto resta `Color.White` anche nello scuro (sfondo bianco delle foto BCE). `FilterSheet` usa `background` come il pannello di modifica collezione e gli `AlertDialog` usano `surface` (`containerColor` esplicito): senza, Material metteva il suo grigio-viola `1E1D24`, fuori palette.
- **Card ovunque**: Years/Countries (bordo 1 dp) e **una card bianca per moneta
  nell'elenco** (angoli 14 dp, 8 dp tra le card): senza, le righe stavano
  direttamente sul fondo e l'elenco era piatto. La barra del titolo ha lo stesso
  colore dello sfondo (`appBarColors()`), altrimenti una fascia chiara spezzava
  la schermata.
- **Barra di ricerca flottante nel tema chiaro**: fondo meno opaco del tema scuro
  (0.78 contro 0.84 dopo la riduzione del 10%) perché il blur si veda, ma non meno: a 0.72 il testo sotto si
  leggeva ancora; contorno scuro da 2 dp. Nel tema scuro 0.84 e bordo 1 dp.
- **Ricerca: griglie filtrano i contenitori, liste cercano le monete.** Years e Countries
  restringono le card (solo per numero d'anno / nome del paese: segnaposto "Filter by
  year…" / "Filter by country…"); All e le liste cercano sempre in tema, paese e anno
  (`CoinListViewModel.compute`), con segnaposto che dicono ESATTAMENTE cosa si può
  scrivere lì: All "Theme, country, year…", anno aperto "Theme or country…", paese
  aperto "Theme or year…" (misurati sul telefono: circa 155 dp di testo, oltre si
  tronca; "Country, year or theme…" era troppo lungo). Vicolo cieco delle griglie
  (`NoMatchState` in `BrowseScreen.kt`): se nessuna card corrisponde e c'è testo,
  "No years match “x”" + pulsante "Search all coins for “x”" che imposta la query del
  ViewModel di All e passa alla scheda All. Scartato rendere globale la ricerca delle
  griglie (il significato della barra cambierebbe mentre si scrive). Il testo iniziale
  della barra si legge dai getter sincroni `currentQuery`/`yearsQueryNow`/
  `countriesQueryNow`, non da `uiState` (in ritardo di qualche fotogramma).
- **Rifiniture dopo il giro in tema chiaro e scuro**: (a) `CoinHeroFallback` (dettaglio senza
  foto): l'icona ha `fillMaxWidth(0.4f).aspectRatio(1f)` in QUESTO ordine, prima era invertito
  e il `€` riempiva tutta la card spingendo fuori il messaggio "Image not yet published by the
  source" (invisibile in entrambi i temi); (b) titoli di sezione delle Impostazioni 20 sp Bold,
  sopra i titoli delle voci (17 sp), che prima erano quasi uguali; (c) titoli dei dialog con
  `DialogTitle` (`components/DialogTitle.kt`, 20 sp Bold) invece del default Material da 24 sp a
  peso normale; (d) pillola "COMMON ISSUE" a 9 sp (era 8): a 10-11 sp toccava o copriva l'anno
  "2022" della card, non c'è spazio in quell'angolo. Poi sistemati: bordo della barra di ricerca nello scuro (1.5 dp al 40%, era 1 dp al 15%), bordo delle righe del pannello di modifica viola come le pillole del dettaglio (era `secondary` bronzo, che sul lilla sembrava rosato), padding inferiore della card COLLECTION 6 dp (era 16: sotto "Edit collection" c'era il doppio dello spazio che sopra). Bandiere con bianco (Cipro, Estonia) sulla card bianca nel chiaro: valutate e lasciate com'erano su decisione dell'utente.
- **Raggi dei bordi a tre livelli**: 22 dp per le card "grandi" (tile della Home `CardShape`, card con la
  foto del dettaglio), 14 dp per tutte le altre card (elenco, Impostazioni, backup, card del dettaglio,
  righe del pannello di modifica, card di Browse: `BrowseCard` ha `shape` esplicito, prima usava il
  default Material da 12 dp), pillola (`RoundedCornerShape(50)`) per finiture e pulsanti. Prima c'erano
  12, 14, 16 e 24 fianco a fianco senza un criterio. Elementi interni piccoli (10 dp del box di stato
  del backup, 7 dp delle caselle, 12 dp del campo prezzo) restano: sono annidati, non card.
- **Font di sistema ingrandito (prova a 130%, `adb shell settings put system font_scale 1.3`, poi
  riportare a 1.0)**: quattro rotture trovate e sistemate senza cambiare nulla a 1.0: "Restore" a una
  riga sola con padding 12 dp (`maxLines = 1`, `softWrap = false`: andava a capo come "Restor/e"),
  riga di statistiche della tile Home fino a 2 righe (`maxLines = 2`: l'intervallo di anni veniva
  tagliato), titolo delle tile Home con `padding(end = 8.dp)` verso la pillola "Coming soon" (ora
  può andare a capo, non la tocca), pillola "COMMON ISSUE" che NON scala con il font
  (`CompositionLocalProvider(LocalDensity ... fontScale = 1f)`: cresceva e copriva l'anno). Restano
  accettabili: titoli dell'elenco tagliati a una riga (card da 72 dp fissi) e email accorciata.
  Non verificati: landscape, TalkBack, schermi molto piccoli.
- **Tipografia: serif solo per l'identità "catalogo", sans per tutto il resto**
  (`Type.kt`; via di mezzo scelta dopo mockup, su suggerimento di un altro assistente
  di rimuovere quasi tutta la serif: la serif su numeri e titoli grandi non è un
  problema di leggibilità e dà il carattere numismatico all'app). Serif =
  `titleLarge` (titolo delle barre in alto, "Euro Coins" compreso) e `headlineMedium`
  (anni grandi delle card Years). Sans = `titleMedium`, corpo, etichette e
  `labelSmall` (i contatori "27 / 36 collected" erano monospace). I `titleLarge`
  che NON sono barre (tile Home, invito backup, "Filter & sort") passano da
  `.copy(fontFamily = FontFamily.Default)`. Pesi alti (headlineMedium e titleLarge
  Bold, titleMedium e labelLarge SemiBold): un serif Medium risultava sottile.
- **Dettaglio moneta = Hero card (foto + titolo) + tre card bianche + crediti in
  una riga** (`CoinDetailScreen.kt`, deciso dopo una lunga serie di mockup). Tutte le
  card sono **bianche** (`colorScheme.surface`; la Hero è `Color.White` fisso perché
  le foto BCE hanno sfondo bianco puro) su fondo grigio-verde, 12 dp tra l'una e
  l'altra; scartate le card grigio-verdi (`surfaceVariant`): il contrasto card/fondo
  scendeva. Il paese non c'è (è nell'header).
  - **Hero**: foto a tutta larghezza della card (padding 12 dp) e titolo sempre
    intero sotto (`titleMedium` Bold, centrato, 2-3 righe). Scartati: titolo con
    ellissi + espansione con freccia (complessità inutile) e titolo fuori dalla card.
  - **Etichette di sezione** identiche: `MINTAGES`, `COLLECTION`,
    `HISTORICAL NOTES` (`SectionLabel`: `labelLarge` Bold, maiuscolo, `linkColor()`).
  - **Tipografia solo sans** in questa schermata: `titleMedium` era serif
    e `labelSmall` monospace: `sansTitleMedium()` e `.copy(fontFamily = FontFamily.Default)` lo
    imponevano localmente; ora il tema è già sans su quegli stili e restano solo ridondanti.
  - **MINTAGES**: tre colonne di larghezza UGUALE (`weight(1f)`), cifra sopra ed
    etichetta sotto centrate sull'asse della propria colonna, senza filetti; cifre
    tutte nello stesso stile (`"tnum"`, mai ridotte anche per `12,600,000`).
    Provati `SpaceEvenly` con colonne larghe quanto il contenuto (decentrato: la
    colonna più larga sposta il baricentro) e i filetti (stringevano le cifre).
    Standard/BU/Proof mostrano le tirature per finitura da Numista quando
    esistono, "—" quando mancano (mai una riga nascosta: non si sa se "manca il dato"
    o "la moneta non ha mai avuto quella finitura"). Niente quarta riga "Other" per
    ora — vedi § dataset e § Backlog.
  - **DETAILS** (zecca fisica, incisore, disegnatore): stessa card di MINTAGES, sotto
    un `HorizontalDivider` leggero (`outline` al 40%) — non una card a parte (risparmia
    bordo/padding, e MINTAGES aveva già un blocco opzionale sotto i numeri, l'avviso sul
    contingente autorizzato). **Non una griglia a 3 colonne pari** come Standard/BU/Proof
    sopra (scartata dopo un mockup): con un solo campo presente si sbilanciava, e i nomi
    di zecche istituzionali lunghe (es. Lettonia, coniata da una zecca tedesca in
    subappalto: "State Mint of Stuttgart / State Mints of Baden-Württemberg") si
    schiacciavano in un terzo di card. Scartata anche la variante a righe impilate
    (etichetta sopra, valore sotto, larghezza intera per tutti e tre): risolveva la
    leggibilità ma triplicava l'altezza della card e rompeva la coerenza visiva con la
    griglia di MINTAGES appena sopra. **Scelta finale, ibrida**: Mint su una riga intera
    (`ValueLabel`, l'unico campo davvero lungo) + Engraver/Designer affiancati in 2
    colonne sotto (nomi di persona, quasi sempre corti) — una sola riga in più rispetto
    alla griglia a 3, non il triplo. Engraver e Designer sono ruoli DISTINTI (chi ha
    inciso il conio contro chi ha ideato il soggetto), mai l'uno il ripiego dell'altro —
    vedi § dataset. Zecche multiple unite dalla pipeline con "; " collassate a un
    conteggio oltre le 3 uniche (`Coin.displayMint()` in `CoinCredits.kt`): la Germania
    conia ogni moneta in tutte e 5 le zecche regionali (mintmark A/D/F/G/J), lo stesso
    elenco fisso di 5 nomi lunghi si ripete identico su 33 monete e mostrarlo per intero
    eccedeva sempre le righe della card — oltre le 3 zecche uniche mostra "N mints" ("5
    mints" per la Germania); con 2-3 zecche (Lussemburgo, Malta, Estonia, Irlanda:
    nessuna ha una zecca propria) restano elencate per intero. Il lato comune europeo
    (incisore/disegnatore quasi sempre "Luc Luycx") resta fuori: valore quasi nullo
    ripetuto su 584 monete.
  - **COLLECTION** (`CollectionCard`): non posseduta = card bianca, messaggio
    centrato e "Add to collection" pieno (48 dp, l'unica azione piena); posseduta =
    card bianca con **bordo verdigris da 2 dp** (`colorScheme.primary`, come badge OWNED e spunte: verde = "posseduta"; era viola `PurpleField*`, cambiato su richiesta lasciando lilla le pillole delle finiture), badge `✓ OWNED` verde
    scuro in alto a destra, **solo le finiture possedute** (una riga per finitura: nome a
    sinistra in colore normale del testo, prezzo in grassetto viola a destra, `—` se
    assente o 0.00) e "Edit collection" (TextButton compatto) a destra. **Riga SOLO
    bordo** (2 dp pieno, non più al 40% di opacità), fondo bianco come il resto della
    card: prima era `LilacLight` a tutta pillola, un terzo blocco di colore pieno nella
    stessa card che ha già il bordo verde e il badge OWNED — "pesante" secondo l'utente,
    cambiato su richiesta (stesso trattamento della card spuntata in `CollectionSheet`,
    sotto). **Angoli a 14 dp, non più pillola ovale** (`RoundedCornerShape(50)`): nell'app
    la pillola è riservata a controlli azionabili ed etichette/badge (pulsanti, FILTER,
    selettore tema, OWNED, COMMON ISSUE), mai a un dato statico non cliccabile — la riga
    finitura+prezzo non lo è (solo "Edit collection" apre il pannello), e la pillola
    suggeriva "si tocca" quando non era così. Ora stessa forma della `FinishCard` del
    pannello di modifica, che mostra lo stesso dato (finitura+prezzo) in modo editabile:
    la card letta e quella modificabile hanno finalmente la stessa forma. Scartati: sfondo
    verde pieno (alternava colori tra le card), righe non possedute tratteggiate,
    pulsante Edit a tutta larghezza.
  - **HISTORICAL NOTES**: `bodyMedium` 14 sp / 20 sp, `TextAlign.Justify` con
    `LineBreak.Paragraph` e `Hyphens.Auto` (bordo destro regolare; la sillabazione
    spezza anche "Croa-tia", dizionario di sistema). 4 righe con ellissi e "Show
    more ∨" / "Show less ∧" centrato in fondo, visibile solo se il testo è
    troncato. **Animazione fatta a mano** (495 ms `FastOutSlowIn`), NON con
    `animateContentSize`: in chiusura il testo tornava subito a 4 righe e le righe
    in più sparivano di colpo lasciando uno spazio vuoto che si restringeva (lo
    scatto). `showFull` tiene il testo a righe piene finché l'altezza animata non
    arriva alle 4 righe. In espansione un `LaunchedEffect` scorre la pagina a ogni
    fotogramma per **centrare la card** nella zona visibile (se è più alta, il bordo
    superiore resta a filo). Scartati: `BringIntoViewRequester` con ritardo fisso e
    `BringIntoViewSpec` lenta (scatto finale), e "scorri solo se il bordo inferiore
    esce" (su schermi alti non scorreva mai).
  - **Crediti**: una riga centrata "Data source · Image license · Credit" con il
    link alla fonte come icona (`OpenInNew`, `linkColor()`, tocco da 48 dp) al posto
    del testo sottolineato; la licenza resta il testo vero (non "Public domain").
  - Tocco sulla foto per ingrandirla: non c'è nel dettaglio (solo nell'elenco).
- **Nomi paese in inglese** presi da `zeccaRaw`, non tradotti nell'app.
- **Barra "x / y collected" della home: animata, con onda vettoriale disegnata a
  mano** (`CollectionProgressBar.kt`/`rememberProgressAnimation`), non il
  `LinearWavyProgressIndicator` ufficiale di Material 3 (esiste solo dalla 1.4,
  ancora alfa; questo progetto è fermo alla 1.3.2 in cache, build sempre
  `--offline` — servirebbe rete per aggiornare la libreria in tutta l'app).
  Tre casi gestiti: primo avvio (`lastShownOwned` nullo nel `HomeViewModel` →
  parte da 0), si torna alla home nello stesso processo (si anima solo la
  differenza dal valore mostrato l'ultima volta, salvato nel ViewModel, non in
  un `remember` che sparirebbe uscendo dalla schermata), il valore cambia a
  schermata aperta (stesso trattamento). "Resume da RAM" non ha bisogno di
  codice: finché il processo resta vivo l'Activity non viene distrutta.
  **Forma attuale: barra "liquido" sottile.** Il testo sta in una `Row` SOPRA la
  barra ("77 / 584 collected" a sinistra, percentuale a un decimale a destra,
  "13.2%"), la barra è un `Canvas` alto **15 dp** con angoli da 7.5 dp (clip
  sull'intero Canvas, traccia vuota compresa) e dentro **due onde** che sono
  percorsi CHIUSI (superficie sinusoidale + curva di raccordo a destra di 8.75
  dp, non un taglio verticale; ampiezza smorzata al 35% negli ultimi 12.5 dp): una frontale
  piena e una di sfondo al 38% della stessa tinta, sfasata di 90° e a 0.7× di
  velocità (parallasse). Sostituisce la barra da 22 dp con testo sotto
  (troppo alta e pesante nella tile) e, prima ancora, un tratto sinusoidale
  con `stroke` e un riflesso scorrevole (scartato: non era l'onda vettoriale
  richiesta). Stati limite: a 0% nessun percorso (una cresta sul bordo vuoto
  sembrava una sbavatura), a 100% riempimento pieno con gli angoli del clip.
  **Un solo `Path` riusato** (`remember`) con `reset()` a ogni disegno: niente
  allocazioni per fotogramma. Tutto ciò che varia per fotogramma (fasi,
  valore, agitazione) è letto dentro il blocco di disegno, non nella
  composizione: la barra si ridisegna senza ricomporre. Il testo usa
  `onPrimary` (`labelColor` passato dalla home): 5.9:1 nel tema chiaro e 6.2:1
  nello scuro, mentre `onSurface` darebbe 2.6:1 e 2.0:1 perché la tile è
  colorata, non una superficie — `onSurface` richiesto due volte, rifiutato
  due volte con i numeri.
  **Ampiezza legata alla velocità reale** del riempimento: `agitation` =
  |velocity| / velocità di picco attesa (2.75 × differenza / durata, la
  pendenza massima della FastOutSlowIn), quindi un salto di 1 moneta e uno da 0
  a 77 si agitano allo stesso modo relativo; continua perché la curva ha
  derivata nulla a inizio e fine. Ampiezza 1.25 dp a riposo e 1.9 dp al massimo
  (scelta dell'utente: la differenza tra "in salita" e "ferma" è poca, l'onda
  a riposo continua a scorrere piano — fase infinita con
  `rememberInfiniteTransition`, non si appiattisce più a linea dritta come
  nel giro precedente; costo: un ridisegno continuo di un Canvas piccolo
  finché la home è visibile, il frame clock si ferma da solo in background).
  **Altezza 15 dp = i 12 dp originali +25%, con TUTTA l'onda in proporzione**
  (ampiezze, lunghezza d'onda, curva del bordo, smorzamento, angoli): l'utente
  ha visto un mockup del +50% (18 dp) e ha scelto di restare a 15, perché 18 dp
  si avvicina alla pillola da 22 dp già giudicata troppo pesante nella tile.
  Per cambiare ancora l'altezza scalare TUTTE le costanti `Wave*` insieme, non
  solo `WaveBoxHeight`. "Larghezza" nel senso di spessore, non orizzontale: in
  orizzontale la barra occupa già tutta la tile.
  Lunghezza d'onda FISSA in dp (`WaveWavelength`, 20 dp, non proporzionale alla
  larghezza — con "N creste sull'intera barra" a inizio riempimento si
  vedeva una sola gobba, perché nella parte colorata ci stava meno di una
  lunghezza d'onda intera). Years/Countries restano con la barra dritta di
  Material (`animation = null`): centinaia di onde insieme sarebbe rumore.
  **Temporizzazione**: riempimento `FastOutSlowInEasing` (1100 ms al primo
  avvio, preceduto da un'attesa di 450 ms dopo che i dati sono pronti, perché
  l'animazione di apertura dell'app ne copriva l'inizio; 600 ms per un
  aggiornamento, senza attesa); fase dell'onda `LinearEasing` (1800 ms a
  ciclo — valori più bassi provati e scartati, "vibravano"); pulse finale
  (`EmphasizedEasing`, cubic-bezier 0.2,0,0,1) **solo sul testo**, non sul
  `Canvas`: scalare in verticale un disegno che contiene un'onda la stira per un
  istante, ed è quello il "salto"/"sobbalzo" segnalato una volta
  dall'utente, non un difetto della sinusoide.
  **Nota sul processo**: l'utente chiede un mockup di ogni modifica grafica
  PRIMA del codice (vedi memoria), e i mockup animati vanno fatti col pulsante
  "Riavvia animazione" (era sparito in un giro e l'ha richiesto).

## Backlog e decisioni aperte

Nella **pipeline dati** (repo separato, va fatto lì):
- **Id stabile per moneta** emesso dalla pipeline: sostituirebbe `Coin.stableKey`
  (che si rompe se un `tema` viene corretto). Al momento del cambio va migrata
  la collezione esistente (mappa chiave vecchia → id).
- **Tirature per qualità**: fatto per Standard/BU/Proof (Numista,
  `tiratura_numista_*` — vedi § dataset). Resta aperto solo il bucket
  "Other" (`tiratura_numista_altro`, tirature con commento non
  classificabile): deliberatamente non mostrato in UI per ora, decidere se
  e come rappresentarlo (una quarta riga? solo quando supera Standard+BU+
  Proof messi insieme? nessuna riga, solo nel footer come nota?) prima di
  mapparlo in `CoinJson`/`Coin`.
- Mese di emissione come campo dedicato; meccanismo per "supplementi manuali
  verificati" se emergono altri gap nel dataset (San Marino 2012 era l'unico
  noto, risolto — vedi § dataset).

Nell'**app**:
- **Regular Issues, fuori scope della prima versione** (§ omonima):
  ricerca/filtro/ordinamento nella griglia Countries; gestione di
  `possibileIncongruenza` in UI.
- **Collezione su Regular Issues** (§ omonima): deduplicazione se l'utente
  scrive lo stesso anno due volte nella stessa qualità (oggi l'ultima riga
  sovrascrive silenziosamente); scroll interno del pannello non verificato
  con molte annate insieme; export/backup su Drive non estesi a
  `regular_collection_items` (oggi solo `collection_items` delle
  commemorative, vedi § Backup su Google Drive).
- **DETAILS del dettaglio taglio (Regular Issues) sempre vuota** (§ omonima):
  il dataset non ha zecca fisica/incisore/disegnatore per taglio. Se la
  pipeline li aggiungesse un giorno, `DetailsSection` è già pronta a
  riceverli (va solo tolto il fisso `NO_VALUE` in `RegularMintageCard`).
  **MINTAGES invece ha già tutta la struttura pronta** (`RegularIssueImage
  .tirature`, card con somma, sheet per anno): resta solo da far sì che la
  pipeline la valorizzi — vedi § MINTAGES nella sezione omonima.
- Note libere e data di acquisto sulla collezione; valuta diversa dall'euro;
  export CSV.
- **Data di emissione (mese) nel dettaglio**: oggi non c'è (non è salvata in nessun
  campo, vedi § dataset) e "zecca" nei dati coincide con il paese: per mostrarla
  serve prima un campo dedicato nella pipeline.
- **Miniature nell'APK (WebP, ~3-6 KB l'una)** invece di scaricare le foto BCE:
  darebbe elenco istantaneo e offline, ma sono copie di immagini con licenza
  "uso editoriale": chiarire prima il permesso con la BCE (serve prima di
  pubblicare l'app) e farle produrre dalla pipeline dati.
- **Gate da ricontrollare prima di pubblicare l'app (o rendere pubblico il
  repo pipeline)**: le tirature Numista (`tiratura_numista_*`) — e ora anche
  zecca fisica/incisore/disegnatore (`zeccaFisicaRaw`/`incisoreRetroRaw`/
  `disegnatoreRetroRaw`, stessa fonte `GET /types/{id}`) — sono state
  raccolte con l'API ufficiale sotto l'eccezione "Personal Project" del suo
  ToS (Sezione 8.4) — eccezione che riguarda **solo** la conservazione dei
  dati, non la Sezione 11 ("Prohibited uses"), che vieta comunque
  l'estrazione sistematica del catalogo senza eccezioni. Rischio accettato
  consapevolmente finché repo pipeline privato e app non pubblicata (vedi
  `NOTES.md` § Tirature Numista nella pipeline). Da ridecidere esplicitamente
  a quel punto: permesso scritto da Numista, o togliere quei campi da quanto
  finisce in `coins.json`.
- Confronto backup ↔ collezione locale (oggi lo stato non dice "up to date").
- Monetizzazione: Play Billing, AdMob e consenso GDPR (UMP) — oggi solo il banner
  segnaposto "Go Pro".

## Setup

Richiede Android Studio con SDK 37 installato. `local.properties` (non
versionato) deve puntare al tuo Android SDK:

```
sdk.dir=C:\\Users\\<utente>\\AppData\\Local\\Android\\Sdk
```

```bash
./gradlew.bat :app:assembleDebug
```

Nota: su questa macchina `gradlew.bat` da shell fallisce con "-classpath
requires class path specification" (`CLASSPATH` vuoto nello script). Da Android
Studio funziona; da terminale si può lanciare il jar del wrapper direttamente:

```bash
java -Dorg.gradle.appname=gradlew -jar gradle/wrapper/gradle-wrapper.jar :app:assembleDebug
```
