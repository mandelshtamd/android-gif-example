plugins {
  id("com.android.application")
  kotlin("android")
  kotlin("kapt")
  id("dagger.hilt.android.plugin")
  id("org.jlleitschuh.gradle.ktlint")
  id("com.jaredsburrows.license")
  id("com.getkeepsafe.dexcount") version "3.0.1"
}

android {
  compileSdk = deps.build.compileSdk

  defaultConfig {
    applicationId = "com.burrowsapps.example.gif"
    versionCode = 1
    versionName = "1.0"
    minSdk = deps.build.minSdk
    targetSdk = deps.build.targetSdk

    testApplicationId = "burrows.apps.example.gif.test"
    testInstrumentationRunner = "test.CustomTestRunner" // "androidx.test.runner.AndroidJUnitRunner"
    testInstrumentationRunnerArguments += mapOf(
      "disableAnalytics" to "true",
      "clearPackageData" to "true"
    )

    resourceConfigurations += setOf("en")
    vectorDrawables.useSupportLibrary = true
  }

  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = deps.versions.java
    targetCompatibility = deps.versions.java
  }

  buildFeatures {
    viewBinding = true
  }

  sourceSets {
    val commonTestSources = "src/commonTest/java"
    val commonTestResources = "src/commonTest/resources"

    getByName("test").apply {
      java.srcDirs(commonTestSources)
      resources.srcDirs(commonTestResources)
    }

    getByName("androidTest").apply {
      java.srcDirs(commonTestSources)
      resources.srcDirs(commonTestResources)
    }
  }

  lint {
    textReport = true
    textOutput("stdout")
    isCheckAllWarnings = true
    isWarningsAsErrors = true
    lintConfig = file("${project.rootDir}/config/lint/lint.xml")
    isCheckReleaseBuilds = false
    isCheckTestSources = true
    isAbortOnError = false
  }

  signingConfigs {
    getByName("debug") {
      storeFile = file("${project.rootDir}/config/signing/debug.keystore")
      storePassword = deps.build.signing.pass
      keyAlias = deps.build.signing.alias
      keyPassword = deps.build.signing.pass
    }
  }

  buildTypes {
    getByName("debug") {
      applicationIdSuffix = ".debug"
      versionNameSuffix = "-dev"

      buildConfigField(
        "String",
        "BASE_URL",
        if (rootProject.extra["ci"] as Boolean) {
          "\"http://localhost:8080\""
        } else {
          "\"https://g.tenor.com\""
        }
      )
    }

    // Apply fake signing config to release to test "assembleRelease" locally
    getByName("release") {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles += listOf(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        file("${project.rootDir}/config/proguard/proguard-rules.txt")
      )
      signingConfig = signingConfigs.getByName("debug")

      buildConfigField("String", "BASE_URL", "\"https://g.tenor.com\"")
    }
  }

  testOptions {
    animationsDisabled = true
    unitTests.apply {
      isReturnDefaultValues = true
      isIncludeAndroidResources = true
    }
    execution = "ANDROIDX_TEST_ORCHESTRATOR"
  }

  packagingOptions {
    resources.excludes += listOf(
      "**/*.kotlin_module",
      "**/*.version",
      "**/kotlin/**",
      "**/*.txt",
      "**/*.xml",
      "**/*.properties",
    )
  }

  dependenciesInfo {
    includeInApk = false
    includeInBundle = false
  }
}

kapt {
  correctErrorTypes = true
  mapDiagnosticLocations = true

  arguments {
    arg("dagger.formatGeneratedSource", "disabled")
    arg("dagger.fastInit", "enabled")
    arg("dagger.experimentalDaggerErrorMessages", "enabled")
    arg("moshi.generated", "javax.annotation.Generated")
  }
}

licenseReport {
  generateCsvReport = false // TODO default should be false
  generateHtmlReport = true
  generateJsonReport = false // TODO default should be false
}

