# euro-coins-app

## Scopo del progetto

App Android per sfogliare il catalogo delle monete da 2€ commemorative
dell'Eurozona (2004-oggi). Consuma il dataset prodotto dalla pipeline dati
in un repo separato — **[euro-coins-data-pipeline](../euro-coins-data-pipeline)**
— e non fa scraping né validazione dati di persona: quella responsabilità
resta interamente nella pipeline.

## Come i dati arrivano nell'app

1. La pipeline produce `data/processed/ecb_coins.jsonl` (uno schema
   pydantic `MonetaCommemorativa` per riga — vedi `src/models.py` in quel
   repo per il significato di ogni campo).
2. Quel JSONL viene esportato come array JSON unico in
   `app/src/main/assets/coins.json` (vedi comando sotto). **Non è generato
   automaticamente da uno script di questo repo** — va rieseguito a mano
   quando il dataset della pipeline cambia:

   ```bash
   python -c "
   import json
   records = [json.loads(l) for l in open(r'..\euro-coins-data-pipeline\data\processed\ecb_coins.jsonl', encoding='utf-8')]
   json.dump(records, open('app/src/main/assets/coins.json', 'w', encoding='utf-8'), ensure_ascii=False, separators=(',', ':'))
   "
   ```

3. Al primo avvio, `CoinRepository.ensureSeeded()` legge quell'asset e
   popola il database Room locale (`coins.db`) — solo se il DB è vuoto,
   quindi non sovrascrive dati già presenti a ogni avvio.
