# Regole R8 della build di release (vedi CLAUDE.md § Rilascio). Le librerie che usano la
# riflessione (Room, Billing, Ads, UMP, Credential Manager, Coil, OkHttp) portano già le loro
# regole "consumer" dentro l'AAR: qui c'è solo ciò che è dell'app o che conviene rendere esplicito.
# Il rischio vero di R8 è che rompa IN SILENZIO il parsing JSON o Room: dopo ogni modifica di
# queste regole si prova la release (avvio, seeding di coins.json e regular_issues.json,
# elenchi, dettaglio, collezione, backup).

# --- Informazioni per leggere gli stack trace di Play Console (il mapping.txt viaggia con l'AAB) ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- kotlinx.serialization -------------------------------------------------------------------
# Le classi @Serializable dell'app (CoinJson, RegularIssueJson, BackupFile e gli annidati, i blob
# JSON salvati in Room come RegularIssueImage/RegularIssueMintage/RegularIssueYearMint, CoinQuality)
# trovano il proprio serializzatore con `Companion.serializer()` e la classe `$serializer`: se R8 le
# toglie o le rinomina il parsing fallisce a runtime, non in compilazione.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Companion e serializer() di ogni classe @Serializable dell'app
-keepclassmembers @kotlinx.serialization.Serializable class com.michele.eurocoins.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.michele.eurocoins.**$$serializer { *; }
-keepclassmembers class com.michele.eurocoins.**$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# Gli enum serializzati o salvati per NOME (CoinQuality nel JSON del backup e nei blob Room,
# ThemeMode/BrowseMode/RegularBrowseMode nelle SharedPreferences) devono tenere i nomi delle costanti.
-keepclassmembers enum com.michele.eurocoins.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    <fields>;
}

# --- Room -------------------------------------------------------------------------------------
# CoinDatabase_Impl e i DAO generati si caricano per nome (riflessione di Room); le entity e i
# TypeConverter sono chiamati dal codice generato, ma si tengono per non dipendere dalle regole
# della libreria in caso di aggiornamento.
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.TypeConverter <methods>;
}
-dontwarn androidx.room.paging.**

# --- Avvisi attesi, non errori ----------------------------------------------------------------
# OkHttp (usato da Coil) cita provider TLS opzionali che su Android non ci sono.
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
