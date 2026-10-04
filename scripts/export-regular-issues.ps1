<#
.SYNOPSIS
    Esporta app/src/main/assets/regular_issues.json unendo i tre dataset della pipeline dati.

.DESCRIPTION
    Sorgenti (repo euro-coins-data-pipeline, data/processed/):
      - ec_national_sides.jsonl                    una riga per SERIE nazionale (testo EC, immagini BCE)
      - numista_divisional.jsonl                   un record per TYPE Numista (descrizione, incisore,
                                                   zecca, tirature per anno e qualità)
      - ecb_national_sides_coin_descriptions.jsonl testo BCE per (paese, taglio): ripiego di Numista

    Numista non conosce le "serie" dell'UE: spezza i type anche per i cambi del lato comune europeo
    ("1st map"/"2nd map") e, per alcuni paesi, in modo più fine (Belgio). Si abbina quindi per ANNI:
      1. di ogni serie si calcola l'intervallo [inizio, inizio_successiva - 1] (la prima serie parte
         da -infinito, l'ultima va a +infinito). L'inizio viene dall'anno nell'intestazione della serie
         ("2014 - second series"), oppure da $StartOverrides dove l'intestazione non lo dice.
      2. ogni type va per intero alla serie con cui ha più anni in comune (non riga per riga: il 2005
         vaticano esiste sia come Giovanni Paolo II sia come Sede Vacante, type distinti).
      3. tirature = unione delle righe dei type assegnati; descrizione/incisore/disegnatore/zecca
         = quelli del type con più anni nella serie ("type principale").
    Per ogni taglio partecipano all'intervallo solo le serie che hanno un'immagine di quel taglio
    (Francia 2022: solo 1 e 2 euro) o che non ne hanno affatto (Vaticano 2026: segnaposto che
    "assorbe" il type 2026 invece di lasciarlo alla serie precedente).

    Dove Numista non ha il type (Bulgaria, Monaco 2025 cent) si ripiega sul testo BCE, salvo per le
    serie la cui fonte è più recente della BCE (bcl = Lussemburgo 2026): lì meglio nessun testo che
    quello della serie precedente.

    -ExcludeNumista: esporta solo ciò che non viene da Numista (testo BCE, niente tirature/zecca (anche per anno)/
    incisore/disegnatore). Il gate sulla ridistribuzione di Numista è descritto in CLAUDE.md
    (§ Backlog) e in NOTES.md della pipeline: serve questo interruttore se andrà ripreso.
#>
param(
    [string]$PipelineDir = (Join-Path $PSScriptRoot '..\..\euro-coins-data-pipeline'),
    [string]$Output = (Join-Path $PSScriptRoot '..\app\src\main\assets\regular_issues.json'),
    [switch]$ExcludeNumista
)

$ErrorActionPreference = 'Stop'
$processed = Join-Path $PipelineDir 'data\processed'

function Read-Jsonl([string]$name) {
    Get-Content (Join-Path $processed $name) -Encoding UTF8 |
        Where-Object { $_.Trim() } |
        ForEach-Object { $_ | ConvertFrom-Json }
}

# Anno d'inizio delle serie la cui intestazione non lo contiene (chiave "paese|ordine_cronologico").
# Verificati sui type Numista (cambio di ritratto/stemma = nuovo type a quell'anno).
$StartOverrides = @{
    'Monaco|2' = 2006; 'Monaco|3' = 2025
    "Citt$([char]0xE0) del Vaticano|2" = 2005; "Citt$([char]0xE0) del Vaticano|3" = 2006; "Citt$([char]0xE0) del Vaticano|4" = 2014
    "Citt$([char]0xE0) del Vaticano|5" = 2017; "Citt$([char]0xE0) del Vaticano|6" = 2026
    'Lussemburgo|2' = 2026
}
# Serie la cui fonte è più recente della BCE: il testo BCE descriverebbe la serie PRECEDENTE.
$NoEcbFallbackSources = @('bcl', 'vaticanstate_cfn')
$NegInf = -9999
$PosInf = 9999