4. **Le immagini non sono bundlate nell'APK**: vengono caricate on-demand
   con Coil direttamente dagli URL originali della fonte
   (`Coin.urlImmagineFonte`, salvato così com'era in `data/processed/`),
   con cache su disco automatica di Coil dopo il primo caricamento. Scelta
   deliberata (vedi conversazione che ha avviato questo repo): APK leggero
   a scapito di richiedere rete la prima volta che si vede un'immagine.

## Stack tecnico

- Kotlin, Jetpack Compose (Material 3) — nessuna View/XML per la UI
- Room per la persistenza locale
- Coil 3 (`coil3.compose.AsyncImage`) per il caricamento immagini on-demand
- kotlinx.serialization per il parsing di `coins.json`
- Navigation Compose per la navigazione tra schermate
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
│   ├── CountryFlags.kt       # Coin.flagEmoji() — bandiera da codice ISO
│   └── CollectionProgress.kt # Progress + fakeOwnedIds() (PLACEHOLDER, vedi sotto)
└── ui/
    ├── theme/                # palette "verdigris/bronzo" coerente col
    │                         # report di riconciliazione della pipeline dati
    ├── components/           # CollectionProgressBar (barra "x / y collected")
    ├── home/                 # ingresso: due tile (commemorative / circolanti)
    ├── browse/               # commemorative: Years / Countries / All
    ├── list/                 # elenco filtrato (CoinFilter) + ricerca
    ├── detail/                # dettaglio moneta, licenza/attribuzione immagine
    └── navigation/           # home -> browse -> lista filtrata -> dettaglio
```

Navigazione: `HomeScreen` (start) → `BrowseScreen` (selettore Years /
Countries / All) → `CoinListScreen` filtrato per anno o paese (`CoinFilter`)
→ `CoinDetailScreen`. Years e Countries sono griglie di card; "All" è
l'elenco completo con ricerca. Years e Countries hanno un chip che inverte l'ordine (Years: dal più recente / dal 2004; Countries: A → Z / Z → A) e la griglia torna in cima a ogni inversione. La tile "Circulation" della home è
tratteggiata e senza azione finché la pipeline non produce quel dataset.
Il paese si passa in rotta come `Coin.paese` (valore stabile, non il nome
mostrato) con `Uri.encode`, perché "Città del Vaticano" e "Paesi Bassi"
hanno spazi/accenti.

**Barre "x / y collected" — dati FINTI per ora.** Home, card anno e card
paese leggono un `Set<Long>` di id posseduti da `fakeOwnedIds()`
(`CollectionProgress.kt`, una moneta ogni quattro). È un segnaposto in
attesa del punto "segna come posseduta": sostituire quella sola funzione
con la tabella di collezione reale e tutte le barre si aggiornano. Non
mostrare quei numeri come veri.

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
  `"Vatican City"` (14), `"Netherlands"` (3) vs `"The Netherlands"` (1).
  `displayCountry()` normalizza solo questi 2 casi (chiave sul `paese`
  stabile), fallback a `zeccaRaw` per tutti gli altri. `paese`/
  `zeccaEmittente` restano nell'entity Room e nella ricerca (come
  fallback, insieme a `zeccaRaw`) per compatibilità con lo schema
  pydantic della pipeline, ma non vengono più renderizzati direttamente.
- `licenzaImmagine` resta invece un **valore italiano** mostrato verbatim
  (es. `"Copyright zecca emittente (uso editoriale)"`): a differenza del
  nome paese, per questo campo la pipeline non salva un testo originale
  in inglese da cui attingere, quindi tradurlo qui richiederebbe
  costruire una mappa italiano→inglese duplicata rispetto all'enum
  `LicenzaImmagine` della pipeline, che si disallineerebbe silenziosamente
  ad ogni nuovo valore aggiunto là. Non lo facciamo.
- **Se in futuro serve l'italiano come lingua dei contenuti**, arriverà come
  dato aggiuntivo dalla pipeline (una nuova fonte/campo), non come
  traduzione automatica del dataset esistente — vedi il commento su
  `Coin.kt` (`tema`/`noteStoriche`). Fino ad allora non introdurre
  assunzioni tipo "un solo blob di testo per lingua" che renderebbero quel
  giorno più doloroso, ma non costruire infrastruttura i18n prima che serva
  davvero.

## Cose da sapere sul dataset (non ovvie dal codice)

- **499 monete**, non 504: le 5 emissioni congiunte dell'Eurozona non sono
  nel dataset per scelta della pipeline (non riconducibili a un singolo
  paese) — non aspettarti di trovarle.
- **San Marino 2012 manca** deliberatamente (gap noto, non un bug
  dell'app): vedi `NOTES.md` nella pipeline dati.
- **La tiratura per 7 paesi** (Italia, Slovacchia, Slovenia, Grecia,
  Germania, Lituania, Lussemburgo) è quasi certamente un contingente
  autorizzato, non la tiratura reale della singola moneta — l'app lo
  segnala in UI nel dettaglio moneta (`PAESI_TIRATURA_SOSPETTA` in
  `CoinDetailViewModel.kt`), ma se estendi quella logica altrove tienilo a
  mente. Dettaglio completo in `NOTES.md` nella pipeline dati.
- 4 monete hanno `immaginePlaceholder = true` (BCE non ha ancora
  pubblicato l'immagine reale): l'app lo gestisce mostrando un'icona al
  posto dell'immagine, non un errore.
- **Distinto da quanto sopra**: un `urlImmagineFonte` presente ma che
  fallisce il caricamento a runtime (link scaduto, rete assente) mostra
  un'icona diversa ("immagine non caricata", `Icons.Filled.BrokenImage`)
  invece del placeholder "non ancora pubblicata" — vedi
  `SubcomposeAsyncImage`/`AsyncImagePainter.State.Error` in
  `CoinListScreen.kt`/`CoinDetailScreen.kt`. La pipeline dati ha uno
  script (`scripts/validate_image_links.py`) che controlla periodicamente
  se qualcuno dei 495 URL è morto, per distinguere "capita raramente in
  rete" da "gap permanente nei dati" prima di documentarlo in NOTES.md.

## Setup

Richiede Android Studio con SDK 37 installato. `local.properties` (non
versionato) deve puntare al tuo Android SDK:

```
sdk.dir=C:\\Users\\<utente>\\AppData\\Local\\Android\\Sdk
```

```bash
./gradlew.bat :app:assembleDebug
```
