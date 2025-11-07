import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.app1"
    compileSdk = 35

    // 🔧 --- Controle automático de versão ---
    val versionPropsFile = file("version.properties")
    val versionProps = Properties()

    if (versionPropsFile.exists()) {
        versionProps.load(FileInputStream(versionPropsFile))
    }

    // Lê o código atual (ou inicia em 100)
    val currentVersionCode = versionProps.getProperty("VERSION_CODE", "100").toInt()
    val newVersionCode = currentVersionCode + 1

    // Atualiza o arquivo version.properties
    versionProps.setProperty("VERSION_CODE", newVersionCode.toString())
    versionProps.store(versionPropsFile.writer(), null)

    // Monta o nome da versão (1.0.xxx)
    val versionNameStr = "1.0.$newVersionCode"
    // 🔧 --- Fim do controle automático ---

    defaultConfig {
        applicationId = "com.example.app1"
        minSdk = 24
        targetSdk = 35

        versionCode = newVersionCode
        versionName = versionNameStr

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Nome personalizado do APK
    applicationVariants.all {
        val variant = this
        outputs.all {
            val output = this
            if (output is com.android.build.gradle.internal.api.ApkVariantOutputImpl) {
                output.outputFileName =
                    "JatoFinancas-${variant.buildType.name}-v${variant.versionName}.apk"
            }
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.github.bumptech.glide:glide:4.13.0")
    implementation("com.github.dewinjm:monthyear-picker:1.0.2")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.google.android.material:material:1.14.0-alpha05")
    implementation("com.github.skydoves:colorpickerview:2.3.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}