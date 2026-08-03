plugins {
    id("org.jetbrains.intellij.platform") version "2.10.4"
    // Must be >= the Kotlin the target platform bundles: IDEA 2026.1 ships metadata 2.4.0,
    // which a 2.2.0 compiler cannot read off the bundled Kotlin plugin's jars.
    kotlin("jvm") version "2.4.0"
}

group = "com.ontalent.ftcsnippets"
version = "1.5.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "243"
            // No until-build: JetBrains advises against it for 2024.3+, and pinning one is
            // what forced a re-release every time Android Studio moved. Nothing here uses
            // an API that a newer platform is likely to drop.
            untilBuild = provider { null }
        }
    }

    // `./gradlew verifyPlugin` needs at least one IDE to check against, otherwise it
    // fails outright. CONTRIBUTING tells contributors to run it, so give it a target.
    pluginVerification {
        ides {
            recommended()
        }
    }

    buildSearchableOptions = false
    //instrumentCode = project.hasProperty("productionBuild")
}

dependencies {
    intellijPlatform {
        // Platform 261, the base of Android Studio Quail (2026.1.x).
        intellijIdea("2026.1.3")  // Unified — replaces intellijIdeaCommunity()
        bundledPlugin("com.intellij.java")

        // Compile-time only: plugin.xml declares Kotlin as an *optional* dependency so
        // Java-only users are never forced to install the Kotlin plugin.
        bundledPlugin("org.jetbrains.kotlin")

        pluginVerifier()
        zipSigner()
    }

    testImplementation("junit:junit:4.13.2")
}

tasks {
    compileKotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    compileTestKotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    runIde {
        jvmArgs = listOf("-Xmx1024m", "-XX:ReservedCodeCacheSize=512m")
    }
}