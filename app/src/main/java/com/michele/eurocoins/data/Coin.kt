package com.michele.eurocoins.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Una moneta da 2€ commemorativa, così come vive nel database locale. */
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
