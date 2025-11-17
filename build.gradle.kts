plugins {
    id("org.jetbrains.intellij") version "1.17.4"
    kotlin("jvm") version "2.0.0"
}

group = "com.ontalent.ftcsnippets"
version = "1.2.0"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

intellij {
    version.set("2024.3.7")
    type.set("IC") // IntelliJ Community Edition SDK
    plugins.set(listOf("java")) // ✅ gives access to PSI & inspections
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }

    compileTestKotlin {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("243") // ✅ matches 2024.3.*
        untilBuild.set("252.*")
    }

    named<org.jetbrains.intellij.tasks.RunIdeTask>("runIde") {
        jvmArgs = listOf("-Xmx1024m", "-XX:ReservedCodeCacheSize=512m")
    }

    named<org.jetbrains.intellij.tasks.BuildSearchableOptionsTask>("buildSearchableOptions") {
        enabled = false
    }
}