dependencies {
  // Java 8+
  coreLibraryDesugaring(deps.android.desugarJdkLibs)

  // Debug only
  debugImplementation(deps.squareup.leakcanary)

  // Kotlin
  implementation(platform(kotlin("bom", deps.versions.kotlin)))
  implementation(kotlin("stdlib"))
  implementation(kotlin("stdlib-common"))
  implementation(kotlin("stdlib-jdk7"))
  implementation(kotlin("stdlib-jdk8"))
  testImplementation(kotlin("test"))
  testImplementation(kotlin("test-junit"))
  testImplementation(kotlin("test-common"))

  // KotlinX
  implementation(platform(deps.kotlin.coroutines.bom))
  implementation(deps.kotlin.coroutines.android)
  implementation(deps.kotlin.coroutines.core)
  implementation(deps.kotlin.coroutines.corejvm)
  implementation(deps.kotlin.coroutines.corejdk8)
  testImplementation(deps.kotlin.coroutines.test)
  testImplementation(deps.kotlin.coroutines.testjvm)
  androidTestImplementation(deps.kotlin.coroutines.test)
  androidTestImplementation(deps.kotlin.coroutines.testjvm)

  // Dagger / Dependency Injection
  implementation(deps.google.dagger.dagger)
  kapt(deps.google.dagger.compiler)
  kaptTest(deps.google.dagger.compiler)
  kaptAndroidTest(deps.google.dagger.compiler)
  compileOnly(deps.misc.javaxInject)
  compileOnly(deps.misc.jsr250)
  compileOnly(deps.misc.jsr305)
  testImplementation(deps.google.dagger.testing)
  androidTestImplementation(deps.google.dagger.testing)

  // AndroidX
  implementation(deps.android.annotation)
  implementation(deps.android.activity)
  implementation(deps.android.activityktx)
  implementation(deps.android.appcompat)
  implementation(deps.android.core)
  implementation(deps.android.corektx)

  // AndroidX UI
  implementation(deps.android.cardview)
  implementation(deps.android.constraintlayout)
  implementation(deps.android.recyclerview)
  implementation(deps.google.material)

  // AndroidX Lifecycle
  implementation(deps.android.common)
  implementation(deps.android.commonjdk8)
  implementation(deps.android.extensions)
  implementation(deps.android.viewmodel)
  implementation(deps.android.viewmodelktx)
  implementation(deps.android.livedata)
  implementation(deps.android.livedatacore)
  implementation(deps.android.livedatacorektx)
  implementation(deps.android.livedataktx)
  kapt(deps.android.compiler)

  // Image Loading
  implementation(deps.glide.glide)
  implementation(deps.glide.integration)
  kapt(deps.glide.compiler)

  // OkIO
  implementation(deps.squareup.okio)

  // OkHTTP
  implementation(platform(deps.squareup.okhttp.bom))
  implementation(deps.squareup.okhttp.okhttp)
  implementation(deps.squareup.okhttp.interceptor)
  testImplementation(deps.squareup.okhttp.mockwebserver)
  androidTestImplementation(deps.squareup.okhttp.mockwebserver)

  // Retrofit
  implementation(deps.squareup.moshi.moshi)
  implementation(deps.squareup.moshi.adapters)
  implementation(deps.squareup.retrofit.moshi)
  implementation(deps.squareup.retrofit.retrofit)
  kapt(deps.squareup.moshi.compiler)

  testImplementation(deps.android.test.annotation)
  testImplementation(deps.android.test.core)
  testImplementation(deps.android.test.ext.junit)
  testImplementation(deps.google.truth)
  testImplementation(deps.junit)
  testImplementation(deps.mockito.inline)
  testImplementation(deps.mockito.kotlin)
  testImplementation(deps.reflections)
  testImplementation(deps.robolectric.robolectric)

  androidTestUtil(deps.android.test.orchestrator)

  debugImplementation(deps.android.test.core) // See https://stackoverflow.com/a/69476166/950427
  androidTestImplementation(deps.android.test.annotation)
  androidTestImplementation(deps.android.test.core)
  androidTestImplementation(deps.android.test.espresso.contrib) {
    exclude("org.checkerframework") // See https://github.com/android/android-test/issues/861#issuecomment-872582819
  }
  androidTestImplementation(deps.android.test.espresso.core)
  androidTestImplementation(deps.android.test.espresso.intents)
  androidTestImplementation(deps.android.test.ext.junit)
  androidTestImplementation(deps.android.test.runner)
  androidTestImplementation(deps.android.test.rules)
  androidTestImplementation(deps.google.truth)
  androidTestImplementation(deps.junit)
  androidTestImplementation(deps.robolectric.annotations)
}
