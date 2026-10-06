import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// local.properties (sdk.dir, google.webClientId, admob.*) e keystore.properties (firma di release)
// non sono versionati: ognuno ha i propri. Vedi CLAUDE.md § Rilascio.
fun loadProperties(name: String): Properties = Properties().apply {
    rootProject.file(name).takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

val localProps = loadProperties("local.properties")
val keystoreProps = loadProperties("keystore.properties")

// ID di test pubblici di AdMob (documentati da Google): le build di debug li usano SEMPRE, anche
// se local.properties ha quelli veri, per non generare mai impressioni o clic reali durante lo
// sviluppo (rischio di sospensione dell'account AdMob). La release usa quelli veri se presenti.
val testAdmobAppId = "ca-app-pub-3940256099942544~3347511713"
val testAdmobBannerUnitId = "ca-app-pub-3940256099942544/9214589741"
val testAdmobInterstitialUnitId = "ca-app-pub-3940256099942544/1033173712"
val realAdmobAppId = localProps.getProperty("admob.appId").orEmpty()
val realAdmobBannerUnitId = localProps.getProperty("admob.bannerUnitId").orEmpty()
val realAdmobInterstitialUnitId = localProps.getProperty("admob.interstitialUnitId").orEmpty()
val hasRealAdmobIds = realAdmobAppId.isNotBlank() && realAdmobBannerUnitId.isNotBlank() && realAdmobInterstitialUnitId.isNotBlank()

android {
    namespace = "com.michele.eurocoins"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.michele.eurocoins"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ID client OAuth "Web application" della Google Cloud Console, letto da
        // local.properties (non versionato): google.webClientId=xxxx.apps.googleusercontent.com
        // Vuoto = il login Google resta disabilitato e la UI lo segnala.
        val webClientId = localProps.getProperty("google.webClientId").orEmpty()
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$webClientId\"")

        // Prodotto in-app del Pro (acquisto una tantum, non consumabile): l'ID va creato uguale in Play Console.
        buildConfigField("String", "PRO_PRODUCT_ID", "\"euro_coins_pro\"")
    }

    // Firma di release con la chiave di UPLOAD letta da keystore.properties (storeFile, storePassword,
    // keyAlias, keyPassword). Senza il file la release resta non firmata: per le prove sul telefono si
    // firma a mano con la chiave di debug (CLAUDE.md § Rilascio).
    signingConfigs {
        if (keystoreProps.getProperty("storeFile") != null) {
            create("upload") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"$testAdmobBannerUnitId\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_UNIT_ID", "\"$testAdmobInterstitialUnitId\"")
            manifestPlaceholders["admobAppId"] = testAdmobAppId
        }
        release {
            // R8: riduzione del codice, delle risorse e offuscamento. Le regole per kotlinx.serialization,
            // Room e le altre librerie sono in proguard-rules.pro.
            optimization {
                enable = true
            }
            proguardFiles("proguard-rules.pro")
            signingConfigs.findByName("upload")?.let { signingConfig = it }
            buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"${realAdmobBannerUnitId.ifBlank { testAdmobBannerUnitId }}\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_UNIT_ID", "\"${realAdmobInterstitialUnitId.ifBlank { testAdmobInterstitialUnitId }}\"")
            manifestPlaceholders["admobAppId"] = realAdmobAppId.ifBlank { testAdmobAppId }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.kotlinx.serialization.json)

    // Backdrop blur ("vetro") della barra di ricerca flottante
    implementation(libs.haze)

    // Login Google (Credential Manager) + autorizzazione Drive per il backup
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.id)
    implementation(libs.play.services.auth)

    // Pro (rimozione pubblicità): acquisto in-app, banner AdMob e consenso GDPR (UMP)
    implementation(libs.billing.ktx)
    implementation(libs.play.services.ads)
    implementation(libs.ump)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// Il pacchetto per Play non deve partire con gli ID di test di AdMob: le pubblicita' di prova
// non pagano e l'app verrebbe pubblicata cosi'. Per costruire un .aab di prova: -PallowTestAds.
abstract class CheckAdmobIds : DefaultTask() {
    @get:Input abstract val hasRealIds: Property<Boolean>

    @get:Input abstract val allowTestAds: Property<Boolean>

    @TaskAction
    fun check() {
        if (!hasRealIds.get() && !allowTestAds.get()) {
            throw GradleException(
                "bundleRelease senza ID AdMob veri: metti admob.appId, admob.bannerUnitId e admob.interstitialUnitId " +
                    "in local.properties (oppure passa -PallowTestAds per un pacchetto di prova con le pubblicita' di test).",
            )
        }
    }
}

val checkAdmobIds = tasks.register<CheckAdmobIds>("checkAdmobIds") {
    hasRealIds.set(hasRealAdmobIds)
    allowTestAds.set(providers.gradleProperty("allowTestAds").isPresent)
}
tasks.matching { it.name == "bundleRelease" }.configureEach { dependsOn(checkAdmobIds) }
