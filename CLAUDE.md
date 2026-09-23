# euro-coins-app

## Scopo del progetto

App Android per sfogliare il catalogo delle monete da 2€ commemorative
dell'Eurozona (2004-oggi). Consuma il dataset prodotto dalla pipeline dati
in un repo separato — **[euro-coins-data-pipeline](../euro-coins-data-pipeline)**
— e non fa scraping né validazione dati di persona: quella responsabilità
resta interamente nella pipeline.

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
│   ├── CountryFlags.kt       # Coin.flagEmoji() — bandiera da codice ISO
│   ├── CollectionProgress.kt # Progress (x / y possedute)
│   ├── CoinKey.kt            # Coin.stableKey — chiave stabile per la collezione
│   ├── CoinQuality.kt        # Standard / BU / Proof
│   ├── CollectionItem.kt     # @Entity: moneta posseduta in una qualità
│   ├── CollectionDao.kt
│   └── backup/               # BackupFile, GoogleAccountManager, DriveBackupClient, BackupService
└── ui/
    ├── theme/                # palette "verdigris/bronzo" coerente col
    │                         # report di riconciliazione della pipeline dati
    ├── components/           # CollectionProgressBar, CollectionSheet (qualità + prezzo), ThemeModePill,
    │                         # PriceFormat, FloatingSearchBar (vetro/Haze), FilterSheet
    ├── home/                 # ingresso: due tile (commemorative / circolanti)
    ├── browse/               # commemorative: Years / Countries / All
    ├── list/                 # elenco filtrato (CoinFilter), CoinListOptions, ricerca
    ├── backup/               # schermata Backup (login Google, backup/ripristino su Drive)
    ├── detail/                # dettaglio moneta, licenza/attribuzione immagine
    └── navigation/           # home -> browse -> lista filtrata -> dettaglio; home -> backup
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
"Paese · Anno", tinta come quel testo (`MaterialTheme.colorScheme.primary`)
per leggersi come parte dell'etichetta invece che un accento nuovo. **Barra
flottante in basso** (`FloatingSearchBar`: pillola con ricerca + pulsante FILTER, sfondo vetro con blur reale via libreria Haze, `hazeSource` sulla lista/griglia sottostante; sotto Android 12 resta il solo fondo semitrasparente) in ogni scheda di Browse e in ogni lista filtrata; ogni scheda ha query e filtri propri. Il pannello FILTER (`FilterSheet`) contiene anche l'ordinamento (Years: dal più recente / dal 2004; Countries: A → Z / Z → A; liste: per anno o paese) più filtri Collection (All/Incomplete/Complete sulle griglie, All/Owned/Missing + qualità sulle liste); il pallino sul pulsante segnala un filtro attivo. Liste e griglie lasciano `floatingBarClearance()` di padding in fondo. La griglia (e ora anche l'elenco) torna in cima a ogni cambio d'ordine: lo stato di scorrimento si ricrea con `key(...)` nella stessa composizione, NON con un `LaunchedEffect`, che arrivava un fotogramma dopo e faceva vedere l'ordine nuovo scorso a metà (scritte che sembravano sovrapporsi). **Elenco monete**: il tocco sulla miniatura (area 60 dp, solo se la moneta ha la foto) apre `CoinImageDialog` (foto grande, paese · anno, tema, "Close"/"Details"); il tocco sul resto della riga apre il dettaglio. `PrefetchThumbnails` accoda in Coil le foto delle 24 monete oltre l'ultima visibile, così sono già nella cache su disco quando la riga arriva (le foto pesano ~130 KB l'una da BCE, vedi Decisioni di prodotto). Dettagli della barra non ovvi: alta 72 dp e larga quasi tutto lo schermo (margini 8 dp) per coprire per intero la riga sottostante; fondo molto opaco (0.94) perché con testo chiaro su fondo scuro il solo blur lascia il testo leggibile; sta in un Box esterno a schermo intero che assorbe i tocchi ("zona morta", `BarDeadZone` sopra + margine sotto) per non aprire monete vicine per errore (blocca anche il trascinamento iniziato lì). **Tastiera**: `MainActivity` ha `windowSoftInputMode="adjustNothing"` e la barra si solleva con `WindowInsets.ime`/`navigationBars` via `offset`, senza `imePadding()` e senza molle: con il ridimensionamento della finestra attivo l'altezza della tastiera veniva contata due volte, e una molla sopra l'animazione di sistema partiva in ritardo. La tile "Circulation" della home è
tratteggiata e senza azione finché la pipeline non produce quel dataset.
Il paese si passa in rotta come `Coin.paese` (valore stabile, non il nome
mostrato) con `Uri.encode`, perché "Città del Vaticano" e "Paesi Bassi"
hanno spazi/accenti.

