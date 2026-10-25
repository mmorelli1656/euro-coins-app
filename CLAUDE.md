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

   **Autore del lato nazionale (2026-10-16)**: la pipeline ha un campo unico `autore_retro` (+ `_livello`,
   `_fonte`, `_nota`, branch `designer-lato-nazionale` della pipeline): il designer se una fonte ufficiale
   (BCE, Commissione, Gazzetta, Banka Slovenije) o Numista `designers` lo dichiara, altrimenti l'incisore
   Numista come livello "probabile". `disegnatore_retro_raw` e `incisore_retro_raw` restano i grezzi Numista.
   **Non si rifà l'export di `coins.json`** per averlo (annullerebbe le correzioni a mano delle 2026, della
   foto di Monaco 2025, ecc.): si rilancia

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File scripts\merge-commemorative-designers.ps1
   ```

   che aggiunge SOLO `autore_retro` e `autore_retro_livello` a ogni record, abbinando per (paese, anno,
   tema); lo script si ferma se il dataset non ha `autore_retro` (cartella o branch sbagliati). **Gli script
   leggono la CARTELLA della pipeline, qualunque branch sia in checkout: nessun push serve all'app.**
   `CoinJson.toEntity()` mostra come Designer l'`autore_retro` "certo", e il "probabile" solo se Numista non ha un designer suo (vedi sotto, "Designer incerto"); altrimenti il
   grezzo Numista (`Coin.disegnatoreRetroRaw` tiene il valore mostrato: nessuna migrazione Room). `export-regular-issues.ps1` fa lo stesso per i tagli
   (`Get-DesignerInfo`). **Fonti nazionali (2026-10-18)**: la pipeline (commit d2da663, branch `designer-lato-nazionale`) ha importato dalle banche
   centrali/zecche/ministeri 58 commemorative e 89 tagli Regular (`autore_retro_fonte` = `banca_centrale`, URL e frase nella nota;
   ricerca e CSV in `C:\Users\mik16\EuroCoins-release\designers\`, `LEGGIMI.md`). Non si citano nell'app. Risultato:
   **525/613 commemorative con un nome (377 certi + 148 probabili)** e **287/303 tagli Regular (257 + 30 probabili)**; prima
   319 e 176 (e 182/25 senza il campo consolidato). Buchi: 88 commemorative (le 29 del 2026 in parte, Andorra, Monaco, Francia…).
   **Designer incerto** (2026-10-18/19, quattro giri di mockup con il proprietario): l'etichetta è SEMPRE "Designer" e il nome ha lo
   stesso aspetto di un nome certo; un nome "probabile" (le fonti nominano l'incisore e non dicono se ha ideato il disegno) ha in più
   un cerchio con il punto esclamativo in BRONZO (`Icons.Outlined.ErrorOutline`, `colorScheme.secondary`) accanto all'etichetta
   (`DesignerValue`, `ui/detail/DesignerCredit.kt`); il tocco sulla riga apre il dialog "About this credit" (la stessa icona accanto al
   titolo; "The sources credit this artist, but the role may be approximate." / "Got it", due righe con "may be approximate."
   unito da spazi non separabili; prima erano quattro, poi tre con "approximate." da solo a capo); un'etichetta "Designer" con l'icona accanto sembrava non allineata al nome, quindi a
   sinistra c'è uno spazio vuoto grande come l'icona che la tiene centrata; senza nome "Designer: —". **Storia**: icona ⓘ attaccata al nome (spostava il nome), poi nome in corsivo con
   "Designer ⓘ" verdigris (troppi segnali), poi solo l'ⓘ verde: il proprietario ha notato che chi guarda la legge come "maggiori
   informazioni sul designer" e non nota che alcuni nomi ce l'hanno e altri no. **Il bronzo era già nella palette** (`secondary`, solo
   decorativo: euro del segnaposto, icone `€` delle righe); qui prende il significato "attenzione", è il suo unico uso (non il rosso del
   distruttivo). Scartati: triangolo (dice "errore", ma il nome non è sbagliato), punto interrogativo verde (si confonde con un aiuto
   generico), icona tonale, etichetta con una domanda, corsivo.
   Dato: `Coin.autoreRetroLivello` (colonna nullable dalla v12, `MIGRATION_11_12`) e `RegularIssueImage.disegnatoreProbabile` (blob JSON,
   default false); un probabile non cancella mai un designer Numista già presente. Scartate l'etichetta neutra "Credited to" e una nota
   fissa sotto il nome. **zwei-euro.com NON c'è più** (2026-10-16): per ~250 monete era l'unica fonte, ma la sua
   nota legale ammette copie solo per uso privato non commerciale e la pipeline l'ha rimossa; per una
   settimana scarsa di prove l'app (non pubblicata) li aveva avuti (566 monete), tolti prima del push.
   Buchi: nessuna fonte ufficiale o Numista per 109 commemorative con "—" (le 29 del 2026, Andorra, Monaco,
   Francia, ecc.). **Solo il Designer in interfaccia** (2026-10-18, decisione del proprietario: "l'unica figura che vorrò esporre sarà quella del designer"): la scheda DETAILS mostra Mint e Designer (`DetailsSection(mint, designer)`, etichetta "Designers" con più nomi), l'Engraver NON si mostra più. Storia: prima due colonne Engraver/Designer, poi (2026-10-16, mockup) una riga unica con `creditRoles()`/`sameCredit()`, tolta perché i dati dicono che i due ruoli sono quasi sempre la stessa persona (251 commemorative su 328 con entrambi i nomi) e il proprietario vuole un solo ruolo. L'incisore resta nel database e nel JSON (`incisoreRetroRaw`, `incisore_raw`), solo non si mostra. Conseguenza accettata: dove c'è il solo incisore (17 commemorative, 111 tagli Regular) compare "—" finché la ricerca su fonti nazionali non trova il designer; ordine deciso: prima le Regular (fatto di serie, circa 20 ricerche), poi le commemorative paese per paese. Le fonti trovate si tengono nel dataset (URL e frase, in pipeline) ma non si citano nell'app.

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
5. **Serie divisionali: tre file della pipeline, uniti da uno script.** La
   pipeline produce `data/processed/ec_national_sides.jsonl` (schema pydantic
   `SezioneSerieDivisionale`/`ImmagineTaglio`, vedi `src/models.py` in quel
   repo — una riga per "serie" nazionale delle monete 1c-2€, non per
   moneta), più due file per TAGLIO che da ottobre 2026 arricchiscono ogni
   immagine di serie: `numista_divisional.jsonl` (un record per type Numista:
   descrizione, incisore/disegnatore, zecca, tirature per anno e qualità) e
   `ecb_national_sides_coin_descriptions.jsonl` (testo BCE per paese+taglio,
   ripiego dove Numista non ha il type). Non è più un export diretto di un
   solo JSONL: l'unione è nello script, che scrive
   `app/src/main/assets/regular_issues.json`:

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File scripts\export-regular-issues.ps1
   ```

   Stampa un riepilogo (tagli con tirature, fonte dei testi, type Numista
   non abbinati, tagli senza type) da leggere ogni volta che i dataset
   cambiano. Le regole di abbinamento per anni sono in § Regular Issues,
   MINTAGES. L'interruttore `-ExcludeNumista` esporta solo ciò che NON viene
   da Numista (testo BCE, niente tirature/zecca/incisore/disegnatore): è il
   piano B se il gate Numista (§ Backlog) obbligasse a toglierli. Lo script
   è in PowerShell perché su questa macchina non c'è Python; la logica di
   abbinamento sarebbe da spostare nella pipeline (§ Backlog).

   Il file completo pesa ~855 KB con la zecca per anno (era ~650; 330 senza Numista, era 130): le tirature sono la maggior parte
   (fino a 84 voci per taglio). Il `-Depth 8` dello script è necessario
   (serie → immagini → tirature → voce = 4 livelli, il margine è per
   campi futuri). Stesso meccanismo di seeding (`RegularIssueRepository.ensureSeeded()`,
   hash SHA-256 dell'asset, SharedPreferences separate `regular_issues_dataset`)
   e stessa scelta sulle immagini (hotlink, non bundlate).

   **L'asset COMMITTATO è l'export COMPLETO** (senza `-ExcludeNumista`: testo Numista,
   tirature, zecche per anno, incisore, disegnatore). **Decisione del proprietario del
   2026-10-05, che ribalta la precedente**: il repo `euro-coins-app` è PUBBLICO su GitHub
   (verificato il 2026-10-03; la pipeline è privata) e pubblicarvi dati Numista è
   distribuzione, che i Termini API vietano (§3/§11) — rischio noto e accettato
   consapevolmente (§ Backlog, "Gate da ricontrollare"), come per `coins.json`, già
   pubblicato con dati Numista. La cronologia git conserva comunque le versioni precedenti
   (ridotte): un eventuale ritorno al piano B vale solo da lì in poi. **Piano B sempre
   pronto**: `scripts\export-regular-issues.ps1 -ExcludeNumista` scrive l'export senza
   dati Numista (testo BCE soltanto) e l'app lo gestisce (card "—", niente riga Numista,
   `RegularIssuesAssetTest` salta i 7 test sui dati Numista); in quel caso va ricordato che
   installare sul telefono un'APK costruita con l'asset ridotto fa sembrare che tirature,
   zecche per anno e testi Numista manchino. Nell'app restano comunque visibili "Source:
   Numista N#…" e il link (§4 dei Termini API). Dopo ogni modifica ai dataset della pipeline
   si rilancia lo script (senza opzioni) e si committa l'asset.
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
- Google Play Billing 9.1 (`billing-ktx`), Mobile Ads SDK 25.5 (`play-services-ads`) e UMP 4.0
  per il Pro e la pubblicità (§ Pro e pubblicità)
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
│   ├── MintNames.kt          # zecca grezza → paese ("Rome" → "Italy"), solo in visualizzazione
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
│   ├── RegularCollectionItem.kt # @Entity: taglio posseduto in un'annata+qualità+varietà + RegularCollectionEntry (bozza pannello)
│   ├── RegularVarieties.kt   # tabella fissa delle varietà (Grecia 2002 EFS), RegularIssueSeries.varietyFor()
│   ├── RegularCollectionDao.kt
│   ├── RegularMintageSummary.kt # summarizeMintages()/groupMintagesByYear() — logica pura, testata
│   ├── RegularYearMints.kt   # yearMintLabels(): etichette "Mint · …" per periodo (certa/probabile/non nota), logica pura, testata
│   ├── RegularSeriesDenominations.kt # denominationsOf(): gli 8 tagli di una serie, quelli invariati ereditati dalla precedente, ritagliati alla finestra di anni della serie
│   ├── RegularDenominationRows.kt # denominationRows(): le righe di un taglio in tutti i paesi (una per serie), per l'elenco di un taglio e le card Denominations
│   ├── RegularAllRows.kt     # allDenominationRows()/RegularAllSort/matchesAllQuery(): l'elenco di tutte le righe (scheda All), ordine e ricerca, logica pura, testata
│   ├── RegularIssueText.kt   # displayDescription() (senza la frase sul bordo esterno), seriesTitle()/seriesChipLabel()/seriesPeriod() — titoli e periodo delle serie
│   ├── MintLetters.kt        # tirature per lettera di zecca (Germania) da assets/mint_letters.json, asset opzionale; mintLetterCity()
│   ├── MintLetterTable.kt    # mintLetterTable()/formatCompactMintage(): la tabella a zecche affiancate, logica pura, testata
│   ├── backup/               # BackupFile, GoogleAccountManager, DriveBackupClient, BackupService
│   └── pro/                  # Pro e pubblicità: ProBilling (Play Billing), AdsConsent (UMP), InterstitialAds, Monetization, ProState (logica pura testata)
└── ui/
    ├── theme/                # palette "verdigris/bronzo" coerente col
    │                         # report di riconciliazione della pipeline dati
    ├── components/           # CollectionProgressBar, CollectionSheet (qualità + prezzo),
    │                         # RegularCollectionSheet (come CollectionSheet + selettore anno a griglia, Regular Issues),
    │                         # PriceFormat, FloatingSearchBar (vetro/Haze), FilterSheet,
    │                         # CoinThumbnailRing (anello, qualità di ridimensionamento e contrasto delle miniature),
    │                         # ThumbnailSharpen/DetailSharpen + UnsharpMask (nitidezza locale di miniature e foto grande del dettaglio, logica pura testata)
    │                         # + AdBanner (banner AdMob adattivo della Home)
    ├── home/                 # ingresso: due tile (commemorative / regular issues)
    ├── browse/               # commemorative: Years / Countries / All
    ├── list/                 # elenco filtrato (CoinFilter), CoinListOptions, ricerca
    ├── regular/               # Regular Issues: schede Denominations/Countries/All + elenco di un taglio + serie del paese + collezione per taglio
    │                         # + RegularDenominationDetailScreen (dettaglio taglio, building
    │                         # block riusati da ui/detail/)
    ├── settings/             # SettingsScreen unificata + CatalogSettingsScreen (sottopagina Catalogs), SettingsViewModel, UserSettings (prefs `settings`)
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
flottante in basso** (`FloatingSearchBar`: pillola con ricerca + pulsante FILTER, sfondo vetro con blur reale via libreria Haze, `hazeSource` sulla lista/griglia sottostante; sotto Android 12 resta il solo fondo semitrasparente) in ogni scheda di Browse e in ogni lista filtrata; ogni scheda ha query e filtri propri. **Aspetto del pannello (ottobre 2026, dopo mockup)**: titolo "Filter & sort" centrato con Reset a destra, ogni sezione ha il titolo centrato e i chip su una riga a LARGHEZZA UGUALE (`ChoiceSection`/`MultiChoiceSection`, `FilterChip` con angoli da rettangolo morbido: le pillole tonde sono state scartate), altezza che si anima quando una sezione compare e scorrimento se non sta. Le sheet con due assi (Commemorative All, Regular All) hanno due gruppi con intestazione piccola e un filetto per lato, `FilterGroupHeader`: **Sort** ("Sort first by", Country order, Year/Value order, tre scelte indipendenti: `CoinGroup`/`RegularAllGroup`, vedi `sortAll()` e `allDenominationRows()`) e **Filter**; con il predefinito (anno decrescente, paese A → Z) l'elenco di Commemorative All resta nell'ordine del database; **Owned quality compare solo con Collection = Owned** in tutte le liste e uscendone si azzera. Le liste di un anno o di un paese restano con un solo "Sort by" (l'altro asse è costante), senza intestazioni di gruppo, **e senza il pulsante "Default"** (tolto il 2026-10-08: coincideva con la prima voce, "Country A → Z" in un anno e "Newest first" in un paese, perché l'ordine di base è anno decrescente e poi nome del paese mostrato; `CoinSort.DEFAULT` resta solo come valore interno "non scelto", così Reset e pallino del FILTER non cambiano; `CoinSortChoicesTest` lo dimostra su tutto il dataset). Il pannello FILTER (`FilterSheet`) contiene anche l'ordinamento (Years: dal più recente / dal 2004; Countries: A → Z / Z → A; liste: per anno o paese) più filtri Collection (All/Incomplete/Complete sulle griglie, All/Owned/Missing + qualità sulle liste); il pallino sul pulsante segnala un filtro attivo. Liste e griglie lasciano `floatingBarClearance()` di padding in fondo. La griglia (e ora anche l'elenco) torna in cima a ogni cambio d'ordine: lo stato di scorrimento si ricrea con `key(...)` nella stessa composizione, NON con un `LaunchedEffect`, che arrivava un fotogramma dopo e faceva vedere l'ordine nuovo scorso a metà (scritte che sembravano sovrapporsi). **Elenco monete**: card ad **altezza FISSA 80 dp** (`RowHeight`, era 72 dp: non varia con la lunghezza del titolo; miniatura **64 dp** (`ThumbnailSize`; era 52, prima ancora 46 in area 52), MA il cerchio lilla del segnaposto è 62 dp (`PlaceholderSize`, era 50 su 52): a pari riquadro il lilla pieno sembrava più grande della moneta (misurato sullo screenshot: 169 px contro 162-165, dopo 163; **la nota storica parlava di un margine bianco attorno alla moneta, ma misurato il 2026-10-04 su 3 foto BCE la moneta occupa il 97-98% del quadrato: nessun margine da ritagliare**), sottotitolo 13 sp Bold in `primary`, titolo `bodyMedium` SemiBold max 2 righe con ellissi, il testo intero sta nel dettaglio; scelta dopo mockup A+Y, scartate 88 dp a 3 righe e sottotitolo in pillola lilla); il tocco sulla miniatura apre il dettaglio come il resto della riga. Titolo e paese passano da `displayTema()`/`displayCountry()`; testi con 12 dp a destra (prima della casella). Senza foto o foto che non si carica (anche offline con cache svuotata): la stessa icona `€`; MENTRE la foto arriva solo il cerchio lilla, senza icona (il `€` a ogni riapertura sembrava un riscaricamento; vedi § Decisioni di prodotto, "Stato di `SubcomposeAsyncImage`"). L'icona è `Icons.Filled.EuroSymbol` (il glifo pieno: l'outline sottile `Outlined.Euro` "sembrava strano"; scartate anche la 2€ disegnata e una moneta con € dentro) su cerchio lilla, non più SOTTO la foto ma solo dove serve: niente icone diverse per "non pubblicata" e "non caricata". `CoinImageDialog` (foto grande, "Close"/"Details") esiste ancora ma è scollegato: per riattivarlo decommentare il blocco `zoomed` in `CoinListContent` e passare `onImageClick` a `CoinRow`. `PrefetchThumbnails` (`ui/components/ThumbnailPrefetch.kt`, condiviso con le liste Regular Issues) accoda in Coil le foto delle 24 righe oltre l'ultima visibile e le porta fino alla cache IN MEMORIA: riga e precaricamento costruiscono la richiesta con la stessa `thumbnailRequest` (dimensione esplicita 64 dp in px, stesse trasformazioni nello stesso ordine), perché la chiave di memoria le contiene — prima il precaricamento non aveva `ThumbnailSharpen` e scaldava solo il disco, e la riga rifaceva lettura, decodifica e nitidezza all'arrivo; la dimensione esplicita evita anche che `AsyncImage` aspetti la misura. NON misurato (telefono bloccato) (le foto pesano ~130 KB l'una da BCE, vedi Decisioni di prodotto). Regular Issues (§ omonima più sotto) riusa questo stesso trattamento di caricamento/fallback per le immagini dei tagli, non questo elenco: ha una sua schermata. Dettagli della barra non ovvi: **si nasconde scorrendo** (ottobre 2026, `FloatingBarState`/`rememberFloatingBarState` in `FloatingSearchBar.kt`): scorrendo la lista verso il basso scivola fuori dal bordo (40 dp di spostamento: partito da 24, che scattava troppo presto; provato anche 64, troppo; 300 ms, `offset` e non `graphicsLayer` perché Haze legge la posizione dal layout), scorrendo verso l'alto (16 dp) o tornando in cima riappare. Conta solo lo scorrimento REALE della lista (`consumed` di un `NestedScrollConnection` messo con `nestedScroll` sul Box che contiene lista e barra), quindi una lista che non scorre la lascia visibile; con la tastiera aperta resta sempre visibile (si guarda l'IME, non il focus, che il campo tiene anche dopo il tasto indietro) e il focus sul campo la rimostra. Una barra di stato per scheda (`rememberFloatingBarState(state.mode)`). **Niente interruttore nelle Impostazioni, per scelta**: valutati un "Show search bar" e un pulsante FILTER tondo flottante o nella barra in alto (scartato: fuori dalla portata del pollice), l'auto-nascondi copre da solo il bisogno di scorrere senza barra di mezzo; l'interruttore si aggiunge solo se qualcuno chiede di toglierla del tutto. Il padding in fondo (`floatingBarClearance()`) resta fisso. Non verificato sul telefono (non collegato). **Testo e cursore vivono nella barra** (`TextFieldValue` locale, `query` vale solo come valore iniziale): il valore che tornava da un StateFlow del ViewModel arrivava con qualche fotogramma di ritardo e un `BasicTextField(String)` che riceve un valore vecchio riporta indietro testo e cursore (cursore dopo la terza lettera, caratteri persi, blocco in Years); tutta la metà sinistra (lente, margini, altezza intera) è cliccabile e porta il focus al campo (`FocusRequester` + `keyboard.show()`), perché il `BasicTextField` è alto quanto una riga di testo e toccare sopra, sotto o sulla lente non apriva la tastiera; alta 72 dp e larga quasi tutto lo schermo (margini 8 dp) per coprire per intero la riga sottostante; fondo molto opaco (0.84 scuro, 0.78 chiaro; era 0.94/0.88, ridotto a vista) perché con testo chiaro su fondo scuro il solo blur lascia il testo leggibile; sta in un Box esterno a schermo intero che assorbe i tocchi ("zona morta", `BarDeadZone` sopra + margine sotto) per non aprire monete vicine per errore (blocca anche il trascinamento iniziato lì). **Tastiera**: `MainActivity` ha `windowSoftInputMode="adjustNothing"` e la barra si solleva con `WindowInsets.ime`/`navigationBars` via `offset`, senza `imePadding()` e senza molle: con il ridimensionamento della finestra attivo l'altezza della tastiera veniva contata due volte, e una molla sopra l'animazione di sistema partiva in ritardo.
**Dettaglio a pagine** (ottobre 2026, dopo mockup; scelta la variante A): il dettaglio di una moneta
è un `HorizontalPager` (`CoinDetailScreen`), si scorre a sinistra/destra tra le monete della lista di
provenienza, NON in stile Tinder (lì lo swipe è una decisione; qui è solo navigazione, e la pagina segue
il dito mostrando la vicina). **Contatore "4 / 12"** nella barra in alto, pillola lilla a destra
(`PagePositionPill`, cifre tabulari; con una moneta sola non compare); scartati i pallini sotto la barra
(B: con 584 monete in All sono una finestra di 7 che non dice dove sei) e le frecce sulla foto (C: coprono
i bordi e ripetono il contatore). La barra (paese · anno) segue la pagina corrente. **Un solo "indietro"**:
le pagine non sono rotte, dopo 10 swipe il gesto/pulsante indietro riporta comunque alla lista (il gesto
di sistema dal bordo resta del sistema: uno swipe che parte dentro la card cambia moneta). **Lista
fotografata al tocco** (`CoinPagerSession`, `ViewModel` dell'Activity: sopravvive alla rotazione, dopo
la morte del processo ripiega su una pagina sola): copia degli id nell'ordine/filtro/ricerca della lista,
non un riferimento vivo, così con "Missing" attivo spuntare una moneta non sposta le pagine. Viene da
`CoinListViewModel` sia nelle liste filtrate sia nella scheda All. **Al ritorno la lista si apre sulla
moneta in cui si è finiti** (`CoinListViewModel.scrollTo` → `ScrollToViewedCoin` in `CoinListContent`:
centra la riga solo se non è già tutta visibile, aspetta il primo layout; **se la lista ha dovuto scorrere**, la riga arrivata ha un contorno verdigris da 2 dp che compare dopo 150 ms, resta pieno mezzo secondo e sfuma in un secondo (`returnHighlight` + `ReturnHighlight`, `ui/components/ScrollToRequestedItem.kt`; mockup B, scartato lo sfondo lilla perché il lilla è la selezione di un controllo; se la riga era già visibile nessun lampo; la chiave si azzera dopo 2 s o una riga che rientra nella composizione rifarebbe l'effetto)). Un `CoinDetailViewModel` per moneta, chiave `detail-<id>`,
`beyondViewportPageCount = 1` (la foto della prossima è già carica quando entra). **Animazione "mazzo"** (dopo mockup A/B/C, scelta B): la pagina che esce scorre sopra, quella dopo resta ferma al centro sotto di lei a scala 94% e opacità 55% e cresce a 100% man mano che viene scoperta, anche all'indietro (`stackedPage` in `CoinDetailScreen.kt`: `graphicsLayer` sull'offset del pager che annulla la traslazione delle pagine a destra + `zIndex` a indice decrescente + sfondo opaco `background` sulla pagina); a riposo la pagina centrale è identica a prima, nessuno spazio perso. Scartati: scorrimento piatto con vuoto tra le card (A) e striscia sotto anche a riposo (C: pagina più bassa di ~24 dp con bordo e angoli, e suggerisce un mazzo di 2-3 carte quando sono 12 o 584); niente rotazione né uscita "alla Tinder" (lì la card scartata è una decisione). **Rotazione e alzata** (variante D del mockup, dopo "sembra ancora uno scorrimento laterale"): la pagina in cima si inclina di 5° attorno al bordo in basso nel verso del dito, si alza di 8 dp con ingrandimento dell'1,2%, ha ombra (12 dp di elevazione) e angoli da 22 dp, tutto in proporzione e a zero a riposo (rampa su un terzo di pagina); la pagina sotto viene spenta con una velatura del colore di sfondo e NON con `alpha` del layer, che forza il disegno fuori schermo di tutta la pagina a ogni fotogramma. Costanti `STACK_UNDER_SCALE`/`STACK_UNDER_ALPHA`/`STACK_TILT_DEGREES`/`STACK_LIFT_DP`/`STACK_SHADOW_DP`. Prove di fluidità: la build debug è molto più a scatti di una release, giudicare l'animazione su una release firmata con la chiave di debug (`assembleRelease -x lintVital*`, `zipalign` + `apksigner`, installa sopra senza perdere i dati).
 **Suggerimento di scorrimento** (2026-10-08, variante A dopo mockup, `SwipeHint.kt` + `SwipeHintEffect` in `DetailPager.kt`, quindi vale anche per Regular Issues): nessuno capiva che la card si scorre (il contatore "4 / 12" da solo non basta). A pagina ferma da 700 ms la card si sposta di 56 dp verso la pagina che c'è (indietro sull'ultima) e torna in UN solo movimento continuo di 1,5 s fatto di TRE RIMBALZI SMORZATI (`swipeHintBounce`: come una palla, ampiezza 1, 0,38 e 0,15 e durate 1, 0,62 e 0,38 rispetto al primo; mai sotto zero; il primo parte dolce con `sin²`, ogni rimbalzo riparte dalla velocità con cui è arrivato il precedente, quindi nessuna pausa; `SwipeHintCurveTest`). Proposto dal proprietario dopo aver visto la versione a un solo rimbalzo ("è meglio e più smooth, ma con uno smorzamento e un terzo rimbalzo?"); l'effetto "mazzo" scopre la pagina vicina. **La prima versione (quattro `animateScrollBy` in fila con pause e un secondo rimbalzo) fu giudicata "macchinosa e poco smooth"**: ogni tratto partiva e arrivava da fermo. Ora `PagerState.scroll {}` con un ciclo a ogni fotogramma (`withFrameNanos`); misurato con un log temporaneo del `currentPageOffsetFraction`: la versione a un rimbalzo saliva senza interruzioni fino a 0,149 a ~540 ms e scendeva simmetrica fino a zero a ~1,08 s. Si muove il pager solo in UNA direzione (un rimbalzo oltre lo zero farebbe entrare la pagina dall'altro lato): la pagina assestata non cambia, quindi NON conta come moneta guardata e non avvicina l'annuncio. Il tocco o il trascinamento dell'utente lo interrompe da solo. **Regola** (`shouldShowSwipeHint`, `SwipeHintTest`): solo con più di una pagina, solo finché l'utente non ha mai scorso, **al massimo 3 volte** (scelta del proprietario; SharedPreferences `swipe_hint`: `times_shown`, `has_swiped`), mai con le animazioni di sistema disattivate (`ANIMATOR_DURATION_SCALE` = 0, in quel caso non conta). Non ogni avvio: diventerebbe fastidioso. Scartata la didascalia "Swipe for the next coin" (variante B), aggiungibile dopo se il solo rimbalzo non basta. Fornito da `MainActivity` con `LocalSwipeHint`.
 **Anche Regular Issues** (`RegularDenominationDetailScreen`): stesso pager, stessa animazione e stesso contatore, codice condiviso in `ui/detail/DetailPager.kt` (`StackedPager`, `stackedPage`, `PagePositionPill`). Le pagine sono righe (serie GUARDATA + taglio, `RegularPageKey`, chiave String salvabile) nell'ordine della lista di origine, tre contesti: gli 8 tagli di una serie (schermata del paese, ordine per valore decrescente), un taglio in tutti i paesi (elenco di un taglio, con ricerca e filtro correnti), tutte le righe (scheda All, con ordine e filtro correnti); `RegularPagerSession` (ViewModel dell'Activity, copia delle righe al tocco, un solo "indietro") fa da `CoinPagerSession`. Al ritorno la lista di un taglio e la scheda All si aprono sulla riga in cui si è finiti (`ScrollToRequestedItem`, `ui/components/`, helper unico ora usato anche da `CoinListContent`); la schermata del paese NON scorre da sola (Column con `verticalScroll`, non una lista lazy, mantiene la posizione). Un `RegularDenominationDetailViewModel` per riga, chiave `regular-denomination-paese-ordine-taglio`. Non verificato sul telefono.
