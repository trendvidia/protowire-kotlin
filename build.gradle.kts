// SPDX-License-Identifier: MIT
// Copyright (c) 2026 TrendVidia, LLC.

import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
    // vanniktech 0.36.0 requires Kotlin plugin >= 2.2.0; pin to a
    // current stable release. Drop-in compatible with the existing
    // sources (no language-mode 2 features used).
    kotlin("jvm") version "2.3.21"
    `java-library`
    id("com.vanniktech.maven.publish") version "0.36.0"
}

group = "org.protowire"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Java port codecs — composite-build-resolved via includeBuild in
    // settings.gradle.kts. Pulls in protobuf-java transitively; consumers
    // who want the lite tier should depend on the *-android Java modules
    // directly (a future :*-kotlin-android variant could mirror this
    // module's structure when there's demand).
    api("org.protowire:pxf:0.70.0")
    api("org.protowire:pb:0.70.0")
    api("org.protowire:sbe:0.70.0")
    api("org.protowire:envelope:0.70.0")

    // Coroutines for the suspend wrappers + Flow extensions.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")

    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// --- Maven Central publishing ---------------------------------------------
mavenPublishing {
    // Central Portal is the only supported Sonatype host since
    // vanniktech 0.34.0 removed `SonatypeHost` (OSSRH was retired
    // upstream), so no host argument is passed any more.
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates("org.protowire", "protowire-kotlin", project.version.toString())

    configure(
        JavaLibrary(
            javadocJar = JavadocJar.Javadoc(),
            sourcesJar = SourcesJar.Sources(),
        ),
    )

    pom {
        name.set("protowire-kotlin")
        description.set(
            "Kotlin extensions companion for protowire-java — suspend wrappers, " +
                "DSL builders, and Flow extensions over the JVM codec family."
        )
        url.set("https://protowire.org")
        inceptionYear.set("2026")

        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("trendvidia")
                name.set("TrendVidia, LLC.")
                url.set("https://trendvidia.com")
            }
        }

        scm {
            url.set("https://github.com/trendvidia/protowire-kotlin")
            connection.set("scm:git:https://github.com/trendvidia/protowire-kotlin.git")
            developerConnection.set("scm:git:ssh://git@github.com/trendvidia/protowire-kotlin.git")
        }

        issueManagement {
            system.set("GitHub")
            url.set("https://github.com/trendvidia/protowire-kotlin/issues")
        }
    }
}