### Home

Due tile che si dividono l'altezza dello schermo (non due card piccole con
spazio vuoto: era una critica esplicita). **Commemorative**: mosaico 3×3 di
monete reali (9 paesi diversi, prese dal database), titolo, "584 coins · 24
countries", intervallo di anni e barra "x / y collected" — tutti numeri
calcolati dal database, nessun valore scritto a mano. **Circulation**:
tratteggiata, "Coming soon", senza azione. La home è anche dove parte il
seeding del database (`HomeViewModel` chiama `ensureSeeded()`; il Mutex nel
repository evita il doppio inserimento se più ViewModel lo chiamano). In alto
a destra l'icona profilo porta alla schermata Backup.

## Collezione utente

Ogni moneta ha una **casella** nell'elenco (vuota / piena con spunta). Un
tocco apre un pannello dal basso (`CollectionSheet`) con le tre qualità —
**Standard, BU, Proof**, anche più di una insieme — e, per ciascuna
spuntata, il prezzo pagato (facoltativo, in euro, salvato in centesimi).
Lo stesso pannello si apre dal dettaglio ("Add"/"Edit" accanto al riepilogo
"My collection"), così c'è un solo modo di registrare.

- Il pannello lavora su una **bozza** e scrive solo con "Save": chiuderlo
  senza salvare non cambia nulla (un tocco sbagliato non cancella un
  prezzo). Alla prima apertura di una moneta non posseduta "Standard" è già
  spuntata.
- Una moneta conta come "posseduta" (casella piena, barre "x / y collected"
  di home/anni/paesi) se ha almeno una qualità.
- Il salvataggio sostituisce in blocco le qualità della moneta
  (`CollectionDao.replaceForCoin`, transazione) e conserva `addedAt` delle
  voci già esistenti.

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
- **Migrazioni Room esplicite** (DB versione 4, `CoinDatabase.kt`), mai
  `fallbackToDestructiveMigration`: distruggerebbe anche la collezione
  dell'utente. `MIGRATION_1_2` (tabella `collection_items`), `MIGRATION_2_3`
  (`coins.emissioneComune`), `MIGRATION_3_4` (`coins.tiraturaNumista{Standard,Bu,Proof}`,
  colonne nullable, niente `DEFAULT`). Ogni migrazione aggiunta va accodata,
  mai riscritta sopra una già rilasciata (anche in sviluppo: una volta
  installata su un telefono di prova, quel numero di versione è "usato"). Il
  valore delle nuove colonne conta poco: `ensureSeeded()` ripopola comunque
  `coins` da zero appena l'hash dell'asset cambia, la migrazione serve solo a
  far coincidere lo schema SQLite con l'entity Kotlin nel frattempo. Il SQL
  di ogni migrazione deve coincidere con quello generato da Room
  (`build/generated/ksp/.../CoinDatabase_Impl.kt`).
- Non ancora fatto: note libere, data di acquisto, valuta diversa
  dall'euro, export CSV.

### Backup su Google Drive

Schermata "Backup" (icona profilo in alto a destra nella home: icona
generica senza accesso, cerchio con l'iniziale dell'account con l'accesso
fatto): login con Google (Credential Manager) e backup/ripristino della
collezione su Drive.