$series = @(Read-Jsonl 'ec_national_sides.jsonl')
$numista = if ($ExcludeNumista) { @() } else { @(Read-Jsonl 'numista_divisional.jsonl') }
$ecb = @{}
foreach ($d in (Read-Jsonl 'ecb_national_sides_coin_descriptions.jsonl')) { $ecb["$($d.paese)|$($d.taglio)"] = $d }

function Get-SeriesStart($s) {
    if ($s.ordine_cronologico -eq 1) { return $NegInf }
    $key = "$($s.paese)|$($s.ordine_cronologico)"
    if ($StartOverrides.ContainsKey($key)) { return [int]$StartOverrides[$key] }
    if ($s.intestazione_raw -match '(\d{4})') { return [int]$Matches[1] }
    throw "Anno d'inizio sconosciuto per $key (intestazione '$($s.intestazione_raw)'): aggiungerlo a `$StartOverrides"
}

function Get-Overlap([int]$a1, [int]$a2, [int]$b1, [int]$b2) {
    [Math]::Max(0, [Math]::Min($a2, $b2) - [Math]::Max($a1, $b1) + 1)
}

function Get-TypeYears($t) {
    $start = $t.anno_inizio; $end = $t.anno_fine
    if ($null -eq $start -or $null -eq $end) {
        $years = @($t.tirature_per_anno | ForEach-Object { $_.anno })
        if ($years.Count -eq 0) { throw "Type N#$($t.numista_id) senza anni" }
        $m = $years | Measure-Object -Minimum -Maximum
        if ($null -eq $start) { $start = $m.Minimum }
        if ($null -eq $end) { $end = $m.Maximum }
    }
    return @([int]$start, [int]$end)
}

# Intervalli: per (paese, taglio) le serie partecipanti, in ordine, con inizio e fine.
function Get-Participants([string]$paese, [string]$taglio) {
    $all = @($series | Where-Object { $_.paese -eq $paese } | Sort-Object ordine_cronologico)
    $part = @($all | Where-Object {
            $_.immagini.Count -eq 0 -or @($_.immagini | Where-Object { $_.taglio -eq $taglio }).Count -gt 0
        })
    $result = @()
    for ($i = 0; $i -lt $part.Count; $i++) {
        $end = if ($i + 1 -lt $part.Count) { (Get-SeriesStart $part[$i + 1]) - 1 } else { $PosInf }
        $result += [pscustomobject]@{ Series = $part[$i]; Start = (Get-SeriesStart $part[$i]); End = $end }
    }
    return , $result
}

# type -> serie assegnata, per (paese, taglio): chiave "paese|taglio" -> lista di {Type, Participant, Overlap}
$assignments = @{}
$unassigned = @()
foreach ($grp in ($numista | Group-Object { "$($_.paese)|$($_.taglio)" })) {
    $first = $grp.Group[0]
    $participants = Get-Participants $first.paese $first.taglio
    $list = @()
    foreach ($t in $grp.Group) {
        $years = Get-TypeYears $t
        $best = $null; $bestOverlap = 0
        foreach ($p in $participants) {
            $ov = Get-Overlap $years[0] $years[1] $p.Start $p.End
            if ($ov -ge $bestOverlap -and $ov -gt 0) { $best = $p; $bestOverlap = $ov }   # a parità, la serie più recente
        }
        if ($null -eq $best -or $best.Series.immagini.Count -eq 0) {
            $unassigned += "N#$($t.numista_id) $($t.paese) $($t.taglio) $($years[0])-$($years[1])"
            continue
        }
        $list += [pscustomobject]@{ Type = $t; Series = $best.Series; Overlap = $bestOverlap }
    }
    $assignments[$grp.Name] = $list
}

# Anni la cui tiratura la fonte divide tra zecche di PAESI diversi (oggi solo Grecia 2002: zecca
# nazionale + Parigi/Madrid/Finlandia per i pezzi aggiuntivi): per_zecca della pipeline, ridotto ai
# gruppi con una zecca nota e almeno una tiratura. Le zecche dello stesso paese non contano come
# divisione (5 zecche tedesche, due voci romane, "FI"/"Fi" finlandesi): qui basta riconoscere le
# tedesche, l'app applica comunque la regola completa con MintNames.kt (yearMintParts).
function Get-MintCountryKey([string]$mint) {
    if ($mint -match '^(Berlin|Munich|Hamburg Mint|State Mint)') { return 'Germany' }
    return $mint
}

