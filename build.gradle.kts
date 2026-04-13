plugins {
    id("org.jetbrains.intellij.platform") version "2.10.4"
    kotlin("jvm") version "2.2.0"
}

group = "com.ontalent.ftcsnippets"
version = "1.3.1"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "243"
            untilBuild = "253.*"
        }
    }

    buildSearchableOptions = false
    //instrumentCode = project.hasProperty("productionBuild")
}

dependencies {
    intellijPlatform {
        intellijIdea("2025.3.4")  // Unified — replaces intellijIdeaCommunity()
        bundledPlugin("com.intellij.java")

        pluginVerifier()
        zipSigner()
        instrumentationTools()
    }
}

tasks {
    compileKotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    compileTestKotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    runIde {
        jvmArgs = listOf("-Xmx1024m", "-XX:ReservedCodeCacheSize=512m")
    }
}