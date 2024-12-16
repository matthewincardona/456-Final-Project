import com.android.build.api.dsl.Packaging

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.finalproject"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.finalproject"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
        kotlinOptions {
            jvmTarget = "1.8"
        }
        buildFeatures {
            compose = true
        }
        composeOptions {
            kotlinCompilerExtensionVersion = "1.5.1"
        }
        packaging {
            resources {
//                resources.pickFirsts.add("META-INF/AL2.0")
                resources.excludes.add("META-INF/*")
                resources.excludes.add("META-INF/DEPENDENCIES")
                resources.excludes.add("META-INF/LICENSE")
                resources.excludes.add("META-INF/LICENSE.txt")
                resources.excludes.add("META-INF/license.txt")
                resources.excludes.add("META-INF/NOTICE")
                resources.excludes.add("META-INF/NOTICE.txt")
                resources.excludes.add("META-INF/notice.txt")
                resources.excludes.add("META-INF/ASL2.0")
                resources.excludes.add("META-INF/*.kotlin_module")
            }
        }
    }

    dependencies {
        implementation("com.google.code.gson:gson:2.11.0")
        implementation(libs.play.services.auth)
        implementation("com.google.api-client:google-api-client:2.7.1")
        implementation("com.google.oauth-client:google-oauth-client-jetty:1.36.0")
        implementation (libs.play.services.auth.v2101)
        implementation("com.google.api-client:google-api-client-android:2.7.1")
        implementation("com.google.apis:google-api-services-calendar:v3-rev20241101-2.0.0")
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.activity.compose)
        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.ui)
        implementation(libs.androidx.ui.graphics)
        implementation(libs.androidx.ui.tooling.preview)
        implementation(libs.androidx.material3)
        implementation(libs.androidx.ui.test.android)
        implementation(libs.androidx.navigation.compose)
        implementation(libs.play.services.auth)
        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)
        androidTestImplementation(platform(libs.androidx.compose.bom))
        androidTestImplementation(libs.androidx.ui.test.junit4)
        debugImplementation(libs.androidx.ui.tooling)
        debugImplementation(libs.androidx.ui.test.manifest)
    }
}
dependencies {
    implementation(libs.androidx.room.ktx)
}