Il paese si passa in rotta come `Coin.paese` (valore stabile, non il nome
mostrato) con `Uri.encode`, perché "Città del Vaticano" e "Paesi Bassi"
hanno spazi/accenti.

### Home

Due schede di **pari peso** (14 dp tra le due; altezza: vedi sotto, con scroll di riserva),
stessa struttura (`CardContent`): fascia di 4 monete a
bordo scheda (fascia FISSA da 153 dp (era 160: -10% di aria sopra/sotto le monete). ALTEZZA delle schede: `heightIn(min = (spazio - 14 dp) / 2)` con tetto `CardMaxHeight` = 320 dp — dividono lo spazio disponibile, sugli schermi alti l'avanzo resta libero in fondo, e mai sotto il contenuto; con il banner AdMob nel `bottomBar` dello Scaffold (§ Pro e pubblicità) le schede si dividono lo spazio che resta; la colonna ha `verticalScroll` di riserva per schermi bassi. L'avanzo dentro la scheda va tra i testi (`SpaceEvenly`), non alla fascia. Storia: la fascia che prendeva tutto l'avanzo (~185 dp) lasciava le monete "perse in un deserto" (sono limitate dalla larghezza), schede compatte da 128 dp lasciavano la pagina vuota e la variante "superficie unica" con card `wrapContent` e monete su fondo pulito è stata provata e bocciata; 160 dp è il compromesso; cerchi sovrapposti di 14 dp, i due centrali più grandi), titolo,
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
sono le righe dei tagli di tutte le serie (8 per serie, anche i tagli invariati: la somma dei
totali delle card dei paesi), non le 4 monete della fascia — coerente con "584 coins" di
Commemorative, il totale del catalogo; terzo valore "series", non un intervallo di anni: nessuno
è affidabile per le serie, vedi § dataset), e sotto la STESSA barra "liquido" di Commemorative,
ora reale come quella di Commemorative: conta le righe possedute su N
(`HomeViewModel.regularIssueOwnedCount`, `regularProgress`, vedi § Regular Issues per il dettaglio). La home è anche dove parte il
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
  prezzo). Alla prima apertura di una moneta non posseduta NESSUNA finitura è
  spuntata e "Save" è spento finché non c'è una spunta (2026-10-13, come le
  Regular Issues; prima "Standard" era già spuntata: vedi § Collezione su Regular Issues).
- Una moneta conta come "posseduta" (casella piena, barre "x / y collected"
  di home/anni/paesi) se ha almeno una qualità.
