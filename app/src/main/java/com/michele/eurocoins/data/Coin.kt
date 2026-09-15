package com.michele.eurocoins.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Una moneta da 2€ commemorativa, così come vive nel database locale.
 *
 * `tema` e `noteStoriche` sono oggi un unico blob di testo nella lingua della
 * fonte (inglese, per la BCE — vedi CLAUDE.md § Lingua). Non dare per
 * scontato altrove nel codice che sia l'unica rappresentazione testuale
 * possibile: un'eventuale fonte futura in italiano arriverà come dato
 * aggiuntivo dalla pipeline (nuovi record/campi), non come traduzione
 * automatica che sovrascrive questi campi in-place.
 */
@Entity(tableName = "coins")
data class Coin(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val paese: String,
    val anno: Int,
    val tema: String,
    val tiratura: Int?,
    val zeccaEmittente: String,
    val zeccaRaw: String,
    val zeccaFisicaRaw: String?,
    val noteStoriche: String?,
    val urlImmagineFonte: String?,
    val licenzaImmagine: String,
    val attribuzioneImmagineRaw: String?,
    val immaginePlaceholder: Boolean,
    val fonteDati: String,
)