function Get-MintSplit($row) {
    $groups = @($row.per_zecca | Where-Object {
            @($_.zecche_anno | Where-Object { $_ }).Count -gt 0 -and
            ($null -ne $_.tiratura_standard -or $null -ne $_.tiratura_bu -or $null -ne $_.tiratura_proof)
        })
    $countries = @($groups | ForEach-Object { (@($_.zecche_anno | ForEach-Object { Get-MintCountryKey $_ }) | Sort-Object -Unique) -join '+' } | Sort-Object -Unique)
    if ($countries.Count -lt 2) { return @() }
    $result = @()
    foreach ($g in $groups) {
        $s = [ordered]@{ zecche = @($g.zecche_anno | Where-Object { $_ } | Select-Object -Unique) }
        if ($null -ne $g.tiratura_standard) { $s['standard'] = [long]$g.tiratura_standard }
        if ($null -ne $g.tiratura_bu) { $s['bu'] = [long]$g.tiratura_bu }
        if ($null -ne $g.tiratura_proof) { $s['proof'] = [long]$g.tiratura_proof }
        $result += [pscustomobject]$s
    }
    return , $result
}

function Clean([string]$text) {
    if ([string]::IsNullOrWhiteSpace($text)) { return $null }
    return $text.Trim()
}

function Clean-Mints([string]$raw) {
    if ([string]::IsNullOrWhiteSpace($raw)) { return $null }
    $parts = @($raw -split ';' | ForEach-Object { $_.Trim() } | Where-Object { $_ } | Select-Object -Unique)
    if ($parts.Count -eq 0) { return $null }
    return ($parts -join '; ')
}

