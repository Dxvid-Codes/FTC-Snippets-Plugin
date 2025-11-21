//build.gradle.kts

plugins {
    id("org.jetbrains.intellij") version "1.17.4"
    kotlin("jvm") version "2.0.0"
}

group = "com.ontalent.ftcsnippets"
version = "1.2.0"  // Bump version for Otter compatibility

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

intellij {
    version.set("2024.3.1")  // Updated for Otter
    type.set("IC")
    plugins.set(listOf("java"))

    // Speed optimizations
    downloadSources.set(project.hasProperty("downloadSources"))
    instrumentCode.set(project.hasProperty("productionBuild"))
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }

    compileTestKotlin {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("243")
        untilBuild.set("252.*")  // Covers Otter releases
    }

    named<org.jetbrains.intellij.tasks.RunIdeTask>("runIde") {
        jvmArgs = listOf("-Xmx1024m", "-XX:ReservedCodeCacheSize=512m")
    }

    named<org.jetbrains.intellij.tasks.BuildSearchableOptionsTask>("buildSearchableOptions") {
        enabled = false
    }
}