- **Il controllo a destra di ogni riga cambia con la modalità** (2026-10-15, parere UX, variante A dopo mockup,
  `CollectionControl` in `ui/components/`, usato da `CoinRow` e `RegularCoinRow`): con Quick add acceso resta la casella
  quadrata con la spunta (è un interruttore vero); in modalità dettaglio è un pulsante rotondo, "+" a contorno se non
  posseduta e cerchio verde pieno con la matita se posseduta. Motivo: la casella con la spunta promette "tocco =
  spengo", e in dettaglio su una moneta già spuntata apriva un modulo (il proprietario lo trovava fuorviante: "posso
  cliccare un oggetto già spuntato"). + e matita sono gli stessi glifi di "Add / Edit collection" della card
  COLLECTION, e lo stato non dipende dal solo colore (cambia il glifo). Scartato "sempre +" (lo stato resterebbe
  nel solo riempimento, e "+" su una posseduta promette un secondo esemplare che l'app non ha). Le caselle DENTRO il
  pannello (finiture) restano caselle: lì un tocco su una spuntata la spegne davvero.
- Il salvataggio sostituisce in blocco le qualità della moneta
  (`CollectionDao.replaceForCoin`, transazione) e conserva `addedAt` delle
  voci già esistenti.
- **Tastiera nel pannello**: il contenuto usa
  `windowInsetsPadding(navigationBars.union(ime))` e non `navigationBarsPadding()` +
  `imePadding()` insieme, che contavano due volte la barra di navigazione (già inclusa
  nell'altezza della tastiera). Non verificato con la tastiera aperta sul telefono.
- **Layout del pannello** (`CollectionSheet.kt`): titolo a max 2 righe,
  sottotitolo "Paese · Anno", fondo grigio-verde del tema. Una **card da 64 dp
  per finitura** (Standard "Circulation", BU "Uncirculated" (era "Brilliant Uncirculated", che con il tondo della data si troncava sulla finitura non spuntata), Proof
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
    fissata perché il segnaposto "0.00" (al 50% dell'inchiostro: prima era dello stesso colore delle cifre e un BU senza prezzo accanto a uno da 3.00 si leggeva uguale) fa più altezza del campo con una cifra
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

- **Tasto "Remove" nei pannelli della collezione** (2026-10-08, mockup approvato, `RemoveFromCollection.kt`, `RemoveMessageTest`): in
  basso a SINISTRA, in testo rosso senza riquadro (il rosso è del distruttivo, come "Reset collection"; lontano da Cancel e Save),
  **solo se la moneta ha già qualcosa di salvato** (alla prima apertura non c'è nulla da togliere). Toglie TUTTO ciò che il pannello
  vede per quella moneta, dopo una conferma che dice cosa si perde: nelle commemorative le finiture ("This removes Standard and BU
  for …"), nelle Regular le annate con il conto ("This removes all 3 years of 2 euro for Croatia · Series 1: 2023, 2024 and 2025.",
  una sola annata nominata, oltre 6 abbreviata con "…", la varietà come "2002 EFS"). **Nelle Regular vale per il taglio della serie
  GUARDATA**, cioè le annate nella sua finestra: quelle di un'altra serie (stessa moneta, altra riga) restano, come già fa il
  salvataggio. Non serve nessuna funzione nuova nel database: è il salvataggio con zero spunte, che già sostituiva tutto (prima era
  l'unico modo, ma poco scopribile e con una spunta per ogni annata nelle Regular). Verificato sul telefono con Croazia 2 euro (2023 e
  2025) e una commemorativa del 2026: Cancel nella conferma non cambia niente, Remove riporta la riga a "Not owned", i contatori
  della Home tornano a quelli di prima.
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
- **Migrazioni Room esplicite** (DB versione 12 sul ramo `designer-nazionali`: `MIGRATION_11_12` aggiunge `coins.autoreRetroLivello`; la 11 mette la zecca
  nella chiave, § Zecche tedesche), `CoinDatabase.kt`), mai
  `fallbackToDestructiveMigration`: distruggerebbe anche la collezione
  dell'utente. `MIGRATION_1_2` (tabella `collection_items`), `MIGRATION_2_3`
  (`coins.emissioneComune`), `MIGRATION_3_4` (`coins.tiraturaNumista{Standard,Bu,Proof}`,
  colonne nullable, niente `DEFAULT`), `MIGRATION_4_5`
  (`coins.incisoreRetroRaw`/`disegnatoreRetroRaw`, stesso pattern nullable),
  `MIGRATION_5_6` (`CREATE TABLE regular_issue_series`, § Regular Issues —
  tabella nuova, non un `ALTER` su `coins`), `MIGRATION_6_7`
  (`CREATE TABLE regular_collection_items`, § Regular Issues — collezione
  utente sulle monete circolanti, stesso motivo), `MIGRATION_7_8` (`coins.numistaId`, nullable:
  il N# per i crediti Numista), `MIGRATION_8_9` (`regular_collection_items.variety` nella chiave primaria:
  tabella ricostruita, vedi § Collezione su Regular Issues), `MIGRATION_9_10` (`purchasedOn INTEGER`
  nullable su `collection_items` e `regular_collection_items`, due ALTER senza DEFAULT: le voci
  esistenti restano senza data; verificata sul telefono installando sopra la v9 popolata). **Una migrazione che aggiunge una colonna che il JSON già
  contiene (come la 8) NON cambia `coins.json`, quindi l'hash non farebbe ripopolare**:
  `CoinRepository.SEED_VERSION` entra nella chiave salvata (`hash:v2`) e va incrementata in
  quei casi. Ogni migrazione aggiunta va accodata,
  mai riscritta sopra una già rilasciata (anche in sviluppo: una volta
  installata su un telefono di prova, quel numero di versione è "usato"). Il
  valore delle nuove colonne conta poco: `ensureSeeded()` ripopola comunque
  `coins` da zero appena l'hash dell'asset cambia, la migrazione serve solo a
  far coincidere lo schema SQLite con l'entity Kotlin nel frattempo. Il SQL
  di ogni migrazione deve coincidere con quello generato da Room
  (`build/generated/ksp/.../CoinDatabase_Impl.kt`).
- **Data di acquisto** (ottobre 2026, `purchasedOn`, `PurchaseDates.kt`, `PurchaseDateButton`):
  facoltativa e **vuota di default** (chi registra la collezione che ha già non ricorda quando ha
  comprato: "oggi" sarebbe un dato sbagliato). Giorni dall'epoca (`LocalDate.toEpochDay()`), senza
  ora né fuso. **Una data per FINITURA** (corretto il 2026-10-05: prima era UNA per moneta, e una
  finitura aggiunta dopo ereditava in silenzio la data dell'altra, o la sovrascriveva salvando; nelle
  Regular era una per annata e varietà, stesso difetto). **UI** (`FinishCard`, usata anche dal pannello
  delle Regular; scelta dopo cinque giri di mockup): un TONDO da 40 dp (`PurchaseDateButton`) tra le
  scritte e il campo prezzo, solo sulle finiture spuntate (spazio riservato altrimenti: le scritte non
  cambiano larghezza a ogni tocco). Senza data: bordo e icona verdigris su un fondo appena tinto,
  calendario con "+"; con la data: tondo pieno con il calendario spuntato in bianco, e la data scritta
  sulla seconda riga sotto il nome, al posto del sottotitolo ("Circulation"...). Le icone sono
  `calendar-plus`/`calendar-check` di Tabler (MIT, `CalendarIcons.kt`: Material non ha il calendario con
  il più). La data si toglie con "Remove" nel selettore; in bozza si conserva se si toglie e rimette la
  spunta. La card resta da 64 dp (padding verticale 8). **Larghezza**: il telefono dell'utente è 375 dp,
  la card 343; con checkbox da 48 dp, tondo e prezzo la colonna delle scritte era di 89 dp e
  "Brilliant Uncirculated" si tagliava, quindi la casella è compatta (24 dp, `LocalMinimumInteractiveComponentSize`
  azzerato: l'intera card è già `toggleable`) e la colonna ne ha ~120. Scartati: scritta "Add date" da
  12-14 sp sotto il nome (troppo piccola da toccare anche a 32 dp), chip da 32/36 dp, striscia a tutta
  larghezza (+34 dp per card), tondo fantasma senza bordo, la riga della data sul sottotitolo "Paese ·
  Anno" / accanto all'anno nelle Regular. Nessuna migrazione e nessun cambio di backup: nel database la data era già per
  voce (`FinishEntry` = prezzo e data di una finitura, passato da `CollectionSheet` a
  `saveCollection`). Un modello a più ESEMPLARI della stessa finitura (funzione Pro, rimandata: Play
  Billing assente) dovrà cambiare la chiave primaria (indice dell'esemplare, come `variety` in
  `MIGRATION_8_9`) e il backup (v4). Il selettore è quello di Material (`DatePickerDialog` con fondo
  `surface`): dal 1° gennaio 1999 a oggi, niente date future. **Senza data si apre con OGGI
  preselezionato** (dopo mockup): "Add date" + OK basta per "l'ho presa/trovata oggi"; la data resta
  vuota finché non si preme OK, e il default vuoto è deliberato (chi registra la collezione che ha già
  non deve ritrovarsi date inventate, indistinguibili da quelle vere). **Dettaglio**: la riga di ogni finitura
  (e annata, nelle Regular) ha una seconda riga piccola, icona del calendario + "12 Mar 2026" o "No
  date" in grigio (`PurchaseDateLine`, `purchaseDateLabel`; **mai "Bought"**: una moneta può essere
  trovata, ricevuta o ereditata, non solo comprata, e lo stesso vale per ogni testo visibile);
  compare su TUTTE le righe della card solo se almeno una ha la data
  (`anyPurchaseDate`), così chi non usa le date non vede "No date" ripetuto e le righe restano da 44
  dp (con la data 58 dp). La data conta come modifica per lo stato del backup e viaggia nel backup
  v3. `PurchaseDatesTest`. Non ancora fatto: esemplari multipli, note libere, valuta diversa
  dall'euro, export CSV.

### Quick add (2026-10-10)

Impostazione GLOBALE (Impostazioni → sezione "Collection", interruttore "Quick add", spento di default,
`UserSettings.quickAdd`, fornita alle schermate con `LocalQuickAdd` da `MainActivity`): il tocco sulla casella di
una moneta aggiunge o toglie la voce più semplice senza aprire il pannello. **Dentro la stessa card**, sotto un filetto,
una `InfoNote` su più righe (icona allineata alla prima, `InfoNote(AnnotatedString, verticalAlignment = Top)`): "A coin
with **BU or Proof, a year, a price or a date** asks you to confirm before it's removed." (2026-10-10,
variante A dopo mockup, poi il verbo cambiato quando il tocco ha smesso di aprire il pannello (era "opens its details instead, so nothing is removed by mistake"): prima era fuori dalla card e diceva "Coins with a price, date or extra details still open the
form", giudicata poco esplicativa: "extra details" non dice quali e "the form" non è un nome che l'utente conosce, il
pannello si chiama My collection; scartata la variante B, tutto nel sottotitolo, che andava su tre righe strette).

**Decisione presa dopo tre giri (da ricordare): UNA collezione sola, due modi di toccarla.** Prima si era
pensato a due liste separate ("semplice" e "dettagliata", che non si parlano), poi scartato su parere UX: due fonti
di verità obbligano a migrare i dati quando si cambia modalità, a raddoppiare backup e Reset, e chi ha già una
collezione la ritroverebbe vuota. Il modello adottato: il possesso è il dato di base, finitura/anno/prezzo/data sono
dettagli facoltativi sopra; l'on/off è il caso più semplice dello stesso dato. Cambiare impostazione non fa
comparire né sparire monete; barre, filtri, Home, backup e Reset sono identici in entrambi i modi.

- **Regola del tocco** (`QuickAdd.kt`, `quickTapFor`/`quickTapForRegular`, `QuickAddTest`): **la casella vuol dire
  sempre "ce l'ho / non ce l'ho" e non apre mai il pannello** (2026-10-10, scelta dell'utente: prima, con dettagli, il
  tocco apriva il pannello, un gesto con due esiti secondo uno stato che non si vede, "strano"). Nulla in collezione →
  `ADD` (Standard senza prezzo né data; per le Regular la voce senza anno); solo la voce semplice → `REMOVE`, subito;
  qualunque altra cosa (BU, Proof, prezzo, data, un anno vero, la varietà EFS) → `CONFIRM_REMOVE`: dialog "Remove from
  collection?" (`RemoveConfirmDialog`, lo stesso del "Remove" dei pannelli, con `commemorativeRemovalMessage`/
  `regularRemovalMessage` che dicono cosa si perde); Cancel lascia tutto, Remove toglie tutto ciò che la riga vede
  (il salvataggio con zero voci). La conferma è solo una protezione per il caso raro in cui togliere farebbe perdere
  dati scritti a mano; i dettagli si modificano dal dettaglio ("Edit collection"). Nessun Undo (il messaggio
  flottante starebbe sopra la barra di ricerca e l'utente voleva farne a meno). `CoinRepository.quickToggle` /
  `RegularIssueRepository.quickToggle` fanno ADD e REMOVE; la schermata mostra la conferma e, se confermata, salva con
  `onSaveCollection(…, vuoto)`. Punti coperti: elenco commemorative (e scheda All) e le tre schermate Regular con la
  casella (serie del paese, elenco di un taglio, All). **Il dettaglio non cambia**: la card COLLECTION e i suoi
  pulsanti aprono sempre il pannello completo (è lì che si aggiungono i dettagli a una voce semplice).
- **"Anno non indicato" per le Regular** (`NO_YEAR = 0` in `RegularCollectionItem.anno`; il tocco semplice non può
  inventare un anno). Una voce senza anno appartiene alla riga GUARDATA, non alla serie di origine: un taglio
  rimasto invariato è la stessa moneta in più serie (il 5 cent francese è della serie 1 e compare nelle righe delle
  serie 2 e 3) e senza anno non si sa in quale finestra cada. Quindi `SeriesDenomination.viewed` (la serie in cui
  compare la riga) e la voce senza anno si salva con la chiave della serie GUARDATA (`RegularIssueRepository.
  saveCollection`: due scritture se la serie guardata ≠ quella di origine). **Una sola funzione dice quali voci
  sono di una riga**: `SeriesDenomination.rowItems`/`rowItemsFrom` (annate vere nella finestra, più le voci senza
  anno della serie guardata); la usano `regularProgress`, `denominationRows` e i ViewModel delle schermate Regular.
  `SeriesDenomination.contains(NO_YEAR)` è sempre falso.
- **Testo e pannello**: la riga dice "Owned" se ha solo la voce senza anno (non "0 years owned"), "N years owned" con
  anni veri. Nel pannello, `YearOption(NO_YEAR)` ha etichetta "No year" ed è il PRIMO chip, ma solo se la riga ha
  già quella voce (`regularYearOptions` la include tra le voci già possedute): non è un'opzione per chi non usa Quick
  add. Il pannello si apre su di essa. Card COLLECTION del dettaglio: "Standard · No year" (`chipLabel`). Il testo di
  conferma di "Remove" non nomina l'anno quando la voce è solo quella.
- **Backup versione 4** (`BackupFile.SCHEMA_VERSION`): le voci Regular possono avere `anno` 0. Un file v4 non si
  apre con un'app più vecchia (rifiutato come "più nuovo"). Nessun cambio al database (nessuna migrazione: `anno`
  è già un intero nella chiave).
- **Verificato sul telefono** (2026-10-10, build debug, senza salvare dati veri: monete non possedute aggiunte e subito
  tolte, contatori della Home tornati a 93 / 613 e 194 / 328): tocco su una commemorativa (Monaco: la casella si
  riempie, nessun pannello, il secondo tocco la svuota); riga Regular ereditata (Francia 5 cent serie 2: diventa
  "Owned", la serie 1 resta "1 year owned", la serie 3 "Not owned", il secondo tocco la toglie); card COLLECTION
  "Standard · No year"; chip "No year" nel pannello; moneta con un anno vero (Grecia 2 euro, 2002 e 2002 EFS): il
  tocco apriva il pannello (dal 2026-10-10 chiede invece la conferma di rimozione: vedi sopra, ancora da provare sul telefono). **Bug trovato e corretto**: il chip del pannello mostrava "0" (aveva un'etichetta sua);
  ora c'è un'unica `YearOption.chipLabel()` in `RegularYearOptions.kt` usata da pannello e dettaglio.
- **Non fatto**: pulsante "Remove" o conferma per le voci semplici, feedback aptico (valutato e lasciato fuori), un
  modo di convertire le voci senza anno in un anno vero in blocco.

### Primo avvio (2026-10-19, unito a `main` e pubblicato il 2026-10-22; non ancora in un .aab)

Un utente nuovo vede la Home come sempre e, sopra, una scheda dal basso "Welcome to Euro Coins" ("Two quick settings and
you're ready to start your collection.", **Set up** / **Later**). "Set up" porta alle Impostazioni (`settings?tour=true`)
con un percorso di DUE passi: (1) Account and backup evidenziata ("Sign in to back up your collection and restore it on a
new phone.", Skip / Next), (2) la card Quick add ("Quick add is on/off: …", il testo segue l'interruttore, che si può
provare; Back / Done). Done e Skip riportano alla Home. **Scelto dopo due giri di mockup** (`SetupTour.kt`):
la prima idea (due schermate di benvenuto con login e scelta della modalità) è stata sostituita da questa perché usa i
controlli veri nel posto dove si ritrovano e non lascia schermate finte da tenere allineate; il salto alle Impostazioni
parte da un tocco ("Set up"), non da solo (un cambio di schermata senza gesto disorienta). **Evidenziazione senza
spotlight**: bordo da 2,5 dp nel colore primario con un alone, disegnato FUORI dai limiti (4 dp, il contenuto non si sposta);
le altre zone (card Pro, Catalogs, Appearance, Danger zone) a 38% e con i tocchi assorbiti da un `clickable` (non da un
`pointerInput` che consuma: impedirebbe lo scorrimento da lì); la pagina scorre da sola alla zona (`positionInParent`
della sezione Collection), con 220 dp di spazio in fondo per la scheda del passo. "Skip" chiude tutto il percorso.
- **Stato**: `UserSettings.setupTour` (`PENDING`/`DONE`, chiave `setup_tour`), solo "da fare / fatto"; il passo vive nella
  schermata (`rememberSaveable`). "Set up" e "Later" (anche chiudere la scheda) lo mettono a DONE subito: se il processo muore
  a metà non ricompare. **Installazioni esistenti non vedono nulla** (te, i tester, chi aggiorna): senza valore salvato, se
  l'hash del catalogo c'è già in SharedPreferences `dataset` è DONE (`resolveSetupTour`). Si decide alla prima lettura delle
  preferenze, PRIMA del seeding, e si salva subito: altrimenti alla seconda apertura anche un'installazione nuova sembrerebbe
  esistente. Verificato sul telefono: aggiornando sopra l'app esistente il valore è DONE. **Per rivedere il percorso**: nella
  sezione Developer delle Impostazioni (solo build di sviluppo) c'è "Replay setup tour": rimette il valore a PENDING
  (`UserSettings.restartSetupTour`) e torna alla Home, dove ricompare la scheda. Senza, con `adb` e l'app ferma:
  `run-as com.michele.eurocoins sed -i 's/setup_tour">DONE</setup_tour">PENDING</' shared_prefs/settings.xml`.
- **Consenso UMP**: in UE il modulo compare allo stesso avvio, quindi la scheda aspetta `AdsConsent.settled` (modulo chiuso,
  non richiesto o errore) oppure il Pro. Ordine: consenso, poi benvenuto.
- Il testo del passo "account" non cambia se l'utente è già collegato (non succede al primo avvio). Nessun "rivedi il tutorial"
  nelle Impostazioni (non chiesto). `SetupTourTest`. **Verificato sul telefono (2026-10-19, build debug, tema scuro, account
  già collegato)**: scheda sulla Home, passo 1 con cornice e alone, passo 2 con il testo che passa da "off" a "on" toccando
  l'interruttore, Done che riporta alla Home senza che la scheda ricompaia (`setup_tour` = DONE). Non verificati: tema
  chiaro, font ingrandito, utente non collegato (la card account mostra l'invito invece dell'email), Skip e il tasto
  indietro a metà percorso.

### Insights (ramo `insights-euro`, 2026-10-25, NON unito a `main`)

Pagina raggiunta dall'icona grafico accanto all'ingranaggio della Home (`ROUTE_INSIGHTS`, `ui/insights/`), con **tre
schede a segmenti** (stessa grammatica di Years / Countries / All): **History** (si apre per prima: la storia, gli stati
per accordo, la mappa del 2007), **Coins** (di che metalli sono fatte le monete) e **Statistics** (Pro, lucchetto
sull'etichetta da Free). Divisione scelta con il proprietario il 2026-10-25: la pagina era arrivata a più di quattro
schermate con due argomenti scollegati, la storia (da leggere) e i materiali (da toccare); il selettore sta fermo in alto
mentre si scorre, ogni scheda ha le sue fonti e riparte dall'alto. **Statistics è per ora un segnaposto** ("Go Pro" da
Free, "Coming soon" da Pro): una sola scheda e non Collection e Spending separate, per non avere quattro segmenti. Pagina
unica con segmenti e non una voce a parte (con lo scorrimento unico il lucchetto sarebbe finito a metà pagina). Non ancora
fatto: aprire da Pro sull'ultima scheda vista (ora si apre sempre su History). Il nome "Insights" è provvisorio
(questione di secondo ordine). Le statistiche (completamento per paese e catalogo, finiture, spesa con la riga "basato su N
monete con un prezzo") sono nel mockup del 2026-10-24 ma non scritte. Regole decise per le statistiche: niente valore di
mercato (nessuna fonte, e sfiora il consiglio finanziario), niente classifica di rarità dalle tirature (7 paesi hanno un
contingente, non una tiratura).

**"The euro"** (`EuroTab.kt`, dati fissi in `EuroFacts.kt`, `EuroFactsTest`): non viene dalla pipeline né da Numista, sono
fatti pubblici verificati sulle pagine BCE, Commissione e Bundesbank (URL in `HISTORY_SOURCES` e `COIN_SOURCES`, una lista per scheda, mostrati come `SourceCredits`).
Due card più le fonti: (1) **"The story of the Euro"** (con la E maiuscola nel titolo, su richiesta): timeline con una sola tinta. **Ogni voce è un anno con un
sottotesto, SEMPRE, e la stessa forma (anno, sottotesto, paesi); niente titoli** (scelta del 2026-10-25: con titoli solo su
alcune voci sembrava disomogenea; poi anche le voci con solo anno e bandiere "stonavano" accanto a quelle con il
sottotesto, e il layout compatto con l'anno a sinistra è stato tolto: `EuroFactsTest` controlla che nessuna voce sia senza
testo). Per gli ingressi il sottotesto è il numero d'ordine ("The 16th country joins."), che non ripete i nomi dei
paesi e dice quanti sono; un titolo li ripeterebbe. "1957–1998 · 6 steps to the single currency" è chiusa di default (le 6
tappe si aprono con un tocco), poi 1999 ("The euro starts, as money in accounts and payments. Coins and notes come later."
: senza questo "1999" faceva pensare che l'euro circolasse da allora), 2001 Grecia, **2002 "Coins and notes begin, in the
12 countries"** (una tappa senza paesi: è qui che l'euro arriva in tasca) e gli ingressi dal 2007, con bandiera e nome
per intero (due per riga). **Il confronto della mappa del
lato comune sta nella voce del 2007** (la Slovenia è la prima a usare il disegno nuovo, il 1° gennaio 2007): una riga lilla
"Compare the old and the new map" la apre, chiusa di default, e le due foto si scaricano solo allora. **I titoli delle card
sono 20 sp Bold nel colore del testo** (`InsightsTitle`, come le Impostazioni; l'etichetta piccola in verde non bastava). **Il tocco su un paese apre il riquadro con date, vecchia valuta e tasso** (Joined the
euro, Coins and notes, Replaced, Rate: **quattro righe uguali per tutti**; c'era una quinta, "Only euro from", l'ultimo
giorno di doppia circolazione, solo dal 2007 perché per i primi 12 la data non era verificata: tolta il 2026-10-25 perché
disomogenea e poco chiara, e anche dai dati), come nel mockup: la versione con una frase sola era stata una
semplificazione di troppo e al proprietario piaceva il riquadro; **gli stati per accordo stanno in una card a parte, "Also using the euro", NON in fondo alla timeline** (scelta del
2026-10-25: come ultima voce, dopo il 2026, sembrava un fatto avvenuto dopo; non ha una data, Monaco, San Marino e il
Vaticano usano l'euro dal 1999, Andorra dal 2002): per Andorra, Monaco, San Marino e Vaticano ha un riquadro con **le stesse righe degli altri** (Uses the euro since, Coins and notes, Replaced, Rate: scelta
del 2026-10-25, "nella scheda non compaiono le stesse info degli altri stati") più le due solo loro (Own euro coins from,
Agreement). Monaco sostituì il franco francese (6,55957), San Marino e Vaticano la lira (1.936,27); Andorra non aveva una
moneta sua e usava franco e peseta senza corso legale, quindi mostra entrambe le monete con la sigla e i due tassi, ciascuno su una riga (un valore lungo andava
a capo a metà frase), e "2002" come anno d'uso (l'accordo è del 2011, applicato dal 1° aprile 2012). **Semplificata su richiesta**
("troppo pesante e prolissa, con troppi indicatori"): scartati cerchi con il conteggio, legenda a quattro fasi colorate,
barra di avanzamento e 11 tappe prima del 1999. (2) **materiali**: otto monete in scala (`DenominationCoin`) in due
righe e, sotto, UNA CARD che spiega il materiale della moneta toccata (nome, spiegazione con cupronichel e ottone al nichel
dentro la frase, diametro, peso, spessore, bordo, magnete): è la versione del primo mockup, che al proprietario piaceva più
delle tre famiglie con il glossario a parte ("buttato a caso"); la BCE e la Bundesbank non danno le percentuali delle leghe
e non si inventano. **Lato comune** (dentro la voce del 2007): le foto BCE del 50 cent prima e dopo il 2007 in hotlink,
ritagliate sull'area della mappa (`MapPhoto`: la foto è disegnata più grande e spostata; `requiredSize` la CENTRA, lo
spostamento parte dalla metà dell'eccedenza), con una frase (prima: 15 paesi come pezzi separati; dopo: un'Europa continua;
perché: l'allargamento del 1° maggio 2004). Il copyright del lato comune è dell'UE: la Commissione ammette riproduzioni piatte
"non pregiudizievoli per l'immagine dell'euro" e non parla di app né di uso commerciale; stesso rischio accettato per le
altre foto BCE. **Da verificare**: l'anno delle prime monete di San Marino (2002) non è confermato dalla pagina BCE; il
"serpente" del 1972 è stato tolto dall'elenco (fonte debole). Dettaglio dei tassi: Germania e Bulgaria hanno lo stesso,
1,95583. Provato sul telefono (2026-10-25, tema scuro): icona d'ingresso, timeline con titoli, riquadro di un paese, card dei
materiali, ritaglio della mappa (corretto: `requiredSize` centra la foto). Non provati: tema chiaro, font ingrandito,
senza rete (le due foto BCE mancherebbero).

### Impostazioni

Schermata principale (`SettingsScreen`), sezioni: Account and backup (con la card Go Pro
sotto, senza titolo proprio; § Pro e pubblicità), **Collection** (Quick add, § omonima sotto), **Catalogs** (una sola riga
che apre la sottopagina `CatalogSettingsScreen`, vedi sotto), Appearance, Danger zone.

**Riorganizzazione (2026-10-11, dopo mockup)**: la pagina era arrivata a ~2,6 schermate (le card di backup e Pro
sono alte e Quick add aveva aggiunto una sezione). I cinque controlli che si toccano una volta sola, microstati nascosti
e scheda iniziale di ciascun catalogo, stanno ora in una SOTTOPAGINA "Catalogs" (`CatalogSettingsScreen`, route
`settings/catalogs`, le stesse due `CatalogCard` di prima più la card "German mints", una schermata intera), raggiunta
da una riga che è UNA FRASE sul contenuto, uguale per Free e Pro: "Microstates, start tab and German mints"
(`CATALOGS_ROW_TITLE`; variante A scelta il 2026-10-13 dopo mockup). **Storia**: la riga si chiamava "Catalog options"
(ripeteva il titolo della sezione "Catalogs") e sotto aveva il riepilogo dei valori correnti ("Commemorative: Countries
/ Regular Issues: Countries": prolisso, e la scheda iniziale si cambia una volta sola); da Free non diceva che le zecche
tedesche stanno là dentro. Ora le zecche si nominano anche da Free (è l'unico punto dove si scopre che esistono) e
sotto compare una riga SOLO se qualcosa è nascosto (`catalogsHiddenNote`: "Microstates hidden in Commemorative / Regular
Issues / both", "German mints hidden", perché cambia ciò che si vede e i totali). Spazio non separabile (U+00A0) tra
"German" e "mints": senza, a capo dopo "German" lasciava "mints" da solo. `CatalogsSummaryTest`. **Quick add resta IN LINEA sulla principale** (scelta dell'utente, variante "card in linea": è la funzione
nuova e si accende/spegne per provarla, non va cercata in una sottopagina). Appearance resta in linea: una
sottopagina per due controlli sarebbe solo attrito. Scartate: sezioni a fisarmonica (nasconde senza accorciare) e una
sottopagina per sezione (troppo frammentata). Costo: un tocco in più per cambiare uno dei cinque controlli spostati.
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
  default spostava l'etichetta e sbilanciava le larghezze) e larghezza = testo MISURATO (`rememberTextMeasurer`, minimo 64 dp) + padding, SIMMETRICA (il più largo tra il segmento e il suo opposto): con tre segmenti uguali "Denominations" andava a capo nelle Impostazioni, con pesi diversi sui due lati quello di mezzo non stava al centro, e contando i caratteri "Countries" di mezzo andava a capo a sua volta; i selettori con etichette corte restano pari). **Stessa
  scelta nei selettori di Browse (Years / Countries / All) e di Regular Issues** (`icon = {}`): con
  la spunta "Years" risultava visibilmente decentrato; il segmento selezionato si riconosce già dal
  lilla.
- **Switch** "Hide microstates": colori espliciti anche da spento (bordo e pallino
  `onSurfaceVariant`, traccia `background`) per lo stesso motivo; spunta nel pallino solo
  da acceso.


- **Hide microstates** (Andorra, Monaco, San Marino, Città del Vaticano,
  `MICROSTATE_PAESI`; il sottotesto dice "Andorra, Monaco, San Marino, Vatican", senza "City", per stare su una riga accanto allo switch nel layout standard delle righe con interruttore: con "Vatican City" andava a capo lasciando "City" da solo, uno spazio non separabile spostava solo il problema e il sottotesto a tutta larghezza sotto lo switch (provato) staccava la riga dalle altre. Scartata anche la riduzione a 12 sp): il filtro sta in `CoinRepository.coins`/`paesi`
  (`combine` con `UserSettings.hideCommemorativeMicrostates`), quindi elenchi, griglie,
  ricerca e home lo rispettano tutti e i totali "x / y collected" escludono
  i microstati nascosti. Le monete già possedute restano nella collezione e
  nel backup. **Impostazione INDIPENDENTE per catalogo** (ottobre 2026): Regular Issues ha la sua
  (`hideRegularMicrostates`, filtro in `RegularIssueRepository.series`), così si possono nascondere
  solo da uno, dall'altro o da entrambi. **Struttura: una sezione per catalogo** ("Commemorative" e
  "Regular Issues", `CatalogCard`), ognuna con interruttore + scheda iniziale, al posto della vecchia
  "Catalog and display" (variante A scelta dopo mockup; scartata la B, una sola card con un selettore a
  4 stati Off/Comm./Regular/Both: etichette abbreviate, segmenti stretti, due "Default tab" da
  distinguere). La chiave `hide_microstates` resta e vale per le commemorative; quella delle Regular
  (`hide_regular_microstates`) finché non viene toccata EREDITA il valore delle commemorative
  (`resolveHideRegularMicrostates`), così chi aveva l'interruttore acceso non vede cambiare niente.
- **Default tab**: scheda di Commemorative che si apre per prima
  (`UserSettings.defaultTab`, letto alla creazione del `BrowseViewModel`).
  Scartato il riordino completo dei segmenti: i segmenti restano Years /
  Countries / All. **Anche Regular Issues ha la sua** (`UserSettings.defaultRegularTab`, chiave
  `default_regular_tab`, Denominations / Countries / All, predefinito Countries, letta alla creazione di
  `RegularIssuesViewModel`: cambiarla da Impostazioni vale dalla prossima apertura della schermata,
  come per Commemorative). `UserSettingsTest` copre ereditarietà e valore di ripiego.
- **Rotate home coins** (sezione Appearance, interruttore, **acceso di default**;
  `UserSettings.rotateHomeCoins`): le 4 monete della fascia della Home cambiano ogni giorno.
  Solo on/off, senza scegliere la frequenza (scelta dell'utente; scartati "a ogni apertura",
  che scarica 4 foto nuove a ogni avvio, e "settimanale"). `pickShowcase` (`HomeShowcase.kt`)
  sceglie con seme = `LocalDate.toEpochDay()`: DETERMINISTICO (stesso giorno = stesse monete,
  niente da salvare, foto in cache di Coil), una moneta per paese, solo con foto, rispetta Hide
  microstates perché parte da `repository.coins`. Spento: set fisso (i primi 4 paesi). La Home
  precarica in Coil le foto del set di DOMANI (`nextShowcase`): domani è già pronta, anche
  offline se oggi l'app è stata aperta online. **Foto che non si carica: catena per moneta** (`ShowcaseCoin`): foto di oggi → foto dello stesso slot dell'ultimo set mostrato per intero (`UserSettings.lastShowcase`, salvato quando le 4 sono arrivate; sta già nella cache su disco di Coil, quindi regge anche offline) → moneta DISEGNATA (2€ bimetallica, `FallbackCoins`) se non c'è altro. NON ci sono foto BCE nell'APK come set predefinito: licenza "uso editoriale", da chiarire prima di pubblicare (§ Backlog). **NESSUNA animazione di entrata** (scelta dell'utente: la dissolvenza per moneta di Coil dava comparse scaglionate; una dissolvenza coordinata dopo aver atteso tutte le foto risultava "lenta"; le monete devono esserci all'apertura, come prima della rotazione). Per esserci al primo fotogramma la Home non aspetta il database: `UserSettings` salva gli URL del set di OGGI e di DOMANI (`saveShowcaseUrls`, chiavi `showcase_day_<giorno>`) e `HomeViewModel` li usa subito all'avvio (ripiego: l'ultimo set mostrato); quando arriva il database il set calcolato coincide e non si nota nulla. Le foto sono già nella cache su disco perché la Home le precarica il giorno prima. **Decodifica già in `onCreate`** (2026-10-06, `preloadHomeShowcase`, `HomeShowcase.kt`): misurato su release a freddo, `AsyncImage` partiva solo dopo il layout (60-160 ms dopo `onCreate`) e la foto arrivava ~40 ms più tardi dalla cache su disco, quindi il primo fotogramma usciva coi tondi vuoti e le monete comparivano dopo. Ora `MainActivity` accoda le 8 foto prima di `setContent`, in parallelo alla composizione, e la Home le trova in memoria (`MEMORY_CACHE`, pronte prima del primo disegno). Funziona perché la richiesta è costruita in un punto solo (`showcaseRequest`) con `Size.ORIGINAL`: la chiave della cache in memoria contiene la dimensione, e con quella del layout il precaricamento non potrebbe indovinarla; cambiare la richiesta in `ShowcaseCoin` senza passare da lì rompe l'incontro. Gli URL sono quelli del primo fotogramma (`firstFrameShowcaseUrls`, gli stessi del ViewModel). (Prima non gestito: foto che non si carica il
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
- **Reset collection**: la riga nella Danger zone dice solo il totale ("Removes 6 coins from this device", una riga sola: con il dettaglio per sezione andava su tre righe); il dialog di conferma con il numero di monete, diviso per sezione (un ELENCO: "This removes the following coins:" e una voce per sezione, "• 85
  Commemorative", "• 4 Regular Issues", `OwnedCounts.rows()`, `OwnedCountsTest`: i nomi dei cataloghi come nel resto dell'app, senza "coins" in coda (lo dice l'intestazione: niente plurale da accordare, scelta dell'utente), la sezione vuota saltata. La frase andava a capo male ("85 coins from Commemorative and 4 coins from Regular Issues", 71 caratteri: a capo dopo "from", visto sul telefono; la versione accorciata "85 Commemorative and 4 Regular Issues coins" non è stata verificata a schermo prima di passare all'elenco); le righe dell'elenco sono corte e non si spezzano. Con una sezione sola resta un elenco di una voce, stessa forma sempre); svuota
  `collection_items` **e** `regular_collection_items` (`CoinRepository.resetCollection` +
  `RegularIssueRepository.resetCollection`). Prima toglieva solo le commemorative e lasciava
  le righe Regular senza dirlo. I tagli Regular si contano distinti per (serie, taglio), non per
  annata. Il backup su Drive non viene toccato: un nuovo backup dopo il reset lo sovrascrive.
  **Aspetto del dialog (2026-10-10, mockup approvato, scelta dell'utente: "adatterei il Reset al Replace e non il
  viceversa")**: stessa grammatica del dialog di ripristino ("Replace your collection?", § Backup). Frase "This removes
  your collection from this phone.", riquadro "THIS REMOVES" con una riga per catalogo e il Total
  (`CountsBox`/`coinsText`, `ui/components/DialogParts.kt`, condivisi con il ripristino: non possono divergere), poi la
  nota sul backup (`resetBackupNote`). **La nota è rossa con icona (`DialogWarning`) solo se qualcosa andrebbe perso**
  (non collegato, modifiche non salvate, nessun backup: `resetBackupNoteIsWarning`), testo normale se il backup è
  aggiornato (una rassicurazione non è un avviso). Sostituisce l'elenco puntato "This removes the following coins:"
  descritto sopra; il testo scorre con il font ingrandito.

### Backup su Google Drive

Sezione "Account and backup" della schermata Impostazioni (ingranaggio in
alto a destra nella home; la vecchia schermata Backup è stata assorbita): login con Google (Credential Manager) e backup/ripristino della
collezione su Drive.

- **UI** (`BackupSection`): senza accesso una card d'invito + "Sign in with
  Google"; con l'accesso l'email, una card di stato in evidenza (barra di
  avanzamento durante le operazioni), "Back up now"
  (2/3 della riga, pieno) e "Restore" (1/3, a contorno: è quello che
  sovrascrive), sotto un filetto l'interruttore "Back up automatically". "Sign out" è un TextButton nel colore primario a destra
  dell'email, sulla stessa riga.
- **Stato del backup, calcolato in locale** (ottobre 2026, `BackupStatus`/`backupStatus()`): l'app
  tiene una copia dell'ultimo file caricato o ripristinato (`BackupSnapshotStore`, `filesDir/
  backup_snapshot.json`, nel formato del backup; `exportedAt` = data dell'ultimo backup da questo
  telefono) e la confronta con le voci attuali, senza chiamare Drive e quindi anche offline. Tre
  stati: **"Up to date"** (coincidono), **"N changes not backed up"** (voci aggiunte, tolte o
  modificate: una voce = una finitura, o anno+finitura+varietà nelle Regular; un reset compare come
  "tutte tolte"), e **non verificabile** (nessuna istantanea: titolo "Backup found" + data, o "Not
  backed up yet"). Un'istantanea v1 non conteneva Regular Issues, quindi tutte le righe Regular
  locali risultano "non salvate": non si afferma mai "aggiornato" per dati che su Drive non ci sono.
  Il ripristino della versione precedente cancella l'istantanea (la collezione locale non
  coincide con l'attuale su Drive); la disconnessione pure (appartiene all'account). Scartato un
  pallino sull'ingranaggio della Home: con il backup automatico il caso "dimenticato per
  settimane" quasi non esiste. **Collezione vuota** (`BackupUiState.localEmpty`): la card non
  invita a fare un backup (che non avrebbe senso, errore trovato dall'utente: "che senso ha farmi
  fare il backup se non ci sono monete?") ma dice "Nothing to back up yet" / "Add coins to your
  collection first.", o, se c'è un backup, "Collection is empty" + "Your last backup (data) is
  still on Google Drive." (vale anche dopo un reset, al posto di "N changes not backed up").
  "Back up now" a collezione vuota NON carica niente (`EMPTY_COLLECTION_MESSAGE`, controllato prima
  del dialog di sovrascrittura e di nuovo in `BackupService.backup`): un backup vuoto non serve e
  cancellerebbe quello buono. Conseguenza accettata: non si può svuotare di proposito il backup su
  Drive dall'app.
- **Salvataggio automatico** (`AutoBackup`, ottobre 2026): quando l'utente esce dall'app
  (`MainActivity.onStop`) e la collezione è cambiata (`Pending`), carica in silenzio il backup
  (token Drive senza schermate: `GoogleAccountManager.silentDriveToken`; se serve il consenso non
  parte e mostra l'errore). Solo gli stati **Pending**: con "non verificabile" questo telefono non
  ha mai fatto un backup e non sovrascrive in silenzio quello di un altro telefono (lì decide
  l'utente col backup manuale e la sua conferma). **Mai una collezione vuota** (dopo un Reset
  caricherebbe il niente sopra un backup buono: il caso da cui è nato tutto questo). Almeno 15
  minuti tra un tentativo e l'altro, anche dopo un errore (`AUTO_BACKUP_MIN_INTERVAL_MS`; l'avevo
  proposto "qualche ora", ma il file è di pochi KB e un backup che aspetta ore lascia le modifiche
  esposte). **Interruttore**: se l'utente non lo ha mai toccato è acceso solo per chi ha già un
  backup da questo telefono (`resolveAutoBackupEnabled`); la sua scelta vince sempre; con
  l'interruttore acceso ma senza backup precedente la scritta dice "Starts after your first backup"
  (altrimenti "When you leave the app": sottotesti corti, stanno su una riga). L'errore dell'ultimo tentativo si mostra nella card ("Automatic backup failed:
  …", icona e tinta d'errore) solo se è più recente dell'ultimo backup riuscito. **Senza WorkManager**:
  non è nella cache Gradle offline (andrebbe scaricato con la rete) e non serve la regolarità di un
  lavoro periodico; il limite è che il sistema può fermare il processo subito dopo `onStop`, ma il
  salvataggio dura un secondo e riparte alla prossima uscita.
- **Versione precedente su Drive** (`DriveBackupClient.upload`): oltre al file attuale
  (`euro-coins-collection.json`) c'è `euro-coins-collection-previous.json`. Prima di sovrascrivere,
  l'attuale diventa "precedente" SOLO se ha almeno 24 ore (`shouldRotatePrevious`) o se la
  precedente non esiste: così salvataggi ravvicinati, anche automatici, non cancellano subito la
  copia buona (un reset seguito da pochi acquisti non porta via la collezione di prima: chi se ne
  accorge ha un giorno). Limite: dopo 24 ore la precedente è quella dell'ultimo salvataggio di più di
  un giorno fa, non una cronologia.
- **Dialog di "Restore"** (`RestoreDialog.kt`, 2026-10-10, mockup approvato): dice COSA si ripristina. Al tocco su
  "Restore" l'app scarica i due file di Drive (pochi KB, `BackupService.restorePreview`, pulsante occupato per
  ~1 s; non si scarica nulla aprendo le Impostazioni) e conta le monete distinte come il Reset
  (`BackupFile.contents()`: una commemorativa con più finiture conta 1, un taglio Regular con più annate o la
  varietà EFS conta 1). Riquadro "THE BACKUP CONTAINS": Commemorative, Regular Issues e Total. **"Anything added since
  will be lost." è in rosso con icona** (scelta dell'utente: è un avviso di perdita dati, il rosso è già il colore
  del distruttivo). Un backup della versione 1 non porta le Regular Issues e il ripristino non le tocca: lo dice
  una riga ("Regular Issues aren't in this backup: yours stay as they are") invece di mostrare "0". **La versione
  precedente è una sezione separata da un filetto**, "OLDER COPY ON DRIVE", con data, contenuto e
  "Restore this one instead": prima era un link senza spiegazione e l'utente non capiva che su Drive ci sono due copie
  (l'ultimo backup e quella di almeno un giorno prima). Non è un `TextButton`: la forma a pillola ritagliava i bordi
  del testo a capo; è un `Text` cliccabile. Se la precedente non si legge (assente, rete, file rovinato) la sezione non
  compare ma l'ultimo backup deve leggersi. Il dialog sta nell'area del testo (sopra Cancel/Replace: AlertDialog non
  ammette una sezione sotto i pulsanti) e scorre con il font ingrandito.
  **La nota sulla perdita dipende dallo stato del backup** (`restoreNote`, `RestoreNoteTest`; stesso criterio del
  Reset: il rosso solo dove qualcosa va perso davvero): collezione aggiornata → testo normale "Your collection is
  already up to date with this backup."; modifiche non salvate → rosso "N changes not backed up will be lost." (le
  parole della card di stato); stato non verificabile → il generico "Anything added since will be lost.". Prima era
  sempre rossa, anche quando telefono e backup coincidevano e non c'era niente da perdere. Rifiniture: il link
  "Restore this one instead" alto 44 dp (era 48, lasciava troppo vuoto sopra i pulsanti); nella nota del Reset la data
  senza parentesi con "from"/"on" davanti ("…backup from 9 Oct 2026, 07:52 has them…"), perché la data tra parentesi,
  tenuta intera da spazi non separabili, andava a capo lasciando una prima riga corta.
- **Pro e pubblicità**: card Go Pro sotto quella del backup, banner nella Home e annuncio a tutto
  schermo — vedi § Pro e pubblicità.
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
  acquisto) senza rompere i backup vecchi. **Versione 3** (ottobre 2026): aggiunge `purchasedOn`
  (facoltativo, default vuoto) a entrambi i tipi di voce; i file v1 e v2 si leggono ancora, con le voci
  senza data, e un file v3 non si apre con un'app più vecchia (rifiutato come "più nuovo", meglio che
  perdere le date in silenzio). **Versione 4** (2026-10-10): le voci Regular possono avere `anno` 0 ("anno non indicato", § Quick add). **Versione 2** (ottobre 2026): aggiunge
  `regularItems` (`RegularBackupItem`: `seriesKey`, taglio, anno, qualità, varietà, prezzo,
  paese, `addedAt`), cioè `regular_collection_items`; prima Regular Issues non era nel backup e
  perdere il telefono voleva dire perdere tutte le righe. **Un file v1 si legge ancora ma NON
  porta Regular** (`BackupFile.includesRegularIssues`, `schemaVersion >= 2`): ripristinarlo
  lascia la collezione Regular locale com'è invece di azzerarla; un v2 con `regularItems` vuoto
  invece è un dato vero e sostituisce. Il conteggio "N entries" di backup/ripristino somma le due
  sezioni, il dialog di sovrascrittura conta monete distinte (commemorative + tagli Regular).
  Non verificato end-to-end con Drive reale (serve l'account): coperto da `BackupFileTest`.
- **Storage**: cartella `appDataFolder` di Drive (scope `drive.appdata`):
  privata e nascosta, l'app non vede altri file dell'utente. Un solo file
  (`euro-coins-collection.json`), ogni backup lo sovrascrive. Chiamate REST
  dirette in `DriveBackupClient` (nessuna libreria client Google).
- **Ripristino = sostituzione** della collezione locale
  (`CollectionDao.replaceAll` e, per un backup v2, `RegularCollectionDao.replaceAll`), con
  dialog di conferma. Le due sostituzioni sono due transazioni consecutive, non una sola.
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
  ancora sulla BCE; **le immagini BCL sono 3 file di gruppo e quella di 1 e 2 euro,
  `1-2-euro.png`, ha DUE monete affiancate (417×211)**: ritagliata a cerchio mostrava un pezzo di
  ciascuna. Non esiste di meglio — la pagina BCE del Lussemburgo descrive ancora il Granduca Enrico,
  Numista non è usabile —, quindi `RegularIssueImages.kt` (`withUsableImages`, applicata in lettura
  in `RegularIssueRepository.series`, non al seeding: l'asset non cambia e non ripopolerebbe) toglie
  quell'URL e mette il segnaposto generico, con anche fonte/licenza/credito dell'immagine vuoti
  perché i crediti non accreditino una foto che non c'è. I centesimi della stessa serie sono monete
  singole e restano. Da togliere quando la BCE pubblica le immagini vere). **Una sola immagine BCE ha lo sfondo NERO**,
  `France_1euro_2022.jpg` (1 euro della serie 2022 e, per eredità, della 2024; segnalata dall'utente
  come "strano riquadro nero" nel dettaglio; scansionati gli angoli di tutti i 295 URL BCE: le altre
  hanno lo sfondo bianco o sono ritagliate a filo): sulla card bianca del dettaglio era un quadrato
  nero (nell'elenco non si vede perché la miniatura è un cerchio). `RegularIssueImageTrim` rileva i
  quattro angoli scuri (`hasDarkBackground`, `CoinDiscDetection.kt`), ricava il disco della moneta
  (`findCoinDisc`: diametro = lato più lungo dei pixel non scuri, perché il bordo in alto e in basso è
  più scuro: 537 px in larghezza e 527 in altezza) e lo ridisegna come cerchio anti-aliasing su fondo
  TRASPARENTE (raggio −1,5 px: il jpeg sfuma verso il nero e lascerebbe un filo scuro); vale anche per
  la fascia Home. Verificato sul telefono. `CoinDiscDetectionTest` copre la logica sui pixel; il
  rendering con `Bitmap` non è testabile in JVM. Per questo `RegularIssueImage` ha un proprio
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
- **Navigazione**: `HomeScreen` → `RegularIssuesScreen` → `RegularIssueCountryScreen` (route
  `regular-issues/{paese}`, `Uri.encode` come per le commemorative) oppure
  `RegularDenominationListScreen` (route `regular-denominations/{taglio}`, vedi sotto).
- **`RegularIssuesScreen`: tre schede, Denominations / Countries / All** (**etichetta mostrata "Values", non "Denominations"**: nel selettore delle Impostazioni, con tre segmenti, la parola lunga andava a capo anche misurando le larghezze; "Values" è la parola già usata da "Sort by value" e "Filter by value…"; il nome interno `RegularBrowseMode.DENOMINATIONS` e il resto di questo file restano "Denominations") (nell'ordine di Years / Countries / All di Commemorative: Denominations occupa il posto di Years, Countries resta in mezzo; il predefinito è comunque Countries; selettore segmentato come
  Years / Countries / All di Browse) **con la stessa barra flottante di ricerca + FILTER delle
  commemorative**, query/ordine/filtro propri di ogni scheda (`RegularGridPrefs`). Riusa i mattoni di
  `BrowseScreen.kt` (`BrowseCard`, `CardGrid`, `CardFooter`, `Progress.matches`, `FloatingSearchBar`,
  `FilterSheet`): resi `internal` invece di duplicarli (prima la griglia dei paesi aveva la sua
  `CountryCard` copiata). **Countries**: la griglia dei paesi, ricerca per nome, FILTER = ordine
  A→Z/Z→A + Collection (All/Incomplete/Complete). **Denominations**: 8 card (2 euro → 1 cent,
  "41 coins" + barra "x / 41 collected", `headlineMedium` come l'anno in Years) **con la moneta
  disegnata in alto a sinistra, nella posizione della bandiera delle card Countries** (stessa
  grammatica tra le due schede, stessa altezza, nessuna collisione con "50 cent" che occupa ~99 dp
  su 149 utili: provata e scartata la moneta nell'angolo in alto a destra, ~12 dp di margine e
  collisione a font grande, e la lista a righe, che avrebbe fatto di Denominations un secondo
  elenco; scelta dell'utente dopo mockup A/B/C). `DenominationCoin` (`ui/regular/DenominationCoin.kt`): **in scala
  reale** — la 2 euro riempie 44 dp, le altre sono proporzionali ai mm veri (25,75 / 24,25 / 23,25 /
  22,25 / 21,25 / 19,75 / 18,75 / 16,25: la 5 cent è più grande della 10 cent, la 50 cent più della
  1 euro) — nei metalli veri (rame, oro nordico, bimetallica: la 1 euro ha l'anello oro e il centro argento, la 2 euro l'anello argento e il centro oro; invertiti per errore nel primo mockup e nella Home, corretto), nello stesso
  riquadro alto 44 dp così i titoli restano allineati. Disegnata e non foto: un taglio ha 41 disegni
  nazionali e la foto di un solo paese direbbe "la 2 euro è questa"; niente rete né licenza.
  Colori duplicati da `RegularCoin` della Home (là attenuata al 62% perché è un ripiego): se i
  metalli cambiano, cambiarli in entrambi. `DenominationCoinTest`. Ricerca per valore
  ("2 euro", "euro", "cent": `matchesDenomination`, senza badare a maiuscole e spazi), FILTER = ordine
  Largest/Smallest first + Collection. **Niente "Years"**: l'anno non è un dato della moneta qui (una
  serie copre decenni, l'anno lo sceglie l'utente al salvataggio). Mockup approvato prima del codice.
  Senza risultati: messaggio senza il pulsante "Search all" di Commemorative (le griglie non portano
  alla scheda All, che si sceglie dal selettore).
  **Scheda All** (ottobre 2026): tutte le 328 righe in un elenco, per coerenza con Years / Countries /
  All di Commemorative. Era stata tolta ("non verrebbe mai utilizzata", niente dati d'uso) e rimessa
  su richiesta dell'utente: è l'unico posto dove cercare una moneta precisa ("Italy 2 euro") e dove
  il filtro Missing vale su tutto il catalogo. **Implementata senza mockup, per scelta dell'utente**
  ("schermata molto derivativa"): è `RegularCoinRow` (80 dp, stessa casella e stesso pannello) dentro
  la schermata di Countries/Denominations, con le righe di `denominationRows()` appiattite
  (`allDenominationRows`, `RegularAllRows.kt`). **Riga come `CoinRow`**: sopra, in `primary` Bold,
  "🇧🇪 Belgium · Series 2" (lo slot di "Paese · Anno"), titolo il taglio ("2 euro"); il paese NON è il
  titolo come nell'elenco di un taglio, perché qui il taglio è ciò che distingue le righe. La serie
  c'è sempre: la Francia ha lo stesso 5 cent in tre serie con la stessa foto, senza "Series N" le tre
  righe sembrerebbero un duplicato. Il tocco apre il dettaglio della serie guardata, la casella il
  pannello (stessa chiave e finestra dell'elenco di un taglio). **FILTER**: gruppo **Sort** con tre scelte INDIPENDENTI
  (`RegularAllOrder`: "Sort first by" Country / Value = quale asse ha la precedenza — era "Group by", rinominato il 2026-10-06 perché prometteva intestazioni di sezione che non ci sono: è solo un ordine; le intestazioni di anno/paese restano un lavoro a parte, con 584 righe sarebbero utili ma toccano lista, scorrimento e pager, "Country order" A → Z / Z → A,
  "Value order" Largest / Smallest first; predefinito paese A → Z, dentro la serie dal taglio più grande =
  il vecchio "Country A → Z"; era un solo "Sort by" a quattro voci che non permetteva di combinare paese e valore)
  + gruppo **Filter** con Collection All/Owned/Missing (`OwnershipFilter`) + "Owned quality" Standard/BU/Proof a scelta multipla come nelle liste di Commemorative (la riga passa se ha almeno un'annata, dentro la finestra della serie, in una delle qualità scelte: `ownsAnyQuality`). **"Owned quality" compare solo con Collection = Owned** e uscendone le qualità spuntate si azzerano (non restano attive di nascosto). **Ricerca**
  (`DenominationRow.matchesAllQuery`): paese, serie, periodo, taglio; "series 2" e un taglio per
  intero ("5 cent", "2euro") devono coincidere ESATTAMENTE con la riga (altrimenti "5" troverebbe i 50
  cent e "2" tutte le serie con un 2 negli anni), il resto è testo libero in paese/serie/periodo;
  "euro"/"cent" da soli trovano tutti i tagli di quel tipo. Segnaposto "Country, value, series…" (non
  misurato sul telefono). Il Default tab nelle Impostazioni ha il terzo segmento. **Miniature**:
  precaricate come in Commemorative (`PrefetchThumbnails` condiviso, `ThumbnailPrefetch.kt`), anche nell'elenco di un taglio;
  non verificato su telefono.
  `RegularAllRowsTest`.
- **`RegularDenominationListScreen`**: un taglio in tutti i paesi, una riga per serie (41),
  `denominationRows()` (`RegularDenominationRows.kt`, stesso conto di `regularProgress`: ordine per paese
  e poi per serie, riga posseduta = almeno un'annata nella finestra della serie). **Il PAESE è il
  titolo** ("🇧🇪 Belgium") e "Series 2 · 2008 – 2013" la riga piccola sopra: il taglio è già nella
  barra in alto, ripeterlo 41 volte sarebbe rumore. Riga = `RegularCoinRow` (estratta da
  `RegularIssueCountryScreen` in `RegularCoinRow.kt`, stessa 80 dp con stato/titolo parametrici): il
  tocco apre il dettaglio taglio della serie GUARDATA, la casella il `RegularCollectionSheet` (chiave
  = serie di origine, titolo = serie guardata, salvataggio con la finestra). Barra flottante:
  ricerca per paese/serie/periodo, FILTER Collection All/Owned/Missing (`OwnershipFilter`).
  `RegularDenominationRowsTest`.
- **Ogni card paese ha la barra di avanzamento**
  (stessa `CollectionProgressBar` dritta di Years/Countries nelle commemorative, sotto "N series":
  "x / y collected"): `regularProgress()` (`CollectionProgress.kt`, usata anche dalla Home, un
  solo conto) = **le RIGHE che l'utente vede nelle schermate delle serie**, cioè ogni taglio di
  ogni serie (`denominationsOf`: 8 per serie, anche i tagli invariati): **Francia 24 (3 × 8),
  Spagna 24, Vaticano 48** (6 serie, anche la 2026 senza foto, vedi sotto): 41 serie × 8 = **328**
  in tutto. **Errore corretto il 2026-10-04**: la prima
  versione contava i DISEGNI distinti (Francia 13, Spagna 18), un numero che non corrisponde a
  niente di visibile — "dovrebbero essere 24 monete" (utente). Una riga è posseduta se il taglio
  ha almeno un'annata in collezione DENTRO la finestra di quella serie (lo stesso criterio della
  casella spuntata nella riga): il 5 cent francese del 2005 riempie la riga della serie 1,
  quello del 2023 la riga della serie 2 (stessa moneta, finestre diverse). **Conta righe, non
  annate**: un taglio con tre annate nella stessa serie conta 1, e la Germania (8 righe) arriva
  al 100% con una moneta per taglio anche se mancano gli altri anni. Senza mockup: componente
  esistente nella stessa posizione di `CardFooter` in `BrowseScreen.kt`. `RegularProgressTest`.
- **`RegularIssueCountryScreen`**: con più di una serie per il paese (Belgio
  3, Monaco 3, Città del Vaticano 6...) un **selettore a chip** in cima
  (`FilterChip`, riuso dello stesso componente di `FilterSheet`) sceglie
  quale mostrare — con una sola serie il selettore non compare.
  - **Titoli: chip "Series 2 · 2022", intestazione "Series 2" con il periodo sotto**
    ("2022 – 2023", "2024 – today", solo "2005" se è un anno solo; `onSurfaceVariant`). **Dimensioni: titolo 20 sp Bold
    (come i titoli di sezione delle Impostazioni), periodo 15 sp, 8 dp prima della prima card**:
    a 16 / 13 sp il titolo non staccava dal testo delle card sotto (14 / 13 sp) ed era più piccolo del
    titolo della barra (suggerito dall'utente guardando il telefono). Titolo ancora verdigris (non
    c'è niente di cliccabile vicino), neutro se sembrasse un link. Variante A scelta dopo mockup, scartata B (tutto su una riga,
    "Series 2 · 2022 – 2023"): il periodo non deve competere col titolo. **Mai
    `intestazioneRaw`** ("2022 – second series 1 and 2 euro coins"...: lunga e disomogenea
    da paese a paese, l'utente non la voleva) e **mai l'anno da solo al posto del numero**:
    due serie del Vaticano partono entrambe nel 2005, i chip direbbero lo stesso. Il testo
    sta in `RegularIssueText.kt` (`seriesTitle`, `seriesChipLabel`, `seriesPeriod`), usato
    anche dal sottotitolo di `RegularCollectionSheet` ("Paese · Series N", senza anno).
    **Periodo** (`seriesPeriod`): inizio = primo anno delle immagini, mai prima del 2002
    (le monete datate 1999-2001 di Belgio/Francia/Monaco non fanno partire una serie), senza
    immagini (Vaticano 2026) l'anno dopo la fine della precedente; fine = la più tarda tra le
    fini esplicite se TUTTE le immagini ne hanno una, altrimenti la vigilia della serie
    successiva (Francia 1: 2021, perché i 5 cent non sono mai cambiati) o aperta.
    **Il numero "Series
    N" è la posizione (1-based) della serie nella lista del paese**, NON
    `RegularIssueSeries.numeroSerieIpotesi`: quel campo
    raggruppa varianti minori sotto lo stesso numero (Belgio: 2002 e 2008 sono
    entrambe "1" nel dataset, un ritocco minore non classificato come nuova
    serie) e con più di 2 serie per paese può ripetersi — bug reale, trovato
    sul telefono (due chip del Belgio dicevano entrambi "Series 1").
  - **La descrizione della serie è una card espandibile** ("ABOUT THIS SERIES"), non più un
    muro di testo che spingeva i tagli fuori schermo, **e sta SOTTO la lista dei tagli** (con
    la sua riga della fonte): qui si usano i tagli e le caselle, il testo è contesto da leggere
    una volta, come ABOUT THIS COIN nel dettaglio (ultima card dopo dati e collezione); in
    cima restano chip, titolo e periodo, e il primo taglio parte subito. Spostata su
    richiesta senza mockup (componente invariato). Costo noto: sta a ~650 dp dal titolo, sotto
    la piega. È `NotesCard` di Commemorative tale e
    quale (4 righe + "Show more", giustificato, animazione a mano — nessun disegno nuovo,
    per questo senza mockup), come ABOUT THIS COIN nel dettaglio taglio. **La fonte ("Series
    text: European Commission") sta FUORI dalla card**, sempre visibile anche a card chiusa:
    l'attribuzione non deve dipendere da un tocco. La pagina è una `Column` con
    `verticalScroll`, non più una `LazyColumn`: `NotesCard` scorre la pagina per centrarsi in
    espansione e vuole uno `ScrollState`; i tagli sono al massimo 8, la lista lazy non
    risparmiava nulla. Senza descrizione né card né riga della fonte.
    **Il testo è lo stesso per tutte le serie di un paese in 18 serie su 41** (Belgio 3, Francia 3,
    Spagna 3, Paesi Bassi 2, Vaticano 1-5, Monaco 1-2): la Commissione europea pubblica UN testo
    per paese e la pipeline lo copia su ogni serie (Francia: un unico testo che parla della prima
    serie, dei 1-2 euro 2022 e dei 10-20-50 cent 2024, sotto ogni chip). Non è un bug dell'app.
    Soluzione adottata (opzione A): `seriesTextLabel()` (`RegularIssueText.kt`) mette
    "ABOUT SERIES 1–3" al posto di "ABOUT THIS SERIES" quando lo stesso testo (già passato da
    `displayDescription()`) è identico su più serie del paese, con i numeri dei chip (elenco
    "1, 3" se non consecutive); il testo proprio (Lussemburgo 2, San Marino, Monaco 3, Vaticano 6)
    resta "ABOUT THIS SERIES". Scartate: la card unica sopra i chip (sbagliata per Monaco e Vaticano,
    dove la serie nuova ha un testo suo) e lo spezzare il testo per serie nella pipeline (solo la
    Francia ha paragrafi marcati; Belgio e Paesi Bassi sono narrativi, e sarebbe una correzione a
    mano contro la regola 5 della pipeline). **Il rimedio vero sarebbe una fonte con un testo per
    serie**: gli avvisi della Gazzetta ufficiale UE "New national sides of euro circulation
    coins" (uno per serie, riuso con attribuzione; già usati dalla pipeline per il Belgio 2014,
    ma i più recenti non sono indicizzati e il testo è breve) o Wikipedia (CC BY-SA, da
    attribuire). Lavoro nella pipeline (scraper + abbinamento avviso → serie per paese e anno +
    export che preferisce il testo per serie): nessuna modifica all'app, la card funziona già e
    `seriesTextLabel` diventa il ripiego per le serie senza fonte propria. Copertura non ancora
    verificata. (Numista non è più esclusa dall'asset pubblico dal 2026-10-05, § Come i dati arrivano nell'app.)
    **`displayDescription()`** (`RegularIssueText.kt`) toglie la frase standard "The coin's outer
    ring depicts the 12 stars of the European flag." (identica in 7 serie su 41, uguale su tutte
    le monete: non dice niente della serie) SOLO in visualizzazione, `descrizione` resta com'è nel
    dato e nel database (nessun ripopolamento). **Toglie anche la frase GENERICA sul bordo del
    2 euro** ("The edge lettering on the €2 coin is ‘2**’ repeated six times, alternately upright
    and inverted", in tutte le sue 8 forme di scrittura: 22 serie su 41, uguale su quasi tutte le
    monete). **Le iscrizioni SPECIFICHE restano** (12 serie: Germania, Grecia, Finlandia, Lettonia,
    Lituania, Bulgaria, Paesi Bassi, Portogallo, Slovenia, Estonia, Austria — "EINIGKEIT UND RECHT
    UND FREIHEIT", "SUOMI FINLAND ***"...): sono parte dell'identità della moneta, e il bordo è già
    nel testo di 30 dei 2 euro nel dettaglio taglio. Nel testo di San Marino 2017 la frase è la
    coda di una frase più lunga dopo un punto e virgola: si toglie lasciando il punto. Non tocca
    la frase belga "not in the outer
    ring" né quella spagnola sulle dodici stelle (un'altra frase, e descrive una scelta di
    disegno): da aggiungere alla stessa regola se l'utente le vuole togliere.
  - **Ogni serie mostra sempre tutti e 8 i tagli** (`denominationsOf`,
    `RegularSeriesDenominations.kt`), anche quelli che non ha cambiato: la Francia 2022 cambia
    solo 1 e 2 euro e la 2024 solo 10-20-50 cent, la Spagna 2015 solo 1 e 2 euro, ma nel
    portafoglio c'è sempre il set intero (prima la serie francese 2 aveva 2 monete). Un taglio
    non cambiato viene dalla serie precedente più recente in cui è ancora in circolazione
    (`annoFine` vuoto o ≥ inizio della serie guardata: i 10-20-50 cent francesi a "seminatore"
    finiscono nel 2023, quindi sono nella serie 2022 ma non nella 2024). Il dato c'era già:
    `annoInizio`/`annoFine` per immagine.
    **Non è una copia ma la STESSA moneta**: stessa chiave di collezione (`SeriesDenomination.series`
    = serie di ORIGINE), stesso conteggio in Home, nessun dato doppio. **Ogni serie è però una
    FINESTRA di tempo** — dal suo primo anno alla vigilia della serie successiva che ha
    immagini — e tutto si ferma ai suoi confini: il 5 cent francese è 1999-2021 nella serie 1,
    2022-2023 nella 2 e 2024-oggi nella 3 (errore reale trovato sul telefono: nella serie 2
    partiva dal 1999, con tirature e zecche della serie 1). `denominationsOf` restituisce
    l'immagine GIÀ ritagliata alla finestra (`annoInizio`/`annoFine`, `tirature` e
    `zecchePerAnno` filtrate), quindi elenco anni del pannello, somme e tabella "by year" sono
    giusti senza che ogni schermata ci pensi. Un'immagine ancora aperta finisce alla vigilia della
    serie successiva; una con fine esplicita la mantiene (il Vaticano 2005 appartiene sia alla
    serie 1 sia alla 2: tagliarla sull'inizio della successiva perderebbe l'annata).
    **Salvataggio** (`RegularIssueRepository.saveCollection(..., window)`): il pannello vede solo gli
    anni della finestra, quindi sostituisce solo quelli e lascia le voci fuori finestra (le annate
    della serie 1 non spariscono salvando dalla 2). Il dettaglio taglio riceve la serie GUARDATA
    nella rotta (`ordine`) e risolve da lì serie di origine e finestra
    (`RegularDenominationDetailViewModel`). **Una serie senza immagini proprie NON eredita
    niente** (Vaticano 2026: i tagli cambiano tutti) **ma ha comunque le sue 8 righe**
    (`REGULAR_DENOMINATIONS`, immagine "vuota" con `urlImmagineFonte = null` e `annoInizio` = inizio
    della serie da `seriesPeriod`, 2026): segnaposto `€` come ogni taglio senza foto, spuntabili. Senza
    righe le 8 monete non si sarebbero potute registrare e la barra del paese non sarebbe mai
    arrivata al 100% (l'utente contava 328 = 41 × 8; il primo conto dava 320 perché la serie 6 non
    aveva righe). Il dettaglio di questi tagli non ha foto, tirature, testo né riga della licenza.
    Non segnalato in UI che un taglio viene da un'altra serie (la riga è identica alle altre):
    un "Series 1 design" nella riga di stato è un'opzione proposta, non decisa.
  Sotto, **una card per taglio** (non più una riga
  orizzontale scorrevole dentro un'unica card di serie — cambiato su
  richiesta, riusa la struttura di `CoinRow` in `CoinListScreen.kt`: card ad
  altezza fissa 80 dp, miniatura 64 dp/segnaposto lilla 62 dp, testo,
  casella a destra), **ordinate per valore DECRESCENTE, dal 2 euro all'1 cent** (`RegularIssueCountryViewModel`,
  `denominationRank`; richiesta dell'utente il 2026-10-04: "di solito i tagli più grossi hanno disegni più
  belli", e non c'è un FILTER con l'ordinamento in questa schermata; coerente con la scheda Denominations,
  che parte da "Largest first"; prima 1 cent → 2 euro): `RegularIssueSeries.immagini` non è
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
  serie 2026 non ancora fotografata dalla fonte) la lista era VUOTA (verificato in
  emulatore/telefono); dal 2026-10-04 mostra le 8 righe con il segnaposto, vedi sotto.
- **`RegularDenominationDetailScreen`** (route `regular-issues/{paese}/{ordine}/{taglio}`,
  `ordineCronologico` e non `seriesKey` diretto — eviterebbe il separatore `|` in un argomento di
  rotta): il tocco sulla riga di un taglio (non più solo la casella) apre un dettaglio a schermo
  intero, stessa struttura di `CoinDetailScreen` — Hero con foto grande (è lo "zoom": nessun
  dialog di ingrandimento separato, come nel dettaglio Commemorative, che non ce l'ha nemmeno lui,
  vedi § Dettaglio moneta più sotto), MINTAGES+DETAILS, COLLECTION, ABOUT THIS COIN, crediti. **La
  descrizione della SERIE (ABOUT THIS SERIES) NON è in questa schermata**: tolta su richiesta
  dell'utente (2026-10-03), resta solo nell'intestazione di `RegularIssueCountryScreen`; ripeterla
  qui duplicava il testo con ABOUT THIS COIN. **DETAILS (Mint/Engraver/Designer) viene da Numista per (serie, taglio)**
  (`RegularIssueImage.zeccaFisicaRaw`/`incisoreRaw`/`disegnatoreRaw`, formattate da
  `displayMint()`/`displayEngraver()`/`displayDesigner()` in `CoinCredits.kt`, zecca come
  nome del paese della zecca come nelle commemorative, § Dettaglio moneta/DETAILS): "—" dove Numista non ha il campo
  (il disegnatore manca su 348 type su 381; l'incisore su 31) o non ha il type (Bulgaria). Prima
  era sempre vuota, ora no. **MINTAGES è dinamica** (§ omonima più sotto) e ora piena. **ABOUT THIS
  COIN** (`RegularIssueImage.descrizione`): descrizione del disegno nazionale di QUEL taglio,
  dopo COLLECTION, **riusa `NotesCard` di Commemorative tale e quale**
  (esportata, etichetta parametrica: 4 righe + "Show more", giustificato, animazione a mano —
  nessun disegno nuovo, per questo nessun mockup) e compare solo se il testo esiste. **Finisce
  sempre con un punto** (`displayCoinDescription()`, `RegularIssueText.kt`): 124 testi Numista su
  303 nell'export completo si fermano senza ("…the twelve stars of Europe"; scritti da utenti) e
  uno BCE finisce con una parentesi. Si aggiunge "." dove manca ".", "!" o "?", anche se seguito da
  una chiusura di virgolette/parentesi; solo in visualizzazione, come le altre pulizie. **I
  paragrafi si uniscono in un blocco solo** (`LINE_BREAKS`: ogni a-capo/riga vuota → uno spazio):
  163 testi su 295 della BCE sono scritti a paragrafi separati da `\n\n` e nella card a 4 righe
  la quarta cadeva spesso sulla riga vuota, con l'ellissi "…" da sola e uno spazio morto (visto sul
  1 cent belga); le note commemorative non hanno mai a-capo. Verificato che non ci sono elenchi
  né a-capo nei testi di serie. Le
  commemorative hanno 2 note su 584 senza punto (Portogallo 2021 finisce con un'iscrizione tra
  virgolette, Malta 2022 si ferma a "…are the inscriptions": troncata dalla fonte): non toccate.
  Il testo è
  Numista (verbatim, inglese, scritto da utenti: più ricco della BCE) o, dove Numista non ha il
  type, la pagina BCE del taglio (`descrizioneFonte` = "numista"/"ecb"; il testo BCE è uno per
  paese e a volte descrive più serie insieme). **Stesso disegno = stesso testo** (2026-10-13, richiesta del
  proprietario, `$SameDesign` in `scripts/export-regular-issues.ps1`): Numista ha un type per taglio e i testi li
  scrivono gli utenti, quindi lo stesso ritratto arrivava con descrizioni diverse (Belgio serie 3: 340, 235, 114 e 72
  caratteri). Una tabella a mano di gruppi (serie + tagli + "donor") copia nei tagli dello stesso disegno il testo
  del più ricco di dettaglio, che non citi cose di un solo taglio (l'"anello esterno" dei bimetallici sui
  centesimi); 86 tagli su 303, solo `descrizione` e `fonte_url` cambiano (verificato confrontando i JSON campo per
  campo). **I gruppi sono ricavati dalle foto (somiglianza dei contorni a 48 px, soglia 0.70) e rivisti a mano, non
  vanno estesi a occhio**: la somiglianza da sola sbagliava (Portogallo ha tre sigilli diversi, Croazia mappa e
  Tesla, San Marino un soggetto per moneta, Spagna Santiago e Cervantes) e quei casi non sono nella tabella; un testo
  copiato su un disegno diverso scrive il falso. Tre cose da sapere: le foto con lo stesso contenuto (hash) erano
  solo 7 gruppi e già coerenti, quindi "stessa immagine" è il disegno e non il file; `numista_id`, tirature, zecca e
  incisore restano del taglio (il link "Data: Numista N#…" dei crediti punta al type del taglio, non a quello del
  testo; l'app non mostra `fonte_url`); i testi BCE (per taglio, "The 5 cent coin features…") non si copiano.
  `RegularIssuesAssetTest.coinsWithTheSameDesignShareTheSameText`. (Lussemburgo 2026 2 euro non aveva testo finché Numista non ha avuto il type, N#585823, ottobre 2026: né
  Numista né BCE, che descriverebbe la serie precedente: la card mancava). `RegularDenominationDetailViewModel` rilegge reattivamente da `RegularIssueRepository.series`/
  `collectionItems` (come `RegularIssueCountryViewModel`), non un fetch singolo come
  `CoinDetailViewModel.getById`: qui non serve un id stabile per riga, il taglio è già una chiave
  dentro la serie. Building block riusati da `CoinDetailScreen.kt` (esportati, non più `private`):
  `DetailCard`, `SectionLabel`, `OwnedBadge`, `FooterLine`, `sansTitleMedium`, `ValueLabel`,
  `NO_VALUE`; `DetailsSection` non prende più un `Coin` ma tre stringhe già pronte (mint, engraver,
  designer), per essere chiamabile anche con tre `NO_VALUE` fissi. Nuova
  `RegularIssueImage.displayImageLicense()` in `ImageLicenseNames.kt` (stessa mappa
  italiano→inglese di `Coin.displayImageLicense()`). **Crediti nel formato comune a tutti i
  dettagli (`SourceCredits`, `ui/detail/`)**: una riga centrata `Source: ECB ↗  Data: Numista
  N#… ↗` (voci distanziate, senza "·": andando a capo restava a inizio riga; `FlowRow` perché con
  font ingrandito non sta su una riga) e sotto `License: … · Credit: …`. Testo del taglio e
  immagine con la stessa fonte (BCE) → una voce "Source"; se differiscono (testo BCE di
  ripiego + immagine BCL del Lussemburgo) due voci "Text"/"Image". "Data: Numista N#<id>"
  (link alla pagina del type) compare ogni volta che esiste un type Numista ed è il minimo che
  chiedono i Termini API (§4: N# visibile e attribuzione): non va tolto. Nomi leggibili in
  `displaySourceName()` (`SourceNames.kt`: `ec_national_sides` → "European Commission"), mai
  lo slug grezzo. **Il testo introduttivo della serie viene dalla Commissione europea** (pagina
  "National sides of euro coins", NON dalla BCE) ed è accreditato dove compare, sotto la
  descrizione in `RegularIssueCountryScreen` ("Series text: European Commission"); non nel
  dettaglio del taglio, dove non c'è più. La BCE ha un testo di paese equivalente
  (`contesto_paese`, 19 paesi su 25) ma uno solo per tutte le serie: tenuta EC su richiesta
  dell'utente (2026-10-03), passare alla BCE è un lavoro da fare nella pipeline.
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
    **Formato dei numeri** (2026-10-13, `formatCardMintage`): con almeno due anni (cioè quando "View by year"
    esiste) le tre colonne sono SEMPRE compatte a UNA cifra decimale, "14.2 B", "10.1 M", "19.0 k" (la stessa
    `formatCompactMintage` delle tabelle per zecca); con un anno solo i numeri sono esatti ("12,345,678"), perché
    non c'è nessun altro posto dove leggerli e sono tutti dello stesso ordine di grandezza. Motivo: prima le somme
    erano a 3 cifre significative con "≈" (`formatApproxTotal`, tolta) e un anno solo restava esatto, quindi una
    card poteva mostrare "≈ 14.2 B · ≈ 10.1 M · 19,000" ("poco armonioso", segnalato dal proprietario). **Scelte
    dell'utente**: una cifra decimale (non tre significative: "7.9 B" invece di "7.87 B", per coerenza con le
    zecche) e **nessuna riga Total nella tabella "by year"** (proposta e scartata): la somma esatta di una
    qualità non è scritta da nessuna parte, si legge solo arrotondata nella card. Le commemorative non cambiano
    (una tiratura per qualità, al massimo 8 cifre, nessuna tabella dove leggere l'esatto).
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
  - **Dati reali dal 2026-10-02** (Numista, `numista_divisional.jsonl` della pipeline: 381 type,
    3954 righe anno; 280 tagli su 303 con almeno una tiratura). Il file è **per type Numista**,
    non per serie, quindi `scripts/export-regular-issues.ps1` li abbina alle serie **per anni**:
    ogni serie ha un intervallo [inizio, inizio della successiva − 1] (inizio dall'anno
    nell'intestazione, o dalla tabella `$StartOverrides` per Monaco/Vaticano/Lussemburgo, dove
    l'intestazione non lo dice) e **ogni type va per intero alla serie con cui ha più anni in
    comune** — non riga per riga, perché il 2005 vaticano esiste come Giovanni Paolo II e come
    Sede Vacante in due type distinti. Per ogni taglio partecipano solo le serie che hanno
    un'immagine di quel taglio (la Francia 2022 cambia solo 1 e 2 euro) o che non ne hanno
    nessuna (Vaticano 2026, che "assorbe" i suoi 8 type invece di lasciarli alla serie
    precedente; poi li scarta perché non ha foto). Descrizione/zecca/incisore/disegnatore sono
    quelli del type con più anni nella serie.
  - **`tiratura` è `Long`**: la Germania 2002 ha 4 miliardi di 1 cent standard e anche le somme
    su tutti gli anni sfondano `Int` (2,1 miliardi) — la prima versione con `Int` avrebbe
    sommato in overflow in silenzio. (Cambiare `Int`→`Long` sul blob JSON Room è sicuro: i
    vecchi numeri si leggono uguale.)
  - **Didascalia "N of M years"**: "all years" solo se quella qualità ha un dato in ogni anno in
    cui il taglio ne ha uno per qualunque qualità. Numista spesso non ha lo standard di un anno,
    e BU/Proof esistono in pochi anni: "all years" su una somma incompleta sarebbe un totale
    falso scritto come esatto. Non distinguibile se il buco è "non coniata" o "dato mancante",
    quindi la didascalia dichiara solo su quanti anni poggia il numero.
  - **Solo Standard/BU/Proof**, come le commemorative: la categoria `tiratura_altro` di Numista
    (809 righe su 3954, quasi tutte "In sets"/lotti/varianti con mintmark) non è mostrata —
    stessa scelta e stesso motivo di § dataset. Conseguenza visibile: a volte la circolazione di
    un anno finisce in `altro` (es. Italia 10 cent 2002, 1,14 miliardi con nota "Type A: small
    signature"), e in quella riga Standard compare "—". Non corretto a mano (regola 5 della
    pipeline).
  - **Numeri a 10+ cifre** (verificato sul telefono, Germania 1 cent): la somma Standard arriva
    a 12.475.760.000 e in una colonna da un terzo andava a capo a metà cifra. `ValueLabel(...,
    shrinkToFit = true)` la tiene su una riga riducendo il corpo oltre 12 caratteri (le colonne
    restano di larghezza uguale, vedi § Dettaglio moneta); nel pannello per anno Standard pesa
    1.5 contro 1 (`columnWeight`), BU/Proof per anno non superano il milione. Esempio di "—"
    che è il dato e non un bug: Vaticano serie 2 (Sede Vacante) 1 cent, 60.000 pezzi tutti in
    `altro` ("In Sets only").
  - **Zecca per anno** (dal 2026-10-04; `RegularIssueImage.zecchePerAnno`, `RegularIssueYearMint`:
    anno + zecche certe + zecche probabili; il file passa da 670 a ~855 KB). Viene dalla pipeline
    (`zecche_anno`/`zecche_anno_probabili` per anno: lettera, marchio, commento, tabelle ufficiali
    di banche centrali, regole nazionali; livello "probabile" da Wikipedia/inferenza dichiarata;
    3731 combinazioni certe, 126 probabili, 63 senza zecca, tutte Lettonia) ed è nello stesso
    stesso export delle tirature (con `-ExcludeNumista` non c'è e la tabella resta com'era).
    Un anno senza voce = zecca NON nota ("not known"), mai "nessuna zecca" (regola 7 della
    pipeline: il probabile non va mai presentato come certo). **UI**: nella tabella "by year"
    un'etichetta pillola `Mint · Finland` sopra il primo anno di ogni periodo con la stessa
    zecca (`MintPeriodLabel`; layout B scelto dall'utente dopo mockup, scartata la zecca in
    seconda riga sotto l'anno); probabile in corsivo con "?" e nota in fondo ("? Probable: from
    Wikipedia or inferred from official data, not confirmed."); non noto spento con solo il bordo.
    **Compare solo se la zecca varia** (`yearMintLabels()`, `RegularYearMints.kt`): più di un
    paese noto negli anni, o almeno un anno non noto accanto a uno noto; con zecca unica
    (Italia, Austria, Germania con le sue 5 zecche che per paese sono una) nessuna etichetta —
    71 type su 381 (Andorra, Belgio, Cipro, Estonia, Grecia, Lussemburgo, Malta, Slovenia, 2 della
    Lettonia). Il dettaglio per lettera (`per_zecca`) è usato solo per l'anno diviso qui sotto
    (non per le 5 zecche tedesche). Il blocco DETAILS resta la lista delle zecche del type Numista
    (`displayMint`), con l'etichetta "Mints" al plurale se sono più paesi: può non coincidere con
    la tabella per anno (es. i type irlandesi elencano 6 zecche ma la regola nazionale della
    pipeline dà la zecca irlandese certa per tutti gli anni: nessuna etichetta, DETAILS sì).
  - **Anno diviso tra zecche** (`per_zecca` della pipeline → `RegularIssueYearMint.perZecca`,
    `RegularIssueMintShare`; logica `yearMintParts()` in `RegularYearMints.kt`). Oggi **solo
    Grecia 2002** (8 tagli): zecca nazionale più Parigi (1-10, 50 cent), Madrid (20 cent) o
    Finlandia (1-2 euro) per i pezzi aggiuntivi, ognuna con le sue tirature. Nella tabella "by
    year" la riga dell'anno resta il TOTALE (coerente con card compatta e somme) e sotto ci sono
    le parti, una riga per paese, più piccole/spente/rientrate su fondo leggermente diverso
    (variante X scelta dall'utente dopo mockup; scartata Y, anno come titolo senza totale).
    **Regola: si divide solo con almeno due PAESI diversi, ognuno con un dato** — le 5 zecche
    tedesche sono un solo paese, le due voci di Italia 2002/2026 e San Marino 2026 sono
    entrambe Roma, "FI"/"Fi" della Finlandia 2022 sono la stessa zecca, i gruppi di Malta e
    Slovenia 2007 senza zecca nota si ignorano: non direbbero niente. Lo script di export
    applica un filtro grossolano (riconosce solo le zecche tedesche) e scrive `per_zecca` solo
    dove passa; l'app riapplica la regola completa con `MintNames.kt`. Nota sotto la tabella
    sugli anni divisi: i BU senza lettera sono contati con la zecca nazionale e comprendono
    pezzi dei set (la pipeline segnala 5.000 BU "Dutch Mint - Set" per taglio in 2002).
  - **Buchi noti**: Bulgaria (nessun type Numista: tirature vuote, testo BCE; nel dettaglio sotto i tre trattini di MINTAGES compare la stessa `InfoNote` con "Mintage data isn't available yet.", per ogni taglio con `tirature` vuote), Francia 2022 2 euro
    (Lussemburgo 2026 2 euro ora presente, N#585823), Monaco serie 3 dei centesimi (Numista
    tiene i cent 2025 nel type della serie 2 e dice che cambiano solo 1/2 euro, la BCE dice tutti
    gli 8: conflitto della pipeline non risolto — le tirature 2025 dei cent restano alla serie 2,
    il testo della serie 3 è quello BCE). Un type può includere anni con dati pre-euro
    (Belgio/Finlandia/Spagna dal 1999): sono monete datate, non vengono tagliati.
- **Scheda Home**: "**N** coins · **M** countries · **P** series" (`RegularIssuesStatsLine` in
  `HomeScreen.kt`, non il generico `StatsLine` di Commemorative: lì il terzo valore è un
  intervallo di anni senza etichetta, qui serve la parola "series"). "Coins" = le righe dei tagli
  di tutte le serie, **8 per serie anche per i tagli rimasti invariati** (`regularProgress().total`:
  328 = 41 serie × 8: le 303 immagini, 17 righe ereditate da Francia e Spagna e le 8 del Vaticano
  2026 senza foto; era 303 = `RegularIssueImage`, cioè i disegni distinti, cambiato il 2026-10-04
  perché la barra del paese deve dire 24 per la Francia e la Spagna, come le righe che si vedono),
  non le 4 monete della fascia. Fascia con foto reali (BCE, sopra) e rotazione giornaliera come Commemorative (§
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
  gestione di `possibileIncongruenza` in UI
  (oggi sempre `false` nel dataset). La catena di ripiego/precaricamento della fascia Home è stata
  allineata a Commemorative (vedi sopra, § Impostazioni "Rotate home coins").

### Collezione su Regular Issues

A differenza delle commemorative, dove `Coin.stableKey` (fonte + anno + paese + tema) identifica
la moneta esatta perché l'anno è nel dataset, qui il dataset descrive solo il **disegno** di un
taglio per una serie — non esiste un anno: lo stesso disegno viene coniato per anni, spesso
decenni. "Moneta posseduta" qui è quindi (serie, taglio, **anno scelto dall'utente da una lista**[, qualità]),
e un utente può avere più annate dello stesso taglio (es. Belgio serie 2, 1 euro, sia 2018 sia
2020) — non è un riuso di `CollectionItem`/`CollectionSheet`, serve un modello diverso.

- **`RegularIssueSeries.stableKey`** (`RegularIssueKey.kt`): `paese` + `ordineCronologico`, stesso
  motivo di `Coin.stableKey` (`RegularIssueSeries.id` si rigenera a ogni reseed). `ordineCronologico`
  e non `numeroSerieIpotesi` perché quest'ultimo NON è univoco per paese (vedi sopra, Belgio).
- **Tabella separata `regular_collection_items`** (`RegularCollectionItem`, chiave primaria
  composita `seriesKey`+`taglio`+`anno`+`quality`+`variety` (quest'ultima dalla v9, `MIGRATION_6_7` la crea, `MIGRATION_8_9` la estende): permette più
  annate E più qualità sullo stesso taglio, mai duplicati sull'identico (anno, qualità). `paese` è
  una copia di quando la voce è stata salvata, stesso scopo "riconoscere un orfano" di
  `CollectionItem.anno/paese/tema`. `RegularIssueRepository.saveCollection(seriesKey, taglio,
  paese, entries)` sostituisce in blocco le voci **di quel taglio** (`RegularCollectionDao
  .replaceForDenomination`, transazione), conserva `addedAt` delle voci esistenti — stesso pattern
  di `CoinRepository.saveCollection`/`CollectionDao.replaceForCoin`.
- **Un taglio conta come "posseduto"** (casella piena nella card, `regularProgress`, barra della
  Home e di ogni paese) se ha almeno un'annata in una qualsiasi qualità DENTRO la finestra della
  serie guardata — **conta le righe, non le annate**: un utente con 2 annate dello stesso taglio
  nella stessa serie fa avanzare la barra di 1, non di 2. Il totale ("N coins" in Home) sono le
  righe di tutte le serie, non i disegni distinti (vedi § Regular Issues, Scheda Home).
- **Punto di ingresso**: una casella a destra di ogni card taglio in `RegularIssueCountryScreen`
  (`CollectionBox`, duplicata da `CoinListScreen.kt` — stesso approccio di `BrowseCard`, non
  condivisa), stessa spunta verdigris di Commemorative. Sopra il titolo del taglio, una riga di
  stato riusa la posizione di "Paese · Anno" di `CoinRow`: "Not owned" (neutro) o "N years owned"
  (`primary` Bold) — dà un'informazione utile invece di ripetere il paese, già nella barra in alto.
  Anche la card COLLECTION di `RegularDenominationDetailScreen` (§ omonima più sopra) apre lo
  stesso pannello con "Add to collection"/"Edit collection": due punti di ingresso allo stesso
  pannello, come nelle commemorative. **Card COLLECTION con più anni** (ottobre 2026, scelta
  dell'utente dopo quattro mockup): la lista piatta finitura × anno × varietà arrivava a 30 righe
  (~1.700 dp) con 10 anni e 3 finiture. Con UN solo anno (o varietà) è identica alla card delle
  commemorative (righe per finitura con data e prezzo, anno nell'etichetta "Standard · 2008"); con
  PIÙ anni in cima ci sono gli anni come `FilterChip` (stile del selettore dell'anno e del FILTER,
  scelto in lilla; "2002 EFS" per la varietà) in ordine CRONOLOGICO dal più vecchio (come la griglia
  degli anni del pannello; scartati il più recente per primo e l'ordine di inserimento, che darebbe
  una fila casuale), e all'apertura è scelto l'ULTIMO AGGIUNTO (`addedAt` più recente, a parità
  l'anno più recente; si ricalcola solo se cambia l'insieme degli anni) e sotto le righe solo per l'anno
  scelto: l'altezza non cresce con gli anni, date e prezzi restano visibili, `animateContentSize`
  smorza il cambio. Oltre i primi 8 chip c'è "+N" / "Less" (numero FISSO, `YearChipsCollapsed`; un
  anno scelto oltre i primi resta visibile). **Non usare `FlowRowOverflow.expandOrCollapseIndicator`**:
  provato, legge `shownItemCount` nella composizione e lancia `IllegalStateException` a ogni apertura
  del dettaglio con più anni (crash sul telefono, 2026-10-05); servirebbe `ContextualFlowRow`. L'altezza delle righe (44 o 58 dp
  con le date) è la stessa per tutti gli anni, calcolata su TUTTE le voci. "Edit collection" apre il
  pannello sull'anno scelto (`RegularCollectionSheet(initialYear)`). Scartati: tabella anni × finiture
  (celle miste di date, pallini e trattini: "un mischione"), tessere uniformi, una riga per anno che si
  apre, lista tipografica, riepilogo con elenco a parte. Non verificato con tanti anni sul telefono.
- **`RegularCollectionSheet`** (`ui/components/RegularCollectionSheet.kt`): **il pannello delle
  commemorative più UN selettore dell'anno** (riscritto il 2026-10-04 su richiesta: prima era una
  lista di righe anno-più-prezzo da digitare, con "Add year" e rimozione, "troppo diversa" dall'altro
  pannello). Le tre card Standard/BU/Proof con il prezzo a destra sono la STESSA `FinishCard`
  (esportata da `CollectionSheet.kt`, non una copia: il campo prezzo squadrato a 12 dp è quello
  vero, nel mockup avevo disegnato per sbaglio una pillola tonda). In cima "Year" e una STRISCIA di
  chip degli anni (`YearStrip`, vedi sotto).
  - **L'anno si sceglie da una lista, non si scrive** (`regularYearOptions` in
    `data/RegularYearOptions.kt`): dal primo anno del taglio nella serie fino all'ultimo, o all'anno
    corrente se la serie è aperta. Niente anni impossibili né doppi (prima due righe con lo stesso
    anno si sovrascrivevano in silenzio). Include anche gli anni GIÀ in collezione fuori
    intervallo, per non perdere voci vecchie. L'intervallo viene da `RegularIssueImage.annoInizio`/
    `annoFine` (scritti da `export-regular-issues.ps1`: intestazioni delle serie + tabella del primo
    anno datato per paese `$FirstDatedYear` + `$EndOverrides` per il Vaticano 2005): **non sono
    dati Numista**, restano nell'asset pubblico. Nella lista c'è anche un anno in cui quel taglio
    non fu coniato (es. alcuni cent): il prezzo di non dipendere da Numista.
  - **Default = il primo anno della serie** (scelta dell'utente), ma non prima del 2002: per Belgio,
    Finlandia, Francia, Paesi Bassi, Spagna (e Monaco, 2001) l'elenco parte da 1999/2001 (monete
    datate prima dell'ingresso in circolazione), il default no. Il chip è scelto ma NESSUNA finitura è spuntata (vedi sotto).
    Con qualcosa già in
    collezione il pannello si apre sulla prima voce posseduta.
    **Nessuna finitura preselezionata** (2026-10-09; prima c'era Standard già spuntata sull'anno di partenza, tolta). Storia
    in quattro tempi, tutti con lo stesso nodo: la spunta preselezionata è un'ipotesi sull'anno (il primo della serie,
    spesso sbagliato) e non si distingue da una scelta dell'utente, quindi chi voleva "2002 e 2003" e chi voleva "solo
    il 2004" facevano lo stesso gesto: (1) restava sull'anno di partenza (fino all'8 ottobre: si temeva che "Save" la
    scrivesse di nascosto); (2) la si fece SPOSTARE sull'anno scelto (per registrare Grecia 2002 e 2002 EFS serviva
    deselezionare e riselezionare); (3) la si fece SPARIRE al primo cambio di anno (per tenere il 2002 e aggiungere il 2003
    bisognava tornare indietro); (4) lasciata ferma e vera (per registrare solo il 2004 si doveva togliere il 2002). **Ora**:
    il pannello si apre sul primo anno ma con tutto vuoto; registrare un anno costa tre tocchi (apri, spunta, Save), più
    anni si spuntano uno per uno senza toccare niente due volte, e il puntino verde sul chip mostra quali anni hanno
    qualcosa. Scelta consigliata e approvata dal proprietario. Scartata: una spunta "suggerita" con aspetto diverso
    (tratteggiata, sparisce se si spunta un altro anno senza averla toccata): risolverebbe entrambi i casi ma aggiunge
    uno stato visivo nuovo per risparmiare un tocco a chi registra l'anno iniziale. **Preselezione tolta anche
    nelle commemorative (2026-10-13, richiesta del proprietario "per uniformità")**: prima lì "Standard" era già
    spuntata ("la moneta è una sola, nessuna ambiguità"), ma chi compra una commemorativa solo in BU o Proof, come
    capita con i set, registrava in più una Standard che non ha senza accorgersene. Il costo è un tocco in più
    (apri, spunta, Save); chi vuole solo "ce l'ho" usa Quick add. **"Save" spento** in entrambi i pannelli finché
    la moneta non è in collezione e nessuna finitura è spuntata (`canSaveCollection`, `CollectionSave.kt`); con
    qualcosa di salvato resta acceso, perché tutto deselezionato + Save toglie (c'è anche il tasto Remove). Sparita
    con la preselezione la logica "morbida" delle zecche (`isSoftDefaultActive`, `noMintTouched`), e con lei la
    classe di bug del doppione "Standard" + "Standard · D".
  - **Bozza per anno + Save**: le spunte e i prezzi sono in mappe chiave (anno, varietà, finitura);
    cambiare anno non li perde, "Save" scrive tutti gli anni insieme, chiudere senza salvare non
    cambia nulla.
  - **`YearStrip`: striscia di chip scorrevole nel pannello** (ottobre 2026, scelta dopo quattro
    mockup; sostituisce `YearGridDialog`, la finestra con la griglia a 5 colonne, che era una finestra
    sopra un pannello, un tocco in più e celle piccole). Stessi `FilterChip` della card COLLECTION del
    dettaglio (rettangoli con angoli morbidi, scelto in lilla), un tocco per cambiare anno, il
    pannello non cresce; puntino verde DAVANTI all'anno se ha finiture spuntate (la bozza non salvata
    conta); si centra sull'anno scelto (`LazyRow` + `animateScrollToItem`) e attraversa il margine
    laterale del pannello per far capire che scorre. Scartate: decenni + griglia nel pannello (+70 dp),
    stepper con frecce, elenco verticale con le finiture. Prima ancora, scartati il menu a tendina
    standard di Material (lista lunga, grigio fuori palette) e una riga di chip senza bordi.
  - **La varietà EFS è una voce a parte dell'elenco**, non un controllo in più: il 2002 greco ha
    due chip, "2002" e "2002 EFS" subito dopo (due monete, l'utente può averle entrambe). Così il
    pannello resta tre card per finitura anche lì.
  - **Il suggerimento sull'EFS** (2026-10-11, mockup approvato, `VarietyInfo.kt`): chi non conosce il collezionismo
    greco non sa cosa sia "2002 EFS". **Compare SOLO con la varietà EFS scelta** (`EfsHint`, una riga sotto i chip
    dell'anno nel pannello e nella card COLLECTION del dettaglio): "Look for the letter **S** in a star · **What's EFS?**",
    con la lettera giusta per il taglio che si ha davanti (`greekEfsLetterFor`), senza aprire niente; il tocco sulla riga
    apre `EfsInfoDialog`. **"What's EFS?" è nel colore dei link** perché la riga con sola icona e testo grigio non faceva
    capire che si tocca (domanda dell'utente, 2026-10-11; variante C dopo mockup, scartati "Learn more" che andava a capo
    e la pillola con freccia, che accanto ai chip sembra una scelta d'anno). **Prima era una "i" accanto a "Year", visibile su ogni anno e con troppo testo** (paragrafo,
    frase personalizzata, tabella, nota: corretto su segnalazione dell'utente). Il dialog ora è una frase con **E**, **F**,
    **S** in grassetto ("Greek 2002 coins minted abroad carry a letter in a star: E Spain, F France, S Suomi (Finland).
    Athens coins have none.") e la tabella raggruppata per lettera (`greekEfsLetterGroups()`, derivata dalla stessa
    tabella `GREEK_EFS_LETTER` che offre la varietà, così non diverge; riga del taglio corrente evidenziata). L'altezza
    della riga nel pannello si anima per non far saltare le card sotto. **Non dice su quale stella cade la lettera**: il
    dato non c'è e non si inventa (servirebbe una fonte verificata e foto). `EfsLetterGroupsTest`. Scartata l'icona
    dentro il chip: il suo tocco serve a sceglierlo.
  - **Card spuntata: solo bordo (2.5 dp pieno)**, mai fondo lilla: viene da `FinishCard`, quindi
    non può più restare indietro rispetto alle commemorative come era successo con la card
    duplicata.
- **Varietà EFS della Grecia 2002** (`variety` nella chiave di `regular_collection_items`, DB
  versione 9, `MIGRATION_8_9`). Le monete greche del 2002 coniate all'estero hanno una lettera
  nella stella (E = Madrid sul 20 cent, F = Parigi su 1-2-5-10-50 cent, S = Finlandia su 1-2
  euro) e sono diverse da quelle coniate ad Atene: stesso anno e qualità, due monete, e l'utente
  può averle entrambe. `RegularCollectionItem.variety` è `""` per la moneta normale e `"EFS"`
  (`VARIETY_EFS`) per la variante; sta nella chiave primaria, quindi la migrazione **ricostruisce
  la tabella** (SQLite non cambia una chiave con ALTER: crea `_new`, copia con varietà `''`,
  drop, rename) — verificata sul telefono installando sopra la v8 popolata (5 voci regolari e 96
  commemorative intatte, `PRAGMA integrity_check` ok). **Dove si offre**: tabella FISSA in codice
  (`RegularVarieties.kt`, `RegularIssueSeries.varietyFor(taglio, anno)`: Grecia, serie 1, 2002,
  lettera per taglio), NON derivata da `zecchePerAnno`, che è dato Numista, assente con
  l'export `-ExcludeNumista`: la funzione sparirebbe con il piano B; un test sul dataset completo controlla che
  le lettere coincidano con la zecca estera del dato. **UI**: vedi `YearOption` sopra, una voce
  "2002 · EFS variety" nell'elenco degli anni (con "letter S in the star" nell'accessibilità). La
  card COLLECTION del dettaglio mostra "Standard · 2002 · EFS". Il conteggio "N years owned" della
  lista del paese conta gli anni distinti (2002 normale + 2002 EFS = 1 anno), la barra della home
  conta i tagli: invariati. `anno + qualità + varietà` è la chiave: il pannello non può più
  produrre due voci uguali (una cella per anno, una spunta per finitura).
- **Backup e reset** (ottobre 2026): la collezione Regular è nel backup Drive (v2, varietà EFS
  compresa) e il Reset la svuota insieme alle commemorative, vedi § Backup su Google Drive e
  § Impostazioni.
- **Non ancora fatto** (vedi anche § Backlog): nessun "due esemplari dello stesso anno e finitura"
  (una voce per anno, finitura e varietà, come per le commemorative).

## Pro e pubblicità

Deciso il 2026-10-06 dal proprietario (la mia raccomandazione era togliere il banner "Go Pro" dalla v1
e riaggiungerlo quando c'è qualcosa da vendere; scelto invece di costruire tutto prima della
pubblicazione). Codice in `data/pro/`, solo librerie Google (Billing 9.1, Mobile Ads 25.5, UMP 4.0).

- **Cosa compra il Pro: l'assenza di pubblicità e, dal 2026-10-11, le tirature per zecca tedesca** (vedi
  sotto). Prodotto in-app NON consumabile
  `euro_coins_pro` (`BuildConfig.PRO_PRODUCT_ID`, da creare con lo stesso ID in Play Console: un ID
  eliminato non si riusa). Funzioni Pro future (più esemplari della stessa moneta, quantità, export
  CSV) non esistono ancora: se arrivano, lo stesso acquisto le sblocca (la card dice ancora solo
  "Remove all ads": **da aggiornare quando le zecche arrivano agli utenti**).
- **Zecche tedesche A/D/F/G/J (Pro, ramo `german-mints`, 2026-10-11).** Prima funzione Pro oltre ai banner. Tre
  fasi concordate: (1) dati, (2) visualizzazione, (3) registrazione per lettera. **Fatte la 1 e la 2; la 3 NO**
  (chiede una migrazione di `collection_items`, backup v5, regole del Quick add; per le Regular basta `variety`,
  già nella chiave). Principi decisi: **i totali (barre, Home, "N coins") NON cambiano** (la lettera sarà un
  dettaglio sopra il "posseduto", come qualità/prezzo/data); **mai nascondere o cancellare ciò che l'utente ha
  registrato se perde il Pro**; solo Germania ma con struttura generica.
  - **Dato**: `assets/mint_letters.json` (75 KB) da `scripts/export-mint-letters.ps1`, FILE SEPARATO da
    `coins.json`/`regular_issues.json` perché sono dati Numista (piano B = togliere solo questo file; l'app lo
    tratta come opzionale, `MintLetters.load` → `EMPTY`). Circolanti da `numista_divisional.jsonl`
    (`per_zecca`, più il totale dell'anno quando la lettera è una sola) sommando i type che condividono un anno;
    commemorative dai `data/raw/numista/issues_<N#>.json` (la pipeline li somma in un numero), con i `numista_id`
    di `coins.json`. Chiave: circolanti `(paese, taglio, anno)`, commemorative `(numistaId, anno)`. Lo script
    CONTROLLA le somme per lettera contro `coins.json` e `MintLettersAssetTest` contro i totali del catalogo
    Regular: vanno rilanciati entrambi se cambiano i dataset. Non in Room (statico, nessun dato utente).
    Buchi noti: 2 € circolante 2007/2009/2012/2013/2015/2018 (nessun dato Numista), 1 €/10 cent/50 cent 2026
    (lettere senza numeri, non esportati), 2 commemorative 2026 senza N#, una riga "In Sets" senza lettera
    (20 cent 2019, 78.400 pezzi, ignorata, bucket `altro` non mostrato).
  - **Visualizzazione** (mockup approvato, variante C): nel pannello "by year" di un taglio con lettere, in Pro, una
    riga per anno e UNA COLONNA PER LETTERA con selettore Standard/BU/Proof in cima (`MintLetterMatrix`,
    `mintLetterTable()`, legenda "A Berlin · D Munich …"). **Numeri sempre a UNA cifra decimale** con
    `formatCompactMintage` ("960.0 M", "119.4 M", "85.0 k", "4.0 B"; scelta del proprietario: coerenza e più spazio
    tra le colonne; "125.37 M" diventa "125.4 M", il numero esatto sta nella card e nella tabella dei totali).
    **Il totale dell'anno è l'ULTIMA COLONNA**, in Medium, separata da un filetto verticale. Storia: la prima
    versione non lo aveva (il proprietario lo ha notato); poi una seconda riga sotto l'anno ("Total 600,000,000",
    "· 2 of 5 mints"), scartata dopo averla vista sul telefono ("stona": un elemento diverso dalla griglia);
    scelta l'ultima colonna sul mockup a 375 dp, scartata "subito dopo l'anno" (sembrava una lettera in più). È la
    somma delle zecche con il dato: i "—" accanto dicono quali mancano, per questo non c'è "N of 5".
    **Separazione tra le colonne** (variante C del mockup, dopo "ancora poco netta"): filetto verticale tra le
    zecche e tra queste e il totale (`MatrixLine`), colonna Total su un velo chiaro (`surface` al 50%, la tinta
    delle righe "parte" della Grecia); nessun filetto tra anno e prima zecca. Numeri a **11 sp** e anno a 38 dp:
    a 12 sp "800.0 M" toccava i filetti sul telefono (375 dp). Scartate: fondo alternato per colonna (affollato
    con i filetti orizzontali). **Il pannello si chiude trascinando solo da maniglia, titolo e sottotitolo**
    (`protectFromSheetDrag`, `keepScrollInside` in `ui/components/KeepScrollInside.kt`): prima ogni punto fuori
    dalla lista era una maniglia e la riga "Year" chiudeva tutto; provato con `adb` prima e dopo.
    **Nessuna evidenziazione della zecca più bassa**: il bronzo fu proposto nel mockup e scartato dal proprietario
    (colore nuovo per i dati). **Nessun segmento "Sets"** (i pezzi "In Sets" di Numista, bucket `altro`, 122 anni su
    191 per la Germania, proposto e scartato dal proprietario il 2026-10-11): la tabella resta Standard/BU/Proof.
  - **Perché tanti "—" (Standard 62 anni su 191, BU 123): non sono dati mancanti.** Controllato il 2026-10-11 con la
    lista "Vom Bundesministerium der Finanzen erteilte Aufträge zur Prägung von Euro-Umlaufmünzen" della Bundesbank
    (PDF, aggiornato all'08.12.2025, URL `bundesbank.de/resource/blob/599312/…/praegung-euro-umlaufmuenzen-data.pdf`;
    anno, lettera e taglio in milioni di pezzi, **ordini di coniazione, non tirature effettive**; testo leggibile
    inflando gli stream, nessun OCR): tutti i 62 anni senza Standard su Numista NON hanno nessun ordine (10 cent
    2005-2016, 50 cent 2005-2021, 1 euro dal 2006…: la Germania aveva scorte e coniò solo set da collezione), e i 9
    anni con meno di 5 lettere hanno ordini solo per quelle (1 euro 2003: solo A e J). Dove i due elenchi si
    sovrappongono (607 combinazioni anno/lettera/taglio) 483 sono IDENTICI, 74 entro il 5%, 50 differiscono di più
    (2019 e 2020 soprattutto, NON indagati). Il PDF NON è stato importato: non aggiunge nessun anno a Numista. Sotto
    Standard la tabella dice "— means none were struck for circulation". Il BU "—" è altro: Numista conta i BU dei
    set come "In Sets" (bucket `altro`, non mostrato), per questo la maggioranza manca. Lo script di estrazione
    non è nel repo (era uno strumento di verifica); rifarlo se serve: i filetti spessi a tutta larghezza (x≈77,
    w≈637) separano gli anni, le lettere A D F G J a x≈207 raggruppano gli ordini, i tagli sono otto colonne a
    x≈250…670, e un'etichetta ruotata con "(" nel blocco marca la 2 € commemorativa.
  - Nelle commemorative, "View by mint" sotto i tre numeri di MINTAGES
    (`CoinMintLettersEntry`) apre un pannello con una riga per lettera e il totale (colonna della lettera a
    larghezza fissa: con "  Città" in coda la città partiva dove finiva la lettera e non si allineava, 2026-10-13). **Senza Pro**: nella tabella by
    year una riga lilla "Go Pro to split by mint (A, D, F, G, J)" (`ProLockedRow`), nelle commemorative il
    pulsante lilla con lucchetto; il tocco apre `MintLettersProPrompt` ("Not now" / "Go Pro"). **Testi del blocco
    (2026-10-13)**: dove qualcosa è bloccato l'invito è un'azione ("Go Pro to unlock." nella nota di Impostazioni →
    Catalogs → German mints, "Go Pro to split by mint …" nella riga), non "Part of Pro": stesso verbo del
    pulsante "Go Pro"; "Become Pro" scartato (suona come un'iscrizione, è un acquisto una tantum) e niente "!".
    Invariati i dialog ("Part of Euro Coins Pro, with no ads.": il pulsante "Go Pro" è già lì) e le etichette
    del badge/TalkBack. **"Go Pro" porta alle
    Impostazioni** (`ProAccess.onGoPro` → `openSettings()`), non avvia l'acquisto: resta SOLO la pillola col prezzo
    a farlo. `LocalProAccess` e `LocalMintLetters` sono forniti da `MainActivity` (il `NavController` è stato
    sollevato lì). **Non verificato sul telefono.**
  - **Bug trovato e corretto di passaggio** in `export-regular-issues.ps1`: Germania 20 cent 2007 Standard
    mostrava 200.000 invece di 108.900.000 ("Doppione… tenuta la prima": due type Numista per lo stesso anno,
    lato comune vecchio e nuovo). Ora le righe con lo stesso (anno, qualità) si SOMMANO. Era l'unico caso.
  - **FASE 3 — registrare la lettera in collezione: FATTA sul ramo `german-mints-phase3` (2026-10-12), NON unita a
    `main`.** Passi 1-5 fatti e provati sul telefono, tema chiaro compreso e backup/ripristino su Drive vero
    (vedi sotto); **non provato**: il backup v5 dalla build di RELEASE (R8) installata. **Passi 2-5 (2026-10-12)**: riga **Mint** (`MintStrip`,
    chip a peso: "No mint" 2,2 e le lettere 1, non scorrevole, la J usciva di lato) nei pannelli delle commemorative
    (sotto il sottotitolo) e delle Regular (sotto la striscia degli anni); bozza per (zecca, finitura) / (anno,
    varietà, zecca, finitura); si apre sulla zecca che ha qualcosa; puntino sul chip dove c'è una finitura in bozza.
    **Con Pro** il pannello vede e salva TUTTE le voci (`editableMints = null`); **senza Pro** vede solo "No mint",
    le lettere sono bloccate (lucchetto "Mint 🔒 Pro", tocco = invito "Record the mint") e il salvataggio le lascia
    intatte (verificato sul telefono: salvare un BU semplice senza Pro non cancella "Standard · D"). **Nessuna
    preselezione** (dal 2026-10-13; prima ce n'era una "morbida" di "Standard" senza zecca, che spariva se si
    spuntava qualcosa in una lettera per non salvare il doppione "Standard" + "Standard · D"). Rimozione: il "Remove" del pannello toglie ciò che il pannello vede, la
    conferma del Quick add toglie TUTTO (`onRemoveAll`); i testi nominano le zecche ("This removes BU and Standard
    (D) for …", "… Mint D is included."). Card COLLECTION dei dettagli: una riga per finitura e zecca ("Standard · D")
    e i cinque cerchi A D F G J con "1 of 5 mints" (`MintLetterBadges`; solo con Pro o dove c'è già una lettera: un
    Pro scaduto non nasconde i dati). **I cerchi sono SOLO informativi, nessun filtro per zecca e nessun tocco** (decisione del 2026-10-15, dopo due prove scartate dal proprietario). Provati e tolti: (1) il tocco su una lettera che apriva il pannello su quella zecca (e41a487, revert d55660f): duplicava "Edit collection"; (2) il filtro per zecca come i chip degli anni (769fbc9, revert 510f224): per un anno una moneta ha al massimo 15 righe (3 finiture per 5 zecche, circa 750 dp, il caso del collezionista che ha tutto), tipicamente 1-5, e il filtro aggiungeva una modalità (cerchi pieni, vuoti e scelti, messaggio vuoto) per un problema che quasi nessuno ha. I chip degli anni servivano per 30 righe. **Non riproporre** senza un caso reale di card troppo lunga. Lezione: se il proprietario chiede un comportamento esplicito la raccomandazione alternativa va detta ma non sostituita senza il suo via libera. **Testo della card Pro** (2026-10-12): "No ads · German mints" (era "Remove
    all ads"; una riga, accanto alla pillola del prezzo: da controllare che non si tronchi con il font ingrandito).
    **Interruttore "German mints"** (Impostazioni → Catalogs → card "German mints", "Show mints", acceso di
    default, `UserSettings.showMints`, `LocalShowMints`; spostato da "Collection" il 2026-10-12: nasconde una parte
    dei cataloghi come "Hide microstates", non è un comportamento come Quick add; "Collection" resta col solo Quick
    add, e la riga di Catalogs nelle Impostazioni nomina "German mints hidden" solo se nascoste): è un'**opzione del Pro**, senza
    Pro è attenuata e non si tocca, mostra "acceso" (valore effettivo) e la funzione si vede sempre, bloccata e con
    gli inviti; il valore salvato torna a valere col Pro. Spento (con Pro) nasconde la FUNZIONE (riga Mint, cerchi,
    "View by mint", tabella a zecche affiancate, inviti al Pro), MAI i dati: le voci con zecca restano, il pannello
    vede solo "No mint" e le salva intatte, e nella card COLLECTION si leggono con la loro lettera. **Build di release (R8)**
    costruita il 2026-10-12: le classi `@Serializable` nuove (`BackupItem`, `RegularBackupItem`, `MintLettersJson`…)
    sono tenute e non rinominate (`mapping.txt`/`seeds.txt`); non installata sul telefono. **Backup verificato
    sul telefono con Drive vero (2026-10-12, build debug)**: con "Standard · D" registrata, "Back up now" scrive
    `schemaVersion` 5 con `"mint":"D"` (94 + 205 voci, stato "Up to date"; la copia locale è
    `files/backup_snapshot.json`, leggibile con `run-as`), il dialog "Restore" dice 94 commemorative + 191 Regular
    ("up to date"), "Replace" ripristina 299 voci e la zecca D sopravvive; tolta la voce e rifatto il backup, il file
    torna a `schemaVersion` 4, nessuna zecca, 93 + 205 voci (Drive di nuovo "pulito"). **Tema chiaro verificato**:
    riga Mint (chip lilla selezionato, puntino), cerchi A D F G J nella card COLLECTION, "View by mint" e tabella a
    zecche affiancate (filetti e velo sul totale leggibili), interruttore "German mints". **Interruttore spento**
    (provato): niente riga Mint né pulsante né cerchi, la voce "Standard · D" resta elencata nella card, e un Save
    senza modifiche non la tocca. **Card Pro in Free**: "No ads · German mints" sta su una riga con la pillola
    "2,99 €". Nei test su telefono: in Free il layout delle Impostazioni cambia (la card Pro è più alta), le
    coordinate vanno prese dal dump di uiautomator e non a memoria.
    **Passo 1 (2026-10-11)**: `mint` nelle due
    entity e nella chiave (`NO_MINT = ""`, `MintFinish`), `MIGRATION_10_11` (SQL in `CollectionMigration.kt`),
    pianificatori puri `planCommemorativeSave`/`planRegularSave` (`CollectionSave.kt`, con `editableMints`: le zecche
    che il pannello può cambiare; i ViewModel passano `{NO_MINT}`, quindi i pannelli di oggi non toccano le voci con
    zecca), `mintLettersFor(paese)`, Quick add (la zecca è un dettaglio), backup v5 scritta SOLO con almeno una zecca
    (`BackupFile.versionFor`), chiavi di `BackupStatus` con la zecca. **Verificato**: `CollectionMigrationTest`
    confronta lo SQL con quello generato da Room; sul telefono, installando sopra la v10 popolata: `user_version`
    11, `integrity_check` ok, 93 + 204 voci, tutte con `mint = ''`, Home 93/613 e 194/328 invariate; la copia di
    `purchasedOn`/prezzi/`addedAt` provata anche su una copia del database con date e prezzi messi a mano (sulle
    voci vere sono tutte vuote) con `sqlite-jdbc` dalla cache Gradle. **Lacune volute del passo 1**: i pannelli
    non hanno ancora la riga Mint; "Remove" e la conferma del Quick add passano da `onSaveCollection(…, vuoto)` con
    scope `{NO_MINT}`, quindi con voci con zecca NON le toglierebbero (da rivedere al passo 4); le card COLLECTION dei
    dettagli mostrano la prima voce per qualità. Nessuna voce con zecca esiste finché non c'è la UI.
    **Decisioni** (mockup approvato dal proprietario): nei pannelli di collezione una riga
    "Mint" con i chip **"No mint"** (sempre presente, PRIMO e predefinito, nome parallelo a "No year"; "None" scartato:
    ambiguo in una riga di lettere) e A D F G J; puntino verde sulle lettere con qualcosa in bozza. Nelle Regular la
    riga sta SOTTO la striscia degli anni ed è sempre visibile (l'anno è preselezionato dall'apertura: farla comparire
    solo dopo un tocco sull'anno cambierebbe l'altezza del pannello e nasconderebbe la funzione). **Ogni zecca è una
    moneta**: prezzo e data per (zecca, finitura). **Il tocco rapido resta "No mint"** (la voce semplice). I totali
    NON cambiano (una moneta posseduta conta una volta). **Senza Pro** le lettere già registrate restano visibili e
    nella scheda di dettaglio i cinque cerchi A D F G J (pieni = posseduta, "3 of 5 mints") restano; aggiungere o
    cambiare una lettera apre l'invito al Pro. La riga Mint compare solo per i paesi con lettere (`mintLettersFor`,
    oggi la Germania), anche senza dati di tirature.
  - **Modello e migrazione 10 → 11.** `mint: String = ""` NELLA CHIAVE di entrambe le tabelle (`""` = "No mint", poi
    "A".."J"): `collection_items` PK (coinKey, quality, mint); `regular_collection_items` PK (seriesKey, taglio, anno,
    quality, variety, mint). Riuso di `variety` scartato (mescola due concetti, backup ambiguo). Una sola
    `MIGRATION_10_11` che RICOSTRUISCE le due tabelle come la 8 → 9 (CREATE `_new` con la PK nuova, INSERT … SELECT con
    `mint = ''`, DROP, RENAME): SQLite non cambia una chiave con ALTER. **Attenzione**: la 8 → 9 non copiava
    `purchasedOn` (arrivato dopo con la 9 → 10), la 10 → 11 DEVE copiarlo, insieme a `priceCents` e `addedAt`. Il SQL
    deve coincidere con quello generato da Room (`build/generated/ksp/.../CoinDatabase_Impl.kt`): se differisce l'app
    non parte per NESSUNO. Verifica come le altre: installare la build sopra la v10 popolata sul telefono (copia del
    database sul PC prima), controllare i conteggi (stesse righe, tutte con `mint = ''`) e `PRAGMA integrity_check`.
    **Non si torna indietro**: con il database a v11 una build v10 non parte (Room non scende di versione) senza
    rimettere il file del database dalla copia.
  - **Codice da toccare.** `CollectionItem`/`RegularCollectionItem`/`RegularCollectionEntry` (campo `mint`),
    `CoinRepository.saveCollection` (da `Map<CoinQuality, FinishEntry>` a una mappa su (zecca, finitura), `existing`
    per (zecca, qualità)) e `RegularIssueRepository.saveCollection` (la chiave diventa anno+varietà+zecca+qualità,
    `addedAt` conservato per quella chiave), `QuickAdd.kt` (la voce semplice richiede ANCHE `mint == ""`: una voce con
    zecca è `CONFIRM_REMOVE`, e `commemorativeRemovalMessage`/`regularRemovalMessage` nominano le lettere),
    `BackupFile`/`BackupStatus` (`mint` nelle chiavi di confronto), `CollectionSheet`/`RegularCollectionSheet` (bozza su
    (zecca, anno, varietà, finitura) e riga Mint), card COLLECTION dei due dettagli (riga per finitura con zecca,
    cerchi A D F G J), `joinQualities` (con più zecche le qualità si ripetono: distinte), e i punti che raggruppano per
    moneta (`regularProgress`, barre, `ownsAnyQuality`): vanno solo VERIFICATI con un test, devono già contare una volta.
  - **Backup.** `mint` default `""` in `BackupItem` e `RegularBackupItem`; i file v1-v4 si leggono con zecca vuota.
    **Versione 5 scritta SOLO se almeno una voce ha una zecca**, altrimenti si continua a scrivere la 4: chi non usa le
    lettere non obbliga un secondo telefono ad aggiornare per ripristinare (un v5 si rifiuta come "più nuovo", come
    per v3 e v4). `BackupStatus`: le chiavi di confronto includono la zecca (le istantanee vecchie hanno `""`, quindi
    nessuna modifica spuria dopo l'aggiornamento).
  - **Free dopo un Pro scaduto: attenzione al salvataggio.** `replaceForCoin`/`replaceForDenomination` SOSTITUISCONO
    tutte le voci della moneta: se il pannello senza Pro edita solo le voci "No mint" e salva, cancellerebbe le voci con
    zecca. Quindi: senza Pro la bozza parte con TUTTE le voci (quelle con zecca in sola lettura, intatte) e il
    salvataggio le riscrive identiche; "Remove" nel pannello toglie solo le voci "No mint". Da coprire con un test.
  - **Ordine di lavoro**: (1) modello, migrazione, DAO e repository con test sulla logica pura (Quick add, backup
    v4/v5 e ordine di versione, chiavi di `BackupStatus`, conteggi); (2) pannello commemorative; (3) pannello Regular;
    (4) card di dettaglio e messaggi; (5) Pro/Free; (6) prova sul telefono sopra la v10 popolata, ripristino di un
    backup v4 e v5 (il parsing; Drive vero non è provabile qui), reset. Prima di pubblicare: nuovo .aab con
    `versionCode` più alto e card Pro aggiornata.
- **`ProBilling`**: Play è l'unica fonte di verità, in locale c'è una copia (SharedPreferences `pro`,
  `is_pro`) perché un utente Pro offline non deve rivedere gli annunci. Riallineamento a ogni ritorno in
  primo piano (`MainActivity`, `repeatOnLifecycle(STARTED)`) e dal pulsante Restore. **Una verifica
  fallita non toglie il Pro** (`resolveCachedPro`: solo un esito certo di "nessun acquisto", cioè un
  rimborso, lo toglie); un pagamento in sospeso non sblocca niente; gli acquisti completati si
  confermano (`acknowledgePurchase`: senza, Play rimborsa dopo 3 giorni). **Nessuna verifica lato
  server delle ricevute**: per un acquisto che toglie un banner è un rischio accettato; da rivedere se
  il Pro sbloccasse funzioni a pagamento. Debug: la chiave `debug_pro` in `shared_prefs/pro.xml` simula
  il Pro (solo `BuildConfig.DEBUG`, sparisce in release). **Interruttore "Simulate Pro"** (2026-10-08, mockup A): nelle
  Impostazioni delle build di sviluppo, sezione "Developer" con etichetta "DEBUG ONLY" e bordo tratteggiato, prima di
  Danger zone (`DeveloperSection`, `ProBilling.setDebugProSimulated`): toglie banner e annuncio a tutto schermo e fa
  diventare "Euro Coins Pro" la card Pro subito, senza `adb` né riavvio. Serve a vedere l'app con e senza pubblicità.
  Nella release non esiste (`BuildConfig.DEBUG` falso, R8 toglie il ramo). Per vedere gli annunci della VERSIONE DI
  PLAY senza rischio, registrare il telefono come dispositivo di test in AdMob (Impostazioni → Dispositivi di test, ID
  dal log: `Use RequestConfiguration.Builder().setTestDeviceIds(...)`): mai cliccare gli annunci veri.
  **Provato sul telefono (release)**: connessione a Play, "Restore purchase" ("No purchase found…") e
  tocco su Go Pro senza prodotto in Play (messaggio d'uso, nessun crash). **NON provato: l'acquisto
  vero** (serve l'app in Play Console con il prodotto attivo e un tester di licenza), il rimborso, il
  pagamento in sospeso.
- **`AdsConsent`** (UMP): il messaggio di consenso e i testi si configurano nella console AdMob
  (Privacy e messaggi → GDPR; senza non compare nessun modulo). `MobileAds.initialize` parte SOLO
  dopo `canRequestAds` (l'SDK raccoglie dati da quando parte). "Ad privacy" nelle Impostazioni
  compare solo dove UMP lo richiede (UE/Regno Unito). **Un utente Pro non passa dal consenso.** Provato
  sul telefono (in Italia) con il messaggio di test di Google: il modulo compare, rifiutando non
  compare nessun annuncio, con il consenso arrivano gli annunci. Così deve essere: chi rifiuta non vede
  pubblicità (non è un difetto).
- **Banner** (`ui/components/AdBanner.kt`): adattivo ancorato, SOLO nella Home, nel `bottomBar` dello
  Scaffold (le due schede si dividono lo spazio che resta: con il banner entrano ancora per intero sul
  telefono dell'utente). Spazio riservato mentre carica (le schede non saltano quando arriva), altezza 0
  se non c'è nessun annuncio, nuovo tentativo dopo 15 s, poi 30, poi 60 (`bannerRetryDelayMs`). **La vista vive FUORI dalla
  composizione** (`BannerAd` in `data/pro/`, tenuta da `Monetization`, con un `MutableContextWrapper` che punta
  all'Activity solo mentre la Home è agganciata): prima la `AdView` nasceva a ogni ingresso nella Home e ogni
  ritorno rifaceva la richiesta, quindi "ogni tanto solo lo spazio" per qualche secondo (misurato nei log
  dell'SDK: 3 ritorni = 3 richieste; ora 0). Una nuova richiesta parte solo se l'annuncio ha più di 2 minuti o
  l'ultimo tentativo è fallito. Scartato negli elenchi e nei dettagli: dove si
  consulta e si registra, e collide con la barra flottante di ricerca/filtri.
  `getCurrentOrientationAnchoredAdaptiveBannerAdSize` è deprecata in favore di
  `getLargeAnchoredAdaptiveBannerAdSize` (più alta, ~40 dp in più alla Home): se sparisse dall'SDK, passare
  a quella e rivedere l'altezza delle schede.
- **Annuncio a tutto schermo** (`InterstitialAds`): ogni **6 monete guardate E almeno 30 secondi**
  dall'ultimo annuncio, `isInterstitialDue`, costanti `INTERSTITIAL_EVERY_COINS`/`INTERSTITIAL_MIN_GAP_MS`.
  **Riscritto il 2026-10-08** (prima 10 monete e 3 minuti, solo all'uscita dal dettaglio, tutto in memoria):
  l'utente lo vedeva "molto di rado e dopo più di 10 monete". Tre cause, tutte corrette: (1) compariva SOLO uscendo
  dal dettaglio, ma il dettaglio a pagine serve proprio a sfogliare senza uscire, quindi chi guardava 30 monete di
  fila lo vedeva una volta sola, all'uscita; (2) contatore e orologio ripartivano a ogni avvio dell'app e i 3
  minuti si contavano dall'apertura: una sessione sotto i 3 minuti non lo vedeva mai (ora salvati in
  SharedPreferences `interstitial`, `coins_since_last` e `last_shown_at` con l'orologio di sistema; alla prima
  installazione l'orologio parte da ora; orario del telefono portato indietro = si riparte da zero,
  `elapsedSince`); (3) se l'annuncio non era pronto saltava il turno senza ritentare il caricamento a dovere
  (ora ritenta alla moneta successiva, non più spesso di ogni 30 s, e scarta quelli scaduti dopo 55 minuti,
  Google li invalida a un'ora). **6 monete e non 10 perché una serie Regular Issues ne ha 8** (scelta
  dell'utente: con 10 chi guarda una serie sola non lo vedrebbe mai; `InterstitialPolicyTest` lo fissa). Si conta
  ogni moneta guardata (si entra nel dettaglio, si scorre a una pagina nuova), NON i tocchi: spuntare una casella
  o salvare non lo avvicina. **Compare in due punti**: uscendo dal dettaglio (un unico
  `OnDestinationChangedListener` in `EuroCoinsNavHost`: copre freccia e gesto indietro) e SFOGLIANDO, SUBITO appena una
  pagina si assesta (`showWhenSettled`; scelta del proprietario del 2026-10-08: la prima versione aspettava 1,5 s di
  pagina ferma, ma ogni scorrimento successivo annullava l'attesa e chi sfogliava di continuo, 23 monete di fila, lo
  vedeva solo quando si fermava; rischio noto: l'annuncio può comparire mentre inizia lo scorrimento successivo e un
  tocco può finirci sopra, cosa che le policy di Google non gradiscono, da tenere d'occhio). Mai mentre si scrive o si
  registra, mai all'apertura, mai
  se l'app non è in primo piano. Si precarica a metà strada; se non è pronto non compare (nessuna attesa).
  **Pausa portata da 90 a 30 secondi il 2026-10-08** (scelta del proprietario): a ~4 s a moneta le 6 monete sono
  pronte dopo ~24 s e comandava la pausa, quindi l'annuncio compariva dopo ~23 monete, non ogni 6 (misurato dai
  log dell'SDK: partito esattamente 90 s dopo l'azzeramento di prova). Con 30 s parte ogni ~7-8 monete a quel
  ritmo; ancora un freno contro due annunci ravvicinati. Altre pause valutate: 20 s (più fastidioso), 60 s (rado),
  nessuna (sconsigliato). Il contatore perde al massimo una moneta con scorrimenti simulati molto veloci (10 su 11).
  Scelta del proprietario ("dopo un certo numero di tocchi o di monete aperte"); le soglie vanno ritoccate
  guardando il feedback reale. Verificato sul telefono con gli ID di test: dopo la pausa e 6 monete sfogliate
  l'annuncio compare da solo, senza uscire dal dettaglio, e contatore e orario si azzerano. Non provato con
  gli ID veri (riempimento basso finché l'app non è pubblicata e collegata allo store in AdMob).
- **AdMob: app e unità create il 2026-10-07** (app "Euro Coins", Android, "non ancora pubblicata"; stato "Richiede revisione" normale finché non è collegata allo store). Gli ID veri sono già in `local.properties` della cartella principale e del worktree (non versionati); sono identificativi pubblici, finiscono comunque nell'app. **Dopo la pubblicazione su Play**: in AdMob collegare l'app alla scheda dello store, così parte la revisione per gli annunci veri (di solito un paio di giorni; fino ad allora gli annunci veri sono limitati). Primo .aab con gli ID veri: `~/EuroCoins-release/euro-coins-1.0-vc1.aab` (versionCode 1, firmato con la chiave di upload, nessun ID di test dentro).
- **ID AdMob**: `local.properties` (non versionato) `admob.appId`, `admob.bannerUnitId`,
  `admob.interstitialUnitId`. **Debug usa SEMPRE gli ID di test pubblici di Google** (mai impressioni
  né clic veri durante lo sviluppo: rischio di sospensione dell'account AdMob); la release usa quelli
  veri se ci sono, altrimenti quelli di test. `bundleRelease` FALLISCE senza ID veri
  (`checkAdmobIds`), salvo `-PallowTestAds`: **dal 2026-10-08 il flag FORZA gli ID di test anche se local.properties ha quelli veri**
  (prima saltava solo il controllo e, con gli ID veri presenti, la release usava i veri: un .aab "di prova" avrebbe avuto annunci
  veri). Serve al test chiuso: un clic di un tester su un annuncio vero conta come traffico non valido per il TUO account AdMob.
  Un .aab con gli annunci di test non va pubblicato in produzione. **Mai cliccare sugli annunci veri dal proprio telefono.** Il manifest ha l'ID app
  (`com.google.android.gms.ads.APPLICATION_ID`, obbligatorio: senza l'SDK manda in crash l'app).
- **Card nelle Impostazioni** (`ProSection`/`ProCard`, dopo due giri di mockup, 2026-10-08): sotto il backup,
  "Go Pro" / "Remove all ads", a destra un **pulsante pieno verdigris a pillola col prezzo di Play, nella
  valuta dell'utente** ("Remove ads" se il prodotto non si carica): è l'unico invito a pagare e SOLO lui avvia
  l'acquisto (prima la riga intera, con una freccia `>` che prometteva una navigazione e non un pagamento).
  Sotto un filetto e DUE METÀ uguali con le scritte centrate, separate da un filetto verticale corto da 18 dp: "Restore Pro" e "Ad privacy" (solo UE/UK; fuori,
  Restore occupa tutta la larghezza), testo verdigris. Provati e scartati come piede: scritte agli estremi (a sinistra/destra, "non allineate e amalgamate": due cose a caso), centrate con un punto, sull'asse del testo senza filetto. Testi accorciati: "Restore purchase"
  non diceva cosa si recupera, "Ad privacy choices" era gergo legale; scartato "Manage ad consent", troppo
  lungo. Prima erano due `TextButton` liberi sul fondo che "sembravano messi a caso": la prima versione del piede era già a
  due metà ma con filetto a tutta altezza (48 dp), poi alleggerita. Scartate: B pulsante a tutta
  larghezza o a misura di testo sotto il titolo (centrato, allineato al testo, a destra: "non mi piace
  nessuna"), C azioni nella riga sotto il testo, card a righe con icona e freccia, due pulsanti a contorno.
  L'esito di un ripristino o acquisto è una riga di testo sotto la card. Verificato sul telefono (tema scuro, UE; pillola con padding 22×10 dp, resa più larga su richiesta); non il
  tema chiaro né il font ingrandito. Con il Pro attivo la card diventa "Euro
  Coins Pro" / "No ads. Thank you!", bordo 2 dp e cerchio verdigris, spunta a destra, e sparisce la riga
  dei pulsanti. L'icona è sempre `WorkspacePremium` (non una corona). Regola del proprietario per questi
  testi: corti, una riga, niente a capo.
- **Privacy**: gli annunci aggiungono al manifest `AD_ID`, `ACCESS_ADSERVICES_*`, `WAKE_LOCK`, più
  `com.android.vending.BILLING`. La **scheda Data safety** di Play Console e la **privacy policy** vanno
  scritte di conseguenza (identificatore pubblicitario, dati di utilizzo e del dispositivo raccolti da
  AdMob, acquisti): non è lavoro di questo repo.

## Rilascio

Costruire e pubblicare su Google Play. Stato al 2026-10-06. **Il lint NON è stato eseguito**
(`lint-gradle` non è in cache offline): `lintVital*` si salta con `-x`. Non dire di averlo fatto.

- **Versione**: `versionCode = 2`, `versionName = "1.0"` in `app/build.gradle.kts` (il 2 dal 2026-10-08: il telefono di prova ha
  l'app con `versionCode` 2, e Android RIFIUTA di installare sopra una versione più bassa — `INSTALL_FAILED_VERSION_DOWNGRADE` — mentre
  disinstallare cancellerebbe la collezione; quindi `main` non deve scendere sotto il numero installato sul telefono). Ogni pacchetto
  caricato su Play deve avere un `versionCode` PIÙ ALTO del precedente (il test interno ha il 1; il pacchetto del test chiuso è il 2, nel ramo `closed-test-vc2`).
- **R8** (`optimization { enable = true }` + `proguardFiles("proguard-rules.pro")`): codice, risorse e
  offuscamento. Le regole dell'app (`app/proguard-rules.pro`) tengono i serializzatori
  `kotlinx.serialization` delle classi `@Serializable` (`CoinJson`, `RegularIssueJson`, `BackupFile`, i blob
  JSON di Room come `RegularIssueImage`), i nomi delle costanti degli enum salvati per nome (`CoinQuality`,
  `ThemeMode`…), Room, e i `-dontwarn` attesi di OkHttp; le altre librerie (Billing, Ads, UMP, Credential
  Manager, Coil, Haze, Compose) portano le loro. **Il rischio vero è che R8 rompa in silenzio il parsing JSON o
  Room**: dopo ogni modifica delle regole o delle dipendenze si prova una release sul telefono (sotto).
  Il `mapping.txt` (~76 MB, comprende tutte le librerie) viaggia dentro l'.aab
  (`BUNDLE-METADATA/…/proguard.map`) e Play lo usa per decodificare gli stack trace. Nessun avviso di R8
  nell'ultima build.
- **Chiave di upload**: `C:\Users\mik16\EuroCoinsKeys\euro-coins-upload.jks` (PKCS12, RSA 4096, alias
  `euro-coins-upload`, valida fino al 2054), con `keystore.properties` accanto (percorso e password,
  generate casualmente). **Fuori dal repo e ignorati da git** (`*.jks` e `keystore.properties` in
  `.gitignore`); `app/build.gradle.kts` legge `keystore.properties` dalla radice del progetto: in un
  worktree o in un altro clone va copiato lì (il percorso del `.jks` è assoluto, il file si copia tale e
  quale). **Fare subito un backup sicuro del `.jks` e del suo `keystore.properties`** (gestore di password,
  non il repo): perderli vuol dire non poter più firmare gli aggiornamenti con la stessa chiave. Con Play
  App Signing (obbligatorio per i nuovi .aab) la perdita della chiave di UPLOAD si risolve con un reset
  chiesto a Play, ma la chiave di FIRMA vera la tiene Google. Per rigenerarla: `keytool -genkeypair -v
  -keystore <file>.jks -alias euro-coins-upload -keyalg RSA -keysize 4096 -validity 10000` (in PKCS12 la
  password della chiave è quella del keystore: `keyPassword` = `storePassword`).
- **Build del pacchetto per Play** (.aab firmato con la chiave di upload):

  ```bash
  export JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
  ./gradlew.bat :app:bundleRelease -x lintVitalRelease -x lintVitalAnalyzeRelease -x lintVitalReportRelease
  # per un pacchetto di PROVA con gli annunci di test: aggiungere -PallowTestAds
  ```

  Esce in `app/build/outputs/bundle/release/app-release.aab` (~10 MB). Verificare la firma:
  `jarsigner -verify -certs app-release.aab` (deve nominare `CN=Euro Coins Upload`; l'avviso "invalid
  certificate chain" è normale, il certificato è autofirmato).
- **Cartella di lavoro nuova (worktree) = copiare `local.properties`** da `C:\Users\mik16\Github\euro-coins-app\local.properties`
  (`sdk.dir`, `google.webClientId`, ID AdMob; non è nel repo). Senza, la build non trova l'SDK o ha il login Google
  disabilitato ("Google sign-in isn't set up"): una build così NON va installata sul telefono del proprietario.
  Il worktree ha anche la sua cartella `build`: il primo build è lento. `keystore.properties` serve solo per
  l'.aab di Play (percorso della chiave di upload già dentro).
- **Prova sul telefono: MAI con la chiave di upload.** Il telefono ha l'app firmata con la chiave di
  DEBUG; una firma diversa non si installa sopra (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`) e l'unica via
  sarebbe disinstallare, che CANCELLA la collezione reale. **Non disinstallare mai l'app.** Per le prove si
  costruisce la release e la si firma a mano con la chiave di debug:

  ```bash
  ./gradlew.bat :app:assembleRelease -x lintVitalRelease -x lintVitalAnalyzeRelease -x lintVitalReportRelease
  BT="$LOCALAPPDATA/Android/Sdk/build-tools/36.0.0"
  "$BT/zipalign.exe" -f -p 4 app/build/outputs/apk/release/app-release.apk allineato.apk   # app-release-unsigned.apk senza keystore.properties
  "$BT/apksigner.bat" sign --ks ~/.android/debug.keystore --ks-pass pass:android --key-pass pass:android \
      --ks-key-alias androiddebugkey --out prova.apk allineato.apk
  adb -s 487e0cf2 install -r prova.apk
  ```

  Prima di installare, copia del database sul PC (`adb shell "run-as com.michele.eurocoins base64
  databases/coins.db" | tr -d '\r' | base64 -d`, anche `-wal`/`-shm`). Con `keystore.properties` presente
  `assembleRelease` firma già con la chiave di upload: l'`apksigner sign` sopra la RIFIRMA (sostituisce la
  firma). **Per esercitare il seeding da zero sotto R8 senza toccare la collezione** (è in tabelle
  separate): aggiungere un a-capo finale a `coins.json` e `regular_issues.json` in una build di prova (l'hash
  cambia, le tabelle del catalogo si ripopolano), poi `git checkout` degli asset. **Il telefono è
  condiviso con altre sessioni**: se un'altra build è stata installata nel frattempo la schermata è quella
  sbagliata (controllare `dumpsys package com.michele.eurocoins | grep lastUpdateTime`). Lo schermo si
  blocca da solo dopo poco: da script non si sblocca né si cambia il timeout.
- **Verificato sul telefono con la release ottimizzata (R8, ID di test)**: avvio, seeding di
  `coins.json` e `regular_issues.json` (584 monete, 328 righe Regular, hash cambiato), collezione intatta
  (2 commemorative, 4 Regular), Home con banner, Countries, elenco di un paese con le foto di Coil, dettaglio
  a pagine con tirature e dati Numista, Impostazioni con la card Pro, Restore purchase, Go Pro senza
  prodotto, modulo di consenso UMP, serie e dettaglio di un taglio Regular (Andorra: tirature, zecche, dati Numista,
card COLLECTION con gli anni, pannello di collezione aperto e annullato), annuncio a tutto schermo (dopo 10
monete guardate e 3 minuti, uscendo dal dettaglio: compare, si chiude con la X, si torna alla lista).
- **`allowBackup="false"`** (scelta del proprietario, raccomandata): senza, Android ripristinerebbe da solo
  su un telefono nuovo il database, l'istantanea `backup_snapshot.json` e le preferenze, incoerenti con il
  catalogo seminato e con il backup su Drive (che resta l'unica via di ripristino: si accede con Google e si
  usa Restore). Costo: tema e scheda iniziale non tornano da soli su un telefono nuovo; niente
  trasferimento da telefono a telefono dei dati dell'app.
- **Controlli finali fatti**: `debuggable` assente nel manifest unito della release (falso); nome app "Euro
  Coins" e icona adattiva con monocromatica (`mipmap-anydpi-v26`, minSdk 26 quindi nessun PNG di ripiego);
  nessun `Log.*` né `println` nel codice; permessi del manifest unito: INTERNET, BILLING, AD_ID,
  ACCESS_ADSERVICES_*, WAKE_LOCK, ACCESS_NETWORK_STATE, USE_BIOMETRIC/FINGERPRINT (queste ultime dalle
  librerie Google, da riportare nella Data safety).

### Checklist OAuth / Google Sign-In in produzione (non fattibile da codice)

Il client OAuth **Android** della Google Cloud Console è legato a package + SHA-1 della chiave di
firma. Con Play App Signing l'app installata dallo store è firmata con la chiave di Google, non con
quella di upload né con quella di debug:

1. Play Console → l'app → Test e rilascio → **Integrità dell'app** (App integrity) → "Firma delle app":
   copiare lo **SHA-1 del certificato di firma** (e anche quello del certificato di upload).
2. Google Cloud Console → API e servizi → Credenziali → il client OAuth **Android** `com.michele.eurocoins`:
   aggiungere (o creare un secondo client Android con) lo SHA-1 della chiave di firma di Play. Lo SHA-1
   della chiave di DEBUG di questa macchina è `84:FF:C7:96:40:B6:51:AC:10:64:4A:08:EC:CA:EE:03:22:7E:02:81`
   (serve alle build locali: verificare che ci sia già nel client, il login in debug funziona oggi).
3. **Schermata di consenso OAuth** → portarla da "Test" a **"In produzione"**: in test i token scadono
   dopo 7 giorni e può accedere solo chi è tra gli utenti di test. Lo scope `drive.appdata` è
   "non sensibile": di solito non richiede la verifica di Google, ma va controllato in console.
4. Provare il login e il backup da una build installata da Play (test interno), non da quella locale.

**Stato della checklist (2026-10-08): passi 1-3 FATTI, il 4 NO.** Progetto Google Cloud **"Euro App"** (Google Auth Platform):
- **Passo 1-2**: SHA-1 del certificato di FIRMA di Play `83:55:2F:28:6B:D1:26:06:F8:E0:68:97:BD:51:19:5C:75:EF:F0:4A` (la si trova in Play Console
  → Protetto con Play → "Proteggi la chiave di firma dell'app" → "Gestisci la firma dell'app di Google Play"; la voce "Integrità dell'app" è stata spostata
  lì e non contiene più la firma). Registrata in un **secondo client OAuth Android** (un client Android ammette UN solo SHA-1, quindi non si modifica
  quello esistente): pacchetto `com.michele.eurocoins`. Il client Android originale (SHA-1 di debug) e quello "Applicazione web" (`google.webClientId`)
  NON vanno toccati. La SHA-1 della chiave di CARICAMENTO è `0C:61:5E:9F:C2:77:70:4D:C6:BB:1D:97:E1:7B:50:17:1F:45:05:04` e non serve. Un client nuovo può
  impiegare **alcune ore** per attivarsi.
- **Passo 3**: schermata di consenso **In produzione** (era "Test": solo utenti di prova elencati e accesso che scade dopo 7 giorni). Per poter premere
  "Pubblica app" non bastavano nome app e email: **Google ha richiesto anche "Home page" e "Link alle norme sulla privacy"** (senza asterisco ma
  necessari, finché mancavano il pulsante restava grigio con il messaggio "completa la configurazione nella pagina Branding") e il dominio
  `mmorelli1656.github.io` tra i **Domini autorizzati**. Entrambi i link puntano a `https://mmorelli1656.github.io/euro-coins-app/privacy/` (la radice del
  sito dà 404: non esiste una vera home page). Nome mostrato "Euro Coins", email di assistenza e di contatto `jacko1656@gmail.com`; nessun logo (un logo
  avvierebbe la verifica del marchio) e nessun termine di servizio. Il permesso `drive.appdata` è non sensibile: nessuna verifica di Google richiesta.
- **Passo 4 (da fare)**: provare login e backup da un'app installata da Play, dopo qualche ora dall'attivazione del client. Non verificato finora.
- Nota: `keytool` su questa macchina (lingua italiana) fallisce con `MissingFormatArgumentException`: aggiungere `-J-Duser.language=en`.

### Stato della pubblicazione (aggiornato il 2026-10-07)

**Fatto**
- Account sviluppatore Play (nome pubblico `mik1656`), app "Euro Coins" creata in Play Console con
  package `com.michele.eurocoins`, gratuita. Da verificare che sia stata creata come "App" e non come
  "Gioco" (i testi della Dashboard dicono "gioco"; la voce "Impostazioni dello store" si vede solo dopo
  le attività iniziali).
- **Test interno**: caricato `euro-coins-1.0-vc1.aab` (versione 1, 1.0), tester = lista "Io" (l'email
  del Play Store del telefono). Un solo avviso, innocuo: mancano i simboli di debug del codice nativo
  (si possono aggiungere più avanti per leggere meglio crash e ANR). Play App Signing accettato.
- **Prodotto Pro**: "Prodotti a pagamento singolo" → ID prodotto `euro_coins_pro` (verificato identico
  a `BuildConfig.PRO_PRODUCT_ID`), nome "Euro Coins Pro", opzione di acquisto `acquisto-pro` (tipo
  "Acquista", con "compatibilità con le versioni precedenti"), ATTIVO in 174 paesi. **Prezzo: 2,99 €**
  deciso dal proprietario, con i prezzi ritoccati a mano per paese perché Google aveva letto il 2,99
  come NETTO e aggiunto l'IVA (Italia 3,69 €): verificare in tabella che Italia e area euro mostrino
  il prezzo finale voluto. Il prezzo si cambia dopo senza ricreare il prodotto, vale per i nuovi acquisti.
- **Profilo pagamenti** completato (nome sull'estratto conto "EURO COINS APP"; dati fiscali e conto
  bancario inseriti dal proprietario, non passano da qui). **Registrato alla commissione di servizio del
  15%** (gruppo di account creato, nessun account associato). Questioni fiscali (partita IVA, imposte
  sugli incassi): il proprietario deve sentire un commercialista, l'assistente non può consigliare.
- **AdMob**: app e unità create, messaggio GDPR (UMP, Google CMP) **pubblicato** il 2026-10-07 con
  "Nega il consenso" in primo piano, link alla privacy policy, elenco predefinito di 198 partner e
  "aggiungi automaticamente le origini annuncio"; l'anteprima diceva "0 partners" ma l'elenco è
  quello predefinito. Il modulo può comparire nell'app fino a un'ora dopo.
- **Privacy policy** pubblicata (vedi sopra) e ID AdMob veri già nell'app (`euro-coins-1.0-vc1.aab`).

**Aggiornamento serale del 2026-10-07**
- Scheda dello store compilata (EN), icona, immagine in primo piano e screenshot caricati (file in
  `C:\Users\mik16\EuroCoins-release\store\`, 9:16 per i tablet in `tablet-9x16\`); categoria "Libri e
  consultazione" (l'app era stata creata come "Gioco" e il proprietario l'ha corretta in "App"); pubblico
  13+; classificazione contenuti, annunci, sicurezza dei dati, ID pubblicità e le altre dichiarazioni sono
  compilati e in "Modifiche non ancora inviate per la revisione" (si inviano solo dopo i passaggi del test chiuso).
- **BLOCCO ATTUALE: test chiuso.** L'account è personale e recente: servono 12 tester attivi per 14 giorni
  CONSECUTIVI prima di poter richiedere la produzione (se scendono sotto 12 il conto riparte). Il pulsante
  "Invia app per la revisione" resta grigio finché non si configura il canale "Test chiusi - Alpha" (paesi,
  tester, release; riusare l'.aab 1.0 da libreria). **Il proprietario non ha ancora i tester e ha
  sospeso questa parte** ("mi è passata la voglia"): non insistere, riprendere solo se lo chiede.
  Valutato AppHive (scambio di test tra sviluppatori, Google Group `apphive-testers@googlegroups.com`):
  probabilmente 16 app sempre le stesse da aprire ogni giorno per 14 giorni, da NON fare sul profilo
  principale (telefono personale con la collezione vera); non verificato. Servizi a pagamento: sconsigliati.
  Alternativa: persone conosciute e gruppi di collezionisti.
- Il codice dell'app NON è cambiato oggi dopo `euro-coins-1.0-vc1.aab` (versionCode 1): ogni nuovo .aab
  per Play deve avere versionCode > 1 (e `-PallowTestAds` NON va usato per quello da pubblicare).

**Da fare, in quest'ordine**
1. **Dichiarazioni dell'app** (Dashboard di Play Console): privacy policy (URL sopra), annunci = sì,
   accesso all'app = nessun login obbligatorio (il login Google è facoltativo, solo per il backup),
   classificazione dei contenuti (questionario), pubblico di destinazione = non bambini, e la **scheda
   Data safety** (identificatore pubblicitario e dati AdMob, acquisti, email per il login, nessuna
   analisi propria; coerente con la privacy policy).
2. **Prova del modulo di consenso sul telefono** con la release che ha gli ID veri (`rel4.apk` nello
   scratchpad della sessione, già costruita ma NON installata; la release `rel3` oggi sul telefono ha gli
   ID di test). Non cliccare mai sul banner con gli ID veri; meglio registrare il telefono come
   dispositivo di test in AdMob.
3. **Prova dell'acquisto vero** dal link del test interno, con il proprio account come tester di licenza.
4. **Test chiuso**: se l'account è personale e recente, 12 tester per 14 giorni prima della produzione.
5. Dopo la pubblicazione: collegare l'app alla scheda dello store in AdMob; checklist OAuth/SHA-1 di
   Play App Signing (§ Checklist OAuth sopra) e schermata di consenso OAuth in produzione.
6. Scheda dello store (testi EN/IT, icona 512, grafica 1024×500, schermate), `versionCode` più alto per
   ogni nuovo .aab.

### Cosa resta in Play Console (fuori da questo repo)

- Creare l'app, il prodotto in-app `euro_coins_pro` (non consumabile, prezzo) e attivarlo; aggiungere i
  tester di licenza per provare l'acquisto; collegare l'app all'ID app AdMob e creare le unità annuncio
  (banner e interstitial, poi i loro ID in `local.properties`); configurare il messaggio GDPR in AdMob
  (Privacy e messaggi) e `app-ads.txt` se c'è un sito.
- **Privacy policy: FATTA il 2026-10-07**, pubblicata con GitHub Pages (cartella `docs/` di `main`, sorgente `docs/privacy/index.html`, EN + IT) all'URL `https://mmorelli1656.github.io/euro-coins-app/privacy/`: è quello da incollare in Play Console. Contatto pubblico `jacko1656@gmail.com`, sviluppatore "mik1656". **Va aggiornata** (e la data in cima ritoccata) se l'app cambia: nuovi SDK, analisi dei crash, nuovi dati. Restano la scheda **Data safety** classificazione dei contenuti; categoria;
  materiali dello store (icona 512, grafica in primo piano, schermate).
- **Test chiuso**: con un account personale recente servono almeno 12 tester per 14 giorni prima di
  poter richiedere l'accesso alla produzione.
- Dati: le commemorative del 2026 mancano nel dataset (lavoro della pipeline).
- Accessibilità: ci sono ~24 `contentDescription = null` da rivedere; non verificati tema chiaro, font
  ingrandito, landscape e TalkBack sulla build di rilascio.
- **Licenze**: foto BCE in hotlink ("uso editoriale") e dati Numista nell'asset e nel repo pubblico:
  decisione del proprietario del 2026-10-06 (§ Backlog, "Gate da ricontrollare").

### Non provato con la release ottimizzata (da fare prima di pubblicare)

Rotazione e ripresa dell'app, acquisto vero (§ Pro e pubblicità), salvataggio di una moneta dal
pannello (si è solo aperto e annullato, per non toccare la collezione reale), backup/ripristino su Drive (il parsing di `BackupFile` sotto R8 è coperto solo dalle regole,
non da una prova: serve un backup vero), `-ExcludeNumista`.

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

- **613 monete dal 2026-10-08** (584 + 29 del 2026). **Nota nella lista dell'anno 2026** (`InfoNote`, `ui/components/`: icona "i" `primary` da 16 dp + testo grigio da 13 sp, senza riquadro; scelta dopo mockup, testo C): "More coins and details are on the way." sotto "N coins", solo per `CoinFilter.Year(2026)` senza `commonOnly` (`IN_PROGRESS_YEAR`/`IN_PROGRESS_NOTE` in `CoinListScreen.kt`, `headerNote`); a 2026 completo si cancellano le due costanti. Scartati i testi più lunghi (andavano a due righe) e "coming soon". **Come sono entrate**: la BCE non ha ancora la pagina
  2026, quindi la pipeline le ha prese dagli emittenti nazionali (`fonte_dati = "emittente_nazionale"`,
  mostrato come "National issuer" nei crediti; `eurlex_notice` = "EU Official Journal" quando usciranno gli
  avvisi UE). **Senza foto** (`url_immagine_fonte` nullo: segnaposto `€`), **senza dati Numista** (tirature per
  finitura, zecca, incisore: "—"), e **TUTTE con `tiratura` nulla dal 2026-10-09** (scelta del proprietario:
  meglio "—" che un numero non vero; quello degli emittenti nazionali è un volume annunciato o un
  contingente, es. Germania 30 milioni, e la riga Standard lo mostrava come tiratura; 22 azzerate a mano
  nel `coins.json`, 7 lo erano già: **rifacendo l'export vanno azzerate di nuovo**, `NewCoinNotesTest`
  fallisce se tornano). Tolti anche i segni editoriali tra parentesi quadre dalle due note di San Marino
  ("[...]", "[from …]", stesso test). **Il testo sul "contingente autorizzato" non rimanda più a `NOTES.md`
  della pipeline** (era una nota di sviluppo mostrata all'utente, anche sulle monete vecchie). **Le note delle 2026 sono state sistemate nel dato**, non in
  visualizzazione (scelta del proprietario: la regola "iniziale maiuscola e punto finale" vale solo per le nuove
  2026, le 584 vecchie restano com'erano): 22 note con testo; 12 erano in spagnolo/francese/italiano (7 con
  prefisso `[es]`/`[fr]`/`[it]`, 5 riassunti italiani "Sintesi…" della pipeline) e sono state **tradotte in
  inglese a mano** (traduzione nostra, non della fonte), 6 solo maiuscola (4 già a posto). Le traduzioni sono
  solo in `coins.json`: `ecb_coins.jsonl` ha ancora gli originali, quindi **rifacendo l'export vanno
  riapplicate** (`NewCoinNotesTest` fallisce se tornano testi non inglesi o minuscoli). 7 monete 2026 non hanno
  nota (Malta x2, Italia Collodi, Lussemburgo x2, Vaticano x2). Mancano ancora Lituania, Grecia, Belgio, Andorra, Monaco, Lettonia (elenco in
  `NOTES.md` della pipeline). **`coins_with_mintages.jsonl` NON include ancora le 2026** (il passo Numista
  richiede Python e la chiave API, non disponibili qui): l'export di `coins.json` è stato fatto unendo a mano
  i 584 record di `coins_with_mintages.jsonl` con i 29 record 2026 di `ecb_coins.jsonl` (PowerShell,
  `@($m) + @($new)`). **Non usare `ecb_coins.jsonl` da solo**: `scrape_ecb.py` lo riscrive da zero e perde tutti
  i campi Numista. Quando la pipeline rigenera `coins_with_mintages.jsonl` con le 2026 (e le loro foto), si
  riesporta col comando di § Come i dati arrivano nell'app. Le chiavi di collezione dei record vecchi non
  cambiano.
- **584 monete (fino al 2025)**, non 499: include le 85 delle 5 emissioni congiunte
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
- **Foto duplicate (controllo del 2026-10-09)**: hash del contenuto di tutti gli 877 URL immagine
  (commemorative e Regular Issues). **Un solo errore vero: Monaco 2025**, dove la pagina BCE usa
  `comm_2025/Monaco.jpg` (il Marquisat des Baux) anche per il Comté de Carladès: nel `coins.json`
  il Carladès è stato lasciato **senza foto** (`url_immagine_fonte` nullo, `immagine_placeholder`
  vero, licenza "Sconosciuta - da verificare", come i 4 del Vaticano) finché la BCE non pubblica la
  sua. **Rifacendo l'export dalla pipeline va riapplicato** (`CoinImageDuplicatesTest` fallisce se
  due monete tornano a condividere una foto); da correggere anche nella pipeline. Gli altri
  duplicati sono legittimi: tagli diversi con lo stesso disegno nazionale (Bulgaria 2/5 cent,
  Vaticano serie 5 1/2/5 cent, Monaco serie 1 10/20 cent, i file di gruppo BCL del Lussemburgo per i
  centesimi: monete singole, solo quello di 1 e 2 euro ne ha due, già gestito). Nota a margine: le
  foto BCE della Bulgaria sono da ~7000×7000 px (non 540), pesanti da scaricare.
- **Distinto da quanto sopra**: un `urlImmagineFonte` presente ma che
  fallisce il caricamento a runtime (link scaduto, rete assente) usa la
  STESSA icona `€` (elenco e dettaglio: prima il dettaglio mostrava un
  `MonetizationOn` a dollaro e l'elenco un vuoto offline). La distinzione sta
  solo nel testo del dettaglio: "Image not yet published by the source"
  (dato) contro "Couldn't load this image" (rete/link); mentre carica, o se
  il caricamento resta in sospeso senza rete, solo l'icona senza testo. Misure: 26 dp nell'elenco;
  **nel dettaglio (commemorative e Regular Issues) dal 2026-10-09 è un TONDO lilla con l'euro bronzo**
  (`HeroPlaceholder`, `ui/detail/HeroPlaceholder.kt`, variante B dopo mockup: tondo al 76% della larghezza
  della foto, euro al 45% del tondo, messaggio sotto), come nell'elenco ma in scala grande, per
  uniformarsi al resto dell'app; prima era l'euro nudo al 40% della larghezza (22,5% ancora prima).
  Colori FISSI (`LilacLight`/`BronzeLight`, non del tema): la Hero card è bianca anche nello scuro e il
  lilla scuro starebbe male su bianco. Scartati il tondo al 58% (più piccolo della moneta che
  sostituisce) e al 96% (un disco lilla pieno e pesante, con il messaggio costretto dentro). Vedi
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
  con il trailer `Co-Authored-By`. **Dal 2026-10-07 (app in pubblicazione su Google Play) NON si
  pubblica più direttamente su `main`**: ogni chat lavora su un ramo/worktree suo e NON fa merge né
  push su `main` senza che il proprietario lo chieda esplicitamente in quella chat (un'approvazione
  data in un'altra chat non vale). Motivo: `main` è ciò da cui si costruisce il pacchetto per Play, e una
  modifica non voluta o non provata finirebbe in produzione. Il proprietario fa il merge da sé o
  autorizza la chat a farlo.
- **Modifiche all'app che devono arrivare agli utenti** (grafica, funzioni, correzioni): dopo il merge
  serve un NUOVO pacchetto per Play con `versionCode` PIÙ ALTO di quello caricato (oggi 1 → 2, e così via;
  `versionName` a piacere), costruito senza `-PallowTestAds` e firmato con la chiave di upload (§ Rilascio).
  Ricordarlo al proprietario a fine lavoro: fino al nuovo .aab, la modifica resta solo nel repo. Prima di un
  nuovo .aab: test, release di prova sul telefono firmata con la chiave di debug (§ Rilascio), e mai
  disinstallare l'app dal telefono.
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
`ProStateTest` (proprietà del Pro, una verifica fallita non lo toglie, pagamento in sospeso) e
`InterstitialPolicyTest` (6 monete E 30 secondi, né una né l'altra da sole, più l'orologio portato indietro) coprono la logica pura di Pro e
annuncio a tutto schermo; il billing e gli annunci veri non sono testabili in JVM. Oggi 290 test, nessuno saltato (`CoinDesignersTest`: autore certo vs probabile, 319 designer nel catalogo; `CollectionMintsTest`: salvataggio per zecca, Quick add, backup v5; `CollectionMigrationTest`: SQL della migrazione 10-11 contro lo schema di Room) (`MintLettersAssetTest` incrocia `mint_letters.json` con gli altri due asset, `MintLetterTableTest` la tabella e i numeri compatti).
`UnsharpMaskTest` copre `unsharpMask()` (immagine uniforme invariata, intensità 0 = identità, bordo
netto accentuato dai due lati e invariato lontano, valori nel campo 0-255, alfa intatta); il
rendering con `Bitmap` di `ThumbnailSharpen` non è testabile in JVM.
`BackupFileTest` copre il formato v2 (andata e ritorno con varietà e prezzo, un v1 che si legge
ma non porta Regular, un v2 con Regular vuota che invece sostituisce, qualità sconosciuta saltata,
versione più nuova rifiutata). `BackupStatusTest` copre il confronto con l'ultimo backup (ordine
irrilevante, aggiunte/tolte/modificate, finiture, chiave delle Regular, reset, istantanea v1) e la
rotazione della versione precedente; `AutoBackupPolicyTest` le regole del salvataggio automatico
(mai vuota, solo `Pending`, intervallo minimo, interruttore predefinito).
`MicrostatesTest` legge il `coins.json` vero e controlla che i nomi in
`MICROSTATE_PAESI` esistano (24 paesi -> 20 nascondendoli) e che `stableKey` sia
unica. `RegularMintageSummaryTest` copre `summarizeMintages()`/`groupMintagesByYear()`
con dati sintetici: somma vs. anno singolo, qualità che non si mescolano, ordine
crescente, anni con più qualità che si fondono in una riga sola, somma oltre `Int`
(miliardi), didascalia "N of M years". `RegularIssuesAssetTest` legge il
`regular_issues.json` vero con le stesse classi dell'app (se lo script cambia forma o un
campo nuovo perde il default, il test lo dice prima del crash all'avvio sul telefono) e
fissa le scelte di abbinamento: Vaticano 2005 diviso tra le serie 1 e 2, Belgio a tre
intervalli senza sovrapposizioni, Germania 4 miliardi, zecche come paese e deduplicate, zecca per anno
(Lussemburgo/Slovenia con etichette, Italia/Germania/Austria senza; Grecia 2002 divisa, solo quella).
`RegularYearMintsTest` e
`MintNamesTest` coprono `yearMintLabels()` e la mappa zecca → paese; `RegularVarietiesTest` la tabella EFS. `RegularSeriesDenominationsTest` (+ `RegularSeriesWindowTest`, nello stesso file) fissa gli 8 tagli di Francia serie 2/3 e Spagna serie 3, il Vaticano 2026 che non eredita ma ha le sue 8 righe, e le finestre senza sovrapposizioni (5 cent francese 1999-2021 / 2022-2023 / 2024-oggi); `SeriesPeriodTest` i periodi dei titoli; `RegularIssueTextTest` la frase tolta dalle descrizioni; `RegularIssueImagesTest` il segnaposto del Lussemburgo 1/2 euro (solo quelle due immagini cambiano nel catalogo); `RegularProgressTest` i conti 24/24/48 e il totale 328; `RegularDenominationRowsTest` le righe per taglio (41 ciascuno, ordine, finestre, ricerca); `SeriesTextLabelTest` (nello stesso file di `SeriesPeriodTest`) le etichette "ABOUT SERIES 1–3". Gli unit test che leggono gli
asset non si rilanciano da soli se cambia l'asset: `:app:cleanTestDebugUnitTest`. Non ci sono test
di UI
né di backup (serve un account Google reale). Il lint non gira offline
(`lint-gradle` non è in cache): serve la rete.

## Verifica su emulatore e telefono

Non descritta nei file di build, utile per non rifare gli stessi giri. **Richiesta esplicita del proprietario (2026-10-06): niente emulatore, solo il telefono** (la sezione sull'emulatore sotto resta come riferimento storico). Il telefono è condiviso con altre sessioni e si blocca da solo: vedi § Rilascio.


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
  64 dp (da qui il precaricamento); WebP non serve finché non le serviamo noi.
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
- **Visibilità delle miniature negli elenchi** (commemorative e Regular Issues, 2026-10-04).
  Misurato prima di intervenire: foto BCE 270×270, la moneta ne occupa il 97-98% (niente margine
  da ritagliare); la miniatura ne usa ~140-190 px fisici, quindi il limite non è la risoluzione ma
  la grandezza sullo schermo; le monete sono chiare (luminanza media 187-213/255) su una card
  bianca e il contorno si perdeva. Quattro interventi: (1) **miniatura 64 dp, card 80 dp** (era 52/72:
  +23% di diametro, ~10% di densità dell'elenco in meno), l'unico che aggiunge dettaglio vero;
  (2) **anello da 1 dp** (`onSurfaceVariant` al 35%, `CoinThumbnailRing` in `ui/components/`)
  disegnato SOPRA la foto e solo a foto caricata (non sul segnaposto lilla); (3)
  **`FilterQuality.Medium`** (`ThumbnailFilterQuality`) sulle `SubcomposeAsyncImage` delle miniature:
  Coil non riduce da 270 px alla misura del riquadro (precisione inesatta), la riduzione la fa
  Compose e con la qualità bassa di default era morbida (ipotesi, valutata a occhio sul
  telefono: "va meglio"); (4) **nitidezza locale + contrasto 1.2** (2026-10-06, dopo i primi tre).
  Il primo tentativo fu il solo contrasto globale (`ColorFilter` con `ColorMatrix`, `THUMBNAIL_CONTRAST`
  1.2, poi 1.35: troppo poca differenza, "funziona meglio sulle monete già luminose"; scuriva ancora
  le ombre delle monete scure, dove i dettagli già si perdevano). Confronto su 14 monete prese dalla
  cache Coil del telefono, simulato sul PC con la stessa matrice (originale / contrasto 1.2 / solo
  nitidezza / nitidezza + 1.2): la nitidezza locale (unsharp mask) rende leggibili scritte, stelle e
  rilievi, il contrasto globale no. **Scelta dell'utente: nitidezza + 1.2** (il parere dell'assistente
  era la sola nitidezza, differenza marginale: l'anello esterno delle bimetalliche si scurisce un po').
  `ThumbnailSharpen` (`ui/components/`, `coil3.transform.Transformation`): riduce la foto alla misura
  richiesta (168 px sul telefono dell'utente, mai ingrandisce) e SOLO DOPO applica
  `unsharpMask()` (`UnsharpMask.kt`, logica pura testata: due passate del kernel 3×3 gaussiano, sigma
  ~1,2 px, `THUMBNAIL_SHARPEN_AMOUNT` = 0.9, l'alfa non cambia): fatta a 270/540 px verrebbe sfocata
  dal ridimensionamento successivo di Compose. Il risultato sta nella cache in memoria di Coil (la
  chiave contiene la trasformazione). Il contrasto resta un `ColorFilter` (`ThumbnailColorFilter`,
  perno sui chiari a 170/255 e non a 128, altrimenti i chiari si schiarirebbero verso il bianco dello
  sfondo; `THUMBNAIL_CONTRAST` = 1.2, 1.0 lo spegne, oltre ~1.4 l'argento brucia). Solo le
  miniature degli elenchi; nelle Regular Issues la nitidezza viene dopo `RegularIssueImageTrim`;
  `PrefetchThumbnails` non ha la trasformazione (precarica solo la cache su disco). Valori da
  ritoccare: `THUMBNAIL_SHARPEN_AMOUNT` (sopra ~1.2 compare un alone sui bordi) e `THUMBNAIL_CONTRAST`.
  Punto debole noto: nel ritaglio a fondo trasparente della 1 euro francese 2022 (`RegularIssueImageTrim`
  su sfondo nero) la nitidezza lavora anche sul bordo con alfa parziale: non verificato sul telefono.
  Le due liste hanno la STESSA misura per scelta dell'utente: provato
  68/60 dp con 4 dp tra le card per far stare le 8 monete di una serie in una schermata (calcolo:
  8 × 72 = 576 dp contro ~590 visibili sul suo telefono), poi scartato per tenere le liste
  identiche; **con 80/64 le 8 righe di una serie non ci stanno più tutte insieme** e l'ultima esce
  in parte. A 68 dp i titoli commemorativi su due righe sforerebbero (serve togliere il padding
  verticale della colonna di testo). Scartato il tocco prolungato per ingrandire (un'azione in più
  chiesta all'utente). Il boost di contrasto, scartato in un primo momento perché altera i colori
  reali della moneta, è stato poi provato su richiesta dell'utente e tenuto a 1.2 insieme alla
  nitidezza (vedi (4)). La fascia Home non è stata toccata.
  **(5) Foto grande del dettaglio: solo nitidezza 0.45, niente contrasto** (2026-10-06, su
  domanda dell'utente "ha senso anche nella moneta grande?"). Il problema lì è diverso dalle
  miniature: non la perdita di dettaglio per la RIDUZIONE ma la morbidezza di un INGRANDIMENTO (foto
  BCE commemorative da 270 px in un riquadro di ~840 px sul telefono dell'utente: circa 3 volte; le
  Regular Issues hanno 540 px, circa 1,5 volte). Il contrasto non si applica: a quella grandezza il
  bordo si legge già e alterare i colori reali dell'argento si nota molto di più (e in un'app da
  collezionisti è un difetto). `DetailSharpen` (stesso file di `ThumbnailSharpen`,
  `DETAIL_SHARPEN_AMOUNT` = 0.45) lavora sulla foto alla sua risoluzione ORIGINALE, senza
  ridimensionarla, e lascia l'ingrandimento a Compose: fatta dopo, gli aloni sarebbero larghi tre
  volte. Confronto su tre monete (Germania 2006 chiara e pulita, Finlandia EMU 2009 spenta, Turingia
  2022 scura da 540 px) con 0.4 e 0.6 sulla foto mostrata a 620 px: su quelle da 270 px 0.4 rende più
  definite scritte, stelle e rilievi senza aloni, a 0.6 compaiono contorni più duri; sulla Turingia
  (540 px) la differenza è minima e la nitidezza non schiarisce le ombre. Applicata a
  `CoinDetailScreen` (`CoinHero`) e `RegularDenominationDetailScreen` (dopo `RegularIssueImageTrim`);
  nelle Regular Issues dà poco perché le foto sono quasi sempre da 540 px. Non verificato sul telefono
  con un confronto affiancato.
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
  year…" / "Filter by country…"; per i tagli di Regular Issues "Filter by value…", la parola
  del suggerimento "filtered by value", perché "Filter by denomination…" si troncava di un
  carattere; nell'elenco di un taglio "Country, series, year…", più corto del vecchio "Country,
  series or year…" che si troncava a "yea…"); All e le liste cercano sempre in tema, paese e anno
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
- **Rifiniture dopo il giro in tema chiaro e scuro**: (a) il segnaposto del dettaglio senza
  foto (allora `CoinHeroFallback`, ora `HeroPlaceholder`, tondo lilla): l'icona aveva `fillMaxWidth(0.4f).aspectRatio(1f)` in QUESTO ordine, prima era invertito
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
  accettabili: titoli dell'elenco tagliati a una riga (card da 80 dp fissi) e email accorciata.
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
    **Stato "posseduta" in alto** (2026-10-06, variante A dopo mockup): `HeroOwnedBadge`, la stessa
    pillola `✓ OWNED` della card COLLECTION, sull'angolo in alto a destra della foto (anche nel
    dettaglio taglio Regular), perché la card COLLECTION sta ~1.700 px sotto e chi arrivava da una
    lista non vedeva se la moneta era posseduta senza scorrere. Sta DENTRO la pagina, quindi segue lo
    swipe del pager senza altro (verificato). **Sempre con i colori del tema chiaro** (`OwnedBadge(dark
    = false)`: verde `2F4A38`, testo bianco): la Hero è bianca fissa anche nello scuro e la versione
    scura (salvia chiaro) su bianco si leggerebbe male. La moneta è un disco inscritto nel quadrato
    della foto, quindi l'angolo è libero (a distanza ravvicinata ma senza sovrapposizione, visto sul
    telefono). Scartati: **B** solo spunta tonda (senza testo, si confonde con la casella delle
    liste), **C** pillola nella barra accanto al contatore (stretta con titolo e "1 / 584" e
    richiede di portare lo stato della pagina corrente fino alla `TopAppBar`), **D** pillola più
    bordo verdigris sulla Hero (secondo segnale per la stessa informazione). Non verificato nel tema chiaro.
    **Riga della licenza** (`FooterLine`): su UNA riga. Andava a capo lasciando "use)" da solo; un primo
    tentativo con `LineBreak.Heading` (interruzione bilanciata) la spezzava in due righe uguali, ma la
    causa era il margine: `SourceCredits` dava 8 dp per lato alla colonna (oltre ai 16 dp della pagina) e la
    riga ("License: Copyright of the issuing mint (editorial use)", ~330 dp a 13 sp) non stava nei 327 dp.
    Ora il margine sta solo sulla riga delle fonti. Con un credito in coda va comunque a capo; `Heading`
    resta per quel caso. Verificato sul telefono (Croatia).
  - **Etichette di sezione** identiche: `MINTAGES`, `COLLECTION`,
    `ABOUT THIS COIN` (`SectionLabel`: `labelLarge` Bold, maiuscolo, `linkColor()`). **Si chiamava
    "HISTORICAL NOTES", rinominata il 2026-10-04** per uniformarla alle Regular Issues: il testo
    BCE di una commemorativa descrive il disegno e le iscrizioni ("The design shows a
    CARABINIER…"), lo stesso genere dei testi Numista dei tagli, non storia; il nome veniva dal
    campo `noteStoriche`. Stessa etichetta nel dettaglio taglio; la serie è "ABOUT THIS SERIES" o
    "ABOUT SERIES 1–3".
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
    vedi § dataset. **Zecca = nome del paese della zecca** (`MintNames.kt`, `mintCountry()`:
    "Rome" → "Italy", "Monnaie de Paris" → "France", "Royal Dutch Mint" → "Netherlands",
    "Kremnica" → "Slovakia", "Royal Mint" → "United Kingdom"...), non il testo grezzo che
    mescolava città, istituzioni e nomi lunghissimi. Scelto il nome del paese e non
    l'aggettivo ("Italian"): prima implementato con gli aggettivi, poi cambiato su richiesta
    (coerenza con `displayCountry()`); il prezzo è che "Germany" su una moneta lettone si
    confonde col paese emittente, ma è la scelta dell'utente. Senza la parola "Mint" nel
    valore: l'etichetta DETAILS già la dice. Risponde a "dove è stata coniata", non a chi ha
    emesso: Vaticano e San Marino → "Italy", Lettonia → "Germany". Zecche multiple (unite
    dalla pipeline con "; ") → paesi distinti dopo la mappatura, separati da ", ": le 5 zecche
    regionali tedesche (mintmark A/D/F/G/J, su ogni moneta tedesca) collassano da sole in un
    solo "Germany", per questo non esiste più il conteggio "N mints". **Le stringhe grezze
    restano in `zeccaFisicaRaw` e nel dataset della pipeline** (la mappa agisce solo in
    visualizzazione: servono per usi futuri, es. zecca per anno); una zecca non in tabella
    compare grezza e `MintNamesTest`/`RegularIssuesAssetTest` falliscono finché non si aggiunge
    una riga. Il lato comune europeo
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
  - **ABOUT THIS COIN** (già HISTORICAL NOTES): `bodyMedium` 14 sp / 20 sp, `TextAlign.Justify` con
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
  - **Crediti** (`SourceCredits`, lo stesso formato delle Regular Issues): `Source: ECB ↗  Data:
    Numista N#… ↗` e sotto `License: … · Credit: …`. "Source" = testo e immagine BCE (link
    all'immagine, icona `OpenInNew`, `linkColor()`); "Data" = tirature, zecca e incisore, che
    sono Numista: l'attribuzione con N# mancava (la regola dei Termini API §4), il campo
    `Coin.numistaId` (colonna dalla v8, `numista_id` in `coins.json`) serve a questo. Le poche
    monete senza abbinamento Numista non hanno la voce "Data". La licenza resta il testo vero
    (non "Public domain"). **Leggibilità sul fondo chiaro** (2026-10-06): i crediti stanno sul
    fondo grigio-verde, non in una card bianca, e nel tema chiaro erano l'unico testo piccolo (12 sp,
    peso normale) scuro su un mezzo tono. Misurato sullo screenshot del telefono: testo ~9:1
    (inchiostro `1F2620` su `C0C8BC`) contro ~15:1 nel tema scuro, icone di link ~5:1 contro ~8:1; sopra le
    soglie WCAG, ma nel chiaro "affondavano" (il problema è il peso visivo, non il colore). Ora testo
    **13 sp Medium** (`SourceCredits`, `FooterLine` per la licenza, e la riga "Series text: …" di
    `RegularIssueCountryScreen`, stesso fondo) e icone **16 dp** con `creditIconColor()`
    (`LinkColor.kt`, verdigris `253A2D`, ~7:1; nel tema scuro resta il primario). `linkColor()` non è
    stato scurito: serve anche alle etichette di sezione e ai pulsanti testuali nelle card bianche,
    dove va bene. Scartati: un inchiostro più scuro (da 9 a 11:1, esce dalla palette) e, come
    ripiego se non bastasse, i crediti in una card bianca (15:1 come il resto, ma un blocco più
    pesante di quanto serva a una riga di fonti). Non verificato con uno screenshot reale del tema scuro.
  - Tocco sulla foto per ingrandirla: non c'è nel dettaglio (solo nell'elenco).
  - **Nitidezza leggera sulla foto grande** (`DetailSharpen`, 0.45, alla risoluzione originale
    prima dell'ingrandimento; niente contrasto): vedi § Decisioni di prodotto, "Visibilità delle
    miniature", punto (5).
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
  gestione di `possibileIncongruenza` in UI.
- **Collezione su Regular Issues** (§ omonima): export CSV ancora assente. Il backup su Drive e il
  reset la coprono dal v2 (§ Backup su Google Drive). Il problema della deduplicazione dell'anno
  scritto a mano non esiste più (anno scelto da lista).
- **Regular Issues: spostare l'abbinamento nella pipeline.** Oggi l'unione dei tre file
  (type Numista → serie per anni, § MINTAGES) vive in `scripts/export-regular-issues.ps1`
  perché su questa macchina non c'è Python; è logica sui dati, quindi il posto giusto è la
  pipeline (un `ec_national_sides_enriched.jsonl` come `coins_with_mintages.jsonl`, e questo
  repo tornerebbe a un export diretto). Da farlo quando Python è disponibile. Da sistemare
  lì anche: conflitto Monaco 2025 cent (Numista vs BCE), 2 euro 2026 del Lussemburgo e 2 euro
  2022 della Francia non ancora su Numista, Bulgaria senza type.
- **Regular Issues: righe "per anno" ancora non mostrate**: bucket `altro` di Numista (quasi
  sempre "In sets") e lettere di zecca per anno; il blocco DETAILS mostra la zecca del type
  principale, non quella di ogni anno.
- Note libere sulla collezione; valuta diversa dall'euro;
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
  finisce in `coins.json` **e in `regular_issues.json`** (da ottobre 2026 anche le
  divisionali portano dati Numista: tirature per anno, zecca, incisore, disegnatore e il
  testo del taglio; la regola 6 della pipeline dice esplicitamente "niente dati Numista
  nell'app senza autorizzazione scritta" — scelta consapevole del proprietario, stessa
  logica del rischio già accettato). Per le divisionali il piano B è già pronto:
  `scripts/export-regular-issues.ps1 -ExcludeNumista` — **ma dal 2026-10-05 l'asset committato è l'export COMPLETO** (decisione del proprietario, repo pubblico: `regular_issues.json` e `coins.json` su GitHub contengono dati Numista, rischio noto e accettato). Nell'app restano visibili "Source:
  Numista N#…" e il link (§4 dei Termini API).
  **Decisione del proprietario del 2026-10-06, per la pubblicazione su Google Play**: si assume lui la
  responsabilità delle licenze, cioè le foto BCE in hotlink (licenza "uso editoriale", mai ospitate) e i
  dati Numista nell'asset e nel repo pubblico. Non è un compito dell'assistente: il gate non è chiuso da un
  permesso ma da questa scelta. Il piano B per le divisionali resta pronto (`-ExcludeNumista`).
- Backup: fatto il confronto con la collezione locale ("Up to date"), il salvataggio automatico e
  la versione precedente su Drive (§ Backup su Google Drive). Non verificati end-to-end con un account
  Drive reale; il salvataggio automatico in particolare dipende dal sistema che lascia vivere il
  processo qualche secondo dopo l'uscita dall'app. Il Reset mostra nel dialog se la collezione è
  ripristinabile e da quando (`resetBackupNote`, `ResetBackupNoteTest`): due paragrafi (l'elenco "This removes the following coins:" e l'esito sul backup, con spazi non separabili dentro la data perché a capo non resti "07:44)" da solo, visto sul telefono). **Nessun consiglio**: c'era "Restore before adding new coins: the next backup replaces this one." e si è tolto (decisione del 2026-10-06): il dialog conferma un'azione voluta, la frase serviva a chi si pente dopo, e il testo non risolve il rischio vero, cioè che dopo un reset il backup automatico, appena si aggiunge una moneta, sovrascrive quello buono (la copia precedente su Drive resta solo se il file aveva più di 24 ore). Se il rischio preoccupa, il rimedio è nel comportamento (sospendere il salvataggio automatico dopo un reset finché non c'è un backup manuale o un restore), non nel testo: decisione di prodotto aperta. Prima era un blocco unico che chiudeva con la nota tecnica "(the previous version stays on Drive)", e la ripeteva anche senza backup. Verificato sul telefono solo il caso "backup aggiornato"; gli altri tre sono coperti dal test.
- Monetizzazione: fatti Play Billing, banner, annuncio a tutto schermo e consenso UMP (§ Pro e pubblicità);
  restano da provare end-to-end in Play Console. Funzioni Pro future (più esemplari della stessa moneta,
  quantità, export CSV) oggi NON esistono: il Pro toglie solo la pubblicità.

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