$qualityOrder = @('STANDARD', 'BU', 'PROOF')
$report = @()
foreach ($s in $series) {
    foreach ($img in $s.immagini) {
        $key = "$($s.paese)|$($img.taglio)"
        $mine = @()
        if ($assignments.ContainsKey($key)) {
            $mine = @($assignments[$key] | Where-Object { $_.Series.ordine_cronologico -eq $s.ordine_cronologico })
        }

        # tirature: unione delle righe dei type assegnati, una voce per (anno, qualità) non nulla
        $tirature = @()
        $seen = @{}
        foreach ($m in $mine) {
            foreach ($row in $m.Type.tirature_per_anno) {
                $values = @{ STANDARD = $row.tiratura_standard; BU = $row.tiratura_bu; PROOF = $row.tiratura_proof }
                foreach ($q in $qualityOrder) {
                    if ($null -eq $values[$q]) { continue }
                    $k = "$($row.anno)|$q"
                    if ($seen.ContainsKey($k)) { Write-Warning "Doppione $key $k (N#$($m.Type.numista_id)): tenuta la prima"; continue }
                    $seen[$k] = $true
                    $tirature += [pscustomobject][ordered]@{ anno = [int]$row.anno; quality = $q; tiratura = [long]$values[$q] }
                }
            }
        }
        $tirature = @($tirature | Sort-Object anno, @{ Expression = { $qualityOrder.IndexOf($_.quality) } })

        # zecca per anno: solo per gli anni con una tiratura (sono quelli della tabella "by year"),
        # una voce per anno con le zecche certe (zecche_anno) e/o probabili (zecche_anno_probabili);
        # senza voce = zecca non nota. Campi vuoti omessi (il default dell'app è la lista vuota).
        $zecchePerAnno = @()
        $seenYears = @{}
        $tirYears = @{}
        foreach ($t in $tirature) { $tirYears[[int]$t.anno] = $true }
        foreach ($m in $mine) {
            foreach ($row in $m.Type.tirature_per_anno) {
                $anno = [int]$row.anno
                if (-not $tirYears.ContainsKey($anno) -or $seenYears.ContainsKey($anno)) { continue }
                $cert = @($row.zecche_anno | Where-Object { $_ } | Select-Object -Unique)
                $prob = @($row.zecche_anno_probabili | Where-Object { $_ } | Select-Object -Unique)
                if ($cert.Count -eq 0 -and $prob.Count -eq 0) { continue }
                $seenYears[$anno] = $true
                $entry = [ordered]@{ anno = $anno }
                if ($cert.Count -gt 0) { $entry['zecche'] = $cert }
                if ($prob.Count -gt 0) { $entry['probabili'] = $prob }
                $split = Get-MintSplit $row
                if ($split.Count -gt 0) { $entry['per_zecca'] = $split }
                $zecchePerAnno += [pscustomobject]$entry
            }
        }
        $zecchePerAnno = @($zecchePerAnno | Sort-Object anno)

        # type principale: più anni nella serie, a parità il più recente
        $main = $null
        if ($mine.Count -gt 0) {
            $main = ($mine | Sort-Object @{ Expression = 'Overlap'; Descending = $true }, @{ Expression = { $_.Type.anno_inizio }; Descending = $true } | Select-Object -First 1).Type
        }

        $descrizione = $null; $descFonte = $null; $fonteUrl = $null
        if ($main) {
            $descrizione = Clean $main.descrizione_retro
            if ($descrizione) { $descFonte = 'numista'; $fonteUrl = [string]$main.numista_url }
        }
        if (-not $descrizione -and ($NoEcbFallbackSources -notcontains $s.fonte_dati)) {
            $e = $ecb[$key]
            if ($e -and (Clean $e.descrizione_taglio)) {
                $descrizione = Clean $e.descrizione_taglio; $descFonte = 'ecb'; $fonteUrl = [string]$e.url_fonte
            }
        }

        $extra = [ordered]@{
            descrizione      = $descrizione
            descrizione_fonte = $descFonte
            fonte_url        = $fonteUrl
            numista_id       = if ($main) { [int]$main.numista_id } else { $null }
            zecca_fisica_raw = if ($main) { Clean-Mints $main.zecca_fisica_raw } else { $null }
            incisore_raw     = if ($main) { Clean $main.incisore_retro_raw } else { $null }
            disegnatore_raw  = if ($main) { Clean $main.disegnatore_retro_raw } else { $null }
            tirature         = $tirature
            zecche_per_anno  = $zecchePerAnno
        }
        foreach ($name in $extra.Keys) {
            $img | Add-Member -NotePropertyName $name -NotePropertyValue $extra[$name] -Force
        }

        $report += [pscustomobject]@{
            Serie = "$($s.paese) #$($s.ordine_cronologico)"; Taglio = $img.taglio
            Types = ($mine | ForEach-Object { "N#$($_.Type.numista_id)" }) -join ' '
            Righe = $tirature.Count; Desc = $descFonte
            ZecchePerAnno = $zecchePerAnno.Count; Zecca = [bool]$extra.zecca_fisica_raw; Inc = [bool]$extra.incisore_raw; Dis = [bool]$extra.disegnatore_raw
        }
    }
}

$json = ConvertTo-Json -InputObject @($series) -Compress -Depth 8
[System.IO.File]::WriteAllText($Output, $json, [System.Text.UTF8Encoding]::new($false))

Write-Host "Scritto $Output ($([Math]::Round($json.Length / 1KB)) KB, $($series.Count) serie, $($report.Count) tagli)"
Write-Host ("Tagli con tirature: {0}; descrizione Numista: {1}; descrizione BCE: {2}; senza descrizione: {3}" -f
    @($report | Where-Object { $_.Righe -gt 0 }).Count,
    @($report | Where-Object { $_.Desc -eq 'numista' }).Count,
    @($report | Where-Object { $_.Desc -eq 'ecb' }).Count,
    @($report | Where-Object { -not $_.Desc }).Count)
if ($unassigned.Count -gt 0) { Write-Host "Type Numista non abbinati a nessuna immagine (attesi: serie senza foto):"; $unassigned | ForEach-Object { Write-Host "  $_" } }
$noNumista = @($report | Where-Object { -not $_.Types })
if (-not $ExcludeNumista -and $noNumista.Count -gt 0) {
    Write-Host "Tagli senza type Numista (ripiego BCE o nulla):"
    $noNumista | ForEach-Object { Write-Host ("  {0} {1} -> {2}" -f $_.Serie, $_.Taglio, $(if ($_.Desc) { $_.Desc } else { 'nessun testo' })) }
}
