plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val googleServicesFile = layout.projectDirectory.file("google-services.json").asFile

if (googleServicesFile.exists()) {
    apply(plugin = "com.google.gms.google-services")
} else {
    logger.warn("Falta app/google-services.json. El build fallara con una explicacion clara antes de compilar Android.")
}

android {
    namespace = "com.pedilo.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pedilo.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

tasks.register("checkFirebaseConfig") {
    group = "verification"
    description = "Verifica que exista app/google-services.json antes de compilar Android."
    doLast {
        if (!googleServicesFile.exists()) {
            throw GradleException(
                "Falta app/google-services.json. Crea el proyecto Firebase, registra la app Android com.pedilo.app, descarga google-services.json y colocalo en app/google-services.json."
            )
        }
    }
}

tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn("checkFirebaseConfig")
}
