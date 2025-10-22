plugins {
    id("org.jetbrains.intellij") version "1.17.3"
    kotlin("jvm") version "2.0.0"
}

group = "com.ontalent.ftcsnippets"
version = "1.0.2"

repositories {
    mavenCentral()
}

// Set Java compatibility
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

intellij {
    version.set("2024.3.7") // or match your IDE
    type.set("IC") // "IC" = IntelliJ Community, use "AI" for Android Studio plugin
    updateSinceUntilBuild.set(false)
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }

    compileTestKotlin {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("232")
        untilBuild.set("252.*")
        changeNotes.set(
            """
            Initial release of FTC Snippets plugin.
            Features:
            - Insert motor imports
            - Insert servo imports  
            - Insert sensor imports
            - Insert IMU imports
            - Insert vision imports
            - Insert all FTC imports at once
            """.trimIndent()
        )
    }

    // ✅ Correct way to configure runIde task:
    named<org.jetbrains.intellij.tasks.RunIdeTask>("runIde") {
        jvmArgs = listOf("-Xmx1024m", "-XX:ReservedCodeCacheSize=512m")
    }

    // ✅ Correct way to configure buildSearchableOptions task:
    named<org.jetbrains.intellij.tasks.BuildSearchableOptionsTask>("buildSearchableOptions") {
        enabled = false // Speeds up build
    }
}