- **UI** (`BackupScreen`): senza accesso una card d'invito + "Sign in with
  Google"; con l'accesso l'email, una card di stato in evidenza ("Collection
  saved" + data dell'ultimo backup, o "Not backed up yet", con barra di
  avanzamento durante le operazioni), i pulsanti Back up / Restore e in fondo
  "Sign out" (bordo e testo bronzo, l'accento secondario). Lo stato non
  dice "up to date": confrontare backup e collezione locale non è
  implementato, quindi non lo si afferma.
- **Banner "Go Pro"** (rimozione pubblicità, colore bronzo): oggi solo
  segnaposto, il tocco apre un avviso "coming soon". Non esistono ancora
  Play Billing, AdMob né consenso GDPR (UMP); l'app non è pubblica. Quando
  ci saranno: acquisto dal banner, banner nascosto per gli utenti Pro.

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
  `displayCountry()` normalizza solo questi 2 casi (chiave sul `paese`
  stabile), fallback a `zeccaRaw` per tutti gli altri. `paese`/
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

## Verifica su emulatore e telefono

Non descritta nei file di build, utile per non rifare gli stessi giri:

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
  sopra la foto già visibile (causa non chiarita). Solo icona di errore.
- **Tema: pillola Light / Dark / Auto nella barra della home** (accanto al
  profilo), Auto predefinito = segue il telefono. `ThemePreference` salva la
  scelta in SharedPreferences (`theme`); `MainActivity` riapplica
  `enableEdgeToEdge` a ogni cambio (altrimenti le icone delle barre di sistema
  seguono il tema del telefono e spariscono con un tema forzato). Scartati:
  selettore in Backup (nascosto), pillola a due stati (non si tornerebbe a
  "segui il telefono"). L'ordine è Light, Dark, Auto per scelta dell'utente.
- **Palette del tema chiaro: grigio-verde, non crema.** Il beige/crema faceva
  sembrare tutto piatto e con poco contrasto tra card e fondo (rapporto ~1.13).
  Ora fondo `D0D7CE`, card `FFFFFF` (bianco puro, come le foto BCE), outline
  `A9B3A8`, inchiostro `1F2620` (rapporto fondo/card ~1.47). L'outline è anche
  traccia delle barre di avanzamento. Un **lilla** (`LilacLight`, `E2D9F3`) resta
  come piccolo accento su segmento attivo del selettore, chip selezionati e cerchi
  delle monete senza foto (`secondaryContainer`). Il tema scuro non è stato toccato.
- **Card ovunque**: Years/Countries (bordo 1 dp) e **una card bianca per moneta
  nell'elenco** (angoli 14 dp, 8 dp tra le card): senza, le righe stavano
  direttamente sul fondo e l'elenco era piatto. La barra del titolo ha lo stesso
  colore dello sfondo (`appBarColors()`), altrimenti una fascia chiara spezzava
  la schermata.
- **Barra di ricerca flottante nel tema chiaro**: fondo meno opaco del tema scuro
  (0.88 contro 0.94) perché il blur si veda, ma non meno: a 0.72 il testo sotto si
  leggeva ancora; contorno scuro da 2 dp. Nel tema scuro resta 0.94 e bordo 1 dp.
- **Tipografia**: pesi alti su titoli ed etichette (`Type.kt`: headlineMedium e
  titleLarge Bold, titleMedium e labelLarge SemiBold): un serif Medium risultava
  sottile e le schermate senza gerarchia.
- **Dettaglio moneta = foto in card bianca + una card di dati + banner collezione
  + crediti in piccolo.** La card unica ha titolo, mintage (lista compatta
  Standard / BU / Proof, etichette vicine alle cifre, cifre a destra), divisore con
  16 dp sopra e sotto e note storiche. Il paese non c'è (è nell'header). La
  collezione è un **banner** verdigris con pulsante pieno Add/Edit, perché è l'unica
  azione. "Data source", licenza e credito dell'immagine sono nel piè di pagina; il
  link "Open source image" è verdigris scurito (`linkColor()`, 6:1) e sottolineato
  perché il verdigris del tema (4:1) sta sotto il WCAG AA per testo piccolo.
  **Mintage**: Standard/BU/Proof mostrano le tirature per finitura da Numista
  quando esistono, "—" quando mancano (mai una riga nascosta: non si sa se "manca
  il dato" o "la moneta non ha mai avuto quella finitura", nasconderla
  implicherebbe più certezza di quanta ce ne sia). Niente quarta riga "Other" per
  ora — vedi § dataset e § Backlog.
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
  Geometria dell'onda: lunghezza d'onda FISSA in dp (`WaveWavelength`, non
  proporzionale alla larghezza della barra — con "N creste sull'intera barra"
  a inizio riempimento si vedeva una sola gobba invece di un'onda fitta,
  perché nella parte colorata ci stava meno di una lunghezza d'onda intera).
  L'ampiezza è un valore separato dal riempimento (sale in 250 ms quando parte
  un riempimento, scende in 300 ms con curva ease-out quando finisce, verso lo
  0 = linea dritta). Years/Countries restano con la barra dritta di Material
  (`animation = null`): centinaia di onde insieme sarebbe rumore, non un dettaglio.
  **Tre curve diverse, non una sola**: `EmphasizedEasing` (cubic-bezier
  0.2,0,0,1) per l'allungamento della barra (durata 800 ms al primo avvio, 600
  ms per un aggiornamento) e per il pulse finale; `LinearEasing` per la fase
  dell'onda che scorre (1800 ms a ciclo — valori più bassi provati e scartati,
  "vibravano"); `EaseOutEasing` (cubic-bezier 0,0,0.58,1) per la dissolvenza
  dell'ampiezza a fine riempimento. **Non applicare lo scale/pulse al `Canvas`
  dell'onda** (solo al testo del conteggio): scalare in verticale un disegno
  che contiene un'onda la stira per un istante, ed è quello il "salto"/
  "sobbalzo" segnalato una volta dall'utente, non un difetto della sinusoide.

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
- Catalogo "Circulation" (serie divisionali) quando la pipeline lo produce.
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
  repo pipeline)**: le tirature Numista (`tiratura_numista_*`) sono state
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
